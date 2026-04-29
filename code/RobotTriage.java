package projetrobot;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

/**
 * Robot de triage médical autonome.
 *
 * Protocole d'interaction avec le patient :
 *  1. Détection du patient dans la salle d'attente → déplacement vers lui
 *  2. Invitation à poser le pouce (authentification empreinte digitale)
 *  3. Scan corporel par vision IA (YOLO + HSV)
 *  4. Mesure tension (brassard cylindrique)
 *  5. Mesure température (thermomètre frontal) + SpO2 (doigt)
 *  6. Calcul ESI et orientation
 *  7. Envoi du dossier complet au tableau de bord de la salle d'admission
 *
 * Gestion du kit d'urgence :
 *  - Si un élément est épuisé AVANT une urgence → signalé mais on continue
 *  - Lors de chaque retour à la station (batterie faible OU kit épuisé),
 *    le robot génère un RapportKit et l'affiche aux infirmiers
 *  - Le kit est réapprovisionné automatiquement à la station
 *
 * Sécurité :
 *  - Accès aux données patient protégé par code médecin
 *  - Mode dégradé (cyberattaque) : fonctionnement sur capteurs uniquement
 */
public class RobotTriage extends RobotConnecte {

    // ── Équipement embarqué ─────────────────────────────────────────
    private final KitUrgence            kitUrgence;
    private final AnalyseurUrgence      analyseur;
    private final HashMap<String, DossierMedical> baseDossiersMedicaux;

    // ── État ────────────────────────────────────────────────────────
    private boolean modeDegrade;

    // ── Coordonnées de la station centrale (infirmerie) ─────────────
    private static final int STATION_X = 0;
    private static final int STATION_Y = 0;

    // ── Sécurité : code d'accès aux données médicales sensibles ─────
    private static final String CODE_MEDECIN = "MED-2025";

    private static final DateTimeFormatter LOG_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Constructeur ─────────────────────────────────────────────────

    public RobotTriage(String id, int x, int y) {
        super(id, x, y, 100);
        this.kitUrgence           = KitUrgence.kitStandard();
        this.analyseur            = new AnalyseurUrgence();
        this.baseDossiersMedicaux = new HashMap<>();
        this.modeDegrade          = false;
        ajouterHistorique("RobotTriage initialisé avec kit d'urgence standard");
    }

    // ════════════════════════════════════════════════════════════════
    //  TÂCHE PRINCIPALE
    // ════════════════════════════════════════════════════════════════

