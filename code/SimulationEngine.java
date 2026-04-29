package projetrobot;

import javax.swing.*;
import java.util.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Moteur de simulation v4 – 1 robot RT-ALPHA, 6 patients.
 *
 * Scénarios : 2×ESI1, 1×ESI2, 1×ESI3, 1×ESI4, 1×ESI5
 * Les patients arrivent progressivement par l'accueil.
 * Les infirmiers se déplacent de l'entrée vers la PORTE SOINS.
 * La caméra est synchronisée avec les données réelles du scénario.
 */
class SimulationEngine {

    private final Dashboard ui;

    // ── Robot unique ──────────────────────────────────────────────
    RobotTriage robot1; // RT-ALPHA

    // ── Labels sidebar ────────────────────────────────────────────
    JLabel lblMission, lblMission2; // lblMission2 stub
    JLabel lblStatusBadge1, lblStatusBadge2; // lblStatusBadge2 stub

    // ── État global ───────────────────────────────────────────────
    final SalleState salleState = new SalleState();

    // ── File ──────────────────────────────────────────────────────
    private final List<PatientTriage> fileAttente  = new ArrayList<>();
    private final List<ScenarDef>     triageQueue  = new ArrayList<>();

    // ── Zone de recharge ──────────────────────────────────────────
    static final float RECHARGE_X = 0.10f;
    static final float RECHARGE_Y = 0.90f;

    // ── Position de départ ────────────────────────────────────────
    static final float START_X = 0.04f, START_Y = 0.88f;

    // ── Code sécurité ─────────────────────────────────────────────
    static final String CODE_MEDECIN = "MED-2025";

    // ── Compteur infirmier ────────────────────────────────────────
    private int infirmierCount = 0;

    // ═══════════════════════════════════════════════════════════════
    //  6 SCÉNARIOS : 2×ESI1, 1×ESI2, 1×ESI3, 1×ESI4, 1×ESI5
    // ═══════════════════════════════════════════════════════════════

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

    // ── Obstacles fixes ───────────────────────────────────────────
    static final ObstacleItem[] OBSTACLES_FIXES = {
        new ObstacleItem(0.55f,0.50f,"chariot",false),
        new ObstacleItem(0.45f,0.72f,"chariot",false),
    };

    // ── Chaises de la salle d'attente ─────────────────────────────
    static final float[][] CHAISES_POSITIONS = {
        {0.28f,0.22f},{0.38f,0.22f},{0.50f,0.22f},{0.62f,0.22f},{0.72f,0.22f},
        {0.28f,0.58f},{0.38f,0.58f},{0.50f,0.58f},{0.62f,0.58f},{0.72f,0.58f},
    };

    // ═══════════════════════════════════════════════════════════════
    SimulationEngine(Dashboard ui){ this.ui=ui; }

    // ═══════════════════════════════════════════════════════════════
    //  DÉMARRAGE
    // ═══════════════════════════════════════════════════════════════

    void start(){
        try{
            robot1=new RobotTriage("RT-ALPHA",0,0);
            robot1.demarrer();
            robot1.connecter("WIFI-HOPITAL-5G");
            for(ScenarDef sc:TOUS_SCENARIOS)
                if(sc.dossier!=null)robot1.enregistrerDossier(sc.dossier);
        } catch(RobotException e){
            ui.addLog("[ERREUR INIT] "+e.getMessage(),LogType.CRITICAL);
        }

        initialiserChaises();
        salleState.obstacles.addAll(java.util.Arrays.asList(OBSTACLES_FIXES));
        salleState.robotX1=START_X; salleState.robotY1=START_Y;
        salleState.robot1EnMarche=true;

        ui.addLog("━━━ SYSTÈME DÉMARRÉ — RT-ALPHA actif ━━━",LogType.SUCCESS);
        ui.addLog("Connexion réseau WIFI-HOPITAL-5G établie",LogType.SUCCESS);
        ui.addLog("6 patients en cours d'arrivée (2×ESI1, 1×ESI2, 1×ESI3, 1×ESI4, 1×ESI5)",LogType.INFO);
        ui.updateSalle(salleState);
        ui.updateRobotSidebar(1,100,false,true,true,true);
        ui.updateFile(new ArrayList<>(fileAttente));

        new Thread(this::cycleComplet,"CycleTriage").start();
    }

