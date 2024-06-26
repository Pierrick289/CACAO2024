package abstraction.eq9Distributeur2;

import java.awt.Color;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import abstraction.eqXRomu.contratsCadres.Echeancier;
import abstraction.eqXRomu.contratsCadres.ExemplaireContratCadre;
import abstraction.eqXRomu.contratsCadres.IAcheteurContratCadre;
import abstraction.eqXRomu.contratsCadres.IVendeurContratCadre;
import abstraction.eqXRomu.contratsCadres.SuperviseurVentesContratCadre;
import abstraction.eqXRomu.filiere.Filiere;
import abstraction.eqXRomu.filiere.IActeur;
import abstraction.eqXRomu.general.Journal;
import abstraction.eqXRomu.general.Variable;
import abstraction.eqXRomu.produits.ChocolatDeMarque;
import abstraction.eqXRomu.produits.Feve;
import abstraction.eqXRomu.produits.IProduit;
import abstraction.eqXRomu.filiere.Banque;


////////////// Codé par Maureen Leprince ///////////////////////

// qd on met l'équipe 5 en faillite, on n'a plus de pb de vente bcp trop importante 

public abstract class Distributeur2ContratCadre extends Distributeur2Vente implements IAcheteurContratCadre{
	private SuperviseurVentesContratCadre supCC;
	private List<ExemplaireContratCadre> contratsEnCours;
	private List<ExemplaireContratCadre> contratsTermines;
	protected Journal journal_CC;
	protected Journal journal_negoc;
	private double totalCoutAPayer; //inutilisé dans le code
	//private Echeancier propPrecedente;
	private double coeffDecision;
	
	public Distributeur2ContratCadre() {
		super();
		this.contratsEnCours=new LinkedList<ExemplaireContratCadre>();
		this.contratsTermines=new LinkedList<ExemplaireContratCadre>(); 
		this.journal_CC= new Journal(this.getNom()+" journal Contrat Cadre", this);
		this.journal_negoc = new Journal(this.getNom()+" journal des Négociations", this);
		this.totalCoutAPayer = 0;
		this.coeffDecision=1.5;
	}
	
	public void initialiser() {
		super.initialiser();
		this.supCC = (SuperviseurVentesContratCadre)(Filiere.LA_FILIERE.getActeur("Sup.CCadre"));
	}

	
	public void next() {
		this.journal_CC.ajouter("=== STEP "+Filiere.LA_FILIERE.getEtape()+" ====================");
		this.journal_negoc.ajouter("=== STEP "+Filiere.LA_FILIERE.getEtape()+" ====================");
		super.next();
		this.payerCoutsAcheminement();
		this.lancerNouveauCC();		
	}
	
	public void payerCoutsAcheminement() {
		for (int i=0; i<contratsEnCours.size(); i++  ) {
			double paiement = contratsEnCours.get(i).getPaiementAEffectuerAuStep();
			this.totalCoutAPayer -= paiement;
			if (paiement>0) {
				Filiere.LA_FILIERE.getBanque().payerCout(this, cryptogramme, "Acheminement", this.coutDacheminement(paiement));
			}
		}
	}
	
	public double restantDu(ChocolatDeMarque cm) {
		double res=0;
		for (ExemplaireContratCadre c : this.contratsEnCours) {
			if (c.getProduit().equals(cm)) {
				res+=c.getEcheancier().getQuantite(Filiere.LA_FILIERE.getEtape()+1);
			}
		}
		return res;
	}
	
	
	@Override
	public boolean achete(IProduit produit) { //regarder attractivité peut etre
		if (produit.getType().equals("ChocolatDeMarque")) {
			ChocolatDeMarque cm = (ChocolatDeMarque) produit;
			if (this.stockChocoMarque.get(cm)!=null) {
				return this.stockChocoMarque.get(cm)+this.restantDu(cm)<coeffDecision*this.getVentePrecedente(cm); ///A MODIFIER
			} else {
				return this.getTotalStock(cryptogramme)<100000;
			}
		} else {
			return false;
		}
	}

