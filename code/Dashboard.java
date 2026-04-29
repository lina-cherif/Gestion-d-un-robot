package projetrobot;

import javax.swing.*;

import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;


/**
 * DASHBOARD v4 – 1 robot RT-ALPHA, 6 patients (2×ESI1,1×ESI2,1×ESI3,1×ESI4,1×ESI5)
 */
public class Dashboard extends JFrame {

    static final Color BG_MAIN   = new Color(10,  15,  26);
    static final Color BG_PANEL  = new Color(13,  22,  38);
    static final Color BG_CARD   = new Color(18,  30,  52);
    static final Color BG_DEEP   = new Color( 5,  11,  20);
    static final Color C_CYAN    = new Color(126, 207, 255);
    static final Color C_GREEN   = new Color( 34, 197,  94);
    static final Color C_AMBER   = new Color(245, 158,  11);
    static final Color C_RED     = new Color(239,  68,  68);
    static final Color C_BLUE    = new Color( 59, 143, 212);
    static final Color C_YELLOW  = new Color(212, 225,  87);
    static final Color C_TEAL    = new Color( 77, 208, 160);
    static final Color C_PURPLE  = new Color(167, 139, 250);
    static final Color C_ORANGE  = new Color(251, 146,  60);
    static final Color BORDER    = new Color( 30,  58,  95);
    static final Color TEXT_MAIN = new Color(224, 240, 255);
    static final Color TEXT_DIM  = new Color( 74, 138, 181);

    // Polices augmentées
    static final Font FT_TITLE = new Font("Courier New", Font.BOLD,  14);
    static final Font FT_MONO  = new Font("Courier New", Font.PLAIN, 13);
    static final Font FT_SMALL = new Font("Courier New", Font.PLAIN, 12);
    static final Font FT_TINY  = new Font("Courier New", Font.PLAIN, 11);
    static final Font FT_BIG   = new Font("Courier New", Font.BOLD,  22);
    static final Font FT_MED   = new Font("Courier New", Font.BOLD,  15);

    static final DateTimeFormatter HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");

    SallePanel    sallePanel;
    CameraPanel   cameraPanel;
    BodyScanPanel bodyScanPanel;
    VitauxPanel   vitauxPanel;
    FilePanel     filePanel;
    KitPanelUI    kitPanelUI;
    LogPanelUI    logPanelUI;
    HeaderPanel   headerPanel;
    CmdPanel      cmdPanel;
    DossierPanel  dossierPanel;

    // Robot 1 (seul robot affiché)
    JProgressBar barEnergie1;
    JLabel lblEnergie1, lblMode1, lblReseau1, lblKit1, lblMission1;

    // Stubs robot 2 (engine les référence, ne s'affichent pas)
    JProgressBar barEnergie2 = new JProgressBar();
    JLabel lblEnergie2 = new JLabel(), lblMode2 = new JLabel(),
           lblReseau2  = new JLabel(), lblKit2  = new JLabel(),
           lblMission2 = new JLabel();

    final SimulationEngine engine;