    @Override
    public void effectuerTache() throws RobotException {
        if (!enMarche) {
            throw new RobotException("Le robot doit être démarré pour effectuer un triage.");
        }
        verifierMaintenance();

        // ── 0. Vérification autonome des ressources ─────────────────
        if (this.energie < 20 || kitUrgence.necessiteRecharge()) {
            if (this.energie < 20) {
                System.out.println("\n🔋 [" + id + "] Batterie faible (" + this.energie +
                                   "%). Retour à la station centrale...");
            }
            if (kitUrgence.necessiteRecharge()) {
                System.out.println("⚠  [" + id + "] Kit incomplet. Retour à la station centrale...");
            }
            rentrerAStationEtRecharger();
            return;
        }

        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║   ROBOT TRIAGE – CYCLE DE TRIAGE             ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        // ── 1. Détection et déplacement vers le patient ─────────────
        System.out.println("\n[ÉTAPE 1] Détection du patient dans la salle d'attente...");
        int[] posPatient = detecterPatient();
        System.out.println("→ Patient détecté en position (" + posPatient[0] + "," + posPatient[1] + ")");
        deplacer(posPatient[0], posPatient[1]);
        System.out.println("→ Robot positionné face au patient.");

        // ── 2. Authentification empreinte digitale ──────────────────
        System.out.println("\n[ÉTAPE 2] Veuillez poser votre pouce sur le lecteur...");
        String patientId = scanner_empreinteDigitale();
        System.out.println("→ Patient identifié : " + patientId);

        // ── 3. Scan corporel par vision IA ──────────────────────────
        System.out.println("\n[ÉTAPE 3] Analyse corporelle en cours (vision IA YOLO + HSV)...");
        ResultatVision vision = analyserVision();
        afficherAlerteVision(vision);

        // ── 4. Mesure tension artérielle ────────────────────────────
        System.out.println("\n[ÉTAPE 4] Veuillez placer votre bras dans le cylindre de mesure...");
        int[] tension = mesurerTension();
        System.out.println("→ Tension mesurée : " + tension[0] + "/" + tension[1] + " mmHg");

        // ── 5. Température + SpO2 ───────────────────────────────────
        System.out.println("\n[ÉTAPE 5] Placez votre front devant le thermomètre, puis votre doigt dans le capteur SpO2...");
        double temperature = mesurerTemperature();
        int    pouls       = mesurerPouls();
        int    spo2        = mesurerSpo2();
        System.out.println("→ Température : " + String.format("%.1f", temperature) + "°C");
        System.out.println("→ Pouls : " + pouls + " bpm | SpO2 : " + spo2 + "%");

        SignesVitaux signes = new SignesVitaux(pouls, tension[0], tension[1], temperature, spo2);
        System.out.println("→ " + signes);

        // ── 6. Chargement du dossier médical ────────────────────────
        DossierMedical dossier = null;
        if (!modeDegrade) {
            dossier = chargerDossierMedical(patientId);
            if (dossier != null) {
                System.out.println("\n[ÉTAPE 6] Dossier chargé : " + dossier);
            } else {
                System.out.println("\n[ÉTAPE 6] ⚠ Aucun dossier trouvé pour ce patient (nouveau patient).");
            }
        } else {
            System.out.println("\n[ÉTAPE 6] ⚠ MODE DÉGRADÉ : dossier médical indisponible.");
        }

        // ── 7. Calcul ESI ───────────────────────────────────────────
        System.out.println("\n[ÉTAPE 7] Calcul du niveau de priorité ESI...");
        NiveauESI niveau;
        String    raison = "Évaluation standard";

        try {
            niveau = analyseur.calculerNiveauESI(signes, vision, dossier, modeDegrade, patientId);
            System.out.println("✔ Niveau ESI : " + niveau + " – " + niveau.getDescription());
        } catch (UrgenceVitaleException e) {
            niveau = NiveauESI.ESI_1;
            raison = e.getMessage();
            System.out.println("\n🚨 " + e.getMessage());
            deployerKitUrgence(patientId);
        }

        // ── 8. Actions selon le niveau ──────────────────────────────
        if (niveau != NiveauESI.ESI_1) {
            raison = gererNiveauNonCritique(niveau, signes);
        }

        // ── 9. Audit trail ──────────────────────────────────────────
        enregistrerAuditTrail(patientId, signes, vision, niveau, raison);

        // ── 10. Envoi du dossier complet au tableau de bord ─────────
        envoyerDossierDashboard(patientId, signes, vision, niveau, dossier);

        consommerEnergie(2);
    }

    // ════════════════════════════════════════════════════════════════
    //  RETOUR À LA STATION + RAPPORT KIT
    // ════════════════════════════════════════════════════════════════

    /**
     * Retour autonome à la station centrale.
     * Avant de recharger, génère et affiche un RapportKit pour les infirmiers
     * si le kit n'est pas complet.
     */
    private void rentrerAStationEtRecharger() throws RobotException {
        deplacer(STATION_X, STATION_Y);

        // ── Rapport kit aux infirmiers ───────────────────────────────
        List<ElementKit> manquants = kitUrgence.getManquants();
        String horodatage = LocalDateTime.now().format(LOG_FMT);
        RapportKit rapport = new RapportKit(id, manquants, horodatage);

        System.out.println("\n📋 RAPPORT KIT – Transmission aux infirmiers :");
        System.out.println(rapport.toString());

        if (rapport.necessiteReapprovisionnement()) {
            System.out.println("🔄 Réapprovisionnement du kit en cours...");
        }

        // ── Recharge batterie + réapprovisionnement kit ──────────────
        this.energie = 100;
        this.kitUrgence.reapprovisionner();

        ajouterHistorique("Retour station : batterie 100%, kit réapprovisionné. " +
                          (manquants.isEmpty() ? "Aucun manque." :
                          manquants.size() + " élément(s) réapprovisionnés."));
        System.out.println("✅ [" + id + "] Entièrement rechargé et ravitaillé. Prêt pour les urgences.");
    }