	@Override
	public Echeancier contrePropositionDeLAcheteur(ExemplaireContratCadre contrat) {
		if (!contrat.getProduit().getType().equals("ChocolatDeMarque")) {
			this.journal_negoc.ajouter("Négociation sur qté a échoué car pas du Chocolat de Marque");
			return null;
		}
		Echeancier e = contrat.getEcheancier();
		LinkedList<Double> quantites = new LinkedList<Double>();
		boolean modif = false;
		ChocolatDeMarque cm = (ChocolatDeMarque) contrat.getProduit();
		
		if (Filiere.LA_FILIERE.getChocolatsProduits().contains(cm) && coeffDecision*this.getVentePrecedente(cm)<this.stockChocoMarque.get(cm)+this.restantDu(cm)) {
			this.journal_negoc.ajouter("Négociation qur qté a échoué car quantités en stock et à venir suffisantes par rapport à la demande ces clients, contrat pas intéressant");
			return null;
		}
		
		for (int i=0; i<e.getNbEcheances(); i++) {
			if (Filiere.LA_FILIERE.getEtape()==0) {
				if (e.getQuantite(e.getStepDebut()+i)>5000)  {
					quantites.add(5000.);
					modif=true;
				} else if (Filiere.LA_FILIERE.getChocolatsProduits().contains(cm) && e.getQuantite(e.getStepDebut()+i)>coeffDecision*this.getVentePrecedente(cm)-this.stockChocoMarque.get(cm)-this.restantDu(cm)) {
					quantites.add(Math.max(coeffDecision*this.getVentePrecedente(cm) -this.stockChocoMarque.get(cm) - this.restantDu(cm), 200.));
					modif = true;
				} else {
					quantites.add(Math.max(e.getQuantite(e.getStepDebut()+i),200.));
				}
			} else {
				if (Filiere.LA_FILIERE.getChocolatsProduits().contains(cm) && (e.getQuantite(e.getStepDebut()+i)>coeffDecision*this.getVentePrecedente(cm)-(this.stockChocoMarque.get(cm)+this.restantDu(cm))/e.getNbEcheances() || e.getQuantite(e.getStepDebut()+i)<=200)) {
					if (coeffDecision*this.getVentePrecedente(cm) -(this.stockChocoMarque.get(cm) + this.restantDu(cm))/e.getNbEcheances()<=200) {
						this.journal_negoc.ajouter("Négociation qur qté a échoué car quantités voulues trop faibles, inférieures à 200 par step");
						return null;
					}
					quantites.add(coeffDecision*this.getVentePrecedente(cm) -(this.stockChocoMarque.get(cm) + this.restantDu(cm))/e.getNbEcheances());
					modif = true;
				} else {
					quantites.add(e.getQuantite(e.getStepDebut()+i));
				}	
			}			
		}
		if (modif) {
			Echeancier new_e = new Echeancier(e.getStepDebut(), quantites);
			this.journal_negoc.ajouter("Nouvelle proposition de négociation sur qté pour "+quantites.toString());
			return new_e;
		} else {
			this.journal_negoc.ajouter("Négociation sur qté finie : "+e.getQuantiteTotale()+" sur "+e.getNbEcheances()+" échéances");
			return e;
		}
	}

	@Override
	public double contrePropositionPrixAcheteur(ExemplaireContratCadre contrat) {
		if (!contrat.getProduit().getType().equals("ChocolatDeMarque") || contrat.getProduit()==null) {
			this.journal_negoc.ajouter("Négociation sur prix a échoué car pas du chocolat de Marque");
			return 0.;
		}
		ChocolatDeMarque choco = (ChocolatDeMarque) contrat.getProduit();
		if (!Filiere.LA_FILIERE.getChocolatsProduits().contains(choco)) { //tous les chocos sont censés être définis et stockés initialement, truc bizarre avec CacaoMagic
			this.journal_negoc.ajouter("Négociation sur prix a échoué car chocolat de marque "+choco+ " pas référencé");
			return 0.;
		}
		double prix_limite = (prix(choco)*0.7- this.getCoutStockage())/**contrat.getQuantiteTotale()/contrat.getEcheancier().getNbEcheances()*/;
		if (prix_limite<0) {
			this.journal_negoc.ajouter("Négociation a échoué car nous ne sommes pas en capacité de payer : "+prix_limite);
			return 0.;
		}
		if (contrat.getPrix()<=prix_limite) {
			this.journal_negoc.ajouter("Négociation sur prix finie : "+contrat.getPrix());
			return contrat.getPrix();
		} else {
			if (Filiere.LA_FILIERE.getBanque().verifierCapacitePaiement(this, cryptogramme, prix_limite)) {
				this.journal_negoc.ajouter("Nouvelle proposition de négociation au prix : "+prix_limite);
				return prix_limite;
			} else {
				this.journal_negoc.ajouter("Négociation a échoué car nous ne sommes pas en capacité de payer : "+prix_limite);
				return 0.;
			}
		}		
	}
	
