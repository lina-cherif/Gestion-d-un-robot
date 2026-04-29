package projetrobot;


import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
/**
 * ═══════════════════════════════════════════════════════════════════════
 *  Dashboard2 — Tableau de bord flotte 3 robots + GestionnaireHospitalier
 * ═══════════════════════════════════════════════════════════════════════
 *
 *  Étend le dashboard original avec :
 *   - 3 cartes robot dans la sidebar (RT-ALPHA, RL-BETA, RT-GAMMA)
 *   - Commandes gestionnaire (Alerte générale, Rapport flotte, Sélection)
 *   - Moteur SimulationEngine2 exploitant GestionnaireHospitalier
 */
public class Dashboard2 extends JFrame {

    // ── Palette (identique à Dashboard) ──────────────────────────────
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

    // ── Couleurs par robot ────────────────────────────────────────────
    static final Color C_ROBOT1  = C_CYAN;    // RT-ALPHA — triage
    static final Color C_ROBOT2  = C_ORANGE;  // RL-BETA  — livraison
    static final Color C_ROBOT3  = C_PURPLE;  // RT-GAMMA — triage secours

    // ── Polices ───────────────────────────────────────────────────────
    static final Font FT_TITLE = new Font("Courier New", Font.BOLD,  14);
    static final Font FT_MONO  = new Font("Courier New", Font.PLAIN, 13);
    static final Font FT_SMALL = new Font("Courier New", Font.PLAIN, 12);
    static final Font FT_TINY  = new Font("Courier New", Font.PLAIN, 11);
    static final Font FT_BIG   = new Font("Courier New", Font.BOLD,  22);
    static final Font FT_MED   = new Font("Courier New", Font.BOLD,  15);

    static final DateTimeFormatter HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ── Composants principaux ─────────────────────────────────────────
    SallePanel2    sallePanel;
    CameraPanel    cameraPanel;
    BodyScanPanel  bodyScanPanel;
    VitauxPanel    vitauxPanel;
    FilePanel2     filePanel;
    KitPanelUI2    kitPanel1, kitPanel3;
    LogPanelUI2    logPanelUI;
    HeaderPanel2   headerPanel;
    CmdPanel2      cmdPanel;
    DossierPanel   dossierPanel;
    FlottePanel    flottePanel; // résumé flotte en haut de la sidebar

    // ── Widgets robot 1 (RT-ALPHA) ────────────────────────────────────
    JProgressBar barEnergie1;
    JLabel lblEnergie1, lblMode1, lblReseau1, lblKit1, lblMission1;
    JLabel lblStatusBadge1;

    // ── Widgets robot 2 (RL-BETA) ─────────────────────────────────────
    JProgressBar barEnergie2;
    JLabel lblEnergie2, lblMode2, lblReseau2, lblKit2, lblMission2;
    JLabel lblStatusBadge2;

    // ── Widgets robot 3 (RT-GAMMA) ────────────────────────────────────
    JProgressBar barEnergie3;
    JLabel lblEnergie3, lblMode3, lblReseau3, lblKit3, lblMission3;
    JLabel lblStatusBadge3;

    // engine field
    SimulationEngine2 engine;

    // ═════════════════════════════════════════════════════════════════
    //  CONSTRUCTEUR
    // ═════════════════════════════════════════════════════════════════

