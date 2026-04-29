package projetrobot;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


/**
 * RobotSecurite — Surveille le réseau, détecte les intrusions, protège les données patients.
 * Implémente : RBAC, IDS, Mode Dégradé Anti-Ransomware, Audit Trail légal.
 */
public class RobotSecurite extends RobotConnecte {

    /** Niveaux d'alerte  (du plus bas au plus critique) */
    public enum NiveauAlerte {
        VERT,     // Tout fonctionne normalement
        ORANGE,   // Activité suspecte détectée
        ROUGE,    // Cyberattaque probable en cours
        CRITIQUE  // Cyberattaque confirmée — mode dégradé activé
    }

    /** État général du système hospitalier */
    public enum EtatSysteme {
        NORMAL,        // Fonctionnement standard
        ALERTE,        // Anomalie détectée, surveillance accrue
        DEGRADE,       // Réseau compromis, fonctionnement local
        URGENCE_VITALE // Patients en danger — priorité absolue
    }

    /** Rôles RBAC — contrôle d'accès basé sur les rôles */
    public enum Role {
        INFIRMIER,   // Peut recevoir médicaments, voir alertes
        MEDECIN,     // Peut autoriser missions VITALES
        TECHNICIEN,  // Peut effectuer la maintenance réseau
        ADMIN        // Peut déclencher le mode dégradé, accès total
    }

    /** Types d'anomalies que le robot peut détecter */
    public enum TypeAnomalic {
        ACCES_NON_AUTORISE,   // Tentative d'accès à une ressource interdite
        TROP_DE_REQUETES,     // Trop de requêtes en peu de temps → DDoS simulé
        MODIFICATION_DONNEES, // Tentative de modification des données → Ransomware simulé
        ROBOT_COMPROMIS,      // Un robot isolé tente de reprendre son activité
        BRUTE_FORCE,          // Trop de tentatives de connexion échouées
        HONEYPOT_DECLENCHE    // Accès à une ressource piège → intrusion confirmée
    }

    /** Niveau d'alerte actuel du système */
    private NiveauAlerte niveauAlerte;

    /** État actuel du système hospitalier */
    private EtatSysteme etatSysteme;

    /** Robots sous surveillance (la flotte) */
    private List<Robot> flotteSurveillée;

    /** IDs des robots isolés du réseau (compromis) */
    private Set<String> robotsIsoles;

    /** Tentatives de connexion échouées par utilisateur */
    private Map<String, Integer> tentativesEchouees;

    /** Sessions actives : identifiant → rôle */
    private Map<String, Role> sessionsActives;

    /** Audit Trail : journal inviolable horodaté */
    private List<String> auditTrail;

    /** Compteur de requêtes réseau  */
    private Map<String, List<Long>> requetesParSource;

    /** Énergie minimale pour effectuer une surveillance */
    private static final int ENERGIE_SURVEILLANCE = 5;

    /** Énergie pour chiffrer/déchiffrer des données */
    private static final int ENERGIE_CHIFFREMENT = 3;

    /** Seuil de requêtes avant alerte DDoS (requêtes/minute) */
    private static final int SEUIL_DDOS = 50;

    /** Max tentatives avant verrouillage de compte */
    private static final int MAX_TENTATIVES = 3;

    /** Clé AES simulée (en production : générée via KeyGenerator AES-256) */
    private static final String CLE_AES_SIMULEE = "HOSPITAL_AES_KEY_256";
    
 // --- Honeypot ---
    /**
     * Aucun utilisateur légitime ne devrait jamais accéder à cette ressource.
     * Tout accès = intrusion confirmée à 100%.
     */
    private static final String RESSOURCE_HONEYPOT = "DOSSIER_PATIENT_VIP_CONFIDENTIEL";

    /** Journal des intrusions détectées par le honeypot */
    private ArrayList<String> intrusionsHoneypot;

    public RobotSecurite(String id, int x, int y, int energie) {
        super(id, x, y, energie);
        this.niveauAlerte     = NiveauAlerte.VERT;
        this.etatSysteme      = EtatSysteme.NORMAL;
        this.flotteSurveillée = new ArrayList<>();
        this.robotsIsoles     = new HashSet<>();
        this.tentativesEchouees = new HashMap<>();
        this.sessionsActives  = new HashMap<>();
        this.auditTrail       = new ArrayList<>();
        this.requetesParSource = new HashMap<>();
        ajouterHistorique("RobotSecurite initialisé — Surveillance active.");
        logAudit("SYSTÈME", Role.ADMIN, "INIT", "Système de cybersécurité hospitalière démarré.");
    }

