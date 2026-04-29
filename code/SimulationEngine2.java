
package projetrobot;

import javax.swing.*;
import java.awt.*;

import java.util.List;
import java.util.ArrayList;


/**
 * Moteur de simulation v5 — Flotte multi-robots avec GestionnaireHospitalier
 *
 * Robots :
 *   RT-ALPHA  : RobotTriage   (triage médical, ESI)
 *   RL-BETA   : RobotLivraison (livraisons médicaments/matériel)
 *   RT-GAMMA  : RobotTriage   (triage de secours, 2e robot)
 *
 * Le GestionnaireHospitalier orchestre la flotte :
 *   - Sélection automatique du robot le plus apte
 *   - Alerte générale (arrêt contrôlé)
 *   - Rapport de flotte affiché dans le dashboard
 */
class SimulationEngine2 {

    private final Dashboard2 ui;

    // ── Flotte pilotée par le gestionnaire ───────────────────────────
    GestionnaireHospitalier gestionnaire;
    RobotTriage    robot1;  // RT-ALPHA
    RobotLivraison robot2;  // RL-BETA
    RobotTriage    robot3;  // RT-GAMMA

    // ── Labels sidebar ────────────────────────────────────────────────
    JLabel lblMission1, lblMission2, lblMission3;
    JLabel lblStatusBadge1, lblStatusBadge2, lblStatusBadge3;
    JLabel lblMission; // alias robot1 (compatibilité)

    // ── État global salle ─────────────────────────────────────────────
    final SalleState salleState = new SalleState();

    // ── File d'attente patients ───────────────────────────────────────
    private final List<PatientTriage> fileAttente = new ArrayList<>();

    // ── Flag alerte générale ──────────────────────────────────────────
    private volatile boolean alerteActive = false;

    // ── Positions ─────────────────────────────────────────────────────
    static final float START_X1 = 0.04f, START_Y1 = 0.88f; // RT-ALPHA (bas gauche)
    static final float START_X2 = 0.07f, START_Y2 = 0.82f; // RL-BETA  (à côté)
    static final float START_X3 = 0.04f, START_Y3 = 0.76f; // RT-GAMMA (dessus)

    // ── Obstacles ─────────────────────────────────────────────────────
    static final ObstacleItem[] OBSTACLES_FIXES = {
        new ObstacleItem(0.55f, 0.50f, "chariot", false),
        new ObstacleItem(0.45f, 0.72f, "chariot", false),
    };

    // ── Positions chaises ─────────────────────────────────────────────
    static final float[][] CHAISES_POSITIONS = {
        {0.28f,0.22f},{0.38f,0.22f},{0.50f,0.22f},{0.62f,0.22f},{0.72f,0.22f},
        {0.28f,0.58f},{0.38f,0.58f},{0.50f,0.58f},{0.62f,0.58f},{0.72f,0.58f},
    };

