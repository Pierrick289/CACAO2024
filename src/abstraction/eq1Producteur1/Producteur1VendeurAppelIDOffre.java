package abstraction.eq1Producteur1;
import abstraction.eqXRomu.filiere.Filiere;
import abstraction.eqXRomu.bourseCacao.BourseCacao;
import abstraction.eqXRomu.general.Journal;
import abstraction.eqXRomu.produits.ChocolatDeMarque;
import abstraction.eqXRomu.produits.Feve;
import abstraction.eqXRomu.produits.Gamme;
import abstraction.eqXRomu.produits.IProduit;
import abstraction.eqXRomu.appelDOffre.AppelDOffre;
import abstraction.eqXRomu.appelDOffre.IAcheteurAO;
import abstraction.eqXRomu.appelDOffre.IVendeurAO;
import abstraction.eqXRomu.appelDOffre.OffreVente;
import abstraction.eqXRomu.appelDOffre.SuperviseurVentesAO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;




//J'ai ajoute ca car pourqoui pas? Ca nous aidera a remplir les offres des de
public class Producteur1VendeurAppelIDOffre extends Producteur1VendeurCCadre implements IVendeurAO{

	
	
	
	/*/////////////////////////////////////////////////////////////////////////////////
	 /////////////////////////////////* youssef ben abdeljelil/////////////////////////
	 */
	//prix minimal pour chaque type de fève 
	private double prix_seuil_BQ;
	private double prix_seuil_MQ;
	private double prix_seuil_MQ_E;
	private double prix_seuil_HQ;
	private double prix_seuil_HQ_BE;
	private double prix_seuil_HQ_E;
	
	private double seuil_minimAO; 	// quantité minimale à vendre pour un appel d'offre, cette 
									//quantité est à négliger si les revenus dépassent 
									//celle de la bourse
	private double seuil_maximAO;	//quantité maximale à vendre 0par les appels d'offre , cette quantité 
									//peut etre depassée si les revenus dépassent 
									//ceux de la bourse
	public HashMap <Feve,Double> prix_defaut_feve;//dictionnaire pour chaque feve et son prix
	
	protected ArrayList<Feve> a_ne_pas_vendre=new ArrayList<Feve>();//feve a ne pas vendre pour les AO
	protected ArrayList<IAcheteurAO> black_list_Acheteur_AO=new ArrayList<IAcheteurAO>();//black list pour les acheteurs
	protected HashMap <IAcheteurAO,Double> score_to_black_list; //dictionnaire pour chaque transfo et son scoredans la liste noire
	
	public static int score_seuil_blacklist=10;//score a partir duquel un achteur est ajouté au blacklist
	private Journal journalAO;
	private SuperviseurVentesAO supAO;
	
	
	public Producteur1VendeurAppelIDOffre() {
		super();
		this.journalAO = new Journal(this.getNom()+" journal appel d'offre  ", this);
		

		}
	
	public void initialiser() {
		super.initialiser();
		prix_defaut_feve=new HashMap<Feve, Double>();
		prix_defaut_feve.put(Feve.F_BQ,2217.0);
		prix_defaut_feve.put(Feve.F_MQ,2417.0 );
		prix_defaut_feve.put(Feve.F_HQ,3000.0);
		prix_defaut_feve.put(Feve.F_MQ_E,2600.0 );
		prix_defaut_feve.put(Feve.F_HQ_E,3200.0 );
		prix_defaut_feve.put(Feve.F_HQ_BE,3400.0 );
		//initilaiser les prix, ceci peut varier
		this.supAO = (SuperviseurVentesAO)(Filiere.LA_FILIERE.getActeur("Sup.AO"));
		score_to_black_list=new HashMap<IAcheteurAO, Double>();	// initialiser un dictionnaire vide pour 
																//les acteurs(transfo) 
																//et leurs scores dans la liste noire
	}
	
	
	
	public double revenus_bourse_seuil_AO(AppelDOffre offre,BourseCacao bourse) {
		
		
		return bourse.getCours((Feve)(offre.getProduit())).getMax()*offre.getQuantiteT();
		//évaluer les revenus si on vendait ces quantités en bourse
	}
	