    /**
     * Tâche principale : effectue un cycle complet de surveillance.
     * Analyse le réseau, les comportements suspects, l'intégrité des données.
     */
    @Override
    public void effectuerTache() throws RobotException {
        if (!enMarche) {
            throw new RobotException("Le robot doit être démarré pour effectuer une tâche.");
        }
        verifierEnergie(ENERGIE_SURVEILLANCE);
        verifierMaintenance();

        ajouterHistorique("Cycle de surveillance réseau démarré.");
        consommerEnergie(ENERGIE_SURVEILLANCE);
        heuresUtilisation++;

        // Analyse des comportements suspects dans la flotte
        for (Robot robot : flotteSurveillée) {
            analyserComportement(robot);
        }

        ajouterHistorique("Cycle de surveillance terminé. État : " + etatSysteme);
    }

    /**
     * Le RobotSecurite est FIXE : il ne se déplace pas physiquement.
     * Il surveille via le réseau, pas en se déplaçant dans les couloirs.
     */
    @Override
    public void deplacer(int x, int y) throws RobotException {
        // Journalisation de la tentative pour l'audit
        logAudit("SYSTEME", Role.ADMIN, "DEPLACEMENT_REFUSE",
                 "Tentative de déplacement refusée — RobotSecurite est une station fixe.");

        // Exception levée systématiquement : cette méthode ne doit jamais réussir
        throw new RobotException(
            "RobotSecurite est une station fixe — déplacement impossible. " + "Ce robot surveille via le réseau, pas physiquement.");
    }

    // =========================================================
    // 1. AUTHENTIFICATION & RBAC
    // =========================================================

    /**
     * Authentifie un utilisateur avec SHA-256 et retourne une session.
     * Verrouille le compte après MAX_TENTATIVES échecs.
     *
     * @param identifiant  Nom d'utilisateur
     * @param motDePasse   Mot de passe en clair (sera haché)
     * @param role         Rôle revendiqué
     * @return             Token de session si succès
     * @throws AuthentificationEchoueeException si identifiants incorrects
     * @throws CompteVerrouilleException         si compte verrouillé
     */
    public String authentifier(String identifiant, String motDePasse, Role role)
            throws AuthentificationEchoueeException, CompteVerrouilleException {

        // Vérification verrouillage
        int tentatives = tentativesEchouees.getOrDefault(identifiant, 0);
        if (tentatives >= MAX_TENTATIVES) {
            logAudit(identifiant, role, "CONNEXION_BLOQUEE",
                    "Compte verrouillé après " + MAX_TENTATIVES + " tentatives.");
            throw new CompteVerrouilleException(
                    "Compte '" + identifiant + "' verrouillé. Contactez l'administrateur.");
        }

        // Hachage SHA-256 du mot de passe
        String hashSaisi;
        try {
            hashSaisi = hasherSHA256(motDePasse);
        } catch (NoSuchAlgorithmException e) {
            throw new AuthentificationEchoueeException("Erreur système de hachage.");
        }

        // Simulation : base de données des hash 
        String hashAttendu = getHashAttendu(identifiant, role);

        if (hashAttendu == null || !hashSaisi.equals(hashAttendu)) {
            tentativesEchouees.put(identifiant, tentatives + 1);
            int restantes = MAX_TENTATIVES - (tentatives + 1);
            logAudit(identifiant, role, "ECHEC_CONNEXION",
                    "Mot de passe incorrect. Tentatives restantes : " + restantes);
            throw new AuthentificationEchoueeException(
                    "Identifiants incorrects. Tentatives restantes : " + restantes);
        }

        // Succès : reset tentatives + créer session
        tentativesEchouees.put(identifiant, 0);
        String token = genererToken(identifiant);
        sessionsActives.put(token, role);
        logAudit(identifiant, role, "CONNEXION_REUSSIE", "Session créée : " + token.substring(0, 8) + "...");
        ajouterHistorique("Connexion réussie : " + identifiant + " (" + role + ")");
        return token;
    }

