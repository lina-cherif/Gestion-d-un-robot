package projetrobot;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// ══════════════════════════════════════════════════════════════════════
//  ÉNUMÉRATIONS
// ══════════════════════════════════════════════════════════════════════

/**
 * Catégories de risques médicaux issus du dossier MyHealth.
 */
enum CategorieRisque {
    CARDIOVASCULAIRE,
    RESPIRATOIRE,
    METABOLIQUE,
    IMMUNODEPRIME,
    AUCUN
}

/**
 * Niveaux ESI (Emergency Severity Index) — 1 = le plus urgent.
 */
enum NiveauESI {
    ESI_1("Code Blue – Urgence vitale immédiate"),
    ESI_2("Urgence élevée – Intervention rapide"),
    ESI_3("Urgent – Surveillance nécessaire"),
    ESI_4("Moins urgent – Peut attendre"),
    ESI_5("Non urgent");

    private final String description;
    NiveauESI(String description) { this.description = description; }
    public String getDescription() { return description; }
}

/**
 * Éléments du kit d'urgence embarqué par le RobotTriage.
 * Chaque élément a un stock maximum défini.
 */
enum ElementKit {
    DEFIBRILLATEUR       ("Défibrillateur AED",           1),
    MASQUE_OXYGENE       ("Masque à oxygène",              3),
    GARROT               ("Garrot hémostatique",           2),
    PANSEMENT_COMPRESSIF ("Pansement compressif",          5),
    SERINGUE_ADRENALINA  ("Seringue d'adrénaline",         3),
    ATTELLE              ("Attelle d'immobilisation",      2),
    COLLIER_CERVICAL     ("Collier cervical",              1);

    private final String label;
    private final int    stockMax;

    ElementKit(String label, int stockMax) {
        this.label    = label;
        this.stockMax = stockMax;
    }

    public String getLabel()    { return label; }
    public int    getStockMax() { return stockMax; }
}

// ══════════════════════════════════════════════════════════════════════
//  SIGNES VITAUX
// ══════════════════════════════════════════════════════════════════════

/**
 * Encapsule les constantes physiologiques mesurées par les capteurs du robot.
 */
class SignesVitaux {
    private final int    pouls;
    private final int    tensionSystole;
    private final int    tensionDiastole;
    private final double temperature;
    private final int    spo2;

    public SignesVitaux(int pouls, int tensionSystole, int tensionDiastole,
                        double temperature, int spo2) {
        this.pouls           = pouls;
        this.tensionSystole  = tensionSystole;
        this.tensionDiastole = tensionDiastole;
        this.temperature     = temperature;
        this.spo2            = spo2;
    }

    public int    getPouls()           { return pouls; }
    public int    getTensionSystole()  { return tensionSystole; }
    public int    getTensionDiastole() { return tensionDiastole; }
    public double getTemperature()     { return temperature; }
    public int    getSpo2()            { return spo2; }

    @Override
    public String toString() {
        return "SignesVitaux [Pouls=" + pouls + " bpm" +
               ", Tension=" + tensionSystole + "/" + tensionDiastole + " mmHg" +
               ", Temp=" + String.format("%.1f", temperature) + "°C" +
               ", SpO2=" + spo2 + "%]";
    }
}

// ══════════════════════════════════════════════════════════════════════
//  DOSSIER MÉDICAL
// ══════════════════════════════════════════════════════════════════════

/**
 * Représente le dossier médical simplifié d'un patient (modèle MyHealth).
 */
class DossierMedical {
    private final String               idPatient;
    private final List<CategorieRisque> categories;
    private final int                  age;

    public DossierMedical(String idPatient, int age, List<CategorieRisque> categories) {
        this.idPatient  = idPatient;
        this.age        = age;
        this.categories = categories != null ? categories : new ArrayList<>();
    }

    public boolean aRisque(CategorieRisque cat) {
        return categories.contains(cat);
    }

    public String               getIdPatient()   { return idPatient; }
    public int                  getAge()         { return age; }
    public List<CategorieRisque> getCategories() { return categories; }