    // ═══════════════════════════════════════════════════════════════
    //  CYCLE COMPLET
    // ═══════════════════════════════════════════════════════════════

    private void cycleComplet(){
        sleep(800);

        ui.addLog("━━━ Tous les patients sont déjà en salle — Début triage RT-ALPHA ━━━",LogType.SUCCESS);

        // ── Triage séquentiel par ordre de priorité ────────────────
        // Classer par priorité ESI
        List<ScenarDef> ordonnes=new ArrayList<>(java.util.Arrays.asList(TOUS_SCENARIOS));
        // ESI 1 en premier (déjà dans l'ordre)
        for(ScenarDef sc:ordonnes){
            trierPatient(sc);
            sleep(500);
        }

        // ── Retour zone de recharge ────────────────────────────────
        ui.addLog("━━━ Triage complet — Retour zone de recharge ━━━",LogType.SUCCESS);
        retourZoneRecharge();

        // ── Fin ───────────────────────────────────────────────────
        sleep(1500);
        ui.addLog("━━━ RT-ALPHA — Toutes les tâches complétées ✓ ━━━",LogType.SUCCESS);
        salleState.robot1EnMarche=false;
        salleState.robotStatus1="ARRÊTÉ";
        ui.updateSalle(salleState);
        ui.updateRobotSidebar(1,robot1.getEnergie(),false,true,robot1.getKitUrgence().estComplet(),false);
        if(lblMission!=null)SwingUtilities.invokeLater(()->lblMission.setText("■ ARRÊTÉ"));

        CameraState cam=new CameraState();cam.phase=CameraState.Phase.STOPPED;
        ui.updateCamera(cam);
    }
    // ═══════════════════════════════════════════════════════════════
    //  TRIAGE D'UN PATIENT
    // ═══════════════════════════════════════════════════════════════