    /**
     * Vérifie qu'un token possède le rôle requis pour une action.
     * @throws AccesNonAutoriseException si le rôle est insuffisant
     */
    public void verifierAutorisation(String token, Role roleRequis)
            throws AccesNonAutoriseException, SessionExpireeException {

        Role roleSession = sessionsActives.get(token);
        if (roleSession == null) {
            throw new SessionExpireeException("Session invalide ou expirée.");
        }
        // Hiérarchie des rôles : ADMIN > MEDECIN > TECHNICIEN > INFIRMIER
        if (ordreRole(roleSession) < ordreRole(roleRequis)) {
            logAudit("TOKEN:" + token.substring(0, 8), roleSession, "ACCES_REFUSE",
                    "Rôle " + roleSession + " insuffisant pour action réservée à " + roleRequis);
            throw new AccesNonAutoriseException(
                    "Accès refusé. Rôle requis : " + roleRequis + ", rôle actuel : " + roleSession);
        }
    }

    // =========================================================
    // 2. DÉTECTION D'INTRUSION 
    // =========================================================

    /**
     * Analyse le comportement d'un robot pour détecter des anomalies.
     * Détecte : DDoS, accès non autorisés, modifications de données.
     */
    public void analyserComportement(Robot robot) throws RobotException {
        verifierEnergie(2);
        consommerEnergie(2);

        String robotId = robot.id;

        // Détection DDoS : trop de requêtes en peu de temps
        if (detecterDDoS(robotId)) {
            signalerAnomalic(robotId, TypeAnomalic.TROP_DE_REQUETES,
                    "Volume de requêtes anormal détecté depuis " + robotId);
        }

        // Un robot isolé tente de se reconnecter → intrusion
        if (robotsIsoles.contains(robotId) && robot.enMarche) {
            signalerAnomalic(robotId, TypeAnomalic.ROBOT_COMPROMIS,
                    "Robot isolé " + robotId + " tente d'être actif — possible compromission.");
        }
    }

    /**
     * Simule une attaque par brute force et montre que le système la bloque.
     * @param cible         Identifiant cible de l'attaque
     * @param nbTentatives  Nombre de tentatives simulées
     */
    public void simulerAttaqueBruteForce(String cible, int nbTentatives) {
        ajouterHistorique("SIMULATION : Attaque brute force sur '" + cible + "' (" + nbTentatives + " tentatives).");
        logAudit("ATTAQUANT_SIMULE", Role.INFIRMIER, "BRUTE_FORCE_DEBUT",
                "Tentative d'attaque sur " + cible);

        for (int i = 1; i <= nbTentatives; i++) {
            int tentatives = tentativesEchouees.getOrDefault(cible, 0) + 1;
            tentativesEchouees.put(cible, tentatives);

            if (tentatives >= MAX_TENTATIVES) {
                // Compte verrouillé — attaque bloquée
                niveauAlerte = NiveauAlerte.ORANGE;
                signalerAnomalic(cible, TypeAnomalic.BRUTE_FORCE,
                        "Compte '" + cible + "' verrouillé après " + tentatives + " tentatives.");
                logAudit("IDS", Role.ADMIN, "BRUTE_FORCE_BLOQUE",
                        "Attaque bloquée à la tentative " + i + "/" + nbTentatives);
                ajouterHistorique("✅ ATTAQUE BLOQUÉE après " + tentatives + " tentatives sur '" + cible + "'.");
                return;
            }
        }
    }

    /**
     * Simule une cyberattaque ransomware complète et le basculement en mode dégradé.
     */
    public void simulerAttaqueRansomware() throws RobotException {
        ajouterHistorique(" RANSOMWARE DÉTECTÉ — Chiffrement malveillant en cours sur le réseau...");
        logAudit("RANSOMWARE", Role.ADMIN, "ATTAQUE_DETECTEE",
                "Comportement ransomware identifié : tentative de modification des données patients.");

        niveauAlerte = NiveauAlerte.ROUGE;
        etatSysteme = EtatSysteme.ALERTE;
        ajouterHistorique("Niveau d'alerte passé à ROUGE.");

        // Isolement du réseau principal
        deconnecter();
        logAudit("SYSTEME", Role.ADMIN, "RESEAU_ISOLE", "Déconnexion du réseau principal pour contenir l'attaque.");

        // Activation mode dégradé
        activerModeDegradé();
    }