    public Dashboard2() {
        super("Système Robot Hospitalier v2 — Flotte 3 Robots | GestionnaireHospitalier");
        this.engine = new SimulationEngine2(this);
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1700, 1020);
        setMinimumSize(new Dimension(1500, 860));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_MAIN);
        buildUI();
        setResizable(false);
        engine.start();
        startClock();
    }

    // ═════════════════════════════════════════════════════════════════
    //  CONSTRUCTION UI
    // ═════════════════════════════════════════════════════════════════

    private void buildUI() {
        setLayout(new BorderLayout(3, 3));
        headerPanel = new HeaderPanel2(this);
        add(headerPanel, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(3, 0));
        body.setBackground(BG_MAIN);
        body.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));

        JPanel sidebar = buildSidebar();
        sidebar.setPreferredSize(new Dimension(285, 0));
        body.add(sidebar, BorderLayout.WEST);
        body.add(buildCenter(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    // ── SIDEBAR ───────────────────────────────────────────────────────

    private JPanel buildSidebar() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_MAIN);

        // Résumé flotte (gestionnaire)
        flottePanel = new FlottePanel();
        flottePanel.setMaximumSize(new Dimension(285, 55));
        p.add(flottePanel);
        p.add(Box.createVerticalStrut(3));

        // Carte RT-ALPHA
        p.add(buildRobotCard1());
        p.add(Box.createVerticalStrut(3));

        // Carte RL-BETA
        p.add(buildRobotCard2());
        p.add(Box.createVerticalStrut(3));

        // Carte RT-GAMMA
        p.add(buildRobotCard3());
        p.add(Box.createVerticalStrut(3));

        // Kit RT-ALPHA
        kitPanel1 = new KitPanelUI2("RT-ALPHA Kit");
        kitPanel1.setMaximumSize(new Dimension(285, 180));
        p.add(kitPanel1);
        p.add(Box.createVerticalStrut(3));

        // Dossier
        dossierPanel = new DossierPanel();
        dossierPanel.setMaximumSize(new Dimension(285, 120));
        p.add(dossierPanel);
        p.add(Box.createVerticalStrut(3));

        // Commandes
        cmdPanel = new CmdPanel2(this);
        p.add(cmdPanel);
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── Carte Robot 1 — RT-ALPHA (Triage) ────────────────────────────

    private JPanel buildRobotCard1() {
        JPanel card = mkPanel("RT-ALPHA — Triage");
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(285, 120));

        JPanel top = new JPanel(new BorderLayout(4, 0));
        top.setOpaque(false);
        top.add(mkLabel("✚ RT-ALPHA", C_ROBOT1, FT_MED), BorderLayout.WEST);
        JLabel badge = mkBadge("EN LIGNE", C_GREEN);
        lblStatusBadge1 = badge;
        engine.lblStatusBadge1 = badge;
        top.add(badge, BorderLayout.EAST);
        card.add(top);
        card.add(Box.createVerticalStrut(3));

        card.add(mkLabel("Énergie", TEXT_DIM, FT_TINY));
        card.add(Box.createVerticalStrut(2));
        JProgressBar bar = mkBar(); card.add(bar);
        JLabel lblEn = mkLabel("100%", C_GREEN, FT_SMALL); card.add(lblEn);
        card.add(Box.createVerticalStrut(3));

        JPanel grid = new JPanel(new GridLayout(2, 2, 2, 2));
        grid.setOpaque(false); grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JLabel lblMd = addStat(grid, "Mode",   "NORMAL",    C_GREEN);
        JLabel lblRs = addStat(grid, "Réseau", "CONNECTÉ",  C_GREEN);
        JLabel lblKt = addStat(grid, "Kit",    "COMPLET",   C_GREEN);
        card.add(grid);

        JLabel lblMs = mkLabel("En attente...", TEXT_DIM, FT_TINY);
        lblMs.setAlignmentX(LEFT_ALIGNMENT);
        card.add(Box.createVerticalStrut(2)); card.add(lblMs);

        barEnergie1 = bar; lblEnergie1 = lblEn;
        lblMode1 = lblMd; lblReseau1 = lblRs; lblKit1 = lblKt;
        lblMission1 = lblMs; engine.lblMission1 = lblMs; engine.lblMission = lblMs;
        return card;
    }

    // ── Carte Robot 2 — RL-BETA (Livraison) ──────────────────────────

    private JPanel buildRobotCard2() {
        JPanel card = mkPanel("RL-BETA — Livraison");
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(285, 120));

        JPanel top = new JPanel(new BorderLayout(4, 0));
        top.setOpaque(false);
        top.add(mkLabel("⬡ RL-BETA", C_ROBOT2, FT_MED), BorderLayout.WEST);
        JLabel badge = mkBadge("EN LIGNE", C_GREEN);
        lblStatusBadge2 = badge;
        engine.lblStatusBadge2 = badge;
        top.add(badge, BorderLayout.EAST);
        card.add(top);
        card.add(Box.createVerticalStrut(3));

        card.add(mkLabel("Énergie", TEXT_DIM, FT_TINY));
        card.add(Box.createVerticalStrut(2));
        JProgressBar bar = mkBar(); bar.setValue(78); bar.setForeground(C_GREEN); card.add(bar);
        JLabel lblEn = mkLabel("78%", C_GREEN, FT_SMALL); card.add(lblEn);
        card.add(Box.createVerticalStrut(3));

        JPanel grid = new JPanel(new GridLayout(2, 2, 2, 2));
        grid.setOpaque(false); grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JLabel lblMd = addStat(grid, "Colis",  "Aucun",     C_TEAL);
        JLabel lblRs = addStat(grid, "Réseau", "CONNECTÉ",  C_GREEN);
        JLabel lblKt = addStat(grid, "Dest.",  "N/A",       TEXT_DIM);
        card.add(grid);

        JLabel lblMs = mkLabel("En attente...", TEXT_DIM, FT_TINY);
        lblMs.setAlignmentX(LEFT_ALIGNMENT);
        card.add(Box.createVerticalStrut(2)); card.add(lblMs);

        barEnergie2 = bar; lblEnergie2 = lblEn;
        lblMode2 = lblMd; lblReseau2 = lblRs; lblKit2 = lblKt;
        lblMission2 = lblMs; engine.lblMission2 = lblMs;
        return card;
    }

    // ── Carte Robot 3 — RT-GAMMA (Triage secours) ─────────────────────

    private JPanel buildRobotCard3() {
        JPanel card = mkPanel("RT-GAMMA — Triage Secours");
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(285, 120));

        JPanel top = new JPanel(new BorderLayout(4, 0));
        top.setOpaque(false);
        top.add(mkLabel("★ RT-GAMMA", C_ROBOT3, FT_MED), BorderLayout.WEST);
        JLabel badge = mkBadge("EN LIGNE", C_GREEN);
        lblStatusBadge3 = badge;
        engine.lblStatusBadge3 = badge;
        top.add(badge, BorderLayout.EAST);
        card.add(top);
        card.add(Box.createVerticalStrut(3));

        card.add(mkLabel("Énergie", TEXT_DIM, FT_TINY));
        card.add(Box.createVerticalStrut(2));
        JProgressBar bar = mkBar(); bar.setValue(55); bar.setForeground(C_AMBER); card.add(bar);
        JLabel lblEn = mkLabel("55%", C_AMBER, FT_SMALL); card.add(lblEn);
        card.add(Box.createVerticalStrut(3));

        JPanel grid = new JPanel(new GridLayout(2, 2, 2, 2));
        grid.setOpaque(false); grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JLabel lblMd = addStat(grid, "Mode",   "NORMAL",   C_GREEN);
        JLabel lblRs = addStat(grid, "Réseau", "CONNECTÉ", C_GREEN);
        JLabel lblKt = addStat(grid, "Kit",    "COMPLET",  C_GREEN);
        card.add(grid);

        JLabel lblMs = mkLabel("En attente...", TEXT_DIM, FT_TINY);
        lblMs.setAlignmentX(LEFT_ALIGNMENT);
        card.add(Box.createVerticalStrut(2)); card.add(lblMs);

        barEnergie3 = bar; lblEnergie3 = lblEn;
        lblMode3 = lblMd; lblReseau3 = lblRs; lblKit3 = lblKt;
        lblMission3 = lblMs; engine.lblMission3 = lblMs;
        return card;
    }

    // ── CENTRE ────────────────────────────────────────────────────────

    private JPanel buildCenter() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_MAIN);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.insets = new Insets(2, 2, 2, 2);

        // Salle principale
        sallePanel = new SallePanel2(engine);
        g.gridx=0; g.gridy=0; g.gridwidth=2; g.gridheight=2;
        g.weightx=0.60; g.weighty=0.54;
        p.add(sallePanel, g);

        // Caméra robot
        cameraPanel = new CameraPanel();
        lockProportions(cameraPanel);
        g.gridx=2; g.gridy=0; g.gridwidth=2; g.gridheight=1;
        g.weightx=0.40; g.weighty=0.27;
        p.add(cameraPanel, g);

        // Scan corporel
        bodyScanPanel = new BodyScanPanel();
        lockProportions(bodyScanPanel);
        g.gridx=2; g.gridy=1; g.gridwidth=1; g.gridheight=1;
        g.weightx=0.20; g.weighty=0.27;
        p.add(bodyScanPanel, g);

        // File patients
        filePanel = new FilePanel2(this);
        lockProportions(filePanel);
        g.gridx=3; g.gridy=1; g.gridwidth=1; g.gridheight=1;
        g.weightx=0.20; g.weighty=0.27;
        p.add(filePanel, g);

        // Vitaux
        vitauxPanel = new VitauxPanel();
        lockProportions(vitauxPanel);
        g.gridx=0; g.gridy=2; g.gridwidth=4; g.gridheight=1;
        g.weightx=1.0; g.weighty=0.10;
        p.add(vitauxPanel, g);

        // Logs — taille augmentee
        logPanelUI = new LogPanelUI2();
        lockProportions(logPanelUI);
        g.gridx=0; g.gridy=3; g.gridwidth=4; g.gridheight=1;
        g.weightx=1.0; g.weighty=0.14;
        p.add(logPanelUI, g);

        return p;
    }

    private void lockProportions(JComponent c) {
        c.setPreferredSize(new Dimension(0, 0));
        c.setMinimumSize(new Dimension(0, 0));
    }

    // ═════════════════════════════════════════════════════════════════
    //  API MISE À JOUR UI
    // ═════════════════════════════════════════════════════════════════

    void updateSalle(SalleState s)                   { SwingUtilities.invokeLater(() -> sallePanel.setState(s)); }
    void updateCamera(CameraState s)                 { SwingUtilities.invokeLater(() -> cameraPanel.setState(s)); }
    void updateBodyScan(ScenarDef sc, NiveauESI esi) { SwingUtilities.invokeLater(() -> bodyScanPanel.update(sc, esi)); }
    void updateVitaux(ScenarDef sc, NiveauESI esi)   { SwingUtilities.invokeLater(() -> vitauxPanel.update(sc, esi)); }
    void updateFile(List<PatientTriage> file)         { SwingUtilities.invokeLater(() -> filePanel.update(file)); }
    void updateHeader(NiveauESI esi)                 { SwingUtilities.invokeLater(() -> headerPanel.update(esi)); }
    void addLog(String msg, LogType type)            { SwingUtilities.invokeLater(() -> logPanelUI.add(msg, type)); }
    void highlightPatient(String pid)                { SwingUtilities.invokeLater(() -> sallePanel.setSelectedPatient(pid)); }

    void showDossier(String pid, DossierMedical d, NiveauESI e, String dept, String salle) {
        SwingUtilities.invokeLater(() -> dossierPanel.show(pid, d, e, dept, salle));
    }

    void updateKit(int robotId, KitUrgence kit) {
        if (robotId == 1) SwingUtilities.invokeLater(() -> kitPanel1.update(kit));
        // kitPanel3 pourrait être ajouté pour RT-GAMMA
    }

    void updateFlotte(int actifs, int total) {
        SwingUtilities.invokeLater(() -> flottePanel.update(actifs, total));
    }

    void updateRobotSidebar(int robotId, int energie, boolean modeDegrade,
                             boolean connecte, boolean kitComplet, boolean enMarche) {
        SwingUtilities.invokeLater(() -> {
            Color ec = energie > 60 ? C_GREEN : energie > 30 ? C_AMBER : C_RED;

            switch (robotId) {
                case 1:
                    barEnergie1.setValue(energie); barEnergie1.setForeground(ec);
                    lblEnergie1.setText(energie + "%"); lblEnergie1.setForeground(ec);
                    lblMode1.setText(modeDegrade ? "DÉGRADÉ" : "NORMAL");
                    lblMode1.setForeground(modeDegrade ? C_AMBER : C_GREEN);
                    lblReseau1.setText(connecte ? "CONNECTÉ" : "HORS LIGNE");
                    lblReseau1.setForeground(connecte ? C_GREEN : C_RED);
                    lblKit1.setText(kitComplet ? "COMPLET" : "⚠ INCOMPLET");
                    lblKit1.setForeground(kitComplet ? C_GREEN : C_AMBER);
                    if (lblStatusBadge1 != null) {
                        lblStatusBadge1.setText(enMarche ? " EN LIGNE " : " ARRÊTÉ ");
                        lblStatusBadge1.setBackground(enMarche ? C_GREEN : C_RED);
                    }
                    break;
                case 2:
                    barEnergie2.setValue(energie); barEnergie2.setForeground(ec);
                    lblEnergie2.setText(energie + "%"); lblEnergie2.setForeground(ec);
                    lblMode2.setText(engine.robot2 != null && engine.robot2.isEnLivraison() ?
                            engine.robot2.getColisActuel() : "Aucun");
                    lblMode2.setForeground(engine.robot2 != null && engine.robot2.isEnLivraison() ? C_ORANGE : C_TEAL);
                    lblReseau2.setText(connecte ? "CONNECTÉ" : "HORS LIGNE");
                    lblReseau2.setForeground(connecte ? C_GREEN : C_RED);
                    lblKit2.setText(engine.robot2 != null ? engine.robot2.getDestination() : "N/A");
                    lblKit2.setForeground(TEXT_DIM);
                    if (lblStatusBadge2 != null) {
                        lblStatusBadge2.setText(enMarche ? " EN LIGNE " : " ARRÊTÉ ");
                        lblStatusBadge2.setBackground(enMarche ? C_GREEN : C_RED);
                    }
                    break;
                case 3:
                    barEnergie3.setValue(energie); barEnergie3.setForeground(ec);
                    lblEnergie3.setText(energie + "%"); lblEnergie3.setForeground(ec);
                    lblMode3.setText(modeDegrade ? "DÉGRADÉ" : "NORMAL");
                    lblMode3.setForeground(modeDegrade ? C_AMBER : C_GREEN);
                    lblReseau3.setText(connecte ? "CONNECTÉ" : "HORS LIGNE");
                    lblReseau3.setForeground(connecte ? C_GREEN : C_RED);
                    lblKit3.setText(kitComplet ? "COMPLET" : "⚠ INCOMPLET");
                    lblKit3.setForeground(kitComplet ? C_GREEN : C_AMBER);
                    if (lblStatusBadge3 != null) {
                        lblStatusBadge3.setText(enMarche ? " EN LIGNE " : " ARRÊTÉ ");
                        lblStatusBadge3.setBackground(enMarche ? C_GREEN : C_RED);
                    }
                    break;
            }

            // Mise à jour panneau flotte
            int actifs = 0;
            if (engine.robot1 != null && engine.robot1.isEnMarche()) actifs++;
            if (engine.robot2 != null && engine.robot2.isEnMarche()) actifs++;
            if (engine.robot3 != null && engine.robot3.isEnMarche()) actifs++;
            flottePanel.update(actifs, 3);
        });
    }

    // ═════════════════════════════════════════════════════════════════
    //  UTILITAIRES STATIQUES
    // ═════════════════════════════════════════════════════════════════

    static JPanel mkPanel(String titre) {
        JPanel p = new JPanel();
        p.setBackground(BG_PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                    BorderFactory.createEmptyBorder(), "  " + titre.toUpperCase(),
                    TitledBorder.LEFT, TitledBorder.TOP, FT_TINY, TEXT_DIM),
                BorderFactory.createEmptyBorder(2, 6, 4, 6))));
        return p;
    }

    static JLabel mkLabel(String t, Color c, Font f) {
        JLabel l = new JLabel(t); l.setForeground(c); l.setFont(f); return l;
    }

    static JLabel mkBadge(String t, Color c) {
        JLabel l = new JLabel(" " + t + " "); l.setFont(FT_TINY); l.setForeground(BG_MAIN);
        l.setBackground(c); l.setOpaque(true);
        l.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3)); return l;
    }

    static JLabel addStat(JPanel p, String key, String val, Color c) {
        JLabel k = new JLabel(key + ":"); k.setFont(FT_TINY); k.setForeground(TEXT_DIM);
        JLabel v = new JLabel(val);       v.setFont(FT_SMALL); v.setForeground(c);
        p.add(k); p.add(v); return v;
    }

    static JProgressBar mkBar() {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(100); bar.setForeground(C_GREEN); bar.setBackground(BORDER);
        bar.setBorderPainted(false); bar.setStringPainted(false);
        bar.setPreferredSize(new Dimension(0, 5));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        return bar;
    }

    static Color esiColor(NiveauESI n) {
        if (n == null) return TEXT_DIM;
        switch (n) {
            case ESI_1: return C_RED;   case ESI_2: return C_AMBER;
            case ESI_3: return C_YELLOW; case ESI_4: return C_TEAL;
            default: return new Color(91, 200, 245);
        }
    }

    static Color esiBg(NiveauESI n) {
        if (n == null) return BG_CARD;
        switch (n) {
            case ESI_1: return new Color(61,13,13);  case ESI_2: return new Color(45,26,0);
            case ESI_3: return new Color(28,34,0);   case ESI_4: return new Color(0,36,32);
            default:    return new Color(0,26,45);
        }
    }

    private void startClock() {
        new Timer(1000, e -> headerPanel.tickClock()).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Dashboard2().setVisible(true));
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  FLOTTE PANEL — résumé gestionnaire en haut de sidebar
// ═══════════════════════════════════════════════════════════════════════