    public Dashboard() {
        super("Système Robot Hospitalier — Tableau de Bord  |  Urgences");
        this.engine = new SimulationEngine(this);
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1640, 980);
        setMinimumSize(new Dimension(1400, 820));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_MAIN);
        buildUI();
        setResizable(false);
        engine.start();
        startClock();
    }
    private void lockProportions(JComponent c) {
        c.setPreferredSize(new Dimension(0, 0));
        c.setMinimumSize(new Dimension(0, 0));
    }
    private void buildUI() {
        setLayout(new BorderLayout(3, 3));
        headerPanel = new HeaderPanel(this);
        add(headerPanel, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(3, 0));
        body.setBackground(BG_MAIN);
        body.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));

        JPanel sidebar = buildSidebar();
        sidebar.setPreferredSize(new Dimension(270, 0));
        body.add(sidebar, BorderLayout.WEST);
        body.add(buildCenter(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    private JPanel buildSidebar() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_MAIN);

        p.add(buildRobotCard());
        p.add(Box.createVerticalStrut(4));

        kitPanelUI = new KitPanelUI("RT-ALPHA Kit");
        kitPanelUI.setMaximumSize(new Dimension(270, 210));
        p.add(kitPanelUI);
        p.add(Box.createVerticalStrut(4));

        dossierPanel = new DossierPanel();
        p.add(dossierPanel);
        p.add(Box.createVerticalStrut(4));

        cmdPanel = new CmdPanel(this);
        p.add(cmdPanel);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel buildRobotCard() {
        JPanel card = mkPanel("RT-ALPHA — Robot de Triage");
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(270, 150));

        JPanel top = new JPanel(new BorderLayout(4, 0));
        top.setOpaque(false);
        top.add(mkLabel("🤖 RT-ALPHA", C_CYAN, FT_MED), BorderLayout.WEST);
        JLabel badge = mkBadge("EN LIGNE", C_GREEN);
        engine.lblStatusBadge1 = badge;
        top.add(badge, BorderLayout.EAST);
        card.add(top);
        card.add(Box.createVerticalStrut(3));

        card.add(mkLabel("Énergie", TEXT_DIM, FT_TINY));
        card.add(Box.createVerticalStrut(2));

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(100); bar.setForeground(C_GREEN); bar.setBackground(BORDER);
        bar.setBorderPainted(false); bar.setStringPainted(false);
        bar.setPreferredSize(new Dimension(0, 5));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        card.add(bar);
        JLabel lblEn = mkLabel("100%", C_GREEN, FT_SMALL);
        card.add(lblEn);
        card.add(Box.createVerticalStrut(4));

        JPanel grid = new JPanel(new GridLayout(3, 2, 2, 2));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        JLabel lblMd = addStat(grid, "Mode",   "NORMAL",   C_GREEN);
        JLabel lblRs = addStat(grid, "Réseau", "CONNECTÉ", C_GREEN);
        JLabel lblKt = addStat(grid, "Kit",    "COMPLET",  C_GREEN);
        card.add(grid);

        card.add(Box.createVerticalStrut(3));
        JLabel lblMs = mkLabel("En attente...", TEXT_DIM, FT_TINY);
        lblMs.setAlignmentX(LEFT_ALIGNMENT);
        card.add(lblMs);

        barEnergie1 = bar; lblEnergie1 = lblEn;
        lblMode1 = lblMd; lblReseau1 = lblRs; lblKit1 = lblKt;
        lblMission1 = lblMs; engine.lblMission = lblMs;
        return card;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_MAIN);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.insets = new Insets(2, 2, 2, 2);

     // Carte salle : on augmente sa largeur (weightx passe de 0.60 à 0.75)
        sallePanel = new SallePanel(engine);
        g.gridx=0; g.gridy=0; g.gridwidth=2; g.gridheight=2;
        g.weightx=0.75; g.weighty=0.5; 
        p.add(sallePanel, g);

        // Caméra : on diminue sa taille (weightx/y passent à 0.25)
        cameraPanel = new CameraPanel();
        g.gridx=2; g.gridy=0; g.gridwidth=2; g.gridheight=1;
        g.weightx=0.25; g.weighty=0.15; 
        p.add(cameraPanel, g);

        // Scan corporel (bas droit haut)
        bodyScanPanel = new BodyScanPanel();
        lockProportions(bodyScanPanel); // ◄ Verrouille le contenu
        g.gridx=2; g.gridy=1; g.gridwidth=1; g.gridheight=1;
        g.weightx=0.20; g.weighty=0.28;
        p.add(bodyScanPanel, g);

        // File patients (bas droit bas)
        filePanel = new FilePanel(this);
        lockProportions(filePanel); // ◄ Verrouille le contenu
        g.gridx=3; g.gridy=1; g.gridwidth=1; g.gridheight=1;
        g.weightx=0.20; g.weighty=0.28;
        p.add(filePanel, g);

        // Vitaux (3e ligne, pleine largeur)
        vitauxPanel = new VitauxPanel();
        lockProportions(vitauxPanel); // ◄ Verrouille le contenu
        g.gridx=0; g.gridy=2; g.gridwidth=4; g.gridheight=1;
        g.weightx=1.0; g.weighty=0.18;
        p.add(vitauxPanel, g);

        // Logs (4e ligne, pleine largeur)
        logPanelUI = new LogPanelUI();
        lockProportions(logPanelUI); // ◄ Verrouille le contenu
        g.gridx=0; g.gridy=3; g.gridwidth=4; g.gridheight=1;
        g.weightx=1.0; g.weighty=0.16;
        p.add(logPanelUI, g);

        return p;
    }

    // API
    void updateSalle(SalleState s)                            { SwingUtilities.invokeLater(() -> sallePanel.setState(s)); }
    void updateCamera(CameraState s)                          { SwingUtilities.invokeLater(() -> cameraPanel.setState(s)); }
    void updateBodyScan(ScenarDef sc, NiveauESI esi)          { SwingUtilities.invokeLater(() -> bodyScanPanel.update(sc, esi)); }
    void updateVitaux(ScenarDef sc, NiveauESI esi)            { SwingUtilities.invokeLater(() -> vitauxPanel.update(sc, esi)); }
    void updateFile(List<PatientTriage> file)                  { SwingUtilities.invokeLater(() -> filePanel.update(file)); }
    void updateHeader(NiveauESI esi)                          { SwingUtilities.invokeLater(() -> headerPanel.update(esi)); }
    void addLog(String msg, LogType type)                     { SwingUtilities.invokeLater(() -> logPanelUI.add(msg, type)); }
    void updateKit(int rid, KitUrgence kit)                   { if (rid==1) SwingUtilities.invokeLater(() -> kitPanelUI.update(kit)); }
    void showDossier(String pid, DossierMedical d, NiveauESI e, String dept, String salle) { SwingUtilities.invokeLater(() -> dossierPanel.show(pid,d,e,dept,salle)); }
    void highlightPatient(String pid)                         { SwingUtilities.invokeLater(() -> sallePanel.setSelectedPatient(pid)); }

    void updateRobotSidebar(int robotId, int energie, boolean modeDegrade,
                             boolean connecte, boolean kitComplet, boolean enMarche) {
        if (robotId != 1) return; // seul RT-ALPHA visible
        SwingUtilities.invokeLater(() -> {
            Color ec = energie > 60 ? C_GREEN : energie > 30 ? C_AMBER : C_RED;
            barEnergie1.setValue(energie); barEnergie1.setForeground(ec);
            lblEnergie1.setText(energie + "%"); lblEnergie1.setForeground(ec);
            lblMode1.setText(modeDegrade ? "DÉGRADÉ" : "NORMAL");
            lblMode1.setForeground(modeDegrade ? C_AMBER : C_GREEN);
            lblReseau1.setText(connecte ? "CONNECTÉ" : "HORS LIGNE");
            lblReseau1.setForeground(connecte ? C_GREEN : C_RED);
            lblKit1.setText(kitComplet ? "COMPLET" : "⚠ INCOMPLET");
            lblKit1.setForeground(kitComplet ? C_GREEN : C_AMBER);
            if (engine.lblStatusBadge1 != null) {
                engine.lblStatusBadge1.setText(enMarche ? " EN LIGNE " : " ARRÊTÉ ");
                engine.lblStatusBadge1.setBackground(enMarche ? C_GREEN : C_AMBER);
            }
        });
    }

    static JPanel mkPanel(String titre) {
        JPanel p = new JPanel();
        p.setBackground(BG_PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                    BorderFactory.createEmptyBorder(), "  " + titre.toUpperCase(),
                    TitledBorder.LEFT, TitledBorder.TOP, FT_TINY, TEXT_DIM),
                BorderFactory.createEmptyBorder(2, 6, 6, 6))));
        return p;
    }
    static JLabel mkLabel(String t, Color c, Font f) { JLabel l=new JLabel(t); l.setForeground(c); l.setFont(f); return l; }
    static JLabel mkBadge(String t, Color c) {
        JLabel l = new JLabel(" "+t+" "); l.setFont(FT_TINY); l.setForeground(BG_MAIN);
        l.setBackground(c); l.setOpaque(true); l.setBorder(BorderFactory.createEmptyBorder(1,3,1,3)); return l;
    }
    static JLabel addStat(JPanel p, String key, String val, Color c) {
        JLabel k=new JLabel(key+":"); k.setFont(FT_TINY); k.setForeground(TEXT_DIM);
        JLabel v=new JLabel(val);     v.setFont(FT_SMALL); v.setForeground(c);
        p.add(k); p.add(v); return v;
    }
    static Color esiColor(NiveauESI n) {
        if (n==null) return TEXT_DIM;
        switch(n){case ESI_1:return C_RED;case ESI_2:return C_AMBER;case ESI_3:return C_YELLOW;case ESI_4:return C_TEAL;default:return new Color(91,200,245);}
    }
    static Color esiBg(NiveauESI n) {
        if (n==null) return BG_CARD;
        switch(n){case ESI_1:return new Color(61,13,13);case ESI_2:return new Color(45,26,0);case ESI_3:return new Color(28,34,0);case ESI_4:return new Color(0,36,32);default:return new Color(0,26,45);}
    }
    private void startClock() { new javax.swing.Timer(1000, e -> headerPanel.tickClock()).start(); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Dashboard().setVisible(true));
    }
}