    // ── 6 scénarios patients ──────────────────────────────────────────
    static final ScenarDef[] TOUS_SCENARIOS = {

		// ESI 3 – Fièvre pneumonie
        makeScenario("PAT-001","Patient 1","Fièvre 39.2°C — pneumonie suspectée",
            new SignesVitaux(98,118,76,39.2,95),
            ResultatVision.simuler(false,false,false,false),
            new DossierMedical("PAT-004",55,java.util.Arrays.asList(CategorieRisque.RESPIRATOIRE)),
            false,"PNEUMOLOGIE","Salle P2"),
        
    	// ESI 1 – Inconscient (crise cardiaque)
        makeScenario("PAT-002","Patient 2","Inconscient — crise cardiaque suspectée",
            new SignesVitaux(42,85,50,36.8,91),
            ResultatVision.simuler(false,false,false,true),
            new DossierMedical("PAT-002",68,java.util.Arrays.asList(CategorieRisque.CARDIOVASCULAIRE)),
            false,"RÉANIMATION","Salle R2"),

        // ESI 2 – HTA cardiovasculaire
        makeScenario("PAT-003","Patient 3","HTA sévère + antécédent cardiovasculaire",
            new SignesVitaux(125,165,105,37.8,93),
            ResultatVision.simuler(true,false,false,false),
            new DossierMedical("PAT-003",68,java.util.Arrays.asList(CategorieRisque.CARDIOVASCULAIRE)),
            false,"CARDIOLOGIE","Salle C3"),

        

        // ESI 4 – Traumatologie légère
        makeScenario("PAT-004","Patient 4","Douleur légère au poignet — chute",
            new SignesVitaux(72,118,76,37.0,98),
            ResultatVision.simuler(false,false,false,false),
            new DossierMedical("PAT-005",34,java.util.Arrays.asList(CategorieRisque.AUCUN)),
            false,"TRAUMATOLOGIE","Salle T4"),
        // ESI 1 – Choc hémorragique (jambe amputée)
        makeScenario("PAT-005","Patient 5","Jambe amputée + choc hémorragique",
            new SignesVitaux(140,80,50,37.2,88),
            ResultatVision.simuler(false,true,true,false),null,false,
            "RÉANIMATION","Salle R1"),

        // ESI 5 – Contrôle stable
        makeScenario("PAT-006","Patient 6","Contrôle post-opératoire — stable",
            new SignesVitaux(70,120,78,37.1,98),
            ResultatVision.simuler(false,false,false,false),
            new DossierMedical("PAT-006",28,java.util.Arrays.asList(CategorieRisque.AUCUN)),
            false,"CHIRURGIE AMBULATOIRE","Salle CA1"),
    };

    private static ScenarDef makeScenario(String pid,String nom,String desc,
            SignesVitaux sv,ResultatVision rv,DossierMedical dm,boolean deg,String dept,String salle){
        ScenarDef s=new ScenarDef(pid,nom,desc,sv,rv,dm,deg);
        s.departement=dept;s.salle=salle;return s;
    }
    // ═════════════════════════════════════════════════════════════════
    SimulationEngine2(Dashboard2 ui) { this.ui = ui; }

    // ═════════════════════════════════════════════════════════════════
    //  DÉMARRAGE
    // ═════════════════════════════════════════════════════════════════

    void start() {
        // 1. Créer le gestionnaire
        gestionnaire = new GestionnaireHospitalier("Hôpital Central des Urgences");

        // 2. Créer et enregistrer les robots
        try {
            robot1 = new RobotTriage("RT-ALPHA", 0, 0);
            robot2 = new RobotLivraison("RL-BETA", 0, 0);
            robot3 = new RobotTriage("RT-GAMMA", 0, 0);

            // connecter tous les robots
            robot1.connecter("WIFI-HOPITAL-5G");
            robot2.connecter("WIFI-HOPITAL-5G");
            robot3.connecter("WIFI-HOPITAL-5G");

            // Énergies initiales différentes (simuler usure variable)
            try { robot2.consommerEnergie(22); } catch (Exception ignored) {}
            try { robot3.consommerEnergie(45); } catch (Exception ignored) {}

            // Enregistrer dossiers médicaux dans les robots de triage
            for (ScenarDef sc : TOUS_SCENARIOS) {
                if (sc.dossier != null) {
                    robot1.enregistrerDossier(sc.dossier);
                    robot3.enregistrerDossier(sc.dossier);
                }
            }

            // Ajouter à la flotte via le gestionnaire (démarre automatiquement)
            gestionnaire.ajouterRobot(robot1);
            gestionnaire.ajouterRobot(robot2);
            gestionnaire.ajouterRobot(robot3);

        } catch (RobotException e) {
            ui.addLog("[ERREUR INIT] " + e.getMessage(), LogType.CRITICAL);
        }

        // 3. Initialiser la salle
        initialiserChaises();
        salleState.obstacles.addAll(java.util.Arrays.asList(OBSTACLES_FIXES));
        salleState.robotX1 = START_X1; salleState.robotY1 = START_Y1;
        salleState.robotX2 = START_X2; salleState.robotY2 = START_Y2;
        salleState.robot1EnMarche = true;
        salleState.robot2EnMarche = true;

        // 4. Mise à jour UI initiale
        ui.addLog("━━━ SYSTÈME DÉMARRÉ — Flotte 3 robots active ━━━", LogType.SUCCESS);
        ui.addLog("Gestionnaire : " + gestionnaire.getNomHopital(), LogType.INFO);
        ui.addLog("RT-ALPHA (Triage) | RL-BETA (Livraison) | RT-GAMMA (Triage secours)", LogType.INFO);
        ui.addLog("6 patients en salle (2×ESI1, 1×ESI2, 1×ESI3, 1×ESI4, 1×ESI5)", LogType.INFO);

        ui.updateSalle(salleState);
        ui.updateRobotSidebar(1, 100, false, true, true, true);
        ui.updateRobotSidebar(2, 78, false, true, false, true);
        ui.updateRobotSidebar(3, 55, false, true, true, true);
        ui.updateFile(new ArrayList<>(fileAttente));

        // 5. Lancer le cycle dans un thread séparé
        new Thread(this::cycleComplet, "CycleFlotte").start();
    }

