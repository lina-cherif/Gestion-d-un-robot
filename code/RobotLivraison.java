package projetrobot;

public class RobotLivraison extends RobotConnecte {

    protected String colisActuel;       // null=pas de colis
    protected String destination;
    protected boolean enLivraison;

    public static final int ENERGIE_LIVRAISON  = 15;
    public static final int ENERGIE_CHARGEMENT = 5;

    private static final double ENERGIE_PAR_UNITE_DISTANCE = 0.3;
    private static final double HEURES_PAR_UNITE_DISTANCE  = 0.1;

    // Coordonnées de la station logistique
    private static final int STATION_LOGISTIQUE_X = 0;
    private static final int STATION_LOGISTIQUE_Y = 0;

    public RobotLivraison(String id, int x, int y) {
        super(id, x, y, 100);
        this.colisActuel   = null;
        this.destination   = null;
        this.enLivraison   = false;
    }

    @Override
    public void effectuerTache() throws RobotException {
        if (!enMarche) {
            throw new RobotException("Le robot doit être démarré pour effectuer une tâche.");
        }

        // 1. Vérification autonome de l'énergie
        if (this.energie < 20) {
            System.out.println("🤖 [" + id + "] Batterie faible (" + this.energie + "%). Retour autonome à la station...");
            rentrerAStation();
            return; // On arrête la tâche ici pour se recharger
        }

        // 2. Logique de livraison autonome
        if (enLivraison) {
            System.out.println("🤖 [" + id + "] En route pour livrer le colis : " + colisActuel + " à destination : " + destination);
            // On simule des coordonnées d'arrivée générées par le nom de la destination
            int destX = Math.abs(destination.hashCode() % 50) + 1;
            int destY = Math.abs(destination.hashCode() % 50) + 1;
            faireLivraison(destX, destY);
        } else {
            // S'il n'a pas de colis, il patrouille et attend qu'on lui assigne une mission
            ajouterHistorique("Patrouille en attente d'un nouveau colis");
            System.out.println("🤖 [" + id + "] En attente de colis. Patrouille en cours...");
            consommerEnergie(2); // Consommation de veille
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // Méthode de retour autonome
    // ──────────────────────────────────────────────────────────────────
    private void rentrerAStation() throws RobotException {
        deplacer(STATION_LOGISTIQUE_X, STATION_LOGISTIQUE_Y);
        this.energie = 100;
        ajouterHistorique("Recharge complète à la station logistique.");
        System.out.println("✅ [" + id + "] Entièrement rechargé et prêt pour de nouvelles livraisons.");
    }

    // ──────────────────────────────────────────────────────────────────
    // faireLivraison
    // ──────────────────────────────────────────────────────────────────
    public void faireLivraison(int destX, int destY) throws RobotException {
        deplacer(destX, destY);
        ajouterHistorique("Livraison terminée à " + destination);
        System.out.println("✓ Livraison terminée à " + destination);
        this.colisActuel  = null;
        this.enLivraison  = false;
        this.destination  = null;
    }

    // ──────────────────────────────────────────────────────────────────
    // deplacer  (distance euclidienne, limite 100 unités)
    // ──────────────────────────────────────────────────────────────────
    @Override
    public void deplacer(int destX, int destY) throws RobotException {
        double distance = Math.sqrt(Math.pow(destX - this.x, 2) + Math.pow(destY - this.y, 2));

        if (distance > 100) {
            throw new RobotException(
                "Distance trop grande : " + String.format("%.2f", distance) +
                " unités (maximum autorisé : 100 unités)."
            );
        }

        int energieNecessaire = (int) Math.ceil(distance * ENERGIE_PAR_UNITE_DISTANCE);
        verifierEnergie(energieNecessaire);
        verifierMaintenance();

        consommerEnergie(energieNecessaire);
        this.heuresUtilisation += (int) (distance * HEURES_PAR_UNITE_DISTANCE);

        ajouterHistorique(
            "Déplacement vers (" + destX + "," + destY + ")" +
            " | Distance : " + String.format("%.2f", distance) + " unités" +
            " | Énergie : " + energieNecessaire + "%"
        );

        this.x = destX;
        this.y = destY;
    }

    // Appelé par le gestionnaire pour assigner un ordre au robot
    public void chargerColis(String descriptionColis, String dest) throws RobotException {
        if (enLivraison) {
            throw new RobotException("Le robot est déjà en cours de livraison.");
        }
        verifierEnergie(ENERGIE_CHARGEMENT);

        this.colisActuel   = descriptionColis;
        this.destination   = dest;
        this.enLivraison   = true;
        consommerEnergie(ENERGIE_CHARGEMENT);
        ajouterHistorique("Colis chargé : " + descriptionColis + " → Destination : " + dest);
        System.out.println("✓ Colis \"" + descriptionColis + "\" chargé. Destination : " + dest);
    }

    // --- Getters ---
    public String getColisActuel()  { return colisActuel  != null ? colisActuel  : "Aucun"; }
    public String getDestination()  { return destination  != null ? destination  : "N/A"; }
    public boolean isEnLivraison()  { return enLivraison; }

    @Override
    public String toString() {
        return "RobotLivraison [ID : " + id +
               ", Position : (" + x + "," + y + ")" +
               ", Énergie : " + energie + "%" +
               ", Heures : " + heuresUtilisation +
               ", Colis : " + (colisActuel != null ? 1 : 0) +
               ", Destination : " + getDestination() +
               ", Connecté : " + (connecte ? "Oui" : "Non") + "]";
    }
}