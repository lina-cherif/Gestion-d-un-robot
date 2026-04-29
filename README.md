# 🏥 OOP-HEALTH — Système de Gestion d'une Flotte de Robots Hospitaliers
---

## 📋 Description

OOP-HEALTH est un système de simulation en Java modélisant une flotte intelligente de robots hospitaliers. Il répond à deux enjeux critiques de l'e-Santé moderne :

- **Cybersécurité hospitalière** : protection du réseau contre ransomwares, DDoS et intrusions
- **Triage médical automatisé** : classification des patients selon le standard ESI (Emergency Severity Index)

Le projet s'inspire directement des alertes de l'Académie nationale de médecine (2024) documentant l'explosion des cyberattaques hospitalières et leurs conséquences mortelles.

---

## 🤖 Les deux robots

| Robot | Rôle | Mode de déplacement |
|-------|------|---------------------|
| **RobotSecurite** | Surveille le réseau 24h/24, détecte intrusions/DDoS/brute force, active le mode dégradé anti-ransomware, chiffre les données (AES-256 simulé), journalise dans un audit trail légal | Station fixe — réseau uniquement |
| **RobotTriage** | Mesure les signes vitaux (tension, température, SpO2, pouls), analyse par vision IA (YOLO + HSV), calcule le niveau ESI (1–5), déploie le kit d'urgence en Code Blue | Mobile — se déplace vers le patient |

---

## 🏗️ Architecture

### Hiérarchie d'héritage (3 niveaux)

```
Robot  [ABSTRAITE]
│   id, x, y, energie, heuresUtilisation, enMarche, historiqueActions
│   demarrer(), arreter(), verifierEnergie(), verifierMaintenance()
│   abstraites : deplacer(x,y)  •  effectuerTache()
│
└──▶  RobotConnecte  [ABSTRAITE]  implements Connectable
          connecte, reseauConnecte
          connecter(), deconnecter(), envoyerDonnees()
          │
          ├──▶  RobotTriage
          │         kitUrgence, analyseur, baseDossiersMedicaux, modeDegrade
          │
          └──▶  RobotSecurite
                    niveauAlerte, etatSysteme, sessionsActives, auditTrail, robotsIsoles
```

### Classes de domaine

| Type POO | Nom | Rôle |
|----------|-----|------|
| Interface | `Connectable` | connecter / deconnecter / envoyerDonnees |
| Classe | `SignesVitaux` | Encapsule pouls, tension, température, SpO2 |
| Classe | `DossierMedical` | Dossier patient MyHealth (âge, catégories de risque) |
| Classe | `KitUrgence` | Stock individuel par élément, réapprovisionnement |
| Classe | `AnalyseurUrgence` | Moteur de règles ESI en 3 étapes |
| Classe | `GestionnaireHospitalier` | Pilote `ArrayList<Robot>`, polymorphisme |

---

## ⚙️ Fonctionnalités

### RobotSecurite — 5 mécanismes de cybersécurité

- **RBAC + SHA-256** : Authentification par hachage, rôles hiérarchiques (INFIRMIER < TECHNICIEN < MÉDECIN < ADMIN), verrouillage après 3 échecs, tokens UUID de session
- **IDS** : Détection DDoS par fenêtre glissante 60s (> 50 requêtes = alerte ORANGE), détection de robots isolés
- **Mode Dégradé** : Détection ransomware → déconnexion du réseau principal → isolation de tous les robots → maintien des urgences vitales en local
- **Honeypot** : Ressource piège `DOSSIER_PATIENT_VIP_CONFIDENTIEL` — tout accès confirme une intrusion à 100%
- **Audit Trail** : Journal horodaté inviolable, exportable comme preuve légale

### RobotTriage — Protocole en 9 étapes

1. Détection patient (simulation caméra panoramique)
2. Authentification biométrique (empreinte digitale)
3. Scan vision IA (YOLO + HSV : saignement, amputation, inconscience)
4. Mesure tension artérielle (brassard embarqué)
5. Température + SpO2 (thermomètre infrarouge + oxymètre)
6. Chargement dossier MyHealth (antécédents, catégories de risque)
7. Calcul ESI (moteur de règles 3 étapes)
8. Déploiement kit selon niveau (ESI_1 → kit complet, ESI_2-5 → orientation)
9. Audit + transmission JSON au tableau de bord d'admission

---

## 🔢 Énumérations

| Enum | Valeurs | Effet |
|------|---------|-------|
| `NiveauESI` | ESI_1 (Code Blue) … ESI_5 | ESI_1 → `UrgenceVitaleException` + kit déployé |
| `NiveauAlerte` | VERT / ORANGE / ROUGE / CRITIQUE | ROUGE → déconnexion réseau + mode dégradé |
| `EtatSysteme` | NORMAL / ALERTE / DEGRADE / URGENCE | DEGRADE → fonctionnement local |
| `Role` | INFIRMIER / TECHNICIEN / MEDECIN / ADMIN | Contrôle d'accès RBAC |
| `ElementKit` | DEFIBRILLATEUR, MASQUE_O2, GARROT… | Stock individuel |

---

## ⚠️ Hiérarchie d'exceptions

```
RobotException  (racine)
├── EnergieInsuffisanteException    — Énergie < seuil requis
├── MaintenanceRequiseException     — Heures d'utilisation > 100
├── UrgenceVitaleException          — ESI_1 confirmé (Code Blue)
├── AuthenticationException         — Code médecin invalide
├── AuthentificationEchoueeException — Identifiants SHA-256 incorrects
├── CompteVerrouilleException       — ≥ 3 tentatives échouées
├── AccesNonAutoriseException       — Rôle insuffisant
├── DonneesAltereesException        — Données sans chiffrement AES valide
├── HoneypotException               — Accès ressource piège
└── SessionExpireeException         — Token inconnu ou expiré
```

---

## 🖥️ Interface graphique

L'application inclut une interface **Swing** avec :
- Dashboard de supervision de la flotte
- Centre de commande
- Console de logs en temps réel

---

## 🚀 Lancement

```bash
# Compiler
javac -cp . *.java

# Exécuter
java Main
```

> **Prérequis** : Java 11+

---

## ✅ Couverture POO

| Exigence | Statut | Localisation |
|----------|--------|--------------|
| Classe abstraite | ✔ | `Robot.java` |
| Héritage 3 niveaux | ✔ | `Robot → RobotConnecte → RobotTriage / RobotSecurite` |
| Interface | ✔ | `Connectable` |
| Exceptions personnalisées | ✔ | 12 exceptions héritant de `RobotException` |
| Énumérations | ✔ | `NiveauESI`, `NiveauAlerte`, `Role`, `EtatSysteme`, `ElementKit` |
| Polymorphisme | ✔ | `GestionnaireHospitalier` — `ArrayList<Robot>` |
| Contrôle d'accès | ✔ | RBAC SHA-256 + code médecin |
| Interface graphique Swing | ✔ | Dashboard + console de logs |