    @Override
    public String toString() {
        return "DossierMedical [Patient=" + idPatient +
               ", Âge=" + age +
               ", Risques=" + categories + "]";
    }
}

// ══════════════════════════════════════════════════════════════════════
//  RÉSULTAT VISION (MOCK)
// ══════════════════════════════════════════════════════════════════════

/**
 * Simulation du module de vision IA (YOLO + HSV).
 * En production, ces booléens seraient alimentés par les flux caméra.
 *
 * Note technique : dans un système réel, la détection de saignements utilise
 * la segmentation HSV (teintes rouge/brun-rouge dans la plage H=0-10 / H=160-180),
 * la détectioden  posture/amputation utilise YOLO Pose (keypoints manquants),
 * et la détection d'inconscience repose sur la fermeture des yeux + chute détectée.
 */
class ResultatVision {
    private final boolean agite;
    private final boolean saignementMassif;
    private final boolean amputationDetectee;
    private final boolean inconscient;

    public ResultatVision(boolean agite, boolean saignementMassif,
                          boolean amputationDetectee, boolean inconscient) {
        this.agite              = agite;
        this.saignementMassif   = saignementMassif;
        this.amputationDetectee = amputationDetectee;
        this.inconscient        = inconscient;
    }

    public boolean isAgite()              { return agite; }
    public boolean isSaignementMassif()   { return saignementMassif; }
    public boolean isAmputationDetectee() { return amputationDetectee; }
    public boolean isInconscient()        { return inconscient; }

    /** Usine de simulation : génère un résultat basé sur des paramètres bruts. */
    public static ResultatVision simuler(boolean agite, boolean saignement,
                                         boolean amputation, boolean inconscient) {
        return new ResultatVision(agite, saignement, amputation, inconscient);
    }

    @Override
    public String toString() {
        return "Vision [Agité=" + agite +
               ", Saignement=" + saignementMassif +
               ", Amputation=" + amputationDetectee +
               ", Inconscient=" + inconscient + "]";
    }
}

// ══════════════════════════════════════════════════════════════════════
//  KIT D'URGENCE  (version corrigée avec gestion de stock)
// ══════════════════════════════════════════════════════════════════════

/**
 * Kit d'urgence embarqué sur le RobotTriage.
 *
 * Chaque élément possède un stock individuel.
 * - utiliserElement(e) : décrémente le stock d'un élément précis.
 * - utiliserKit()      : décrémente TOUS les éléments (déploiement complet ESI 1).
 * - necessiteRecharge(): vrai si AU MOINS un élément est épuisé.
 * - getManquants()     : liste les éléments dont le stock est à 0.
 * - reapprovisionner() : remet tous les stocks au maximum.
 */
class KitUrgence {

    /** Stock courant pour chaque élément. */
    private final EnumMap<ElementKit, Integer> stock;

    public KitUrgence() {
        stock = new EnumMap<>(ElementKit.class);
        for (ElementKit e : ElementKit.values()) {
            stock.put(e, e.getStockMax());
        }
    }

    // ── Utilisation ──────────────────────────────────────────────────

    /**
     * Utilise UN exemplaire d'un élément spécifique.
     * @throws KitManquantException si le stock de cet élément est épuisé.
     */
    public void utiliserElement(ElementKit element) throws KitManquantException {
        int courant = stock.getOrDefault(element, 0);
        if (courant <= 0) {
            throw new KitManquantException(element);
        }
        stock.put(element, courant - 1);
    }
    
    /**
     * Exception levée lorsqu'un élément du kit est épuisé.
     */
    class KitManquantException extends Exception {
        public KitManquantException(ElementKit element) {
            super("Élément épuisé : " + element.getLabel());
        }
    }
    /**
     * Déploiement COMPLET du kit (ESI 1 : Code Blue).
     * Décrémente TOUS les éléments de 1 (s'ils sont disponibles).
     */
    public void utiliserKit() {
        for (ElementKit e : ElementKit.values()) {
            int courant = stock.getOrDefault(e, 0);
            if (courant > 0) {
                stock.put(e, courant - 1);
            }
        }
    }

    // ── État du stock ────────────────────────────────────────────────