    private synchronized void trierPatient(ScenarDef sc){
        String pid=sc.patientId;
        ui.addLog("━━━ [RT-ALPHA] "+sc.nom+" — "+sc.desc+" ━━━",LogType.INFO);
        ui.addLog("[RT-ALPHA] TÂCHE : Déplacement vers "+pid,LogType.INFO);

        PatientMarker pm=patientMarker(pid);
        if(pm!=null)pm.enCours=true;
        salleState.robotPatientId=pid;

        if(lblMission!=null)SwingUtilities.invokeLater(()->lblMission.setText("→ Triage "+pid));

        // ── 1. Déplacement ─────────────────────────────────────────
        salleState.robotStatus1="→ "+pid;
        CameraState cam=new CameraState();
        cam.robotId=1;cam.phase=CameraState.Phase.MOVING_CLEAR;
        cam.patientId=pid;cam.scenario=sc;cam.signes=sc.signes;
        ui.updateCamera(cam);

        animerDeplacementRobot(sc.px,sc.py,pid,true);
        sleep(250);

        // ── 2. Infirmier si ESI critique ──────────────────────────
        NiveauESI estimee=estimerESI(sc);
        if(estimee==NiveauESI.ESI_1||estimee==NiveauESI.ESI_2){
            lancerInfirmierVersPorteSoins(pid,sc.px,sc.py);
        }

        // ── 3. Empreinte ─────────────────────────────────────────
        ui.addLog("[RT-ALPHA] TÂCHE : Authentification biométrique "+pid,LogType.INFO);
        cam.phase=CameraState.Phase.THUMB_SCAN;ui.updateCamera(cam);
        sleep(900);
        ui.addLog("[RT-ALPHA] ✔ Empreinte validée — patient identifié",LogType.SUCCESS);

        // ── 4. Scan corporel ─────────────────────────────────────
        ui.addLog("[RT-ALPHA] TÂCHE : Analyse corporelle YOLO + HSV en cours...",LogType.INFO);
        cam.phase=CameraState.Phase.BODY_SCAN;cam.vision=sc.vision;
        ui.updateCamera(cam);sleep(800);
        logVision(sc,pid);

        // ── 5. Mesure constantes (données réelles du scénario) ───
        ui.addLog("[RT-ALPHA] TÂCHE : Mesure constantes vitales...",LogType.INFO);
        cam.phase=CameraState.Phase.MEASURING;cam.signes=sc.signes;
        ui.updateCamera(cam);sleep(900);
        ui.addLog("[RT-ALPHA] ✔ ♥ "+sc.signes.getPouls()+"bpm | SpO2 "+sc.signes.getSpo2()+
            "% | Temp "+String.format("%.1f",sc.signes.getTemperature())+
            "°C | Tension "+sc.signes.getTensionSystole()+"/"+sc.signes.getTensionDiastole()+" mmHg",LogType.INFO);

        // ── 6. Dossier médical ───────────────────────────────────
        if(sc.modeDegrade){
            robot1.activerModeDegrade();
            ui.addLog("[RT-ALPHA] TÂCHE : ⚠ MODE DÉGRADÉ — Triage autonome",LogType.WARNING);
        } else if(sc.dossier!=null){
            ui.addLog("[RT-ALPHA] TÂCHE : Chargement dossier — "+sc.dossier.getAge()+" ans, "+sc.dossier.getCategories(),LogType.INFO);
        } else {
            ui.addLog("[RT-ALPHA] TÂCHE : Aucun dossier — nouveau patient",LogType.INFO);
        }
        sleep(350);

        // ── 7. Calcul ESI ────────────────────────────────────────
        ui.addLog("[RT-ALPHA] TÂCHE : Calcul niveau ESI...",LogType.INFO);
        NiveauESI niveau=NiveauESI.ESI_5;
        AnalyseurUrgence analyseur=new AnalyseurUrgence();
        try{
            niveau=analyseur.calculerNiveauESI(sc.signes,sc.vision,sc.dossier,sc.modeDegrade,pid);
            ui.addLog("[RT-ALPHA] ✔ ESI calculé : "+niveau+" — "+niveau.getDescription(),LogType.SUCCESS);
        } catch(UrgenceVitaleException e){
            niveau=NiveauESI.ESI_1;
            ui.addLog("[RT-ALPHA] 🚨 CODE BLUE ! "+e.getMessage(),LogType.CRITICAL);
            salleState.codeBlue=true;ui.updateHeader(NiveauESI.ESI_1);
            deployerKit(pid);sleep(500);salleState.codeBlue=false;
        }

        // ── 8. Consommation kit ──────────────────────────────────
        consumeKitPartial(robot1,niveau);
        ui.updateKit(1,robot1.getKitUrgence());

        // ── 9. Mise à jour UI — caméra synchronisée ──────────────
        final NiveauESI nFinal=niveau;
        cam.phase=CameraState.Phase.ESI_RESULT;cam.esiResult=nFinal;
        cam.signes=sc.signes;cam.vision=sc.vision;cam.scenario=sc;
        ui.updateCamera(cam);
        ui.updateBodyScan(sc,nFinal);
        ui.updateVitaux(sc,nFinal);
        ui.updateHeader(nFinal);

        if(pm!=null){pm.esi=nFinal;pm.enCours=false;pm.traite=true;}
        salleState.robotPatientId=null;

        // Libérer chaise si traité (ESI 1/2 → porte soins)
        if(nFinal==NiveauESI.ESI_1||nFinal==NiveauESI.ESI_2){
            for(ChairMarker ch:salleState.chaises){
                if(pid.equals(ch.occupantId)){ch.occupee=false;ch.occupantId=null;break;}
            }
        }

        fileAttente.removeIf(p->p.id.equals(pid));
        fileAttente.add(new PatientTriage(pid,sc.desc,nFinal));
        Collections.sort(fileAttente);
        ui.updateFile(new ArrayList<>(fileAttente));
        ui.updateSalle(salleState);

        // ── 10. Orientation ──────────────────────────────────────
        ui.addLog("[RT-ALPHA] TÂCHE : Orientation → "+sc.departement+" — "+sc.salle,LogType.INFO);
        logOrientation(nFinal,pid);
        ui.addLog("[RT-ALPHA] TÂCHE : 📡 Dossier transmis → "+sc.departement+", "+sc.salle,LogType.SUCCESS);
        ui.showDossier(pid,sc.dossier,nFinal,sc.departement,sc.salle);
        sleep(350);

        if(!robot1.getKitUrgence().estComplet())
            ui.addLog("[RT-ALPHA] ⚠ Kit partiellement épuisé — réapprovisionnement à la station",LogType.WARNING);
        if(sc.modeDegrade){
            try{robot1.desactiverModeDegrade("WIFI-HOPITAL-5G");}catch(RobotException ignored){}
            ui.addLog("[RT-ALPHA] ✔ Réseau rétabli",LogType.SUCCESS);
        }
        ui.updateRobotSidebar(1,robot1.getEnergie(),robot1.isModeDegrade(),robot1.isConnecte(),robot1.getKitUrgence().estComplet(),true);
        sleep(600);
    }