    // ═════════════════════════════════════════════════════════════════
    //  CYCLE COMPLET DE SIMULATION
    // ═════════════════════════════════════════════════════════════════

    private void cycleComplet() {
        sleep(1000);

        // ── PHASE 1 : Sélection & rapport gestionnaire ────────────────
        ui.addLog("━━━ GestionnaireHospitalier — Rapport flotte initial ━━━", LogType.SUCCESS);
        String rapport = gestionnaire.rapportFlotte();
        for (String ligne : rapport.split("\n")) {
            ui.addLog(ligne, LogType.INFO);
        }
        sleep(800);

        // ── PHASE 2 : Triage RT-ALPHA — ESI 1 & ESI 2 ────────────────
        ui.addLog("━━━ PHASE 1 — RT-ALPHA : triage patients prioritaires ━━━", LogType.SUCCESS);
        ui.addLog("[Gestionnaire] Sélection robot pour TRIAGE...", LogType.INFO);
        Robot robotTriage1 = gestionnaire.sélectionnerRobotPourMission(
                GestionnaireHospitalier.TypeMission.TRIAGE);
        if (robotTriage1 != null) {
            ui.addLog("[Gestionnaire] ➤ " + robotTriage1.getId() + " sélectionné pour TRIAGE", LogType.SUCCESS);
        }
        sleep(400);

        // Triage des patients critiques par RT-ALPHA
        trierPatient((RobotTriage) robot1, findScenario("PAT-002"), 1); // ESI1 – inconscient
        sleep(500);
        trierPatient((RobotTriage) robot1, findScenario("PAT-005"), 1); // ESI1 – hémorragie
        sleep(500);

        // ── PHASE 3 : RT-GAMMA prend en charge ESI 2 & 3 ────────────
        ui.addLog("━━━ PHASE 2 — RT-GAMMA : triage patients secondaires ━━━", LogType.SUCCESS);
        ui.addLog("[Gestionnaire] Nouvelle sélection TRIAGE (RT-ALPHA occupé)...", LogType.INFO);
        Robot robotTriage2 = gestionnaire.sélectionnerRobotPourMission(
                GestionnaireHospitalier.TypeMission.TRIAGE);
        if (robotTriage2 != null) {
            ui.addLog("[Gestionnaire] ➤ " + robotTriage2.getId() + " sélectionné", LogType.SUCCESS);
        }
        sleep(400);

        trierPatient((RobotTriage) robot3, findScenario("PAT-003"), 3); // ESI2
        sleep(500);
        trierPatient((RobotTriage) robot3, findScenario("PAT-001"), 3); // ESI3
        sleep(400);

        // ── PHASE 4 : RL-BETA — livraisons médicaments ───────────────
        ui.addLog("━━━ PHASE 3 — RL-BETA : livraisons médicaments ━━━", LogType.SUCCESS);
        ui.addLog("[Gestionnaire] Sélection robot pour LIVRAISON...", LogType.INFO);
        Robot robotLiv = gestionnaire.sélectionnerRobotPourMission(
                GestionnaireHospitalier.TypeMission.LIVRAISON);
        if (robotLiv != null) {
            ui.addLog("[Gestionnaire] ➤ " + robotLiv.getId() + " sélectionné pour LIVRAISON", LogType.SUCCESS);
        }
        sleep(400);

        simulerLivraison(robot2, "Adrénaline × 3", "RÉANIMATION Salle R1");
        sleep(600);
        simulerLivraison(robot2, "Moniteur cardiaque", "CARDIOLOGIE Salle C3");
        sleep(600);
        simulerLivraison(robot2, "Kit perfusion", "PNEUMOLOGIE Salle P2");
        sleep(400);

        // ── PHASE 5 : Triage ESI 4 & 5 par RT-ALPHA ─────────────────
        ui.addLog("━━━ PHASE 4 — Fin de triage ESI 4 & 5 ━━━", LogType.INFO);
        trierPatient((RobotTriage) robot1, findScenario("PAT-004"), 1); // ESI4
        sleep(400);
        trierPatient((RobotTriage) robot1, findScenario("PAT-006"), 1); // ESI5
        sleep(500);

        // ── PHASE 6 : Rapport final + polymorphisme alerteGenerale ───
        sleep(800);
        ui.addLog("━━━ TEST ALERTE GÉNÉRALE — polymorphisme arreter() ━━━", LogType.WARNING);
        ui.addLog("[Gestionnaire] Déclenchement alerteGenerale()...", LogType.WARNING);
        gestionnaire.alerteGenerale();
        ui.addLog("[Gestionnaire] RT-ALPHA & RT-GAMMA maintenus (triage)", LogType.WARNING);
        ui.addLog("[Gestionnaire] RL-BETA arrêté (activité secondaire)", LogType.WARNING);
        ui.updateRobotSidebar(2, robot2.getEnergie(), false, robot2.isConnecte(), false, robot2.isEnMarche());
        sleep(600);

        // Redémarrage RL-BETA après alerte
        try { robot2.demarrer(); } catch (RobotException ignored) {}
        ui.addLog("[Gestionnaire] RL-BETA redémarré après alerte", LogType.SUCCESS);
        ui.updateRobotSidebar(2, robot2.getEnergie(), false, robot2.isConnecte(), false, true);
        sleep(500);

        // ── PHASE 7 : Rapport final flotte ───────────────────────────
        ui.addLog("━━━ Rapport final de la flotte ━━━", LogType.SUCCESS);
        String rapportFinal = gestionnaire.rapportFlotte();
        for (String ligne : rapportFinal.split("\n")) {
            ui.addLog(ligne, LogType.INFO);
        }

        // Retour des robots à la station
        retourStation(robot1, 1);
        sleep(300);
        retourStation(robot3, 3);
        sleep(300);

        salleState.robotPatientId  = null;
        salleState.robot2PatientId = null;
        ui.updateSalle(salleState);
        ui.addLog("━━━ Simulation flotte complète ✓ ━━━", LogType.SUCCESS);
    }