    // ════════════════════════════════════════════════════════════════
    //  DÉPLOIEMENT KIT D'URGENCE (ESI 1 – Code Blue)
    // ════════════════════════════════════════════════════════════════

    public void deployerKitUrgence(String patientId) throws RobotException {
        verifierEnergie(10);
        ajouterHistorique("[CODE BLUE] Déploiement du kit d'urgence – patient " + patientId);

        System.out.println("\n🔴 DÉPLOIEMENT DU KIT D'URGENCE :");
        for (ElementKit element : kitUrgence.getEquipements()) {
            System.out.println("   ✓ " + element.getLabel() + " ouvert et prêt à l'usage.");
        }

        // Utilisation complète du kit (tous éléments décrémentés de 1)
        kitUrgence.utiliserKit();

        // Signaler immédiatement si des éléments sont maintenant épuisés
        List<ElementKit> manquantsApres = kitUrgence.getManquants();
        if (!manquantsApres.isEmpty()) {
            System.out.println("⚠  Éléments épuisés après ce déploiement (retour station prévu) :");
            for (ElementKit e : manquantsApres) {
                System.out.println("   – " + e.getLabel());
            }
        }

        System.out.println("   📡 Alerte envoyée au tableau de bord médical !");
        consommerEnergie(10);

        if (connecte) {
            try {
                envoyerDonnees("[CODE_BLUE] Patient " + patientId + " – Kit déployé – " +
                               LocalDateTime.now().format(LOG_FMT));
            } catch (RobotException e) {
                System.out.println("⚠ Alerte réseau échouée : " + e.getMessage());
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  MODE DÉGRADÉ (cyberattaque / perte réseau)
    // ════════════════════════════════════════════════════════════════

    public void activerModeDegrade() {
        this.modeDegrade = true;
        deconnecter();
        ajouterHistorique("[MODE DÉGRADÉ] Réseau coupé – triage sur capteurs + vision uniquement");
        System.out.println("⚠ MODE DÉGRADÉ ACTIVÉ – Fonctionnement autonome (capteurs + vision)");
    }

    public void desactiverModeDegrade(String reseau) throws RobotException {
        connecter(reseau);
        this.modeDegrade = false;
        ajouterHistorique("[MODE NORMAL] Réseau " + reseau + " rétabli");
        System.out.println("✓ Réseau rétabli. Mode dégradé désactivé.");
    }

    // ════════════════════════════════════════════════════════════════
    //  ACCÈS SÉCURISÉ AUX DONNÉES PATIENT
    // ════════════════════════════════════════════════════════════════

    /**
     * Consultation sécurisée du dossier médical.
     * Réservée au personnel médical habilité (code médecin requis).
     *
     * @param codeSaisi  code médecin fourni
     * @param patientId  identifiant du patient
     * @return le dossier médical
     * @throws AuthenticationException si le code est invalide
     */
    public DossierMedical consulterDossierSecurise(String codeSaisi, String patientId)
            throws AuthenticationException {
        if (!CODE_MEDECIN.equals(codeSaisi)) {
            ajouterHistorique("[SÉCURITÉ] Accès refusé au dossier de " + patientId);
            throw new AuthenticationException(
                "Code médecin invalide. Accès au dossier de " + patientId + " refusé."
            );
        }
        ajouterHistorique("[SÉCURITÉ] Accès autorisé au dossier de " + patientId);
        return baseDossiersMedicaux.get(patientId);
    }

    // ════════════════════════════════════════════════════════════════
    //  DÉPLACEMENT
    // ════════════════════════════════════════════════════════════════

    @Override
    public void deplacer(int destX, int destY) throws RobotException {
        double distance = Math.sqrt(
            Math.pow(destX - x, 2) + Math.pow(destY - y, 2)
        );
        if (distance > 100) {
            throw new RobotException(
                "Distance " + String.format("%.2f", distance) + " dépasse le maximum (100)."
            );
        }
        int energieConso = (int) Math.ceil(distance * 0.3);
        verifierEnergie(energieConso);
        verifierMaintenance();
        consommerEnergie(energieConso);
        heuresUtilisation += (int) (distance * 0.1);
        ajouterHistorique("Déplacement vers (" + destX + "," + destY + ") – " +
                          String.format("%.2f", distance) + " u");
        this.x = destX;
        this.y = destY;
    }

    // ════════════════════════════════════════════════════════════════
    //  MÉTHODES HARDWARE / IA (MOCK)
    // ════════════════════════════════════════════════════════════════

    /**
     * Détecte un patient dans la salle et retourne sa position.
     * En production : analyse flux caméra panoramique (détection silhouette YOLO).
     */
    private int[] detecterPatient() {
        int px = (int) (Math.random() * 10) + 1;
        int py = (int) (Math.random() * 10) + 1;
        return new int[]{px, py};
    }

    /**
     * Lit l'empreinte digitale et retourne l'identifiant patient.
     * En production : module biométrique capacitif → requête MyHealth.
     */
    private String scanner_empreinteDigitale() throws AuthenticationException {
        String[] patients = {"PAT-001", "PAT-002", "PAT-003", "PAT-INCONNU"};
        String pid = patients[(int) (Math.random() * patients.length)];
        if (pid.equals("PAT-INCONNU") && modeDegrade) {
            return "ANONYME-" + System.currentTimeMillis();
        }
        return pid;
    }

    /**
     * Mesure la tension artérielle (brassard cylindrique embarqué).
     * @return [systole, diastole] en mmHg
     */
    private int[] mesurerTension() {
        int sys  = 100 + (int) (Math.random() * 80);
        int dias = 60  + (int) (Math.random() * 30);
        return new int[]{sys, dias};
    }

    /** Mesure la température (capteur infrarouge frontal). */
    private double mesurerTemperature() {
        return 36.0 + Math.random() * 4.0;
    }

    /** Mesure le pouls (capteur photopléthysmographique). */
    private int mesurerPouls() {
        return 60 + (int) (Math.random() * 80);
    }

    /** Mesure la saturation en oxygène (oxymètre de pouls). */
    private int mesurerSpo2() {
        return 90 + (int) (Math.random() * 10);
    }

    /** Compile les signes vitaux. */
    private SignesVitaux mesurerConstantes() {
        int[]  t = mesurerTension();
        return new SignesVitaux(mesurerPouls(), t[0], t[1], mesurerTemperature(), mesurerSpo2());
    }

    /**
     * Analyse de la vision IA.
     * NOTE technique : en production, cette méthode consomme les flux caméra RGB.
     * – Détection saignement : segmentation HSV (H=0-10 / H=160-180, S>100, V>50)
     * – Détection amputation/fracture : YOLO Pose – keypoints manquants ou angle anormal
     * – Détection inconscience : yeux fermés (Eye Aspect Ratio < 0.2) + posture effondrée
     * – Agitation : variance des keypoints dans le temps > seuil
     */
    private ResultatVision analyserVision() {
        return ResultatVision.simuler(
            Math.random() < 0.15,   // agitation
            Math.random() < 0.05,   // saignement massif
            Math.random() < 0.03,   // amputation
            Math.random() < 0.08    // inconscience
        );
    }

    private void afficherAlerteVision(ResultatVision v) {
        if (v.isSaignementMassif())   System.out.println("🔴 VISION : Saignement massif détecté !");
        if (v.isAmputationDetectee()) System.out.println("🔴 VISION : Amputation/fracture détectée !");
        if (v.isInconscient())        System.out.println("🔴 VISION : Patient inconscient !");
        if (v.isAgite())              System.out.println("🟡 VISION : Patient agité.");
        if (!v.isSaignementMassif() && !v.isAmputationDetectee()
                && !v.isInconscient() && !v.isAgite()) {
            System.out.println("✅ VISION : Aucune anomalie visuelle détectée.");
        }
    }

    private DossierMedical chargerDossierMedical(String patientId) {
        return baseDossiersMedicaux.get(patientId);
    }

    private String gererNiveauNonCritique(NiveauESI niveau, SignesVitaux s) {
        switch (niveau) {
            case ESI_2:
                System.out.println("🟠 ESI 2 : Mise en attente prioritaire – équipe alertée.");
                ajouterHistorique("Patient ESI 2 – Surveillance immédiate");
                return "Constantes préoccupantes, surveillance immédiate";
            case ESI_3:
                System.out.println("🟡 ESI 3 : Prise en charge sous 30 minutes.");
                ajouterHistorique("Patient ESI 3 – Prise en charge sous 30 min");
                return "Constantes stabilisées, surveillance recommandée";
            default:
                System.out.println("🟢 ESI " + niveau.name().replace("ESI_", "") +
                                   " : Orientation salle d'attente standard.");
                ajouterHistorique("Patient " + niveau + " – Salle d'attente standard");
                return "Patient stable";
        }
    }

    private void enregistrerAuditTrail(String patientId, SignesVitaux signes,
                                        ResultatVision vision, NiveauESI niveau, String raison) {
        String entree = String.format(
            "[AUDIT] %s | Patient: %s | %s | %s | Niveau: %s | Raison: %s | Mode: %s",
            LocalDateTime.now().format(LOG_FMT),
            patientId, signes, vision, niveau, raison,
            modeDegrade ? "DÉGRADÉ" : "NORMAL"
        );
        ajouterHistorique(entree);
        System.out.println("\n📋 " + entree);
    }

    /**
     * Envoie le dossier complet au tableau de bord de la salle d'admission
     * via le réseau hospitalier.
     */
    private void envoyerDossierDashboard(String patientId, SignesVitaux signes,
                                          ResultatVision vision, NiveauESI niveau,
                                          DossierMedical dossier) {
        if (!connecte) {
            System.out.println("⚠  Réseau indisponible – dossier non transmis au dashboard.");
            return;
        }
        String payload = String.format(
            "{patient:%s, esi:%s, %s, %s, dossier:%s}",
            patientId, niveau, signes, vision,
            dossier != null ? dossier.toString() : "INCONNU"
        );
        try {
            envoyerDonnees("[DASHBOARD] " + payload);
            System.out.println("📡 Dossier transmis au tableau de bord de la salle d'admission.");
        } catch (RobotException e) {
            System.out.println("⚠  Échec transmission dashboard : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  GESTION DES DOSSIERS
    // ════════════════════════════════════════════════════════════════

    public void enregistrerDossier(DossierMedical dossier) {
        baseDossiersMedicaux.put(dossier.getIdPatient(), dossier);
        ajouterHistorique("Dossier médical enregistré pour patient " + dossier.getIdPatient());
    }

    // ════════════════════════════════════════════════════════════════
    //  GETTERS & TOSTRING
    // ════════════════════════════════════════════════════════════════

    public boolean    isModeDegrade()    { return modeDegrade; }
    public KitUrgence getKitUrgence()    { return kitUrgence; }

    @Override
    public String toString() {
        return "RobotTriage [ID=" + id +
               ", Pos=(" + x + "," + y + ")" +
               ", Energie=" + energie + "%" +
               ", Heures=" + heuresUtilisation +
               ", Mode=" + (modeDegrade ? "DÉGRADÉ" : "NORMAL") +
               ", Connecté=" + (connecte ? reseauConnecte : "Non") +
               ", Kit=" + (kitUrgence.estComplet() ? "COMPLET" : "INCOMPLET") + "]";
    }
}