    // ═══════════════════════════════════════════════════════════════
    //  RETOUR ZONE DE RECHARGE
    // ═══════════════════════════════════════════════════════════════

    private void retourZoneRecharge(){
        ui.addLog("[RT-ALPHA] → Zone de recharge & réapprovisionnement",LogType.INFO);
        salleState.robotStatus1="→ RECHARGE";ui.updateSalle(salleState);

        CameraState cam=new CameraState();cam.phase=CameraState.Phase.RECHARGING;ui.updateCamera(cam);
        animerDeplacementRobot(RECHARGE_X,RECHARGE_Y,null,false);

        List<ElementKit> manquants=robot1.getKitUrgence().getManquants();
        if(!manquants.isEmpty()){
            ui.addLog("[RT-ALPHA] 📋 RAPPORT KIT — Infirmiers notifiés :",LogType.WARNING);
            for(ElementKit e:manquants)ui.addLog("   À réapprovisionner : "+e.getLabel(),LogType.WARNING);
        }

        salleState.robotStatus1="⚡ EN RECHARGE";ui.updateSalle(salleState);
        ui.addLog("[RT-ALPHA] ⚡ Recharge batterie + réapprovisionnement...",LogType.INFO);
        sleep(2000);

        robot1.getKitUrgence().reapprovisionner();
        ui.addLog("[RT-ALPHA] ✔ Kit réapprovisionné — batterie 100%",LogType.SUCCESS);
        ui.updateKit(1,robot1.getKitUrgence());
        ui.updateRobotSidebar(1,100,robot1.isModeDegrade(),robot1.isConnecte(),true,true);
        salleState.robotStatus1="RECHARGÉ ✓";ui.updateSalle(salleState);
        sleep(400);
    }

    // ═══════════════════════════════════════════════════════════════
    //  INFIRMIER : entrée → patient → porte soins
    // ═══════════════════════════════════════════════════════════════

    private void lancerInfirmierVersPorteSoins(String pid, float patX, float patY){
        new Thread(()->{
            // Créer infirmier à l'entrée
            InfirmierMarker inf=new InfirmierMarker(0.06f,0.50f,pid);
            synchronized(salleState.infirmiers){salleState.infirmiers.add(inf);}
            ui.addLog("[INFIRMIER] 🏃 Court vers "+pid+" (depuis ENTRÉE)",LogType.WARNING);

            // Phase 1 : entrée → patient
            float sx=inf.x,sy=inf.y;
            int steps=28;
            for(int i=1;i<=steps;i++){
                float t=(float)i/steps;inf.x=sx+(patX-sx)*t;inf.y=sy+(patY-sy)*t;
                ui.updateSalle(salleState);sleep(80);
            }
            ui.addLog("[INFIRMIER] ✔ Auprès du patient "+pid+" — kit déployé",LogType.SUCCESS);
            sleep(1200);

            // Phase 2 : patient → porte soins
            inf.target="PORTE_SOINS";
            ui.addLog("[INFIRMIER] 🏃 Conduit "+pid+" vers 🚪 PORTE SOINS",LogType.INFO);
            float px2=inf.x,py2=inf.y;
            float destX=SallePanel.PORTE_SOINS_X-0.03f, destY=SallePanel.PORTE_SOINS_Y;
            for(int i=1;i<=steps;i++){
                float t=(float)i/steps;inf.x=px2+(destX-px2)*t;inf.y=py2+(destY-py2)*t;
                ui.updateSalle(salleState);sleep(40);
            }
            ui.addLog("[INFIRMIER] ✔ Patient "+pid+" transféré vers soins",LogType.SUCCESS);
            sleep(600);

            // Retirer infirmier de la carte
            inf.actif=false;
            synchronized(salleState.infirmiers){salleState.infirmiers.remove(inf);}
            ui.updateSalle(salleState);
        },"Infirmier-"+pid).start();
    }

