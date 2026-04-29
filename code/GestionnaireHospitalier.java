package projetrobot;

import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  GestionnaireHospitalier — Coordinateur de la flotte de robots (§3.2)
 * ═══════════════════════════════════════════════════════════════════════
 *
 *  Pilote une ArrayList<Robot> hétérogène.
 *
 *  Illustre le POLYMORPHISME via :
 *   1. sélectionnerRobotPourMission(TypeMission) — algorithme de sélection
 *      du robot le plus apte à remplir une mission donnée.
 *   2. alerteGenerale() — interrompt toutes les activités secondaires
 *      en appelant arreter() de façon polymorphique sur chaque robot.
 *
 *  Types de missions gérées :
 *   - TRIAGE      → préfère un RobotTriage connecté avec kit complet
 *   - LIVRAISON   → préfère un RobotLivraison sans colis en cours
 *   - SURVEILLANCE→ n'importe quel robot en marche avec de l'énergie
 */
public class GestionnaireHospitalier {

    // ── Énumération des types de missions ────────────────────────────
    public enum TypeMission {
        TRIAGE,
        LIVRAISON,
        SURVEILLANCE
    }

    // ── Flotte de robots ─────────────────────────────────────────────
    private final ArrayList<Robot> flotte;

    // ── Nom de l'hôpital géré ────────────────────────────────────────
    private final String nomHopital;

    // ── Historique des affectations ──────────────────────────────────
    private final List<String> journalAffectations;

    // ── Seuil minimal d'énergie pour être sélectionnable ────────────
    private static final int SEUIL_ENERGIE_MIN = 25;

    // ════════════════════════════════════════════════════════════════
    //  CONSTRUCTEUR
    // ════════════════════════════════════════════════════════════════

    public GestionnaireHospitalier(String nomHopital) {
        this.nomHopital          = nomHopital;
        this.flotte              = new ArrayList<>();
        this.journalAffectations = new ArrayList<>();
        log("GestionnaireHospitalier créé pour : " + nomHopital);
    }

    // ════════════════════════════════════════════════════════════════
    //  GESTION DE LA FLOTTE
    // ════════════════════════════════════════════════════════════════

    /**
     * Ajoute un robot à la flotte.
     * Le robot est démarré automatiquement s'il ne l'est pas encore.
     */
    public void ajouterRobot(Robot robot) throws RobotException {
        flotte.add(robot);
        if (!robot.isEnMarche()) {
            robot.demarrer();
        }
        log("Robot ajouté à la flotte : " + robot.getId() +
            " [" + robot.getClass().getSimpleName() + "]");
    }