// ─────────────────────────────────────────────────────────────────────
//  TYPES DE DONNÉES
// ─────────────────────────────────────────────────────────────────────

enum LogType { INFO, SUCCESS, WARNING, CRITICAL }

class PatientTriage implements Comparable<PatientTriage> {
    final String id, desc; final NiveauESI esi;
    PatientTriage(String id, String desc, NiveauESI esi){ this.id=id; this.desc=desc; this.esi=esi; }
    @Override public int compareTo(PatientTriage o){
        return Integer.compare(Integer.parseInt(esi.name().replace("ESI_","")),
                               Integer.parseInt(o.esi.name().replace("ESI_","")));
    }
}

class ScenarDef {
    final String patientId, nom, desc;
    final SignesVitaux signes; final ResultatVision vision;
    final DossierMedical dossier; final boolean modeDegrade;
    float px, py;
    String departement="MÉDECINE GÉNÉRALE", salle="Salle 01";
    ScenarDef(String pid,String nom,String desc,SignesVitaux sv,ResultatVision rv,DossierMedical dm,boolean deg){
        this.patientId=pid;this.nom=nom;this.desc=desc;this.signes=sv;this.vision=rv;this.dossier=dm;this.modeDegrade=deg;
    }
}

class SalleState {
    java.util.List<PatientMarker>  patients   = new java.util.ArrayList<>();
    java.util.List<ChairMarker>    chaises    = new java.util.ArrayList<>();
    java.util.List<ObstacleItem>   obstacles  = new java.util.ArrayList<>();
    java.util.List<float[]>        chemin1    = new java.util.ArrayList<>();
    java.util.List<float[]>        chemin2    = new java.util.ArrayList<>();
    java.util.List<InfirmierMarker> infirmiers = new java.util.ArrayList<>();
    float robotX1=0.04f, robotY1=0.88f;
    float robotX2=0.04f, robotY2=0.88f;
    boolean robot1EnMarche=true, robot2EnMarche=false;
    String robotStatus1="En attente", robotStatus2="";
    String robotPatientId=null, robot2PatientId=null;
    boolean codeBlue=false;
    String selectedPatientId=null;
}