    // ═══════════════════════════════════════════════════════════════
    //  ANALYSE SUR CLIC PATIENT
    // ═══════════════════════════════════════════════════════════════

    void onPatientClicked(String patientId){
        // Juste mettre à jour l'affichage (les détails sont dans FilePanel)
        ScenarDef sc=findScenario(patientId);
        if(sc==null)return;
        PatientMarker pm=patientMarker(patientId);
        NiveauESI esiActuel=pm!=null?pm.esi:NiveauESI.ESI_5;
        ui.addLog("[OPÉRATEUR] Sélection patient "+patientId+" — détails affichés dans la file",LogType.INFO);
        ui.highlightPatient(patientId);
        // Mettre à jour vitaux et scan avec les données réelles
        if(pm!=null&&pm.esi!=null){
            ui.updateBodyScan(sc,esiActuel);
            ui.updateVitaux(sc,esiActuel);
        }
    }

    void analyserPatientSurClic(String patientId){
        ScenarDef sc=findScenario(patientId);
        if(sc==null){ui.addLog("[ERREUR] Scénario introuvable pour "+patientId,LogType.WARNING);return;}
        String nom="RT-ALPHA";
        ui.addLog("[OPÉRATEUR] Ré-analyse demandée pour "+patientId+" — "+nom+" envoyé",LogType.INFO);
        ui.highlightPatient(patientId);

        final ScenarDef scF=sc;
        new Thread(()->{
            if(salleState.robotPatientId!=null){
                ui.addLog("[RT-ALPHA] Robot occupé — ré-analyse en file",LogType.WARNING);
                sleep(2000);
            }
            salleState.robotStatus1="→ "+patientId+" (ré-analyse)";
            animerDeplacementRobot(scF.px,scF.py,patientId,true);
            sleep(300);

            ui.addLog("[RT-ALPHA] 🔍 Ré-analyse caméra de "+patientId,LogType.INFO);
            CameraState cam=new CameraState();
            cam.robotId=1;cam.phase=CameraState.Phase.BODY_SCAN;
            cam.vision=scF.vision;cam.patientId=patientId;
            cam.signes=scF.signes;cam.scenario=scF;
            ui.updateCamera(cam);sleep(900);

            cam.phase=CameraState.Phase.MEASURING;ui.updateCamera(cam);sleep(800);

            cam.phase=CameraState.Phase.ESI_RESULT;
            PatientMarker pm2=patientMarker(patientId);
            cam.esiResult=pm2!=null?pm2.esi:NiveauESI.ESI_5;
            cam.signes=scF.signes;cam.vision=scF.vision;cam.scenario=scF;
            ui.updateCamera(cam);
            ui.updateBodyScan(scF,cam.esiResult);
            ui.updateVitaux(scF,cam.esiResult);

            ui.addLog("[RT-ALPHA] ✔ Ré-analyse terminée — état "+patientId+" confirmé : "+cam.esiResult,LogType.SUCCESS);
            salleState.robotStatus1="Ré-analyse OK";ui.updateSalle(salleState);
        },"ReAnalyse").start();
    }

    // ═══════════════════════════════════════════════════════════════
    //  ANIMATION DÉPLACEMENT ROBOT
    // ═══════════════════════════════════════════════════════════════