    /**
     * Active le mode dégradé :
     * - Déconnecte tous les robots du réseau principal
     * - Les missions VITALES continuent via données locales
     * - Missions secondaires suspendues
     */
    public void activerModeDegradé() throws RobotException {
        verifierAutorisation_Admin();
        etatSysteme = EtatSysteme.DEGRADE;
        niveauAlerte = NiveauAlerte.CRITIQUE;

        ajouterHistorique(" MODE DÉGRADÉ ACTIVÉ — Réseau principal compromis.");
        logAudit("GESTIONNAIRE", Role.ADMIN, "MODE_DEGRADE_ACTIVE",
                "Basculement sur fonctionnement local. Urgences maintenues.");

        for (Robot robot : flotteSurveillée) {
            if (robot instanceof RobotConnecte) {
                ((RobotConnecte) robot).deconnecter();
                logAudit("ROBOT_" + robot.id, Role.ADMIN, "DECONNEXION_FORCEE",
                        "Robot isolé du réseau principal — passe en mode local.");
            }
        }

        ajouterHistorique("Tous les robots déconnectés du réseau. Missions VITALES maintenues.");
    }

    /**
     * Restaure le fonctionnement normal après résolution de l'incident.
     * @param token Token d'authentification ADMIN requis
     */
    public void restaurerModeNormal(String token)
            throws AccesNonAutoriseException, SessionExpireeException, RobotException {
        verifierAutorisation(token, Role.ADMIN);
        etatSysteme = EtatSysteme.NORMAL;
        niveauAlerte = NiveauAlerte.VERT;
        robotsIsoles.clear();
        connecter("RESEAU_HOSPITALIER_PRINCIPAL");
        ajouterHistorique("✅ MODE NORMAL RÉTABLI — Réseau sécurisé restauré.");
        logAudit("ADMIN_" + token.substring(0, 6), Role.ADMIN, "RESTAURATION",
                "Système restauré. Audit post-incident disponible.");
    }

    /**
     * Isole un robot compromis du réseau.
     * @param robotId ID du robot à isoler
     * @param token   Token ADMIN
     */
    public void isolerRobot(String robotId, String token)
            throws AccesNonAutoriseException, SessionExpireeException {
        verifierAutorisation(token, Role.ADMIN);
        robotsIsoles.add(robotId);
        logAudit("ADMIN_" + token.substring(0, 6), Role.ADMIN, "ISOLATION_ROBOT",
                "Robot " + robotId + " isolé du réseau — comportement suspect.");
        ajouterHistorique("Robot " + robotId + " ISOLÉ du réseau par décision administrative.");
        if (niveauAlerte.ordinal() < NiveauAlerte.ROUGE.ordinal()) {
            niveauAlerte = NiveauAlerte.ORANGE;
        }
    }

    // =========================================================
    // 4. CHIFFREMENT AES (Simulation)
    // =========================================================

    /**
     * Chiffre des données médicales sensibles avant transmission.
     * Simulation AES-CBC (en production : javax.crypto.Cipher AES/CBC/PKCS5Padding).
     *
     * @param donnees Données en clair
     * @param token   Token de session de l'émetteur
     * @return        Données "chiffrées" (représentation simulée)
     */
    public String chiffrerDonnees(String donnees, String token)
            throws AccesNonAutoriseException, SessionExpireeException, RobotException {
        verifierAutorisation(token, Role.TECHNICIEN);
        verifierEnergie(ENERGIE_CHIFFREMENT);
        consommerEnergie(ENERGIE_CHIFFREMENT);

        // Simulation AES : en production → Cipher.getInstance("AES/CBC/PKCS5Padding")
        String donneesCryptees = "[AES-256|CBC|" + CLE_AES_SIMULEE.hashCode() + "]"
                + Base64.getEncoder().encodeToString(donnees.getBytes())
                + "[/AES]";

        logAudit("SYSTEME", sessionsActives.get(token), "CHIFFREMENT",
                "Données chiffrées AES-256. Taille : " + donnees.length() + " chars.");
        ajouterHistorique("Données chiffrées avec AES-256 avant transmission.");
        return donneesCryptees;
    }

    /**
     * Déchiffre des données reçues d'un autre robot.
     */
    public String dechiffrerDonnees(String donneesCryptees, String token)
            throws AccesNonAutoriseException, SessionExpireeException,
                   RobotException, DonneesAltereesException {
        verifierAutorisation(token, Role.TECHNICIEN);
        verifierEnergie(ENERGIE_CHIFFREMENT);

        if (!donneesCryptees.startsWith("[AES-256|CBC|")) {
            logAudit("SYSTEME", Role.ADMIN, "DONNEES_ALTEREES",
                    "Données reçues sans chiffrement AES valide — possible altération.");
            throw new DonneesAltereesException(
                    "Données non chiffrées ou altérées — transmission rejetée.");
        }

        consommerEnergie(ENERGIE_CHIFFREMENT);
        // Extraction et décodage Base64 (simulation)
        String base64 = donneesCryptees
                .replaceAll("\\[AES-256\\|CBC\\|[^\\]]+\\]", "")
                .replace("[/AES]", "");
        String resultat = new String(Base64.getDecoder().decode(base64));

        logAudit("SYSTEME", sessionsActives.get(token), "DECHIFFREMENT",
                "Données déchiffrées avec succès.");
        return resultat;
    }