	public double revenues_AO_prix_defaut(AppelDOffre offre) 
	{ 
		double prix=prix_defaut_feve.get(offre.getProduit());
		return offre.getQuantiteT()*prix;
		//evaluer les revenus si on vendait par appel d'offre
		//le choix szra fait entre la vente en bourse ou la vente 
		//par appel d'offre selon les revenus de chaque méthode
	}
	
	
	public OffreVente proposerVente(AppelDOffre offre) {

		// TODO Auto-generated method stub
	     IAcheteurAO acheteur_AO=offre.getAcheteur();
	     IProduit produit_AO=offre.getProduit();
	     BourseCacao bourse =(BourseCacao)(Filiere.LA_FILIERE.getActeur("BourseCacao"));

	     if (!(produit_AO instanceof Feve)) {
				journalAO.ajouter(Filiere.LA_FILIERE.getEtape()+"testtest "+offre.getProduit());

				return null;//si ce n'est pas un produit feve, rien ne se passe

			}
	     Feve feve_AO = (Feve)produit_AO;
	     
	     if (!(prix_defaut_feve.keySet().contains(feve_AO))) {
				return null;
			}
	     
	     double quantite_AO=offre.getQuantiteT();
	     double quantite_stock = this.getQuantiteEnStock( feve_AO , cryptogramme);
	     
	     //mettre a jour la black_list
	     if (score_to_black_list.containsKey(acheteur_AO)==false) {
				
	    	 score_to_black_list.put(acheteur_AO, 0.0);
		}
	     
	   //revenus AO pour n'importe quels quanties et qualité >revenues
		// bourse pour les memes quanites qualites
	    
	    if (revenus_bourse_seuil_AO(offre,bourse )< revenues_AO_prix_defaut(offre)){
	    	return new OffreVente(offre, this, feve_AO, prix_defaut_feve.get(feve_AO));	
		}
	     
		if (black_list_Acheteur_AO.contains(acheteur_AO)) {
			
			journalAO.ajouter(Filiere.LA_FILIERE.getEtape()+" Appel d'offre impossible, l'acheteur "+acheteur_AO.getNom()+" est dans la liste noire ");
			return null;
		}
		
		//si feve figure dans la liste a ne pas vendre en par AOs
		 if(a_ne_pas_vendre.contains(feve_AO)) {
			journalAO.ajouter(Filiere.LA_FILIERE.getEtape()+" Appel d'offre impossible, le produit "+feve_AO+" n'est pas à vendre");

			return null;
			
		}
		
		 if(quantite_stock<=quantite_AO) {
			journalAO.ajouter(Filiere.LA_FILIERE.getEtape()+" Appel d'offre impossible, quantite_stock<=quantite_AO");
			return null;
		}
		
		
		 if (quantite_AO<seuil_minimAO || quantite_AO>seuil_maximAO) {
			journalAO.ajouter(Filiere.LA_FILIERE.getEtape()+" Appel d'offre impossible, quantite_stock<=quantite_AO");

			return null;
		}
		
		//on va ajouter une strategie pour savoir si on doit porcéder à une vente par appel d'offre 
		//ou non qui dépend des prix en bourse ,les seuils sur les quanites min et max peuvent 
		//changer si le revenu est égal ou supérieur à celui qu'on a pu génerer en bourse pour les quanites 
		//seuil min et max
		
		
		
			return new OffreVente(offre, this, feve_AO, prix_defaut_feve.get(feve_AO));
			
			
			
		
		
		
	}

	@Override
	public void notifierVenteAO(OffreVente propositionRetenue) {
		// TODO Auto-generated method stub
		Feve feve_AO = (Feve)(propositionRetenue.getProduit());
		double prix_AO  = propositionRetenue.getPrixT();
		double quantite_AO = propositionRetenue.getQuantiteT();
		journalAO.ajouter(Filiere.LA_FILIERE.getEtape()+"   Vente par AO de "+quantite_AO+" T de "+feve_AO+" au prix de  "+prix_AO);
		IAcheteurAO acheteur_AO=propositionRetenue.getOffre().getAcheteur();
		
		//ajouter un +1 au score de la blacklist pour cet acheteur pour avoir refusé l'offre
		if (score_to_black_list.containsKey(acheteur_AO)) {
			double score_actuel = score_to_black_list.get(acheteur_AO);
			score_to_black_list.put(acheteur_AO, score_actuel - 1);
		}
		
	}

	@Override
	public void notifierPropositionNonRetenueAO(OffreVente propositionRefusee) {
		// TODO Auto-generated method stub
		Feve feve_AO = (Feve)(propositionRefusee.getProduit());
		double prix_AO  = propositionRefusee.getPrixT();
		double quantite_AO = propositionRefusee.getQuantiteT();
		IAcheteurAO acheteur_AO=propositionRefusee.getOffre().getAcheteur();
		
		//ajouter un +1 au score de la blacklist pour cet acheteur pour avoir refusé l'offre
		if (score_to_black_list.containsKey(acheteur_AO)) {
			double score_actuel = score_to_black_list.get(acheteur_AO);
			score_to_black_list.put(acheteur_AO, score_actuel + 1);
		}
		
		  
		journalAO.ajouter(Filiere.LA_FILIERE.getEtape()+"   Echec de Vente par AO de "+quantite_AO+" T de "+feve_AO+" au prix de  "+prix_AO);
		
	}
	
	public List<Journal> getJournaux() {
		List<Journal> jx=super.getJournaux();
		jx.add(journalAO);
		return jx;
	}

}