    /**
     * @return true si au moins un élément a un stock à 0.
     */
    public boolean necessiteRecharge() {
        for (int s : stock.values()) {
            if (s <= 0) return true;
        }
        return false;
    }

    /**
     * @return la liste des éléments épuisés (stock = 0).
     */
    public List<ElementKit> getManquants() {
        List<ElementKit> manquants = new ArrayList<>();
        for (Map.Entry<ElementKit, Integer> entry : stock.entrySet()) {
            if (entry.getValue() <= 0) {
                manquants.add(entry.getKey());
            }
        }
        return manquants;
    }

    /**
     * @return true si le kit est entièrement plein.
     */
    public boolean estComplet() {
        for (Map.Entry<ElementKit, Integer> entry : stock.entrySet()) {
            if (entry.getValue() < entry.getKey().getStockMax()) return false;
        }
        return true;
    }

    public int getStock(ElementKit element) {
        return stock.getOrDefault(element, 0);
    }
  //  public int setStock

    // ── Réapprovisionnement ──────────────────────────────────────────

    /**
     * Remet tous les éléments du kit au stock maximum.
     */
    public void reapprovisionner() {
        for (ElementKit e : ElementKit.values()) {
            stock.put(e, e.getStockMax());
        }
    }

    // ── Compatibilité : liste des équipements encore disponibles ─────

    /**
     * @return la liste des éléments dont le stock est > 0.
     */
    public List<ElementKit> getEquipements() {
        List<ElementKit> disponibles = new ArrayList<>();
        for (Map.Entry<ElementKit, Integer> entry : stock.entrySet()) {
            if (entry.getValue() > 0) {
                disponibles.add(entry.getKey());
            }
        }
        return disponibles;
    }

    /** Crée un kit d'urgence standard complet (tous éléments au max). */
    public static KitUrgence kitStandard() {
        return new KitUrgence(); // le constructeur initialise déjà au max
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("KitUrgence [\n");
        for (Map.Entry<ElementKit, Integer> entry : stock.entrySet()) {
            sb.append("  ").append(entry.getKey().getLabel())
              .append(" : ").append(entry.getValue())
              .append("/").append(entry.getKey().getStockMax()).append("\n");
        }
        sb.append("]");
        return sb.toString();
    }
    
         /* Utilise partiellement le kit selon le niveau d'urgence.
         * niveau 2 → 2 éléments consommés ; niveau 3 → 1 ; niveau 4-5 → 0
         * @param nb nombre d'éléments à consommer (0 = usage minimal/aucun)
         */
		public void utiliserPartiel(int nb) {
		    int count = 0;
		    for (ElementKit e : getEquipements()) {
		        if (count >= nb) break;
		        int courant = getStock(e);
		        if (courant > 0) {
		            // Remplacement de setStock par un put direct dans la Map
		            stock.put(e, courant - 1); 
		            count++;
		        }
		    }
		}
     
}

// ══════════════════════════════════════════════════════════════════════
//  ANALYSEUR D'URGENCE ESI
// ══════════════════════════════════════════════════════════════════════

/**
 * Moteur de règles en deux étapes pour calculer le niveau ESI.
 */
class AnalyseurUrgence {

    public NiveauESI calculerNiveauESI(SignesVitaux signes,
                                       ResultatVision vision,
                                       DossierMedical dossier,
                                       boolean modeDegrade,
                                       String patientId)
            throws UrgenceVitaleException {

        // ── ÉTAPE 1 : Critères ESI 1 automatiques (vision) ──────────
        if (vision.isSaignementMassif()) {
            lancerCodeBlue(patientId, "Saignement massif détecté (vision HSV)");
        }
        if (vision.isAmputationDetectee()) {
            lancerCodeBlue(patientId, "Amputation/fracture grave détectée (YOLO Pose)");
        }
        if (vision.isInconscient()) {
            lancerCodeBlue(patientId, "Patient inconscient détecté");
        }
        if (signes.getSpo2() < 90) {
            lancerCodeBlue(patientId, "SpO2 critique : " + signes.getSpo2() + "%");
        }

        // ── ÉTAPE 2 : Score de base ──────────────────────────────────
        int score = calculerScoreBase(signes, vision);

        // ── ÉTAPE 3 : Surclassement via dossier médical ─────────────
        if (!modeDegrade && dossier != null) {
            score = appliquerSurclassement(score, signes, dossier, patientId);
        }

        NiveauESI niveau = scoreVersESI(score);

        if (niveau == NiveauESI.ESI_1) {
            lancerCodeBlue(patientId, "Constantes vitales ESI 1 confirmées");
        }

        return niveau;
    }

