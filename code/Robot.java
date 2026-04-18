import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public abstract class Robot {

    protected String id;
    protected int x;
    protected int y;
    protected int energie;
    protected int heuresUtilisation;
    protected boolean enMarche;
    protected List<String> historiqueActions;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss");


    public Robot(String id, int x, int y, int energieInitiale) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.energie = Math.max(0, Math.min(100, energieInitiale));
        this.heuresUtilisation = 0;
        this.enMarche = false;
        this.historiqueActions = new ArrayList<>();
        ajouterHistorique("Robot créé");
    }

    public void ajouterHistorique(String action) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        historiqueActions.add(timestamp + " - " + action);
    }
    
    public void verifierEnergie(int energieRequise) throws EnergieInsuffisanteException {
        if (this.energie < energieRequise) {
            throw new EnergieInsuffisanteException(
                "Énergie insuffisante : " + this.energie + "% disponible, " +
                energieRequise + "% requis."
            );
        }
    }

    
    public void verifierMaintenance() throws MaintenanceRequiseException {
        if (this.heuresUtilisation >= 100) {
            throw new MaintenanceRequiseException(
                "Maintenance requise : le robot a atteint " + heuresUtilisation + " heures d'utilisation."
            );
        }
    }

    public void demarrer() throws RobotException {
        if (energie < 10) {
            ajouterHistorique("Tentative de démarrage échouée (énergie insuffisante : " + energie + "%)");
            throw new EnergieInsuffisanteException(
                "Impossible de démarrer : énergie insuffisante (" + energie + "%). Minimum 10% requis."
            );
        }
        this.enMarche = true;
        ajouterHistorique("Démarrage du robot");
    }

    public void arreter() {
        this.enMarche = false;
        ajouterHistorique("Arrêt du robot");
    }

    public void consommerEnergie(int quantite) {
        this.energie = Math.max(0, this.energie - quantite);
    }

    public void recharger(int quantite) {
        this.energie = Math.min(100, this.energie + quantite);
        ajouterHistorique("Recharge de " + quantite + "% d'énergie. Énergie actuelle : " + this.energie + "%");
    }

    public abstract void deplacer(int x, int y) throws RobotException;

    public abstract void effectuerTache() throws RobotException;

    public String getHistorique() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Historique du robot ").append(id).append(" ===\n");
        for (String action : historiqueActions) {
            sb.append(action).append("\n");
        }
        return sb.toString();
    }

    public String getId() { return id; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getEnergie() { return energie; }
    public int getHeuresUtilisation() { return heuresUtilisation; }
    public boolean isEnMarche() { return enMarche; }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
               " [ID : " + id +
               ", Position : (" + x + "," + y + ")" +
               ", Énergie : " + energie + "%" +
               ", Heures : " + heuresUtilisation + "]";
    }
}