    // ═════════════════════════════════════════════════════════════════
    //  TRIAGE D'UN PATIENT (polymorphisme effectuerTache via RobotTriage)
    // ═════════════════════════════════════════════════════════════════

    private void trierPatient(RobotTriage robot, ScenarDef sc, int robotUiId) {
        if (sc == null || alerteActive) return;

        String pid = sc.patientId;
        PatientMarker pm = patientMarker(pid);

        // Déplacement vers le patient
        ui.addLog("[" + robot.getId() + "] → Déplacement vers " + pid, LogType.INFO);
        animerDeplacement(robot, sc.px, sc.py, robotUiId);
        salleState.robotPatientId = (robotUiId == 1) ? pid : salleState.robotPatientId;
        salleState.robot2PatientId = (robotUiId == 3) ? pid : salleState.robot2PatientId;
        if (pm != null) pm.enCours = true;
        ui.highlightPatient(pid);
        ui.updateSalle(salleState);
        sleep(400);

        // Phase scan
        CameraState cam = new CameraState();
        cam.robotId  = robotUiId;
        cam.patientId = pid;
        cam.phase    = CameraState.Phase.BODY_SCAN;
        cam.vision   = sc.vision;
        cam.signes   = sc.signes;
        cam.scenario = sc;
        ui.updateCamera(cam);
        sleep(300);

        // Calcul ESI
        NiveauESI esi = calculerESI(sc);
        cam.phase     = CameraState.Phase.ESI_RESULT;
        cam.esiResult = esi;
        ui.updateCamera(cam);
        ui.updateBodyScan(sc, esi);
        ui.updateVitaux(sc, esi);
        ui.updateHeader(esi);

        // Logs résultat
        String esiTag = esi.name().replace("ESI_", "");
        Color logColor = esi == NiveauESI.ESI_1 ? null : null;
        LogType lt = esi == NiveauESI.ESI_1 ? LogType.CRITICAL
                   : esi == NiveauESI.ESI_2 ? LogType.WARNING
                   : LogType.SUCCESS;
        ui.addLog("[" + robot.getId() + "] " + pid + " → ESI " + esiTag +
                  " — " + esi.getDescription(), lt);

        if (esi == NiveauESI.ESI_1) {
            ui.addLog("[" + robot.getId() + "] 🚨 CODE BLUE — " + sc.desc, LogType.CRITICAL);
            salleState.codeBlue = true;
            ui.updateKit(robotUiId, robot.getKitUrgence());
            robot.getKitUrgence().utiliserPartiel(3);
            ui.updateKit(robotUiId, robot.getKitUrgence());
        } else {
            robot.getKitUrgence().utiliserPartiel(esi == NiveauESI.ESI_2 ? 2 : 1);
            ui.updateKit(robotUiId, robot.getKitUrgence());
        }

        // Dossier médical
        if (sc.dossier != null) {
            ui.showDossier(pid, sc.dossier, esi, sc.departement, sc.salle);
        }

        // Mise à jour file
        fileAttente.removeIf(p -> p.id.equals(pid));
        fileAttente.add(new PatientTriage(pid, sc.desc, esi));
        java.util.Collections.sort(fileAttente);
        ui.updateFile(new ArrayList<>(fileAttente));

        // Marqueur patient traité
        if (pm != null) { pm.esi = esi; pm.enCours = false; pm.traite = true; }
        ui.updateSalle(salleState);

        // Énergie
        try { robot.consommerEnergie(8); } catch (Exception ignored) {}
        ui.updateRobotSidebar(robotUiId, robot.getEnergie(),
                robot instanceof RobotTriage && ((RobotTriage)robot).isModeDegrade(),
                robot.isConnecte(), robot.getKitUrgence().estComplet(), true);
        sleep(200);
    }