    /**
     * Retire un robot de la flotte et l'arrête.
     */
    public void retirerRobot(String robotId) {
        flotte.removeIf(r -> {
            if (r.getId().equals(robotId)) {
                r.arreter();
                log("Robot retiré de la flotte : " + robotId);
                return true;
            }
            return false;
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  1. ALGORITHME DE SÉLECTION — polymorphisme
    // ════════════════════════════════════════════════════════════════

    /**
     * Sélectionne le robot le plus apte à remplir une mission donnée.
     *
     * Critères généraux (tous types) :
     *  - Robot en marche
     *  - Énergie ≥ SEUIL_ENERGIE_MIN
     *
     * Critères spécifiques par TypeMission :
     *  - TRIAGE      : RobotTriage, kit complet, connecté, non en mode dégradé
     *  - LIVRAISON   : RobotLivraison, pas de colis en cours
     *  - SURVEILLANCE: n'importe quel robot — priorité à la plus haute énergie
     *
     * Illustre le polymorphisme : on appelle getClass(), isEnMarche(),
     * getEnergie() de façon uniforme, puis on fait un cast ciblé pour
     * les critères spécifiques au type concret.
     *
     * @param mission Le type de mission à remplir
     * @return Le robot sélectionné, ou null si aucun n'est apte
     */
    public Robot sélectionnerRobotPourMission(TypeMission mission) {
        Robot meilleur    = null;
        int   meilleurScore = -1;

        log("━━━ Sélection robot pour mission : " + mission + " ━━━");

        for (Robot robot : flotte) {
            // ── Filtre commun ──────────────────────────────────────
            if (!robot.isEnMarche()) {
                log("  [✗] " + robot.getId() + " — arrêté");
                continue;
            }
            if (robot.getEnergie() < SEUIL_ENERGIE_MIN) {
                log("  [✗] " + robot.getId() + " — énergie insuffisante (" +
                    robot.getEnergie() + "%)");
                continue;
            }

            // ── Score de base = énergie disponible ────────────────
            int score = robot.getEnergie();

            // ── Critères spécifiques à chaque type de mission ─────
            switch (mission) {

                case TRIAGE:
                    if (!(robot instanceof RobotTriage)) {
                        log("  [✗] " + robot.getId() + " — n'est pas un RobotTriage");
                        continue;
                    }
                    RobotTriage rt = (RobotTriage) robot;
                    if (rt.isModeDegrade()) {
                        score -= 40; // pénalité mode dégradé
                        log("  [~] " + robot.getId() + " — mode dégradé (-40pts)");
                    }
                    if (!rt.getKitUrgence().estComplet()) {
                        score -= 30; // pénalité kit incomplet
                        log("  [~] " + robot.getId() + " — kit incomplet (-30pts)");
                    }
                    if (!rt.isConnecte()) {
                        score -= 20; // pénalité non connecté
                        log("  [~] " + robot.getId() + " — non connecté (-20pts)");
                    }
                    break;

                case LIVRAISON:
                    if (!(robot instanceof RobotLivraison)) {
                        log("  [✗] " + robot.getId() + " — n'est pas un RobotLivraison");
                        continue;
                    }
                    RobotLivraison rl = (RobotLivraison) robot;
                    if (rl.isEnLivraison()) {
                        log("  [✗] " + robot.getId() + " — déjà en livraison");
                        continue; // pas disponible
                    }
                    if (!rl.isConnecte()) {
                        score -= 15;
                        log("  [~] " + robot.getId() + " — non connecté (-15pts)");
                    }
                    break;

                case SURVEILLANCE:
                    // Tous types acceptés — on favorise l'énergie maximale
                    // Bonus si c'est un robot connecté (meilleure remontée d'info)
                    if (robot instanceof RobotConnecte) {
                        RobotConnecte rc = (RobotConnecte) robot;
                        if (rc.isConnecte()) score += 10;
                    }
                    break;
            }

            log("  [✓] " + robot.getId() +
                " [" + robot.getClass().getSimpleName() + "]" +
                " — score : " + score);

            if (score > meilleurScore) {
                meilleurScore = score;
                meilleur = robot;
            }
        }

        if (meilleur != null) {
            log("  ➤ SÉLECTIONNÉ : " + meilleur.getId() +
                " (score=" + meilleurScore + ")");
            journalAffectations.add("[" + mission + "] → " + meilleur.getId());
        } else {
            log("  ⚠ Aucun robot apte pour la mission " + mission);
        }

        return meilleur;
    }

    // ════════════════════════════════════════════════════════════════
    //  2. ALERTE GÉNÉRALE — polymorphisme sur arreter()
    // ════════════════════════════════════════════════════════════════

    /**
     * Interrompt TOUTES les activités secondaires en cas d'alerte générale.
     *
     * Illustre le polymorphisme : arreter() est appelé uniformément sur
     * chaque Robot de la flotte, qu'il soit RobotTriage ou RobotLivraison.
     *
     * Seuls les robots en cours de traitement d'un ESI 1 sont exemptés
     * (priorité médicale absolue).
     */
    public void alerteGenerale() {
        log("🚨 ALERTE GÉNÉRALE DÉCLENCHÉE — Interruption de toutes les activités");

        int interrompus = 0;
        for (Robot robot : flotte) {
            // Exemption : RobotTriage avec un patient ESI 1 actif
            if (robot instanceof RobotTriage) {
                RobotTriage rt = (RobotTriage) robot;
                // En production, on vérifierait un flag "enUrgenceVitale"
                // Ici on n'interrompt pas un triage en mode critique
                log("  [MAINTENU] " + robot.getId() + " — robot de triage (priorité médicale)");
                continue;
            }

            // Polymorphisme : appel uniforme de arreter()
            if (robot.isEnMarche()) {
                robot.arreter(); // méthode polymorphique définie dans Robot
                log("  [ARRÊTÉ]   " + robot.getId() +
                    " [" + robot.getClass().getSimpleName() + "]");
                interrompus++;
            }
        }

        log("Alerte générale : " + interrompus + " robot(s) interrompu(s).");
    }

    // ════════════════════════════════════════════════════════════════
    //  3. DÉCLENCHER UNE TÂCHE — polymorphisme sur effectuerTache()
    // ════════════════════════════════════════════════════════════════

    /**
     * Sélectionne automatiquement le meilleur robot pour une mission
     * et lui demande d'effectuer sa tâche.
     *
     * Illustre le polymorphisme : effectuerTache() est appellé de façon
     * uniforme, mais l'implémentation concrète diffère selon le type de robot.
     */
    public void déclencherMission(TypeMission mission) {
        Robot robot = sélectionnerRobotPourMission(mission);
        if (robot == null) {
            log("⚠ Mission " + mission + " non exécutée — aucun robot disponible.");
            return;
        }
        try {
            log("→ Déclenchement mission " + mission + " sur " + robot.getId());
            robot.effectuerTache(); // POLYMORPHISME CENTRAL
        } catch (RobotException e) {
            log("⚠ Erreur durant mission " + mission +
                " sur " + robot.getId() + " : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  RAPPORT D'ÉTAT DE LA FLOTTE
    // ════════════════════════════════════════════════════════════════

    /**
     * Génère un rapport complet de l'état de la flotte.
     * Polymorphisme : toString() de chaque Robot est appelé uniformément.
     */
    public String rapportFlotte() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════════════════════╗\n");
        sb.append("║   RAPPORT FLOTTE — ").append(nomHopital).append("\n");
        sb.append("╠══════════════════════════════════════════════════════════╣\n");
        sb.append("║  Robots en flotte : ").append(flotte.size()).append("\n");
        sb.append("╠══════════════════════════════════════════════════════════╣\n");

        int actifs = 0;
        for (Robot robot : flotte) {
            if (robot.isEnMarche()) actifs++;
            // POLYMORPHISME : toString() spécialisé par sous-classe
            sb.append("║  ").append(robot.toString()).append("\n");
        }

        sb.append("╠══════════════════════════════════════════════════════════╣\n");
        sb.append("║  Actifs : ").append(actifs).append(" / ").append(flotte.size()).append("\n");
        sb.append("╠══════════════════════════════════════════════════════════╣\n");
        sb.append("║  Dernières affectations :\n");
        int debut = Math.max(0, journalAffectations.size() - 5);
        for (int i = debut; i < journalAffectations.size(); i++) {
            sb.append("║    ").append(journalAffectations.get(i)).append("\n");
        }
        sb.append("╚══════════════════════════════════════════════════════════╝\n");
        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════════
    //  GETTERS
    // ════════════════════════════════════════════════════════════════

    public ArrayList<Robot>  getFlotte()     { return flotte; }
    public String            getNomHopital() { return nomHopital; }
    public List<String>      getJournal()    { return journalAffectations; }

    public int getNombreRobotsActifs() {
        int count = 0;
        for (Robot r : flotte) if (r.isEnMarche()) count++;
        return count;
    }

    // ════════════════════════════════════════════════════════════════
    //  UTILITAIRE INTERNE
    // ════════════════════════════════════════════════════════════════

    private void log(String msg) {
        System.out.println("[GestionnaireHospitalier] " + msg);
    }
}
