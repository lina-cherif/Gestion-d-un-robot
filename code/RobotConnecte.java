package projetrobot;

public abstract class RobotConnecte extends Robot implements connectable {
	protected boolean connecte;  // Indique si le robot est actuellement connecté à un réseau.
	protected String reseauConnecte; //Nom du réseau auquel le robot est connecté. Si le robot n’est pas connecté, cet attribut est null.

	public RobotConnecte(String id,int x,int y , int energie, int  heuresUtilisation)
	{
		super(id,x,y,energie,heuresUtilisation);
		this.connecte=false ; 
		this.reseauConnecte=null ; 
	}
	public void connecter(String reseau) throws RobotException
	{
		verifierEnergie(5);
        verifierMaintenance();
		this.reseauConnecte=reseau; 
		this.connecte=true ; 
		consommerEnergie(5);
		ajouterHistorique("Connexion réussie au réseau : " + reseau); 
	}
	public  void deconnecter () 
	{
		this.connecte=false ;
		ajouterHistorique("Deconnexion du"+reseauConnecte); 
		this.reseauConnecte=null ; 
		
	}
	public  void envoyerDonnees (String donnees ) throws RobotException 
	{
		if (!connecte)
		{
			throw new RobotException( "Envoi impossible : le robot " + id + " n'est pas connecté à un réseau.");
			
		}
		// ?????????????
		 verifierEnergie(3);
	     verifierMaintenance();
	     consommerEnergie(3);
	     ajouterHistorique("Données envoyées sur [" + reseauConnecte + "] : " + donnees);
	}
	
	public boolean isConnecte()      { return connecte; }
    public String  getReseauConnecte() { return reseauConnecte; }
	

}