    // ═════════════════════════════════════════════════════════════════
    //  LIVRAISON (polymorphisme effectuerTache via RobotLivraison)
    // ═════════════════════════════════════════════════════════════════

    private void simulerLivraison(RobotLivraison robot, String colis, String destination) {
        ui.addLog("[" + robot.getId() + "] 📦 Chargement : " + colis + " → " + destination, LogType.INFO);
        try {
            robot.chargerColis(colis, destination);
        } catch (RobotException e) {
            ui.addLog("[" + robot.getId() + "] ⚠ " + e.getMessage(), LogType.WARNING);
            return;
        }

        // Animation déplacement robot 2 (RL-BETA)
        float destX = 0.30f + (float)(Math.random() * 0.50f);
        float destY = 0.30f + (float)(Math.random() * 0.40f);
        animerDeplacement2(robot, destX, destY);

        // Livraison simulée
        try {
            int dx = (int)(destX * 30);
            int dy = (int)(destY * 30);
            robot.faireLivraison(dx, dy);
        } catch (RobotException e) {
            ui.addLog("[" + robot.getId() + "] ⚠ Livraison échouée : " + e.getMessage(), LogType.WARNING);
            return;
        }

        ui.addLog("[" + robot.getId() + "] ✓ Livraison effectuée → " + destination, LogType.SUCCESS);
        ui.updateRobotSidebar(2, robot.getEnergie(), false, robot.isConnecte(), false, true);
        sleep(200);
    }