    private void animerDeplacementRobot(float tx, float ty, String targetPid, boolean checkObs){
        float sx=salleState.robotX1, sy=salleState.robotY1;
        List<float[]> chemin=calculerChemin(sx,sy,tx,ty);
        salleState.chemin1=chemin;

        CameraState cam=new CameraState();cam.robotId=1;
        cam.patientId=targetPid!=null?targetPid:"";
        ScenarDef sc=targetPid!=null?findScenario(targetPid):null;
        if(sc!=null){cam.scenario=sc;cam.signes=sc.signes;cam.vision=sc.vision;}

        for(int seg=1;seg<chemin.size();seg++){
            float[] from=chemin.get(seg-1),to=chemin.get(seg);
            boolean estDetour=seg>1&&chemin.size()>2&&seg<chemin.size()-1;
            if(estDetour&&checkObs){
                ObstacleItem obst=obstacleProcheDu(from[0],from[1]);
                cam.phase=CameraState.Phase.MOVING_OBSTACLE;
                cam.obstacleType=obst!=null?obst.type:"obstacle";
                ui.updateCamera(cam);
                ui.addLog("[RT-ALPHA] ⚠ Obstacle ("+cam.obstacleType+") — Contournement",LogType.WARNING);
                sleep(350);
            } else {
                cam.phase=CameraState.Phase.MOVING_CLEAR;cam.obstacleType="";
                ui.updateCamera(cam);
            }
            int steps=Math.max(8,(int)(dist(from[0],from[1],to[0],to[1])*65));
            for(int i=1;i<=steps;i++){
                float t=(float)i/steps;
                salleState.robotX1=from[0]+(to[0]-from[0])*t;
                salleState.robotY1=from[1]+(to[1]-from[1])*t;
                List<float[]> restant=new ArrayList<>(chemin.subList(seg-1,chemin.size()));
                restant.set(0,new float[]{salleState.robotX1,salleState.robotY1});
                salleState.chemin1=restant;
                ui.updateSalle(salleState);sleep(30);
            }
        }
        salleState.chemin1.clear();salleState.robotX1=tx;salleState.robotY1=ty;
        ui.updateSalle(salleState);
    }

    // ─────────────────────────────────────────────────────────────
    //  PATHFINDING
    // ─────────────────────────────────────────────────────────────

    private List<float[]> calculerChemin(float sx,float sy,float tx,float ty){
        List<float[]> chemin=new ArrayList<>();chemin.add(new float[]{sx,sy});
        List<ObstacleItem> bloquants=new ArrayList<>();
        for(ObstacleItem ob:salleState.obstacles)
            if(segmentPassePres(sx,sy,tx,ty,ob.x,ob.y,0.09f))bloquants.add(ob);
        if(bloquants.isEmpty()){chemin.add(new float[]{tx,ty});return chemin;}
        bloquants.sort((a,b)->Float.compare(dist(sx,sy,a.x,a.y),dist(sx,sy,b.x,b.y)));
        float curX=sx,curY=sy;
        for(ObstacleItem ob:bloquants){
            float dx=tx-curX,dy=ty-curY,len=(float)Math.sqrt(dx*dx+dy*dy);
            if(len<0.001f)continue;
            float perpX=-dy/len,perpY=dx/len;
            float sign=((ob.x-curX)*perpY-(ob.y-curY)*perpX)>0?-1f:1f;
            float wpX=Math.max(0.05f,Math.min(0.95f,ob.x+perpX*sign*0.13f));
            float wpY=Math.max(0.05f,Math.min(0.95f,ob.y+perpY*sign*0.13f));
            chemin.add(new float[]{wpX,wpY});curX=wpX;curY=wpY;
        }
        chemin.add(new float[]{tx,ty});return chemin;
    }

    private boolean segmentPassePres(float sx,float sy,float tx,float ty,float ox,float oy,float seuil){
        float dx=tx-sx,dy=ty-sy,len2=dx*dx+dy*dy;
        if(len2<0.0001f)return dist(sx,sy,ox,oy)<seuil;
        float t=Math.max(0,Math.min(1,((ox-sx)*dx+(oy-sy)*dy)/len2));
        return dist(sx+t*dx,sy+t*dy,ox,oy)<seuil;
    }
    private ObstacleItem obstacleProcheDu(float x,float y){
        ObstacleItem best=null;float minD=Float.MAX_VALUE;
        for(ObstacleItem ob:salleState.obstacles){float d=dist(x,y,ob.x,ob.y);if(d<minD){minD=d;best=ob;}}
        return best;
    }
    private float dist(float ax,float ay,float bx,float by){return (float)Math.sqrt((bx-ax)*(bx-ax)+(by-ay)*(by-ay));}