class FlottePanel extends JPanel {
    private final JLabel lblActifs   = Dashboard2.mkLabel("3 / 3 actifs", Dashboard2.C_GREEN, Dashboard2.FT_SMALL);
    private final JLabel lblGest     = Dashboard2.mkLabel("GestionnaireHospitalier", Dashboard2.TEXT_DIM, Dashboard2.FT_TINY);
    private final JLabel[] dots      = new JLabel[3];
    private final Color[] dotColors  = { Dashboard2.C_CYAN, Dashboard2.C_ORANGE, Dashboard2.C_PURPLE };
    private final String[] dotNames  = { "✚ RT-ALPHA", "⬡ RL-BETA", "★ RT-GAMMA" };

    FlottePanel() {
        setBackground(new Color(8, 16, 32));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Dashboard2.BORDER),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel top = new JPanel(new BorderLayout(4, 0));
        top.setOpaque(false);
        JLabel ico = Dashboard2.mkLabel("⬡ FLOTTE", Dashboard2.C_CYAN, Dashboard2.FT_TITLE);
        top.add(ico, BorderLayout.WEST);
        top.add(lblActifs, BorderLayout.EAST);
        add(top);
        add(Box.createVerticalStrut(3));
        add(lblGest);
        add(Box.createVerticalStrut(4));