    // ═════════════════════════════════════════════════════════════════
    //  RETOUR STATION
    // ═════════════════════════════════════════════════════════════════

    private void retourStation(Robot robot, int robotUiId) {
        ui.addLog("[" + robot.getId() + "] Retour station de recharge", LogType.INFO);
        float startX = (robotUiId == 1) ? START_X1 : START_X3;
        float startY = (robotUiId == 1) ? START_Y1 : START_Y3;
        animerDeplacement(robot, startX, startY, robotUiId);
        robot.recharger(100 - robot.getEnergie());
        if (robot instanceof RobotTriage) {
            ((RobotTriage) robot).getKitUrgence().reapprovisionner();
            ui.updateKit(robotUiId, ((RobotTriage) robot).getKitUrgence());
        }
        ui.updateRobotSidebar(robotUiId, robot.getEnergie(),
                robot instanceof RobotTriage && ((RobotTriage)robot).isModeDegrade(),
                robot instanceof RobotConnecte && ((RobotConnecte)robot).isConnecte(),
                robot instanceof RobotTriage && ((RobotTriage)robot).getKitUrgence().estComplet(),
                true);
        ui.addLog("[" + robot.getId() + "] ✓ Rechargé à 100%", LogType.SUCCESS);
    }

    // ═════════════════════════════════════════════════════════════════
    //  ANIMATIONS DÉPLACEMENT
    // ═════════════════════════════════════════════════════════════════

    private void animerDeplacement(Robot robot, float tx, float ty, int robotUiId) {
        float sx, sy;
        if (robotUiId == 1) { sx = salleState.robotX1; sy = salleState.robotY1; }
        else                { sx = salleState.robotX1; sy = salleState.robotY1; } // RT-GAMMA partage le canal visuel robot1 dans cet affichage simplifié

        int steps = 20;
        for (int i = 1; i <= steps; i++) {
            float fx = sx + (tx - sx) * i / steps;
            float fy = sy + (ty - sy) * i / steps;
            if (robotUiId == 1) { salleState.robotX1 = fx; salleState.robotY1 = fy; }
            else                { salleState.robotX1 = fx; salleState.robotY1 = fy; }
            ui.updateSalle(salleState);
            sleep(40);
        }
    }