    // ─────────────────────────────────────────────────────────────
    //  KIT
    // ─────────────────────────────────────────────────────────────

    private void deployerKit(String pid){
        try{robot1.deployerKitUrgence(pid);for(ElementKit e:ElementKit.values())ui.addLog("   ✓ "+e.getLabel()+" déployé",LogType.WARNING);ui.updateKit(1,robot1.getKitUrgence());}
        catch(RobotException e){ui.addLog("[ERREUR KIT] "+e.getMessage(),LogType.CRITICAL);}
    }
    private void consumeKitPartial(RobotTriage robot,NiveauESI niveau){
        switch(niveau){
            case ESI_1:try{robot.deployerKitUrgence("URGENCE");}catch(RobotException e){}break;
            case ESI_2:robot.getKitUrgence().utiliserPartiel(2);break;
            case ESI_3:robot.getKitUrgence().utiliserPartiel(1);break;
            default:robot.getKitUrgence().utiliserPartiel(0);break;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────

    private void initialiserChaises() {
        String[] patientsId = {"PAT-001", "PAT-002", "PAT-003", null, "PAT-004", null, "PAT-005", null, "PAT-006", null};
        
        // 1. Initialiser toutes les chaises à l'état vide
        for (int i = 0; i < CHAISES_POSITIONS.length; i++) {
            float[] pos = CHAISES_POSITIONS[i];
            salleState.chaises.add(new ChairMarker(pos[0], pos[1], false, null, false));
        }

        // 2. Assoir les patients instantanément
        for (int i = 0; i < patientsId.length; i++) {
            String pid = patientsId[i];
            if (pid != null) {
                ScenarDef sc = findScenario(pid);
                if (sc != null) {
                    ChairMarker chaise = salleState.chaises.get(i);
                    chaise.occupee = true;
                    chaise.occupantId = pid;
                    chaise.occupantEstPatient = true;

                    // Les coordonnées du patient prennent celles de la chaise
                    sc.px = chaise.x + 0.01f;
                    sc.py = chaise.y;

                    int accomp = (sc.vision.isInconscient() ? 2 : (sc.patientId.equals("PAT-003") ? 1 : 0));
                    PatientMarker pm = new PatientMarker(pid, sc.px, sc.py, accomp);
                    pm.esi = NiveauESI.ESI_5;
                    salleState.patients.add(pm);

                    fileAttente.add(new PatientTriage(pid, sc.desc, NiveauESI.ESI_5));

                    // Placer l'accompagnant sur la chaise suivante (qui correspond au "null" du tableau)
                    if (accomp >= 1 && i + 1 < salleState.chaises.size() && !salleState.chaises.get(i+1).occupee) {
                        ChairMarker chaiseAcc = salleState.chaises.get(i + 1);
                        chaiseAcc.occupee = true;
                        chaiseAcc.occupantId = "ACC-" + pid;
                        chaiseAcc.occupantEstPatient = false;
                    }
                }
            }
        }
    }
    PatientMarker patientMarker(String id){
        for(PatientMarker pm:salleState.patients)if(id.equals(pm.id))return pm;return null;
    }

    ScenarDef findScenario(String pid){
        for(ScenarDef s:TOUS_SCENARIOS)if(s.patientId.equals(pid))return s;return null;
    }

    private NiveauESI estimerESI(ScenarDef sc){
        if(sc.vision.isSaignementMassif()||sc.vision.isAmputationDetectee()||sc.vision.isInconscient())return NiveauESI.ESI_1;
        if(sc.signes.getPouls()<50||sc.signes.getSpo2()<90)return NiveauESI.ESI_1;
        if(sc.signes.getTensionSystole()>160)return NiveauESI.ESI_2;
        return NiveauESI.ESI_5;
    }

    private void logVision(ScenarDef sc,String pid){
        ResultatVision v=sc.vision;
        if(v.isSaignementMassif())ui.addLog("[RT-ALPHA] 🔴 VISION: Saignement massif (HSV)",LogType.CRITICAL);
        if(v.isAmputationDetectee())ui.addLog("[RT-ALPHA] 🔴 VISION: Amputation (YOLO Pose)",LogType.CRITICAL);
        if(v.isInconscient())ui.addLog("[RT-ALPHA] 🔴 VISION: Patient inconscient (EAR<0.2)",LogType.CRITICAL);
        if(v.isAgite())ui.addLog("[RT-ALPHA] 🟡 VISION: Agitation détectée",LogType.WARNING);
        if(!v.isSaignementMassif()&&!v.isAmputationDetectee()&&!v.isInconscient()&&!v.isAgite())
            ui.addLog("[RT-ALPHA] ✅ VISION: Aucune anomalie visuelle",LogType.SUCCESS);
    }

    private void logOrientation(NiveauESI n,String pid){
        switch(n){
            case ESI_1:ui.addLog("[RT-ALPHA] → 🚨 RÉANIMATION — équipe alertée",LogType.CRITICAL);break;
            case ESI_2:ui.addLog("[RT-ALPHA] → ⚠ SALLE PRIORITAIRE < 15 min",LogType.WARNING);break;
            case ESI_3:ui.addLog("[RT-ALPHA] → SALLE SOINS < 30 min",LogType.WARNING);break;
            default:ui.addLog("[RT-ALPHA] → SALLE D'ATTENTE — patient stable",LogType.INFO);break;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  COMMANDES UI
    // ═══════════════════════════════════════════════════════════════

    void toggleDegrade(){
        if(robot1.isModeDegrade()){
            try{robot1.desactiverModeDegrade("WIFI-HOPITAL-5G");ui.addLog("[RT-ALPHA] Mode normal rétabli",LogType.SUCCESS);}
            catch(RobotException e){ui.addLog("[ERREUR] "+e.getMessage(),LogType.CRITICAL);}
        } else {
            robot1.activerModeDegrade();
            ui.addLog("[RT-ALPHA] ⚠ Mode dégradé activé — réseau coupé",LogType.WARNING);
        }
        ui.updateRobotSidebar(1,robot1.getEnergie(),robot1.isModeDegrade(),robot1.isConnecte(),robot1.getKitUrgence().estComplet(),true);
    }

    void forceRecharge(){
        ui.addLog("[RT-ALPHA] Recharge forcée — batterie 100%",LogType.SUCCESS);
        ui.updateRobotSidebar(1,100,robot1.isModeDegrade(),robot1.isConnecte(),robot1.getKitUrgence().estComplet(),true);
    }

    void declencherCodeBlue(){
        ui.addLog("[COMMANDE] 🚨 CODE BLUE DÉCLENCHÉ MANUELLEMENT",LogType.CRITICAL);
        ui.updateHeader(NiveauESI.ESI_1);
        if(ui.cmdPanel!=null)ui.cmdPanel.setStatus("CODE BLUE ACTIF",Dashboard.C_RED);
    }

    void consulterDossier(String code){
        String pid=salleState.robotPatientId;
        if(pid==null||pid.isEmpty()){ui.cmdPanel.setStatus("Aucun patient actif",Dashboard.C_AMBER);return;}
        try{
            DossierMedical d=robot1.consulterDossierSecurise(code,pid);
            if(d!=null){
                ui.addLog("[SÉCURITÉ] ✔ Accès dossier "+pid+" autorisé: "+d,LogType.SUCCESS);
                ui.cmdPanel.setStatus("Dossier "+pid+" ouvert ✔",Dashboard.C_GREEN);
                ScenarDef sc=findScenario(pid);
                if(sc!=null)ui.showDossier(pid,d,null,sc.departement,sc.salle);
            } else ui.cmdPanel.setStatus("Dossier "+pid+" introuvable",Dashboard.C_AMBER);
        } catch(AuthenticationException e){
            ui.addLog("[SÉCURITÉ] ✖ Accès refusé: "+e.getMessage(),LogType.CRITICAL);
            ui.cmdPanel.setStatus("✖ ACCÈS REFUSÉ",Dashboard.C_RED);
        }
    }

    void sleep(long ms){ try{Thread.sleep(ms);}catch(InterruptedException ignored){} }
}