class PatientMarker {
    String id; float x,y; NiveauESI esi;
    boolean enCours, traite;
    int accompagnant; // 0=seul,1=accompagné,2=inconscient+accompagné
    PatientMarker(String id,float x,float y,int acc){this.id=id;this.x=x;this.y=y;this.accompagnant=acc;}
}

class InfirmierMarker {
    float x,y; String target; boolean actif; int animPhase;
    InfirmierMarker(float x,float y,String target){this.x=x;this.y=y;this.target=target;this.actif=true;}
}

class ChairMarker {
    float x,y; boolean occupee; String occupantId; boolean occupantEstPatient;
    ChairMarker(float x,float y,boolean occ,String oid,boolean isPatient){
        this.x=x;this.y=y;this.occupee=occ;this.occupantId=oid;this.occupantEstPatient=isPatient;
    }
}

class ObstacleItem {
    float x,y; String type; boolean mobile;
    ObstacleItem(float x,float y,String type,boolean mobile){this.x=x;this.y=y;this.type=type;this.mobile=mobile;}
}

class CameraState {
    enum Phase { WAITING, MOVING_CLEAR, MOVING_OBSTACLE, THUMB_SCAN, BODY_SCAN, MEASURING, ESI_RESULT, RECHARGING, STOPPED }
    Phase phase=Phase.WAITING;
    String patientId="", obstacleType="";
    NiveauESI esiResult=null;
    ResultatVision vision=null;
    SignesVitaux signes=null;    // signes vitaux du patient consulté
    ScenarDef scenario=null;    // scénario complet synchronisé
    int robotId=1;
}