    private int calculerScoreBase(SignesVitaux s, ResultatVision v) {
        int score = 5;
        if (s.getPouls() > 130)                                  score = Math.min(score, 3);
        else if (s.getPouls() < 50)                              score = Math.min(score, 2);
        if (s.getSpo2() < 94)                                    score = Math.min(score, 3);
        if (s.getSpo2() < 90)                                    score = Math.min(score, 1);
        if (s.getTemperature() > 40.0 || s.getTemperature() < 35.0) score = Math.min(score, 2);
        else if (s.getTemperature() > 38.5)                      score = Math.min(score, 3);
        if (s.getTensionSystole() < 90 || s.getTensionSystole() > 180) score = Math.min(score, 2);
        if (v.isAgite())                                         score = Math.min(score, 2);
        return score;
    }

    private int appliquerSurclassement(int score, SignesVitaux s,
                                        DossierMedical d, String patientId)
            throws UrgenceVitaleException {
        if (s.getSpo2() < 94 && d.aRisque(CategorieRisque.RESPIRATOIRE)) {
            lancerCodeBlue(patientId, "SpO2 " + s.getSpo2() + "% + antécédent RESPIRATOIRE");
        }
        if (s.getPouls() > 120 && d.aRisque(CategorieRisque.CARDIOVASCULAIRE)) {
            score = Math.min(score, 2);
        }
        if (d.getAge() > 75 || d.aRisque(CategorieRisque.IMMUNODEPRIME)) {
            score = Math.max(1, score - 1);
        }
        if (d.aRisque(CategorieRisque.METABOLIQUE) && s.getTemperature() > 39.0) {
            score = Math.min(score, 2);
        }
        return score;
    }

    private NiveauESI scoreVersESI(int score) {
        switch (score) {
            case 1:  return NiveauESI.ESI_1;
            case 2:  return NiveauESI.ESI_2;
            case 3:  return NiveauESI.ESI_3;
            case 4:  return NiveauESI.ESI_4;
            default: return NiveauESI.ESI_5;
        }
    }

    private void lancerCodeBlue(String patientId, String raison)
            throws UrgenceVitaleException {
        throw new UrgenceVitaleException(patientId, 1, raison);
    }
}

// ══════════════════════════════════════════════════════════════════════
//  RAPPORT KIT — envoyé à l'infirmerie lors du retour à la centrale
// ══════════════════════════════════════════════════════════════════════

/**
 * Objet transmis aux infirmiers lors du retour du robot à la station centrale.
 * Indique les éléments du kit qui doivent être réapprovisionnés.
 */
class RapportKit {
    private final String        robotId;
    private final List<ElementKit> manquants;
    private final String        horodatage;

    public RapportKit(String robotId, List<ElementKit> manquants, String horodatage) {
        this.robotId    = robotId;
        this.manquants  = new ArrayList<>(manquants);
        this.horodatage = horodatage;
    }

    public boolean necessiteReapprovisionnement() {
        return !manquants.isEmpty();
    }

    public String getRobotId()          { return robotId; }
    public List<ElementKit> getManquants() { return manquants; }

    @Override
    public String toString() {
        if (manquants.isEmpty()) {
            return "[" + horodatage + "] RAPPORT KIT Robot " + robotId +
                   " : Kit complet, aucun réapprovisionnement nécessaire.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(horodatage).append("] RAPPORT KIT Robot ").append(robotId)
          .append(" : ").append(manquants.size()).append(" élément(s) à réapprovisionner :\n");
        for (ElementKit e : manquants) {
            sb.append("   ⚠ ").append(e.getLabel()).append("\n");
        }
        return sb.toString();
    }
}