    private void animerDeplacement2(RobotLivraison robot, float tx, float ty) {
        float sx = salleState.robotX2, sy = salleState.robotY2;
        int steps = 20;
        for (int i = 1; i <= steps; i++) {
            salleState.robotX2 = sx + (tx - sx) * i / steps;
            salleState.robotY2 = sy + (ty - sy) * i / steps;
            ui.updateSalle(salleState);
            sleep(35);
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  CALCUL ESI
    // ═════════════════════════════════════════════════════════════════

    private NiveauESI calculerESI(ScenarDef sc) {
        ResultatVision v = sc.vision;
        SignesVitaux   s = sc.signes;
        if (v.isSaignementMassif() || v.isAmputationDetectee() || v.isInconscient()) return NiveauESI.ESI_1;
        if (s.getSpo2() < 90 || s.getPouls() < 50) return NiveauESI.ESI_1;
        if (s.getTensionSystole() > 160 || s.getPouls() > 120) return NiveauESI.ESI_2;
        if (s.getTemperature() > 38.5 || s.getSpo2() < 95) return NiveauESI.ESI_3;
        if (s.getTemperature() > 37.5) return NiveauESI.ESI_4;
        return NiveauESI.ESI_5;
    }

    // ═════════════════════════════════════════════════════════════════
    //  COMMANDES UI
    // ═════════════════════════════════════════════════════════════════

    void toggleDegrade() {
        if (robot1.isModeDegrade()) {
            try {
                robot1.desactiverModeDegrade("WIFI-HOPITAL-5G");
                ui.addLog("[RT-ALPHA] Mode normal rétabli", LogType.SUCCESS);
            } catch (RobotException e) {
                ui.addLog("[ERREUR] " + e.getMessage(), LogType.CRITICAL);
            }
        } else {
            robot1.activerModeDegrade();
            ui.addLog("[RT-ALPHA] ⚠ Mode dégradé activé", LogType.WARNING);
        }
        ui.updateRobotSidebar(1, robot1.getEnergie(), robot1.isModeDegrade(),
                robot1.isConnecte(), robot1.getKitUrgence().estComplet(), true);
    }

    void declencherAlerteGenerale() {
        ui.addLog("[Gestionnaire] 🚨 ALERTE GÉNÉRALE — arrêt flotte", LogType.CRITICAL);
        alerteActive = true;
        gestionnaire.alerteGenerale();
        ui.addLog("[Gestionnaire] Robots secondaires arrêtés", LogType.WARNING);
        ui.updateRobotSidebar(2, robot2.getEnergie(), false, robot2.isConnecte(), false, robot2.isEnMarche());
        ui.updateRobotSidebar(3, robot3.getEnergie(), false, robot3.isConnecte(),
                robot3.getKitUrgence().estComplet(), robot3.isEnMarche());
    }

    void forceRecharge() {
        robot1.recharger(100); robot2.recharger(100); robot3.recharger(100);
        if (!robot2.isEnMarche()) { try { robot2.demarrer(); } catch (RobotException ignored) {} }
        if (!robot3.isEnMarche()) { try { robot3.demarrer(); } catch (RobotException ignored) {} }
        alerteActive = false;
        ui.addLog("[Gestionnaire] ⚡ Recharge forcée — tous robots 100%", LogType.SUCCESS);
        ui.updateRobotSidebar(1, 100, robot1.isModeDegrade(), robot1.isConnecte(), robot1.getKitUrgence().estComplet(), true);
        ui.updateRobotSidebar(2, 100, false, robot2.isConnecte(), false, true);
        ui.updateRobotSidebar(3, 100, robot3.isModeDegrade(), robot3.isConnecte(), robot3.getKitUrgence().estComplet(), true);
    }

    void afficherRapportFlotte() {
        String rapport = gestionnaire.rapportFlotte();
        ui.addLog("━━━ RAPPORT FLOTTE ━━━", LogType.SUCCESS);
        for (String ligne : rapport.split("\n")) {
            ui.addLog(ligne, LogType.INFO);
        }
    }

    void onRobotClicked(int robotId) {
        // Afficher le scan, la caméra et les données du patient scanné par ce robot
        String pid = (robotId == 1) ? salleState.robotPatientId : salleState.robot2PatientId;
        Robot robot = (robotId == 1) ? robot1 : (robotId == 2 ? robot2 : robot3);

        if (robot == null) return;

        ui.addLog("[Clic] Robot " + robot.getId() + " sélectionné", LogType.INFO);

        if (pid == null || pid.isEmpty()) {
            // Robot sans patient actif — afficher état du robot
            ui.addLog("[" + robot.getId() + "] Aucun patient en cours de scan", LogType.INFO);
            ui.addLog("[" + robot.getId() + "] Énergie : " + robot.getEnergie() + "% | Statut : " + (robot.isEnMarche() ? "EN MARCHE" : "ARRÊTÉ"), LogType.INFO);
            // Réinitialiser la caméra pour montrer l'état robot
            CameraState cam = new CameraState();
            cam.robotId   = robotId;
            cam.patientId = null;
            cam.phase     = CameraState.Phase.BODY_SCAN;
            ui.updateCamera(cam);
            return;
        }

        ScenarDef sc = findScenario(pid);
        if (sc == null) return;

        PatientMarker pm = patientMarker(pid);
        NiveauESI esi = (pm != null && pm.esi != null) ? pm.esi : NiveauESI.ESI_5;

        ui.addLog("[" + robot.getId() + "] Affichage scan patient " + pid, LogType.INFO);

        // Afficher caméra
        CameraState cam = new CameraState();
        cam.robotId   = robotId;
        cam.patientId = pid;
        cam.phase     = CameraState.Phase.ESI_RESULT;
        cam.vision    = sc.vision;
        cam.signes    = sc.signes;
        cam.scenario  = sc;
        cam.esiResult = esi;
        ui.updateCamera(cam);

        // Afficher scan corporel
        ui.updateBodyScan(sc, esi);

        // Afficher vitaux
        ui.updateVitaux(sc, esi);

        // Afficher dossier
        if (sc.dossier != null) {
            ui.showDossier(pid, sc.dossier, esi, sc.departement, sc.salle);
        }

        // Mettre en surbrillance le patient
        salleState.selectedPatientId = pid;
        ui.highlightPatient(pid);
    }

    void onPatientClicked(String pid) {
        ScenarDef sc = findScenario(pid);
        if (sc == null) return;
        PatientMarker pm = patientMarker(pid);
        NiveauESI esi = pm != null ? pm.esi : NiveauESI.ESI_5;
        ui.addLog("[Clic] Patient " + pid + " sélectionné", LogType.INFO);
        if (sc.dossier != null) ui.showDossier(pid, sc.dossier, esi, sc.departement, sc.salle);
    }

    void consulterDossier(String code) {
        String pid = salleState.robotPatientId;
        if (pid == null || pid.isEmpty()) {
            if (ui.cmdPanel != null) ui.cmdPanel.setStatus("Aucun patient actif", Dashboard2.C_AMBER);
            return;
        }
        try {
            DossierMedical d = robot1.consulterDossierSecurise(code, pid);
            if (d != null) {
                ui.addLog("[SÉCURITÉ] ✔ Accès dossier " + pid + " autorisé", LogType.SUCCESS);
                if (ui.cmdPanel != null) ui.cmdPanel.setStatus("Dossier " + pid + " ✔", Dashboard2.C_GREEN);
                ScenarDef sc = findScenario(pid);
                if (sc != null) ui.showDossier(pid, d, null, sc.departement, sc.salle);
            }
        } catch (AuthenticationException e) {
            ui.addLog("[SÉCURITÉ] ✖ Accès refusé : " + e.getMessage(), LogType.CRITICAL);
            if (ui.cmdPanel != null) ui.cmdPanel.setStatus("✖ ACCÈS REFUSÉ", Dashboard2.C_RED);
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════

    private void initialiserChaises() {
        String[] patientsId = {"PAT-001","PAT-002","PAT-003",null,"PAT-004",null,"PAT-005",null,"PAT-006",null};
        for (float[] pos : CHAISES_POSITIONS) {
            salleState.chaises.add(new ChairMarker(pos[0], pos[1], false, null, false));
        }
        for (int i = 0; i < patientsId.length; i++) {
            String pid = patientsId[i];
            if (pid == null) continue;
            ScenarDef sc = findScenario(pid);
            if (sc == null) continue;
            ChairMarker ch = salleState.chaises.get(i);
            ch.occupee = true; ch.occupantId = pid; ch.occupantEstPatient = true;
            sc.px = ch.x + 0.01f; sc.py = ch.y;
            int accomp = sc.vision.isInconscient() ? 2 : (pid.equals("PAT-003") ? 1 : 0);
            PatientMarker pm = new PatientMarker(pid, sc.px, sc.py, accomp);
            pm.esi = NiveauESI.ESI_5;
            salleState.patients.add(pm);
            fileAttente.add(new PatientTriage(pid, sc.desc, NiveauESI.ESI_5));
            if (accomp >= 1 && i + 1 < salleState.chaises.size() && !salleState.chaises.get(i+1).occupee) {
                ChairMarker ca = salleState.chaises.get(i + 1);
                ca.occupee = true; ca.occupantId = "ACC-" + pid; ca.occupantEstPatient = false;
            }
        }
    }

    PatientMarker patientMarker(String id) {
        for (PatientMarker pm : salleState.patients) if (id.equals(pm.id)) return pm;
        return null;
    }

    ScenarDef findScenario(String pid) {
        for (ScenarDef s : TOUS_SCENARIOS) if (s.patientId.equals(pid)) return s;
        return null;
    }

    void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }
}