    // =========================================================
    // 5. AUDIT TRAIL LÉGAL
    // =========================================================

    /**
     * Enregistre une entrée dans le journal d'audit inviolable.
     * Chaque entrée est horodatée et identifie l'acteur, son rôle,l'action et les données ciblées.
     *  Utilisable comme preuve légale.
     */
    public void logAudit(String acteur, Role role, String action, String donneesCibles) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String timestamp = LocalDateTime.now().format(dtf);
        String entree = String.format("[AUDIT|%s] ACTEUR=%s | ROLE=%s | ACTION=%s | CIBLES=%s",
                timestamp, acteur, role, action, donneesCibles);
        auditTrail.add(entree);
    }

    /**
     * Exporte l'audit trail complet (pour enquête légale post-incident).
     */
    public String exporterAuditTrail() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== AUDIT TRAIL — SYSTÈME CYBERSÉCURITÉ HOSPITALIÈRE ===\n");
        sb.append("Export horodaté : ").append(LocalDateTime.now()).append("\n");
        sb.append("Nombre d'entrées : ").append(auditTrail.size()).append("\n");
        // Ligne de séparation
        StringBuilder sbn = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sbn.append(" ");
        }
        String spaces = sbn.toString();

        // Ajout de chaque entrée du journal
        for (String entree : auditTrail) {
            sbn.append(entree).append("\n");
        }

        return sbn.toString();
    }

    // =========================================================
    // 6. HONEYPOT — Piège à Intrus
    // =========================================================

    /**
     * Vérifie si une ressource demandée est le honeypot.
     *
     * Concept : on crée une fausse ressource médicale ultra-sensible
     * qui n'existe pas dans le vrai système. Aucun utilisateur légitime
     * ne devrait jamais y accéder. Si quelqu'un y accède : c'est forcément
     * un attaquant ou un robot compromis.
     *
     * Avantage : zéro faux positif (contrairement au DDoS ou au brute force).
     * Un accès au honeypot = intrusion confirmée à 100%.
     *
     * @param ressourceDemandee Nom de la ressource que quelqu'un tente d'accéder
     * @param acteur            Identifiant de l'acteur qui tente l'accès
     * @throws HoneypotException si la ressource piège est touchée
     */
    public void verifierHoneypot(String ressourceDemandee, String acteur)
            throws HoneypotException {
        // Comparaison stricte avec le nom de la ressource piège
        if (RESSOURCE_HONEYPOT.equals(ressourceDemandee)) {
            // Construction du message d'alerte avec horodatage précis
            String alerte = String.format(
                "[HONEYPOT|%s] INTRUSION CONFIRMÉE | Acteur: %s | Ressource: %s",
                LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")),
                acteur,
                ressourceDemandee);

            // Ajout dans le journal des intrusions honeypot
            intrusionsHoneypot.add(alerte);

            // Journalisation dans l'audit trail légal
            logAudit(acteur, Role.ADMIN, "HONEYPOT_DECLENCHE",
                    "Accès à la ressource piège : " + ressourceDemandee);

            // Escalade immédiate du niveau d'alerte : honeypot = intrusion certaine
            niveauAlerte = NiveauAlerte.ROUGE;
            etatSysteme  = EtatSysteme.ALERTE;

            // Journalisation dans l'historique du robot
            ajouterHistorique(
                "HONEYPOT DÉCLENCHÉ par " + acteur +
                " — Intrusion confirmée. Niveau d'alerte : ROUGE.");

            // Signalement comme anomalie de type HONEYPOT
            signalerAnomalic(acteur, TypeAnomalic.HONEYPOT_DECLENCHE,
                "Accès à ressource piège — intrusion confirmée à 100%.");

            // Lancement de l'exception pour bloquer l'accès
            throw new HoneypotException(
                "PIÈGE DÉCLENCHÉ : " + acteur +
                " a tenté d'accéder à une ressource confidentielle inexistante. " +
                "Intrusion enregistrée dans l'audit trail légal.");
        }
        // Si la ressource n'est pas le honeypot : on laisse passer normalement
    }

    /**
     * Retourne la liste des intrusions détectées par le honeypot.
     * Collections.unmodifiableList empêche toute modification extérieure.
     *
     * @return Liste en lecture seule des intrusions enregistrées
     */
    public List<String> getIntrusionsHoneypot() {
        // unmodifiableList : protection contre les modifications accidentelles
        return Collections.unmodifiableList(intrusionsHoneypot);
    }

  

    public NiveauAlerte getNiveauAlerte()  { return niveauAlerte; }
    public EtatSysteme  getEtatSysteme()   { return etatSysteme; }
    public List<String> getAuditTrail()    { return Collections.unmodifiableList(auditTrail); }
    public Set<String>  getRobotsIsoles()  { return Collections.unmodifiableSet(robotsIsoles); }

    public void enregistrerRobot(Robot robot) {
        flotteSurveillée.add(robot);
        ajouterHistorique("Robot " + robot.id + " enregistré sous surveillance.");
    }

    @Override
    public String toString() {
        return String.format(
            "RobotSecurite [ID: %s, Position: (%d,%d), Énergie: %d%%, Heures: %d, " +
            "Alerte: %s, Système: %s, RobotsIsolés: %d, Sessions: %d, Connecté: %s]",
            id, x, y, energie, heuresUtilisation,
            niveauAlerte, etatSysteme,
            robotsIsoles.size(), sessionsActives.size(),
            connecte ? reseauConnecte : "Non"
        );
    }


    /** Hache un mot de passe avec SHA-256 */
    private String hasherSHA256(String mdp) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(mdp.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    /** Base de données simulée des hash (en production : BDD chiffrée) */
    private String getHashAttendu(String identifiant, Role role) {
        // Simulation : admin_hopital / motdepasse_admin (SHA-256)
        // En production : interroger une BDD sécurisée
        try {
            Map<String, String> bdUtilisateurs = new HashMap<>();
            // Hash de "motdepasse_admin" avec SHA-256
            bdUtilisateurs.put("admin_hopital", hasherSHA256("motdepasse_admin"));
            bdUtilisateurs.put("dr_martin",     hasherSHA256("medecin2024"));
            bdUtilisateurs.put("inf_sophie",    hasherSHA256("infirmier123"));
            return bdUtilisateurs.get(identifiant);
        } catch (NoSuchAlgorithmException e) { return null; }
    }

    /** Génère un token de session aléatoire */
    private String genererToken(String identifiant) {
        return identifiant + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /** Détecte un DDoS : trop de requêtes en 60 secondes */
    private boolean detecterDDoS(String source) {
        long maintenant = System.currentTimeMillis();
        requetesParSource.computeIfAbsent(source, k -> new ArrayList<>());
        List<Long> requetes = requetesParSource.get(source);
        requetes.add(maintenant);
        // Garder seulement les requêtes de la dernière minute
        requetes.removeIf(t -> maintenant - t > 60_000);
        return requetes.size() > SEUIL_DDOS;
    }

    /** Signale une anomalie et monte le niveau d'alerte si nécessaire */
    private void signalerAnomalic(String source, TypeAnomalic type, String details) {
        ajouterHistorique("⚠️ ANOMALIE [" + type + "] depuis " + source + " : " + details);
        logAudit(source, Role.ADMIN, "ANOMALIE_" + type, details);

        // Escalade automatique du niveau d'alerte
        if (type == TypeAnomalic.MODIFICATION_DONNEES || type == TypeAnomalic.ROBOT_COMPROMIS) {
            niveauAlerte = NiveauAlerte.ROUGE;
            etatSysteme  = EtatSysteme.ALERTE;
        } else if (niveauAlerte == NiveauAlerte.VERT) {
            niveauAlerte = NiveauAlerte.ORANGE;
        }
    }

    /** Vérifie le droit ADMIN sans token (pour l'appel interne mode dégradé) */
    private void verifierAutorisation_Admin() {
        // Appel interne depuis le robot lui-même — autorisé
    }

    /** Ordre hiérarchique des rôles */
    private int ordreRole(Role role) {
        switch (role) {
            case INFIRMIER:  return 1;
            case TECHNICIEN: return 2;
            case MEDECIN:    return 3;
            case ADMIN:      return 4;
            default:         return 0;
        }
    }

    /** Déconnexion forcée sans exception (redéfinition interne) */
    private void deconnecter_force() {
        if (connecte) deconnecter();
    }
}