	public void lancerNouveauCC() {
		for (ChocolatDeMarque cm : this.stockChocoMarque.keySet()) { 
			if (this.stockChocoMarque.get(cm)+restantDu(cm)<coeffDecision*this.getVentePrecedente(cm)) {
				this.journal_CC.ajouter("Contrat Cadre pour "+cm+" ?");
				this.journal_CC.ajouter("Pas assez de "+cm+" en stock donc Contrat Cadre à lancer");
				double parStep = 0;
				int nbStep = 12;
				parStep = Math.max(Math.min(Filiere.LA_FILIERE.getAttractivite(cm)*2000, coeffDecision*(Filiere.LA_FILIERE.getVentes(cm, Filiere.LA_FILIERE.getEtape()-1)-(this.stockChocoMarque.get(cm)+this.restantDu(cm))/nbStep)),200);	
				if (parStep*nbStep>200000) {
					parStep = 15000;
				}
				if (parStep>10000) {
					nbStep = 3;
				}
				if (parStep>=SuperviseurVentesContratCadre.QUANTITE_MIN_ECHEANCIER) {
					Echeancier e = new Echeancier(Filiere.LA_FILIERE.getEtape()+1, nbStep, parStep);
					List<IVendeurContratCadre> vendeurs = supCC.getVendeurs(cm);
					this.journal_CC.ajouter("vendeurs possibles :" + vendeurs.toString());
					boolean est_contratPasse = false;
					for (IVendeurContratCadre vendeur : vendeurs ) {
						this.journal_CC.ajouter("   "+vendeur.getNom()+" retenu comme vendeur parmi "+vendeurs.size()+" vendeurs potentiels");
						this.journal_negoc.ajouter("Nouvelle négociation lancée avec "+vendeur.getNom()+" pour du chocolat "+cm);
						ExemplaireContratCadre contrat = supCC.demandeAcheteur(this, vendeur, cm, e, cryptogramme, false);
						if (contrat==null) {
							this.journal_CC.ajouter(Color.RED, Color.white,"   echec des negociations, tentative suivante");
							this.journal_negoc.ajouter(Color.RED, Color.white,"   echec des negociations");
						} else {
							est_contratPasse=true;
							this.journal_CC.ajouter(Color.GREEN, vendeur.getColor(), "   contrat signe");
							this.journal_negoc.ajouter(Color.GREEN, vendeur.getColor(), "   contrat signe");
							notificationNouveauContratCadre(contrat);
							break;
						}
						if (!est_contratPasse) {
							this.journal_CC.ajouter("Contrat cadre a échoué car pas de vendeur");
						}
					}
				} 
			}
			
		}
	}
	
	
	@Override
	public void notificationNouveauContratCadre(ExemplaireContratCadre contrat) {
		this.journal_CC.ajouter("Nouveau Contrat Cadre : "+contrat.toString());
		this.contratsEnCours.add(contrat);
		this.totalCoutAPayer += contrat.getPrix();
	}

	@Override
	public void receptionner(IProduit p, double quantiteEnTonnes, ExemplaireContratCadre contrat) {
		this.journal_CC.ajouter("Livraison du produit "+quantiteEnTonnes+" tonnes de "+p+", issu du contrat #"+contrat.getNumero());
		if (p.getType().equals("ChocolatDeMarque")) {
			this.getStockChocoMarque().put((ChocolatDeMarque) p, quantiteEnTonnes + this.getStockChocoMarque().get((ChocolatDeMarque)p));
			//this.totalStocksChocoMarque.ajouter(this, quantiteEnTonnes, cryptogramme);
		}
		if (Filiere.LA_FILIERE.getEtape() == contrat.getEcheancier().getStepFin()) {
			this.contratsTermines.add(contrat);
			this.contratsEnCours.remove(contrat);
		}
	}

	
	
	public List<Journal> getJournaux() {
		List<Journal> res= super.getJournaux();
		res.add(this.journal_CC);
		res.add(this.journal_negoc);
		return res;
	}
	
}