        JPanel dotsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dotsRow.setOpaque(false);
        for (int i = 0; i < 3; i++) {
            dots[i] = new JLabel("● " + dotNames[i]);
            dots[i].setFont(Dashboard2.FT_TINY);
            dots[i].setForeground(dotColors[i]);
            dotsRow.add(dots[i]);
        }
        add(dotsRow);
    }

    void update(int actifs, int total) {
        lblActifs.setText(actifs + " / " + total + " actifs");
        lblActifs.setForeground(actifs == total ? Dashboard2.C_GREEN :
                                actifs > 0      ? Dashboard2.C_AMBER : Dashboard2.C_RED);
        repaint();
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  HEADER PANEL 2
// ═══════════════════════════════════════════════════════════════════════

class HeaderPanel2 extends JPanel {
    private final JLabel lblClock = new JLabel("00:00:00");
    private final JLabel lblAlert = new JLabel("Système nominal");
    private final JLabel dotAlert;
    private boolean alertBlink = false;

    HeaderPanel2(Dashboard2 ui) {
        setBackground(new Color(8, 14, 28));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Dashboard2.BORDER),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        setLayout(new BorderLayout(8, 0));
        setPreferredSize(new Dimension(0, 46));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel ico   = new JLabel("⬡");
        ico.setFont(new Font("Courier New", Font.BOLD, 20));
        ico.setForeground(Dashboard2.C_CYAN);
        JLabel title = new JLabel("Système Robot Hospitalier v2 — Flotte 3 Robots | GestionnaireHospitalier");
        title.setFont(Dashboard2.FT_TITLE); title.setForeground(Dashboard2.C_CYAN);
        left.add(ico); left.add(title);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        JLabel net = new JLabel("✚ RT-ALPHA  ⬡ RL-BETA  ★ RT-GAMMA | WIFI-HOPITAL-5G");
        net.setFont(Dashboard2.FT_SMALL); net.setForeground(Dashboard2.C_GREEN);
        lblClock.setFont(new Font("Courier New", Font.BOLD, 14));
        lblClock.setForeground(new Color(74, 138, 181));
        dotAlert = new JLabel("• "); dotAlert.setFont(Dashboard2.FT_SMALL); dotAlert.setForeground(Dashboard2.C_GREEN);
        lblAlert.setFont(Dashboard2.FT_SMALL); lblAlert.setForeground(Dashboard2.TEXT_MAIN);
        right.add(net); right.add(lblClock); right.add(dotAlert); right.add(lblAlert);
        add(left, BorderLayout.WEST); add(right, BorderLayout.EAST);

        new Timer(500, e -> {
            if (alertBlink) {
                boolean on = (System.currentTimeMillis() / 500) % 2 == 0;
                dotAlert.setForeground(on ? Dashboard2.C_RED : new Color(80, 0, 0));
            }
        }).start();
    }

    void tickClock() { lblClock.setText(LocalDateTime.now().format(Dashboard2.HH_MM_SS)); }

    void update(NiveauESI esi) {
        if (esi == null) { resetAlert(); return; }
        switch (esi) {
            case ESI_1:
                alertBlink = true; dotAlert.setForeground(Dashboard2.C_RED);
                lblAlert.setText("🚨 CODE BLUE ACTIF"); lblAlert.setForeground(Dashboard2.C_RED); break;
            case ESI_2:
                alertBlink = false; dotAlert.setForeground(Dashboard2.C_AMBER);
                lblAlert.setText("⚠ Urgence niveau 2"); lblAlert.setForeground(Dashboard2.C_AMBER); break;
            default: resetAlert();
        }
    }

    private void resetAlert() {
        alertBlink = false; dotAlert.setForeground(Dashboard2.C_GREEN);
        lblAlert.setText("Système nominal"); lblAlert.setForeground(Dashboard2.TEXT_MAIN);
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  SALLE PANEL 2 — même logique que SallePanel, référence SimulationEngine2
// ═══════════════════════════════════════════════════════════════════════

class SallePanel2 extends JPanel {
    private SalleState state = new SalleState();
    private int blinkPhase = 0;
    private final SimulationEngine2 engine;
    private String selectedPatientId = null;
    // Screen positions for robot click detection
    private int robot1ScreenX, robot1ScreenY, robot2ScreenX, robot2ScreenY;

    static final float PORTE_SOINS_X = 0.78f;
    static final float PORTE_SOINS_Y = 0.50f;

    SallePanel2(SimulationEngine2 engine) {
        this.engine = engine;
        setBackground(Dashboard2.BG_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Dashboard2.BORDER),
            BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                "  SALLE D'URGENCES — Flotte 3 Robots | Cliquer patient ou robot",
                TitledBorder.LEFT, TitledBorder.TOP, Dashboard2.FT_TINY, Dashboard2.TEXT_DIM)));
        setLayout(new BorderLayout());

        // Bouton agrandir/réduire la salle (centre-haut)
        JButton btnExpand = new JButton("[⬆ Agrandir Salle]");
        btnExpand.setFont(Dashboard2.FT_TINY);
        btnExpand.setForeground(Dashboard2.C_CYAN);
        btnExpand.setBackground(Dashboard2.BG_PANEL);
        btnExpand.setBorder(BorderFactory.createLineBorder(Dashboard2.BORDER));
        btnExpand.setFocusPainted(false);
        btnExpand.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        final boolean[] salleExpanded = {false};
        final Container[] salleOrigParent = {null};
        final Object[] salleOrigConstraints = {null};
        btnExpand.addActionListener(ev -> {
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            if (topFrame == null) return;
            Container mc = topFrame.getContentPane();
            Component body = mc.getComponent(1);
            if (!salleExpanded[0]) {
                salleOrigParent[0] = getParent();
                if (salleOrigParent[0].getLayout() instanceof GridBagLayout)
                    salleOrigConstraints[0] = ((GridBagLayout) salleOrigParent[0].getLayout()).getConstraints(this);
                body.setVisible(false);
                mc.add(this, BorderLayout.CENTER);
                btnExpand.setText("[⬇ Réduire Salle]");
                salleExpanded[0] = true;
            } else {
                mc.remove(this);
                if (salleOrigConstraints[0] != null) salleOrigParent[0].add(this, salleOrigConstraints[0]);
                else salleOrigParent[0].add(this);
                body.setVisible(true);
                btnExpand.setText("[⬆ Agrandir Salle]");
                salleExpanded[0] = false;
            }
            topFrame.revalidate(); topFrame.repaint();
        });
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 1));
        topBar.setOpaque(false);
        topBar.add(btnExpand);
        add(topBar, BorderLayout.NORTH);

        // Canvas de dessin principal
        JPanel canvas = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintSalle(g, getWidth(), getHeight());
            }
        };
        canvas.setOpaque(false);
        canvas.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { handleClick(e.getX(), e.getY(), canvas.getWidth(), canvas.getHeight()); }
        });
        canvas.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add(canvas, BorderLayout.CENTER);

        new Timer(80, e -> { blinkPhase = (blinkPhase + 1) % 20; canvas.repaint(); }).start();
    }

    private void handleClick(int mx, int my, int W, int H) {
        int pad = 28, gw = W - pad*2, gh = H - pad*2 - 16;
        int ox = pad, oy = pad + 10;

        // Clic sur Robot 1 (RT-ALPHA)
        if (Math.abs(mx - robot1ScreenX) < 18 && Math.abs(my - robot1ScreenY) < 18) {
            engine.onRobotClicked(1); return;
        }
        // Clic sur Robot 2 (RL-BETA)
        if (Math.abs(mx - robot2ScreenX) < 18 && Math.abs(my - robot2ScreenY) < 18) {
            engine.onRobotClicked(2); return;
        }
        // Clic sur patient
        for (PatientMarker pm : state.patients) {
            int px = ox + (int)(pm.x * gw), py = oy + (int)(pm.y * gh);
            if (Math.abs(mx - px) < 18 && Math.abs(my - py) < 18) {
                selectedPatientId = pm.id; state.selectedPatientId = pm.id;
                repaint(); engine.onPatientClicked(pm.id); return;
            }
        }
    }

    void setState(SalleState s) { this.state = s; repaint(); }
    void setSelectedPatient(String id) { selectedPatientId = id; state.selectedPatientId = id; repaint(); }

    private void paintSalle(Graphics g, int W, int H) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int pad = 28, gw = W - pad*2, gh = H - pad*2 - 16;
        int ox = pad, oy = pad + 10;

        // Sol
        g2.setColor(Dashboard2.BG_DEEP);
        g2.fillRoundRect(ox, oy, gw, gh, 6, 6);

        // Zones
        int accW = gw / 7, attW = (int)(gw * 0.62f), admW = gw - accW - attW;
        g2.setColor(new Color(20, 35, 55));
        g2.fillRect(ox, oy, accW, gh);
        g2.setColor(new Color(12, 24, 42));
        g2.fillRect(ox + accW, oy, attW, gh);
        g2.setColor(new Color(15, 28, 50));
        g2.fillRect(ox + accW + attW, oy, admW, gh);

        // Libellés zones
        g2.setFont(Dashboard2.FT_TINY); g2.setColor(Dashboard2.TEXT_DIM);
        g2.drawString("ACCUEIL", ox + 4, oy + 14);
        g2.drawString("ZONE ATTENTE PATIENTS", ox + accW + 8, oy + 14);
        // ADMIN label now drawn with robot in new_robot_calls section

        // Chaises
        for (ChairMarker ch : state.chaises) {
            int cx = ox + (int)(ch.x * gw), cy = oy + (int)(ch.y * gh);
            g2.setColor(ch.occupee ?
                (ch.occupantEstPatient ? new  Color(30, 50, 80) : new  Color(20, 35, 55))
                : new  Color(18, 30, 48));
            g2.fillRoundRect(cx - 7, cy - 7, 14, 14, 3, 3);
            g2.setColor(Dashboard2.BORDER);
            g2.drawRoundRect(cx - 7, cy - 7, 14, 14, 3, 3);
        }

        // Obstacles
        for (ObstacleItem ob : state.obstacles) {
            int obx = ox + (int)(ob.x * gw), oby = oy + (int)(ob.y * gh);
            g2.setColor(new Color(40, 55, 75));
            g2.fillRoundRect(obx - 10, oby - 6, 20, 12, 4, 4);
            g2.setColor(Dashboard2.TEXT_DIM);
            g2.setFont(new Font("Courier New", Font.PLAIN, 8));
            g2.drawString(ob.type, obx - 9, oby + 4);
        }

        // Patients
        for (PatientMarker pm : state.patients) {
            int px = ox + (int)(pm.x * gw), py = oy + (int)(pm.y * gh);
            Color c = pm.esi != null ? Dashboard2.esiColor(pm.esi) : Dashboard2.TEXT_DIM;
            boolean sel = pm.id.equals(state.selectedPatientId);

            if (sel) {
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
                g2.fillOval(px - 14, py - 14, 28, 28);
            }
            g2.setColor(c); g2.fillOval(px - 8, py - 8, 16, 16);
            if (pm.traite) {
                g2.setColor(Dashboard2.C_GREEN);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(px - 9, py - 9, 18, 18);
            }
            if (pm.enCours) {
                boolean blink = (blinkPhase % 4) < 2;
                if (blink) { g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(2f)); g2.drawOval(px-10,py-10,20,20); }
            }
            g2.setFont(Dashboard2.FT_TINY); g2.setColor(Dashboard2.TEXT_MAIN);
            g2.drawString(pm.id.replace("PAT-","P"), px - 6, py + 18);
        }

        // Robot 1 (RT-ALPHA — cyan — croix medicale)
        drawRobot(g2, state.robotX1, state.robotY1, ox, oy, gw, gh, Dashboard2.C_ROBOT1, "RT-α", state.robot1EnMarche, blinkPhase, 1);

        // Robot 2 (RL-BETA — orange — hexagone)
        drawRobot(g2, state.robotX2, state.robotY2, ox, oy, gw, gh, Dashboard2.C_ROBOT2, "RL-β", state.robot2EnMarche, blinkPhase, 2);

        // Légende robots avec indice de clic
        g2.setFont(Dashboard2.FT_TINY);
        int lx = ox + gw - 160, ly = oy + gh - 50;
        g2.setColor(new Color(20, 35, 60));
        g2.fillRoundRect(lx - 4, ly - 12, 158, 54, 4, 4);
        g2.setColor(Dashboard2.BORDER);
        g2.drawRoundRect(lx - 4, ly - 12, 158, 54, 4, 4);
        g2.setColor(new Color(74, 138, 181, 200));
        g2.setFont(new Font("Courier New", Font.ITALIC, 9));
        g2.drawString("▶ Cliquer robot = voir scan", lx, ly);
        g2.setFont(Dashboard2.FT_TINY);
        drawLegendDot(g2, lx, ly + 14, Dashboard2.C_ROBOT1, "✚ RT-ALPHA (Triage)");
        drawLegendDot(g2, lx, ly + 28, Dashboard2.C_ROBOT2, "⬡ RL-BETA (Livraison)");
        drawLegendDot(g2, lx, ly + 42, Dashboard2.C_ROBOT3, "★ RT-GAMMA (Admin/Sécu)");

        // Zone ADMIN — robot sécurité en vert
        int admX = ox + accW + attW, admCx = admX + admW / 2, admCy = oy + gh / 2;
        g2.setColor(Dashboard2.C_GREEN);
        g2.setFont(Dashboard2.FT_TINY);
        g2.drawString("ADMIN ★", admX + 4, oy + 14);
        g2.setColor(new Color(0, 50, 15));
        g2.fillRoundRect(admCx - 13, admCy - 13, 26, 26, 6, 6);
        g2.setColor(Dashboard2.C_GREEN);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(admCx - 13, admCy - 13, 26, 26, 6, 6);
        g2.setFont(new Font("Courier New", Font.BOLD, 8));
        g2.drawString("SEC", admCx - 9, admCy + 3);
        g2.setFont(Dashboard2.FT_TINY);
        g2.drawString("RT-GAMMA", admCx - 17, admCy + 22);

        g2.dispose();
    }

    private void drawRobot(Graphics2D g2, float rx, float ry, int ox, int oy, int gw, int gh,
                            Color color, String label, boolean enMarche, int blink, int robotId) {
        int x = ox + (int)(rx * gw), y = oy + (int)(ry * gh);
        boolean pulse = (blink % 10) < 5;
        if (robotId == 1) { robot1ScreenX = x; robot1ScreenY = y; }
        else              { robot2ScreenX = x; robot2ScreenY = y; }

        if (enMarche && pulse) {
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
            g2.fillOval(x - 15, y - 15, 30, 30);
        }
        Color drawColor = enMarche ? color : Dashboard2.TEXT_DIM;
        if (robotId == 1) {
            // RT-ALPHA : croix medicale +
            g2.setColor(drawColor);
            g2.fillRoundRect(x - 4, y - 10, 8, 20, 2, 2);
            g2.fillRoundRect(x - 10, y - 4, 20, 8, 2, 2);
        } else {
            // RL-BETA : hexagone livraison
            g2.setColor(drawColor);
            int[] hx = {x, x+8, x+8, x, x-8, x-8};
            int[] hy = {y-10, y-5, y+5, y+10, y+5, y-5};
            g2.fillPolygon(hx, hy, 6);
        }
        // Indicateur cliquable
        g2.setColor(new Color(255,255,255,120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(x - 13, y - 13, 26, 26);
        // Label
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Courier New", Font.BOLD, 7));
        g2.drawString(label, x - 8, y + 22);
    }

    private void drawLegendDot(Graphics2D g2, int x, int y, Color c, String label) {
        g2.setColor(c); g2.fillOval(x, y - 5, 8, 8);
        g2.setColor(Dashboard2.TEXT_DIM); g2.setFont(Dashboard2.FT_TINY);
        g2.drawString(label, x + 12, y + 3);
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  FILE PANEL 2 — identique à FilePanel mais référence Dashboard2
// ═══════════════════════════════════════════════════════════════════════

class FilePanel2 extends JPanel {
    private final DefaultListModel<PatientTriage> model = new DefaultListModel<>();
    private final JList<PatientTriage> list;

    FilePanel2(Dashboard2 ui) {
        setBackground(Dashboard2.BG_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Dashboard2.BORDER),
            BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                "  FILE D'ATTENTE", TitledBorder.LEFT, TitledBorder.TOP, Dashboard2.FT_TINY, Dashboard2.TEXT_DIM)));
        setLayout(new BorderLayout());
        list = new JList<>(model);
        list.setBackground(Dashboard2.BG_DEEP);
        list.setFixedCellHeight(20);
        list.setCellRenderer(new FileRenderer2());
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null); scroll.setBackground(Dashboard2.BG_DEEP);
        scroll.getViewport().setBackground(Dashboard2.BG_DEEP);
        add(scroll, BorderLayout.CENTER);
    }

    void update(List<PatientTriage> file) {
        model.clear();
        for (PatientTriage p : file) model.addElement(p);
    }

    static class FileRenderer2 extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean sel, boolean focus) {
            PatientTriage p = (PatientTriage) value;
            JLabel l = (JLabel) super.getListCellRendererComponent(list, "", index, sel, focus);
            String tag = p.esi.name().replace("ESI_", "");
            l.setText("  ESI" + tag + "  " + p.id + " – " + p.desc);
            l.setFont(Dashboard2.FT_TINY);
            l.setBackground(sel ? new Color(30, 50, 80) : Dashboard2.esiBg(p.esi));
            l.setForeground(Dashboard2.esiColor(p.esi));
            return l;
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  KIT PANEL UI 2
// ═══════════════════════════════════════════════════════════════════════

class KitPanelUI2 extends JPanel {
    private final Map<ElementKit, JProgressBar> bars  = new LinkedHashMap<>();
    private final Map<ElementKit, JLabel> stockLabels = new LinkedHashMap<>();

    KitPanelUI2(String titre) {
        setBackground(Dashboard2.BG_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Dashboard2.BORDER),
            BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                "  " + titre.toUpperCase(), TitledBorder.LEFT, TitledBorder.TOP, Dashboard2.FT_TINY, Dashboard2.TEXT_DIM)));
        setLayout(new GridLayout(0, 1, 0, 2));
        for (ElementKit e : ElementKit.values()) {
            JPanel row = new JPanel(new BorderLayout(4, 0)); row.setOpaque(false);
            String lbl = e.getLabel(); if (lbl.length() > 10) lbl = lbl.substring(0, 8) + "..";
            JLabel name = Dashboard2.mkLabel(lbl, Dashboard2.TEXT_DIM, Dashboard2.FT_TINY);
            name.setPreferredSize(new Dimension(60, 15));
            JProgressBar bar = new JProgressBar(0, e.getStockMax()); bar.setValue(e.getStockMax());
            bar.setForeground(Dashboard2.C_GREEN); bar.setBackground(Dashboard2.BG_DEEP);
            bar.setBorderPainted(false); bar.setStringPainted(false);
            bar.setPreferredSize(new Dimension(0, 8));
            JLabel sl = Dashboard2.mkLabel(e.getStockMax() + "/" + e.getStockMax(), Dashboard2.C_GREEN, Dashboard2.FT_TINY);
            sl.setPreferredSize(new Dimension(32, 15));
            row.add(name, BorderLayout.WEST); row.add(bar, BorderLayout.CENTER); row.add(sl, BorderLayout.EAST);
            bars.put(e, bar); stockLabels.put(e, sl); add(row);
        }
    }

    void update(KitUrgence kit) {
        for (ElementKit e : ElementKit.values()) {
            int stock = kit.getStock(e);
            JProgressBar bar = bars.get(e); JLabel sl = stockLabels.get(e);
            if (bar == null) continue; bar.setValue(stock);
            Color c = stock == 0 ? Dashboard2.C_RED : stock < e.getStockMax() ? Dashboard2.C_AMBER : Dashboard2.C_GREEN;
            bar.setForeground(c); sl.setText(stock + "/" + e.getStockMax()); sl.setForeground(c);
        }
        repaint();
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  LOG PANEL UI 2
// ═══════════════════════════════════════════════════════════════════════

class LogPanelUI2 extends JPanel {
    private final DefaultListModel<String[]> model = new DefaultListModel<>();
    private final JList<String[]> list;
    private boolean logExpanded = false;
    private Container logOrigParent;
    private Object logOrigConstraints;

    LogPanelUI2() {
        setBackground(Dashboard2.BG_DEEP);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Dashboard2.BORDER),
            BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                "  CONSOLE — GestionnaireHospitalier + Flotte",
                TitledBorder.LEFT, TitledBorder.TOP, Dashboard2.FT_TINY, Dashboard2.TEXT_DIM)));
        setLayout(new BorderLayout());

        list = new JList<>(model);
        list.setBackground(Dashboard2.BG_DEEP);
        list.setFixedCellHeight(17);
        list.setCellRenderer(new LogRenderer2());

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.setBackground(Dashboard2.BG_DEEP);
        scroll.getViewport().setBackground(Dashboard2.BG_DEEP);
        scroll.getVerticalScrollBar().setBackground(Dashboard2.BG_DEEP);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        model.addListDataListener(new ListDataListener() {
            public void intervalAdded(ListDataEvent e)  { int l = model.getSize()-1; if (l >= 0) list.ensureIndexIsVisible(l); }
            public void intervalRemoved(ListDataEvent e) {}
            public void contentsChanged(ListDataEvent e) {}
        });

        JButton clear = new JButton("Effacer");
        clear.setFont(Dashboard2.FT_TINY);
        clear.setForeground(Dashboard2.TEXT_DIM);
        clear.setBackground(Dashboard2.BG_PANEL);
        clear.setBorder(BorderFactory.createLineBorder(Dashboard2.BORDER));
        clear.setFocusPainted(false);
        clear.addActionListener(e -> model.clear());

        JButton btnExpand = new JButton("[⬆ Agrandir]");
        btnExpand.setFont(Dashboard2.FT_TINY);
        btnExpand.setForeground(Dashboard2.C_CYAN);
        btnExpand.setBackground(Dashboard2.BG_PANEL);
        btnExpand.setBorder(BorderFactory.createLineBorder(Dashboard2.BORDER));
        btnExpand.setFocusPainted(false);
        btnExpand.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExpand.addActionListener(ev -> {
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            if (topFrame == null) return;
            Container mc = topFrame.getContentPane();
            Component body = mc.getComponent(1);
            if (!logExpanded) {
                logOrigParent = getParent();
                if (logOrigParent.getLayout() instanceof GridBagLayout)
                    logOrigConstraints = ((GridBagLayout) logOrigParent.getLayout()).getConstraints(this);
                body.setVisible(false);
                mc.add(this, BorderLayout.CENTER);
                btnExpand.setText("[⬇ Réduire]");
                logExpanded = true;
            } else {
                mc.remove(this);
                if (logOrigConstraints != null) logOrigParent.add(this, logOrigConstraints);
                else logOrigParent.add(this);
                body.setVisible(true);
                btnExpand.setText("[⬆ Agrandir]");
                logExpanded = false;
            }
            topFrame.revalidate(); topFrame.repaint();
        });

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        topBar.setOpaque(false);
        topBar.add(btnExpand);
        topBar.add(clear);

        add(scroll, BorderLayout.CENTER);
        add(topBar, BorderLayout.NORTH);
    }

    void add(String msg, LogType type) {
        String ts = LocalDateTime.now().format(Dashboard2.HH_MM_SS);
        model.addElement(new String[]{ts, msg, type.name()});
        if (model.getSize() > 300) model.remove(0);
    }

    static class LogRenderer2 extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean sel, boolean focus) {
            String[] row = (String[]) value; LogType type = LogType.valueOf(row[2]);
            JLabel l = (JLabel) super.getListCellRendererComponent(list, "", index, sel, focus);
            l.setText("[" + row[0] + "] " + row[1]); l.setFont(Dashboard2.FT_TINY);
            l.setBackground(sel ? new Color(30,50,80) : Dashboard2.BG_DEEP);
            switch (type) {
                case CRITICAL: l.setForeground(Dashboard2.C_RED);   break;
                case WARNING:  l.setForeground(Dashboard2.C_AMBER); break;
                case SUCCESS:  l.setForeground(Dashboard2.C_TEAL);  break;
                default:       l.setForeground(Dashboard2.TEXT_DIM); break;
            }
            if (row[1].contains("━━━"))               l.setForeground(Dashboard2.C_CYAN);
            if (row[1].contains("Gestionnaire"))       l.setForeground(Dashboard2.C_PURPLE);
            if (row[1].contains("RT-ALPHA"))           l.setForeground(Dashboard2.C_ROBOT1);
            if (row[1].contains("RL-BETA"))            l.setForeground(Dashboard2.C_ROBOT2);
            if (row[1].contains("RT-GAMMA"))           l.setForeground(Dashboard2.C_ROBOT3);
            return l;
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  CMD PANEL 2 — avec boutons gestionnaire
// ═══════════════════════════════════════════════════════════════════════

class CmdPanel2 extends JPanel {
    private final Dashboard2 ui;
    private final JTextField tfCode = new JTextField(7);
    JLabel lblStatus;

    CmdPanel2(Dashboard2 ui) {
        this.ui = ui;
        setBackground(Dashboard2.BG_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Dashboard2.BORDER),
            BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                "  COMMANDES — GESTIONNAIRE", TitledBorder.LEFT, TitledBorder.TOP, Dashboard2.FT_TINY, Dashboard2.TEXT_DIM)));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setMaximumSize(new Dimension(285, 170));

        lblStatus = Dashboard2.mkLabel("Prêt.", Dashboard2.TEXT_DIM, Dashboard2.FT_TINY);

        // Ligne 1 : mode dégradé + recharge
        JPanel r1 = btnRow(
            mkBtn("Mode dégradé", Dashboard2.C_AMBER, e -> ui.engine.toggleDegrade()),
            mkBtn("⚡ Recharge", Dashboard2.C_GREEN, e -> ui.engine.forceRecharge())
        );
        // Ligne 2 : alerte générale + rapport flotte
        JPanel r2 = btnRow(
            mkBtn("🚨 Alerte gén.", Dashboard2.C_RED, e -> ui.engine.declencherAlerteGenerale()),
            mkBtn("📋 Rapport",    Dashboard2.C_CYAN, e -> ui.engine.afficherRapportFlotte())
        );
        // Ligne 3 : accès dossier sécurisé
        JPanel secRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        secRow.setOpaque(false);
        tfCode.setBackground(Dashboard2.BG_DEEP); tfCode.setForeground(Dashboard2.C_CYAN);
        tfCode.setCaretColor(Dashboard2.C_CYAN); tfCode.setFont(Dashboard2.FT_TINY);
        tfCode.setBorder(BorderFactory.createLineBorder(Dashboard2.BORDER));
        JLabel codeLbl = Dashboard2.mkLabel("Code:", Dashboard2.TEXT_DIM, Dashboard2.FT_TINY);
        JButton btnDoss = mkBtn("Dossier 🔐", Dashboard2.C_AMBER, e -> ui.engine.consulterDossier(tfCode.getText().trim()));
        secRow.add(codeLbl); secRow.add(tfCode); secRow.add(btnDoss);

        add(r1); add(Box.createVerticalStrut(3));
        add(r2); add(Box.createVerticalStrut(3));
        add(secRow); add(Box.createVerticalStrut(2));
        add(lblStatus);
    }

    void setStatus(String msg, Color c) { lblStatus.setText(msg); lblStatus.setForeground(c); }

    private JPanel btnRow(JButton a, JButton b) {
        JPanel p = new JPanel(new GridLayout(1, 2, 4, 0)); p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26)); p.add(a); p.add(b); return p;
    }

    private JButton mkBtn(String t, Color fg, ActionListener al) {
        JButton b = new JButton(t); b.setFont(Dashboard2.FT_TINY); b.setForeground(fg);
        b.setBackground(Dashboard2.BG_DEEP);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Dashboard2.BORDER),
            BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(al);
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(18,32,52)); }
            public void mouseExited (MouseEvent e) { b.setBackground(Dashboard2.BG_DEEP); }
        });
        return b;
    }
    
}
