package projetrobot;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DashboardSecurite2 extends JFrame {
    static final Color C_BG_DARK=new Color(8,14,28),C_BG_PANEL=new Color(15,23,42),C_BG_CARD=new Color(22,33,58);
    static final Color C_ACCENT_BLUE=new Color(56,128,255),C_VERT=new Color(34,197,94),C_ORANGE=new Color(251,146,60);
    static final Color C_ROUGE=new Color(239,68,68),C_TEXT_WHITE=new Color(236,242,255),C_TEXT_MUTED=new Color(120,140,170);
    static final Color C_BORDER=new Color(40,55,80),C_GOLD=new Color(250,200,20),C_CYAN=new Color(6,182,212);

    static final Map<String,String[]> UTILISATEURS=new HashMap<>();
    static {
        try {
            UTILISATEURS.put("admin_hopital",new String[]{sha256("motdepasse_admin"),"ADMIN","Administration"});
            UTILISATEURS.put("dr_martin",    new String[]{sha256("medecin2024"),     "MEDECIN","Cardiologie"});
            UTILISATEURS.put("inf_sophie",   new String[]{sha256("infirmier123"),    "INFIRMIER","Urgences"});
            UTILISATEURS.put("tech_omar",    new String[]{sha256("tech2024"),        "TECHNICIEN","Maintenance"});
            UTILISATEURS.put("dr_leila",     new String[]{sha256("onco2024"),        "MEDECIN","Oncologie"});
            UTILISATEURS.put("inf_karim",    new String[]{sha256("soins2024"),       "INFIRMIER","Réanimation"});
        } catch(Exception e){e.printStackTrace();}
    }

    // {id, dept, etage, energie%, etat(0/1/2), mission, roleMin, mapX%, mapY%}
    static final Object[][] RD={
        {"SEC-01","Administration",0,95,0,"Surveillance réseau","ADMIN",0.72,0.78},
        {"URG-01","Urgences",0,78,0,"Transport médicaments et Assistance patient critique","INFIRMIER",0.13,0.60},
        {"CARD-01","Cardiologie",1,88,0,"Livraison dossier patient","MEDECIN",0.38,0.25},
        {"NEUR-01","Neurologie",1,62,1,"Transport analyse labo","MEDECIN",0.55,0.22},
        {"ONCO-01","Oncologie",1,91,0,"Préparation chimiothérapie","MEDECIN",0.70,0.20},
        {"CHIR-01","Chirurgie",2,55,1,"Bloc opératoire – attente","MEDECIN",0.42,0.48},
        {"CHIR-02","Chirurgie",2,12,2,"BATTERIE CRITIQUE","MEDECIN",0.50,0.45},
        {"PED-01","Pédiatrie",1,80,0,"Ronde de soins","INFIRMIER",0.83,0.35},
        {"REA-01","Réanimation",2,98,0,"Surveillance continue","MEDECIN",0.62,0.55},
        {"REA-02","Réanimation",2,33,2,"ISOLÉ – comportement suspect","ADMIN",0.68,0.60},
        {"RAD-01","Radiologie",1,74,0,"Transport IRM","TECHNICIEN",0.28,0.42},
        {"LABO-01","Laboratoire",0,86,0,"Prises de sang – analyses","TECHNICIEN",0.30,0.78},
        {"PHAR-01","Pharmacie",0,92,0,"Distribution médicaments","MEDECIN",0.50,0.78},
        {"MAINT-01","Maintenance",-1,67,1,"Réparation appareil","TECHNICIEN",0.15,0.88},
        {"HYG-01","Hygiène/Entretien",-1,81,0,"Désinfection bloc B","TECHNICIEN",0.35,0.88},
    };

    // {nom, x%, y%, w%, h%, couleur}
    static final Object[][] DEPTS={
        {"Urgences",         0.02,0.50,0.22,0.26,new Color(185,28,28,85)},
        {"Administration",   0.60,0.68,0.22,0.22,new Color(30,64,175,85)},
        {"Cardiologie",      0.28,0.10,0.18,0.22,new Color(126,34,206,85)},
        {"Neurologie",       0.47,0.10,0.16,0.20,new Color(6,95,70,85)},
        {"Oncologie",        0.64,0.10,0.17,0.20,new Color(92,45,145,85)},
        {"Chirurgie",        0.32,0.35,0.24,0.24,new Color(161,98,7,85)},
        {"Réanimation",      0.56,0.42,0.18,0.22,new Color(185,28,28,85)},
        {"Pédiatrie",        0.77,0.22,0.20,0.22,new Color(21,128,61,85)},
        {"Radiologie",       0.14,0.30,0.20,0.22,new Color(14,116,144,85)},
        {"Laboratoire",      0.24,0.67,0.20,0.18,new Color(30,80,130,85)},
        {"Pharmacie",        0.44,0.67,0.17,0.18,new Color(67,20,140,85)},
        {"Maintenance",      0.02,0.80,0.25,0.15,new Color(55,65,81,85)},
        {"Hygiène/Entretien",0.29,0.80,0.18,0.15,new Color(30,80,60,85)},
    };

    // Robots vitaux qui restent opérationnels en mode dégradé
    static final java.util.Set<String> ROBOTS_VITAUX = new java.util.HashSet<>(Arrays.asList(
        "SEC-01",  // Sécurité réseau — toujours actif
        "URG-01",  // Urgences — mission vitale
        "REA-01",  // Réanimation — surveillance continue
        "REA-02"   // Réanimation 2 — maintenu même si suspect
    ));

    String sessionUser=null,sessionRole=null,sessionDept=null,sessionToken=null;
    Map<String,Integer> tentatives=new HashMap<>();
    Map<String,Boolean>  isoles=new HashMap<>();
    int[] energies=new int[RD.length], etats=new int[RD.length];
    List<String> logsC=new ArrayList<>(),auditL=new ArrayList<>();
    boolean modeDegrade=false; int mdStep=0;
    Timer mdTimer=null,refreshTimer=null,honeypotAnimTimer=null;

    CardLayout mainCL; JPanel mainPanel;
    JTextArea consoleArea,mdLog,hpDetails;
    JLabel statusBar,alertBanner,hpCount,mdStatut;
    JProgressBar mdProgress;
    HospitalMap cartePanel,mdCartePanel;
    DefaultTableModel tableModel; JTable table;
    JPanel hpRadar;
    List<HpEvent> hpEvents=new ArrayList<>();
    List<String> hpIntrusions=new ArrayList<>();

    class HpEvent {
        private final String acteur;
        private final String ressource;
        private final String ts;
        private final boolean confirmed;

        public HpEvent(String acteur, String ressource, String ts, boolean confirmed) {
            this.acteur = acteur;
            this.ressource = ressource;
            this.ts = ts;
            this.confirmed = confirmed;
        }

        public String acteur() { return acteur; }
        public String ressource() { return ressource; }
        public String ts() { return ts; }
        public boolean confirmed() { return confirmed; }
    }

    public DashboardSecurite2(){
        setTitle("CyberSec Hospitalier — Système de Sécurité");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1450,920); setMinimumSize(new Dimension(1200,750));
        setLocationRelativeTo(null); getContentPane().setBackground(C_BG_DARK);
        isoles.put("REA-02",true);
        for(int i=0;i<RD.length;i++){energies[i]=(Integer)RD[i][3];etats[i]=(Integer)RD[i][4];}
        mainCL=new CardLayout(); mainPanel=new JPanel(mainCL); mainPanel.setBackground(C_BG_DARK);
        mainPanel.add(buildLogin(),"LOGIN"); mainPanel.add(buildDashboard(),"DASHBOARD");
        add(mainPanel); mainCL.show(mainPanel,"LOGIN");
        log("SYSTÈME","Démarrage cybersécurité hospitalière. SHA-256 actif.");
        log("HONEYPOT","Ressource piège 'DOSSIER_PATIENT_VIP_CONFIDENTIEL' armée.");
        setVisible(true);
    }

    // ═══════ LOGIN ═════════════════════════════════════════
    JPanel buildLogin(){
        JPanel root=new JPanel(new GridBagLayout()); root.setBackground(C_BG_DARK);
        JPanel card=new JPanel(); card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        card.setBackground(C_BG_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(new LineBorder(C_ACCENT_BLUE,2,true),new EmptyBorder(40,50,40,50)));
        card.setPreferredSize(new Dimension(470,570));
        JLabel logo=lbl("",52,Font.PLAIN,C_TEXT_WHITE); logo.setAlignmentX(.5f);
        JLabel titre=lbl("SYSTÈME SÉCURITÉ",22,Font.BOLD,C_TEXT_WHITE); titre.setAlignmentX(.5f);
        JLabel sub=lbl("Centre Hospitalier — Accès Restreint",12,Font.PLAIN,C_TEXT_MUTED); sub.setAlignmentX(.5f);
        JLabel badge=new JLabel("   Communication chiffrée SHA-256  ",SwingConstants.CENTER);
        badge.setFont(new Font("Segoe UI",Font.BOLD,11)); badge.setForeground(C_VERT);
        badge.setBackground(new Color(34,197,94,25)); badge.setOpaque(true);
        badge.setBorder(new LineBorder(C_VERT,1,true)); badge.setAlignmentX(.5f);
        badge.setMaximumSize(new Dimension(300,28));
        JTextField tfId=stf(); JPasswordField pfMdp=new JPasswordField(); spf(pfMdp);
        JComboBox<String> cbRole=scb(new String[]{"ADMIN","MEDECIN","INFIRMIER","TECHNICIEN"});
        JLabel lblH=new JLabel("SHA-256 : …"); lblH.setFont(new Font("Courier New",Font.PLAIN,10));
        lblH.setForeground(C_CYAN); lblH.setAlignmentX(.5f);
        pfMdp.addKeyListener(new KeyAdapter(){@Override public void keyReleased(KeyEvent e){
            String m=new String(pfMdp.getPassword());
            if(m.isEmpty()){lblH.setText("SHA-256 : …");return;}
            try{lblH.setText("SHA-256 : "+sha256(m).substring(0,24)+"…");}catch(Exception ex){}
        }});
        JLabel lblErr=new JLabel(" ",SwingConstants.CENTER);
        lblErr.setFont(new Font("Segoe UI",Font.BOLD,12)); lblErr.setForeground(C_ROUGE); lblErr.setAlignmentX(.5f);
        JButton btnLogin=sbtn("Se connecter",C_ACCENT_BLUE); btnLogin.setAlignmentX(.5f); btnLogin.setMaximumSize(new Dimension(280,44));
        JLabel hint=new JLabel("<html><center><font color='#475569'>admin_hopital/motdepasse_admin • dr_martin/medecin2024 • inf_sophie/infirmier123</font></center></html>",SwingConstants.CENTER);
        hint.setFont(new Font("Segoe UI",Font.PLAIN,10)); hint.setAlignmentX(.5f);
        ActionListener doLogin=e->doLogin(tfId.getText().trim(),new String(pfMdp.getPassword()),(String)cbRole.getSelectedItem(),lblErr);
        btnLogin.addActionListener(doLogin); pfMdp.addActionListener(doLogin);
        card.add(logo);card.add(vg(6));card.add(titre);card.add(vg(4));card.add(sub);card.add(vg(14));
        card.add(badge);card.add(vg(22));card.add(fr("Identifiant",tfId));card.add(fr("Mot de passe",pfMdp));
        card.add(vg(3));card.add(lblH);card.add(vg(10));card.add(fr("Rôle",cbRole));
        card.add(vg(14));card.add(lblErr);card.add(vg(6));card.add(btnLogin);card.add(vg(18));card.add(hint);
        root.add(card); return root;
    }
    JPanel fr(String label,JComponent f){
        JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBackground(C_BG_PANEL);p.setAlignmentX(.5f);
        JLabel l=lbl(label,11,Font.BOLD,C_TEXT_MUTED);l.setAlignmentX(0f);f.setAlignmentX(0f);
        p.add(l);p.add(vg(3));p.add(f);p.add(vg(10));return p;
    }
    void doLogin(String id,String mdp,String role,JLabel err){
        if(id.isEmpty()||mdp.isEmpty()){err.setText("Remplissez tous les champs.");return;}
        int t=tentatives.getOrDefault(id,0);
        if(t>=3){err.setText("Compte verrouillé.");return;}
        String[] info=UTILISATEURS.get(id);
        if(info==null){tentatives.put(id,t+1);err.setText("Identifiant inconnu.");return;}
        try{
            if(!sha256(mdp).equals(info[0])){tentatives.put(id,t+1);err.setText("Mot de passe incorrect. "+(3-t-1)+" essai(s).");return;}
            if(!info[1].equals(role)){err.setText("Rôle incorrect.");return;}
        }catch(Exception ex){return;}
        tentatives.put(id,0);
        sessionUser=id;sessionRole=info[1];sessionDept=info[2];
        sessionToken=id+"_"+UUID.randomUUID().toString().substring(0,8);
        log("SÉCURITÉ"," Connexion : "+id+" ["+role+"] — Dept: "+info[2]);
        audit(id,role,"CONNEXION_REUSSIE","Session: "+sessionToken.substring(0,8)+"…");
        mainCL.show(mainPanel,"DASHBOARD");
        updateSB(); startRefresh();
    }

    // ═══════ DASHBOARD ═══════════════════════════════════════
    JPanel buildDashboard(){
        JPanel root=new JPanel(new BorderLayout()); root.setBackground(C_BG_DARK);
        root.add(buildHeader(),BorderLayout.NORTH);
        alertBanner=new JLabel("",SwingConstants.CENTER);
        alertBanner.setFont(new Font("Segoe UI",Font.BOLD,13));
        alertBanner.setOpaque(true);alertBanner.setVisible(false);
        alertBanner.setBorder(new EmptyBorder(7,0,7,0));
        JTabbedPane tabs=new JTabbedPane();
        tabs.setBackground(C_BG_PANEL);tabs.setForeground(C_TEXT_WHITE);
        tabs.setFont(new Font("Segoe UI",Font.BOLD,13));
        tabs.addTab("  Carte & Robots", buildCarteTab());
        tabs.addTab("  Centre Commande",buildCentreTab());
        tabs.addTab(" Mode Dégradé",   buildModeDegradeTab());
        tabs.addTab("  Console / Logs", buildConsoleTab());
        tabs.addTab(" Honeypot",        buildHoneypotTab());
        JPanel c=new JPanel(new BorderLayout());c.setBackground(C_BG_DARK);
        c.add(alertBanner,BorderLayout.NORTH);c.add(tabs,BorderLayout.CENTER);
        statusBar=new JLabel("  Prêt.",SwingConstants.LEFT);
        statusBar.setFont(new Font("Courier New",Font.PLAIN,11));statusBar.setForeground(C_TEXT_MUTED);
        statusBar.setBackground(new Color(5,10,20));statusBar.setOpaque(true);
        statusBar.setBorder(new EmptyBorder(4,10,4,10));
        root.add(c,BorderLayout.CENTER);root.add(statusBar,BorderLayout.SOUTH);
        return root;
    }
    JPanel buildHeader(){
        JPanel h=new JPanel(new BorderLayout());h.setBackground(C_BG_PANEL);
        h.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0,0,2,0,C_ACCENT_BLUE),new EmptyBorder(11,20,11,20)));
        JPanel l=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));l.setBackground(C_BG_PANEL);
        l.add(lbl("🛡",26,Font.PLAIN,C_TEXT_WHITE));
        l.add(lbl("CyberSec Hospitalier",20,Font.BOLD,C_TEXT_WHITE));
        l.add(lbl("| Surveillance & Sécurité",12,Font.PLAIN,C_TEXT_MUTED));
        JPanel r=new JPanel(new FlowLayout(FlowLayout.RIGHT,12,0));r.setBackground(C_BG_PANEL);
        r.add(lbl("🔐 SHA-256",11,Font.BOLD,C_VERT));r.add(lbl("Session : –",12,Font.PLAIN,C_TEXT_MUTED));
        JButton bd=sbtn("Déconnexion",C_ROUGE);bd.setFont(new Font("Segoe UI",Font.BOLD,11));
        bd.addActionListener(e->deconnecter());r.add(bd);
        h.add(l,BorderLayout.WEST);h.add(r,BorderLayout.EAST);return h;
    }

    // ═══════ TAB 1 : CARTE + TABLEAU BAS ═════════════════════
    JPanel buildCarteTab(){
        JPanel root=new JPanel(new BorderLayout(0,0));root.setBackground(C_BG_DARK);
        cartePanel=new HospitalMap(false);
        cartePanel.setPreferredSize(new Dimension(1200,450));
        JScrollPane cs=new JScrollPane(cartePanel);cs.setBorder(null);cs.getViewport().setBackground(C_BG_DARK);
        JPanel leg=new JPanel(new FlowLayout(FlowLayout.LEFT,16,6));leg.setBackground(C_BG_PANEL);
        leg.setBorder(new MatteBorder(0,0,1,0,C_BORDER));
        leg.add(lbl("Légende :",11,Font.BOLD,C_TEXT_MUTED));
        addLeg(leg,C_VERT,"Opérationnel");addLeg(leg,C_ORANGE,"En alerte");
        addLeg(leg,C_ROUGE,"Critique/Isolé");addLeg(leg,C_CYAN,"Sécurité (fixe)");
        leg.add(lbl("  Double-clic sur un robot pour y accéder.",10,Font.ITALIC,C_TEXT_MUTED));
        // Table
        String[] cols={"ID","Département","Étage","Énergie","État","Mission","Accès min.","Isolé"};
        tableModel=new DefaultTableModel(cols,0){@Override public boolean isCellEditable(int r,int c){return false;}};
        refreshTable();
        table=new JTable(tableModel);
        table.setBackground(C_BG_CARD);table.setForeground(C_TEXT_WHITE);table.setGridColor(C_BORDER);
        table.setFont(new Font("Segoe UI",Font.PLAIN,12));table.setRowHeight(28);
        table.setSelectionBackground(new Color(56,128,255,70));
        table.getTableHeader().setBackground(C_BG_PANEL);table.getTableHeader().setForeground(C_TEXT_WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12));
        table.setDefaultRenderer(Object.class,new RobotRenderer());
        table.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){if(e.getClickCount()==2)acceder();}});
        JScrollPane ts=new JScrollPane(table);ts.setBorder(new MatteBorder(2,0,0,0,C_BORDER));
        ts.getViewport().setBackground(C_BG_CARD);ts.setPreferredSize(new Dimension(1200,200));
        JPanel btnBar=new JPanel(new FlowLayout(FlowLayout.LEFT,10,6));btnBar.setBackground(C_BG_PANEL);
        btnBar.setBorder(new MatteBorder(1,0,0,0,C_BORDER));
        JButton bAcc=sbtn("Accéder (double-clic)",C_ACCENT_BLUE);
        JButton bIso=sbtn("Isoler robot",C_ROUGE);
        JButton bRet=sbtn("Rétablir (Admin)",C_VERT);
        bAcc.addActionListener(e->acceder());bIso.addActionListener(e->isoler());bRet.addActionListener(e->retablir());
        btnBar.add(bAcc);btnBar.add(bIso);btnBar.add(bRet);
        JPanel bot=new JPanel(new BorderLayout());bot.setBackground(C_BG_PANEL);
        bot.add(ts,BorderLayout.CENTER);bot.add(btnBar,BorderLayout.SOUTH);
        root.add(leg,BorderLayout.NORTH);root.add(cs,BorderLayout.CENTER);root.add(bot,BorderLayout.SOUTH);
        return root;
    }

    void refreshTable(){
        if(tableModel==null)return;
        tableModel.setRowCount(0);
        for(int i=0;i<RD.length;i++){
            Object[] rb=RD[i];
            String etg=rb[2].equals(-1)?"Sous-sol":rb[2].equals(0)?"RDC":"Étage "+rb[2];
            String iso=isoles.getOrDefault((String)rb[0],false)?" OUI":"–";
            String rid=(String)rb[0];
            boolean isVital=ROBOTS_VITAUX.contains(rid);
            int et=modeDegrade?(isVital?etats[i]:2):etats[i];
            String etStr;
            if(modeDegrade&&isVital) etStr="✅ VITAL — Maintenu";
            else if(modeDegrade&&!isVital) etStr="⛔ ARRÊTÉ (mode dégradé)";
            else etStr=et==0?"✅ Normal":et==1?"⚠ Alerte":"🔴 Critique";
            tableModel.addRow(new Object[]{rid,rb[1],etg,energies[i]+"%",etStr,rb[5],rb[6],iso});
        }
    }

    // ── Carte Java2D ──────────────────────────────────────────
    class HospitalMap extends JPanel {
        boolean md; float pulse=0f; int pDir=1;
        HospitalMap(boolean md){
            this.md=md; setBackground(C_BG_DARK);
            if(md){new javax.swing.Timer(55,e->{pulse+=0.045f*pDir;if(pulse>=1f)pDir=-1;else if(pulse<=0f)pDir=1;repaint();}).start();}
        }
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int W=getWidth(),H=getHeight();
            // Grille de fond
            g2.setColor(new Color(255,255,255,5));
            for(int x=0;x<W;x+=40)g2.drawLine(x,0,x,H);
            for(int y=0;y<H;y+=40)g2.drawLine(0,y,W,y);
            // Titre
            g2.setFont(new Font("Segoe UI",Font.BOLD,12));g2.setColor(C_TEXT_MUTED);
            g2.drawString("PLAN DE L'HÔPITAL — VUE FONCTIONNELLE",12,20);
            // Overlay mode dégradé
            if(md){
                g2.setColor(new Color(239,68,68,(int)(pulse*30)));g2.fillRect(0,0,W,H);
                g2.setFont(new Font("Segoe UI",Font.BOLD,22));
                g2.setColor(new Color(239,68,68,(int)(pulse*200)));
                FontMetrics fm=g2.getFontMetrics();String txt="⚠  MODE DÉGRADÉ ACTIF  ⚠";
                g2.drawString(txt,(W-fm.stringWidth(txt))/2,H/2-24);
            }
            // Département
            for(Object[] dept:DEPTS){
                String nom=(String)dept[0];
                int dx=(int)((Double)dept[1]*W),dy=(int)((Double)dept[2]*(H-30))+28;
                int dw=(int)((Double)dept[3]*W),dh=(int)((Double)dept[4]*(H-30));
                Color col=md?new Color(239,68,68,55):(Color)dept[5];
                g2.setColor(col);g2.fillRoundRect(dx,dy,dw,dh,10,10);
                Color border=md?new Color(239,68,68,150):bright(col);
                g2.setColor(border);g2.setStroke(new BasicStroke(1.5f));g2.drawRoundRect(dx,dy,dw,dh,10,10);
                g2.setFont(new Font("Segoe UI",Font.BOLD,10));g2.setColor(new Color(255,255,255,195));
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(nom,dx+(dw-fm.stringWidth(nom))/2,dy+14);
            }
            // Robots
            for(int i=0;i<RD.length;i++){
                String id=(String)RD[i][0];
                int rx=(int)((Double)RD[i][7]*W),ry=(int)((Double)RD[i][8]*(H-30))+28;
                boolean iso=isoles.getOrDefault(id,false);
                boolean isVital=ROBOTS_VITAUX.contains(id);
                int et=md?(isVital?etats[i]:2):etats[i];
                int en=energies[i];
                Color rc=id.equals("SEC-01")?C_CYAN:iso?C_ROUGE:et==2?C_ROUGE:et==1?C_ORANGE:C_VERT;
                // En mode dégradé, vital = cyan/vert pulsant, non-vital = rouge
                if(md&&!iso){rc=isVital?(id.equals("SEC-01")?C_CYAN:C_VERT):C_ROUGE;}
                // Halo
                g2.setColor(new Color(rc.getRed(),rc.getGreen(),rc.getBlue(),30));
                g2.fillOval(rx-15,ry-15,30,30);
                // Corps
                g2.setColor(C_BG_CARD);g2.fillOval(rx-8,ry-8,16,16);
                g2.setColor(rc);g2.setStroke(new BasicStroke(iso?2.5f:2f));g2.drawOval(rx-8,ry-8,16,16);
                g2.fillOval(rx-3,ry-3,6,6);
                // X si isolé
                if(iso||et==2){g2.setColor(C_ROUGE);g2.setStroke(new BasicStroke(1.5f));g2.drawLine(rx-6,ry-6,rx+6,ry+6);g2.drawLine(rx+6,ry-6,rx-6,ry+6);}
                // Label
                g2.setFont(new Font("Segoe UI",Font.BOLD,9));
                FontMetrics fm=g2.getFontMetrics();int lx=rx-fm.stringWidth(id)/2;
                g2.setColor(new Color(8,14,28,200));g2.fillRoundRect(lx-2,ry+10,fm.stringWidth(id)+4,13,4,4);
                g2.setColor(rc);g2.drawString(id,lx,ry+21);
                // Barre énergie
                int bw=24,by=ry+25;
                g2.setColor(new Color(40,55,80));g2.fillRoundRect(rx-bw/2,by,bw,3,2,2);
                Color ec=en<20?C_ROUGE:en<50?C_ORANGE:C_VERT;
                g2.setColor(ec);g2.fillRoundRect(rx-bw/2,by,(int)(bw*en/100.0),3,2,2);
            }
            // Boussole
            g2.setColor(C_TEXT_MUTED);g2.setFont(new Font("Segoe UI",Font.PLAIN,9));g2.drawString("N↑",W-22,36);
        }
        Color bright(Color c){return new Color(Math.min(255,c.getRed()+80),Math.min(255,c.getGreen()+80),Math.min(255,c.getBlue()+80),190);}
    }

    void addLeg(JPanel p,Color c,String l){
        JLabel d=new JLabel("●");d.setForeground(c);d.setFont(new Font("Segoe UI",Font.PLAIN,14));
        p.add(d);p.add(lbl(l,11,Font.PLAIN,C_TEXT_MUTED));
    }

    class RobotRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,boolean foc,int row,int col){
            super.getTableCellRendererComponent(t,v,sel,foc,row,col);
            String et=(String)t.getValueAt(row,4),iso=(String)t.getValueAt(row,7);
            if(iso.contains("OUI"))setBackground(new Color(120,20,20,120));
            else if(et.contains("VITAL"))    {setBackground(new Color(20,80,40,160));setForeground(C_VERT);}
            else if(et.contains("ARRÊTÉ"))   {setBackground(new Color(60,20,20,160));setForeground(C_ROUGE);}
            else if(et.contains("Critique")) {setBackground(new Color(90,20,20,80)); setForeground(C_ROUGE);}
            else if(et.contains("Alerte"))   {setBackground(new Color(100,55,10,80));setForeground(C_ORANGE);}
            else {setBackground(sel?new Color(56,128,255,60):C_BG_CARD);setForeground(C_TEXT_WHITE);}
            setBorder(new EmptyBorder(0,8,0,8));return this;
        }
    }

    void acceder(){
        int row=table.getSelectedRow();if(row<0){showInfo("Sélectionnez un robot.");return;}
        String rid=(String)tableModel.getValueAt(row,0),roleMin=(String)tableModel.getValueAt(row,6);
        boolean iso=((String)tableModel.getValueAt(row,7)).contains("OUI");
        if(iso){showWarn("Robot isolé. Un ADMIN doit rétablir l'accès.");return;}
        if(!canAccess(sessionRole,roleMin)){showError("Accès refusé.\nRôle requis : "+roleMin+"\nVotre rôle : "+sessionRole);audit(sessionUser,sessionRole,"ACCES_REFUSE",rid);return;}
        JPasswordField pf=new JPasswordField();spf(pf);
        JLabel lh=new JLabel("SHA-256 : …");lh.setForeground(C_CYAN);lh.setFont(new Font("Courier New",Font.PLAIN,9));
        pf.addKeyListener(new KeyAdapter(){@Override public void keyReleased(KeyEvent e){try{lh.setText("SHA-256: "+sha256(new String(pf.getPassword())).substring(0,20)+"…");}catch(Exception ex){}}});
        JPanel panel=new JPanel();panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));panel.setBackground(C_BG_PANEL);panel.setBorder(new EmptyBorder(10,10,10,10));
        panel.add(lbl("Confirmez votre mot de passe :",12,Font.BOLD,C_TEXT_MUTED));panel.add(vg(4));panel.add(pf);panel.add(vg(4));panel.add(lh);
        if(JOptionPane.showConfirmDialog(this,panel,"Auth — "+rid,JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        String[] info=UTILISATEURS.get(sessionUser);
        try{if(!sha256(new String(pf.getPassword())).equals(info[0])){showError("Mot de passe incorrect.");return;}}catch(Exception ex){return;}
        showInfo("✅ Accès accordé à "+rid+"\nDépartement : "+tableModel.getValueAt(row,1)+"\nMission : "+tableModel.getValueAt(row,5));
        log("ACCÈS","✅ "+sessionUser+" → "+rid);audit(sessionUser,sessionRole,"ACCES_ROBOT",rid);
    }

    void isoler(){
        if(!"ADMIN".equals(sessionRole)){showError("ADMIN requis.");return;}
        int row=table.getSelectedRow();if(row<0){showInfo("Sélectionnez un robot.");return;}
        String rid=(String)tableModel.getValueAt(row,0);
        isoles.put(rid,true);for(int i=0;i<RD.length;i++)if(RD[i][0].equals(rid))etats[i]=2;
        refreshTable();if(cartePanel!=null)cartePanel.repaint();
        log("ADMIN","⛔ Robot isolé : "+rid);audit(sessionUser,sessionRole,"ISOLATION",rid);
        showAlert("⛔ Robot "+rid+" isolé du réseau !",C_ROUGE);
    }

    void retablir(){
        if(!"ADMIN".equals(sessionRole)){showError("ADMIN requis.");return;}
        int row=table.getSelectedRow();if(row<0){showInfo("Sélectionnez un robot.");return;}
        String rid=(String)tableModel.getValueAt(row,0);
        isoles.put(rid,false);for(int i=0;i<RD.length;i++)if(RD[i][0].equals(rid))etats[i]=(Integer)RD[i][4];
        refreshTable();if(cartePanel!=null)cartePanel.repaint();
        log("ADMIN","✅ Rétabli : "+rid);audit(sessionUser,sessionRole,"RETABLISSEMENT",rid);
    }

    // ═══════ TAB 2 : CENTRE COMMANDE ══════════════════════════
    JPanel buildCentreTab(){
        JPanel root=new JPanel(new GridLayout(1,3,14,0));root.setBackground(C_BG_DARK);root.setBorder(new EmptyBorder(14,14,14,14));
        root.add(buildPanelMission());root.add(buildPanelAlerte());root.add(buildPanelDonnees());return root;
    }
    JPanel buildPanelMission(){
        JPanel p=card("📋 Affecter une Mission");
        String[] rids=Arrays.stream(RD).map(r->(String)r[0]).toArray(String[]::new);
        JComboBox<String> cbR=scb(rids);JTextField tfM=stf();
        JComboBox<String> cbU=scb(new String[]{"ROUTINE","PRIORITAIRE","VITALE"});
        JPasswordField pfC=new JPasswordField();spf(pfC);
        JLabel lh=new JLabel("SHA-256 : …");lh.setFont(new Font("Courier New",Font.PLAIN,9));lh.setForeground(C_CYAN);
        pfC.addKeyListener(new KeyAdapter(){@Override public void keyReleased(KeyEvent e){try{lh.setText("SHA-256 : "+sha256(new String(pfC.getPassword())).substring(0,22)+"…");}catch(Exception ex){}}});
        JButton btn=sbtn("Affecter Mission",C_ACCENT_BLUE);
        btn.addActionListener(e->{
            String rob=(String)cbR.getSelectedItem(),mis=tfM.getText().trim(),urg=(String)cbU.getSelectedItem();
            if(mis.isEmpty()){showInfo("Entrez une mission.");return;}
            log("COMMANDE","Mission ["+urg+"] → "+rob+" : "+mis);
            audit(sessionUser,sessionRole,"MISSION",rob+"|"+urg);
            if("VITALE".equals(urg))showAlert("⚠️ MISSION VITALE affectée à "+rob+" !",C_ORANGE);
            showInfo("✅ Mission affectée à "+rob+"\n["+urg+"] "+mis);
        });
        addR(p,new String[]{"Robot cible","Nouvelle mission","Niveau d'urgence","Code de sécurité"},new JComponent[]{cbR,tfM,cbU,pfC});
        p.add(lh);p.add(vg(14));p.add(btn);return p;
    }
    JPanel buildPanelAlerte(){
        JPanel p=card("🚨 Déclencher une Alerte");
        JComboBox<String> cbT=scb(new String[]{"INTRUSION RÉSEAU","RANSOMWARE","BRUTE FORCE","ROBOT COMPROMIS","MODIFICATION DONNÉES","DDOS","URGENCE VITALE"});
        JTextArea ta=new JTextArea(3,20);ta.setBackground(C_BG_CARD);ta.setForeground(C_TEXT_WHITE);ta.setCaretColor(C_TEXT_WHITE);ta.setFont(new Font("Segoe UI",Font.PLAIN,12));ta.setBorder(new LineBorder(C_BORDER));
        JButton b1=sbtn("🚨 Déclencher Alerte",C_ROUGE);
        JButton b2=sbtn("🔴 Activer Mode Dégradé",new Color(110,10,10));
        JButton b3=sbtn("✅ Restaurer Mode Normal",C_VERT);
        b1.addActionListener(e->{String t=(String)cbT.getSelectedItem();log("ALERTE","🚨 ["+t+"]");audit(sessionUser,sessionRole,"ALERTE_"+t.replace(" ","_"),"");showAlert("🚨 ALERTE : "+t,C_ROUGE);});
        b2.addActionListener(e->{if(!"ADMIN".equals(sessionRole)){showError("ADMIN requis.");return;}if(JOptionPane.showConfirmDialog(this,"Activer le MODE DÉGRADÉ ?","Confirmation",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){log("ADMIN","🔴 MODE DÉGRADÉ lancé.");showAlert("🔴 MODE DÉGRADÉ ACTIF",C_ROUGE);}});
        b3.addActionListener(e->{if(!"ADMIN".equals(sessionRole)){showError("ADMIN requis.");return;}log("ADMIN","✅ Mode normal restauré.");alertBanner.setVisible(false);});
        addR(p,new String[]{"Type d'alerte","Description"},new JComponent[]{cbT,new JScrollPane(ta)});
        p.add(vg(12));p.add(b1);p.add(vg(8));p.add(b2);p.add(vg(8));p.add(b3);return p;
    }
    JPanel buildPanelDonnees(){
        JPanel p=card("💾 Données Patient (Chiffrées)");
        JLabel note=new JLabel("<html><font color='#94a3b8'>Réservé aux MÉDECINS.<br>Chiffrement AES-256 avant envoi.</font></html>");note.setFont(new Font("Segoe UI",Font.PLAIN,11));
        JTextField tfP=stf();JTextArea taD=new JTextArea(4,20);taD.setBackground(C_BG_CARD);taD.setForeground(C_TEXT_WHITE);taD.setCaretColor(C_TEXT_WHITE);taD.setFont(new Font("Segoe UI",Font.PLAIN,12));taD.setBorder(new LineBorder(C_BORDER));
        JLabel lAES=new JLabel("AES-256 : (données chiffrées)");lAES.setFont(new Font("Courier New",Font.PLAIN,9));lAES.setForeground(C_CYAN);
        JButton btn=sbtn("🔐 Chiffrer & Envoyer",C_ACCENT_BLUE);
        btn.addActionListener(e->{
            if(!"MEDECIN".equals(sessionRole)&&!"ADMIN".equals(sessionRole)){showError("Seuls les MÉDECINS peuvent soumettre des données patient.");audit(sessionUser,sessionRole,"ACCES_REFUSE_DONNEES","Rôle insuffisant");return;}
            String pid=tfP.getText().trim(),data=taD.getText().trim();
            if(pid.isEmpty()||data.isEmpty()){showInfo("Remplissez tous les champs.");return;}
            String ch="[AES-256|CBC|HOSPITAL_KEY]"+Base64.getEncoder().encodeToString(data.getBytes())+"[/AES]";
            lAES.setText("AES : "+ch.substring(0,Math.min(30,ch.length()))+"…");
            log("DONNÉES","🔐 Patient "+pid+" — données chiffrées AES-256.");audit(sessionUser,sessionRole,"MODIF_DONNEES","Patient "+pid);
            showInfo("✅ Données chiffrées.\nPatient : "+pid+"\nAES : "+ch.substring(0,28)+"…");
        });
        p.add(note);p.add(vg(12));addR(p,new String[]{"ID Patient","Données médicales"},new JComponent[]{tfP,new JScrollPane(taD)});p.add(vg(4));p.add(lAES);p.add(vg(14));p.add(btn);return p;
    }

    // ═══════ TAB 3 : MODE DÉGRADÉ SIMULATION ══════════════════
    JPanel buildModeDegradeTab(){
        JPanel root=new JPanel(new BorderLayout(10,10));root.setBackground(C_BG_DARK);root.setBorder(new EmptyBorder(14,14,14,14));
        // Header
        JPanel hdr=new JPanel(new BorderLayout());hdr.setBackground(new Color(90,10,10,120));
        hdr.setBorder(BorderFactory.createCompoundBorder(new LineBorder(C_ROUGE,2,true),new EmptyBorder(12,18,12,18)));
        JLabel tit=new JLabel("🔴 MODE DÉGRADÉ ANTI-RANSOMWARE — SIMULATION INTERACTIVE",SwingConstants.CENTER);
        tit.setFont(new Font("Segoe UI",Font.BOLD,17));tit.setForeground(C_ROUGE);
        JLabel desc=new JLabel("<html><center><font color='#94a3b8'>Inspiré des cyberattaques réelles (Hôpital d'Armentières 2024 / NHS 2024).<br>Ransomware détecté → Isolement réseau → Mode local → Missions vitales maintenues.</font></center></html>",SwingConstants.CENTER);
        desc.setFont(new Font("Segoe UI",Font.PLAIN,12));
        hdr.add(tit,BorderLayout.NORTH);hdr.add(desc,BorderLayout.CENTER);
        // Corps
        JPanel body=new JPanel(new BorderLayout(10,0));body.setBackground(C_BG_DARK);
        // Carte mode dégradé
        mdCartePanel=new HospitalMap(false);mdCartePanel.setPreferredSize(new Dimension(680,300));
        JScrollPane ms=new JScrollPane(mdCartePanel);ms.setBorder(new LineBorder(C_BORDER));
        mdStatut=new JLabel("État : En attente",SwingConstants.CENTER);
        mdStatut.setFont(new Font("Segoe UI",Font.BOLD,12));mdStatut.setForeground(C_TEXT_WHITE);mdStatut.setBorder(new EmptyBorder(5,0,5,0));
        mdProgress=new JProgressBar(0,7);mdProgress.setStringPainted(true);mdProgress.setString("Simulation non démarrée");
        mdProgress.setBackground(C_BG_CARD);mdProgress.setForeground(C_ROUGE);mdProgress.setFont(new Font("Segoe UI",Font.BOLD,11));
        JPanel ms2=new JPanel(new BorderLayout(0,4));ms2.setBackground(C_BG_DARK);ms2.add(ms,BorderLayout.CENTER);
        JPanel statRow=new JPanel(new BorderLayout(0,3));statRow.setBackground(C_BG_DARK);statRow.add(mdStatut,BorderLayout.NORTH);statRow.add(mdProgress,BorderLayout.CENTER);
        ms2.add(statRow,BorderLayout.SOUTH);
        // Log
        mdLog=new JTextArea();mdLog.setEditable(false);mdLog.setBackground(new Color(8,3,3));mdLog.setForeground(C_ROUGE);mdLog.setFont(new Font("Courier New",Font.PLAIN,12));mdLog.setBorder(new EmptyBorder(8,10,8,10));
        JScrollPane ls=new JScrollPane(mdLog);ls.setBorder(BorderFactory.createCompoundBorder(new LineBorder(C_ROUGE,1),new EmptyBorder(0,0,0,0)));
        body.add(ms2,BorderLayout.CENTER);body.add(ls,BorderLayout.EAST);
        ls.setPreferredSize(new Dimension(380,0));
        // Étapes
        JPanel etapes=buildEtapes();
        // Boutons
        JPanel btnR=new JPanel(new FlowLayout(FlowLayout.CENTER,14,8));btnR.setBackground(C_BG_DARK);btnR.setBorder(new MatteBorder(1,0,0,0,C_BORDER));
        JButton bL=sbtn("🔴 Lancer la Simulation",C_ROUGE);bL.setFont(new Font("Segoe UI",Font.BOLD,13));
        JButton bS=sbtn("⏹ Stopper",C_BG_CARD);JButton bR=sbtn("🔄 Réinitialiser",new Color(30,80,130));
        bL.addActionListener(e->{if(!"ADMIN".equals(sessionRole)){showError("ADMIN requis.");return;}lancerMD();});
        bS.addActionListener(e->stopMD());bR.addActionListener(e->resetMD());
        btnR.add(bL);btnR.add(bS);btnR.add(bR);
        root.add(hdr,BorderLayout.NORTH);root.add(etapes,BorderLayout.WEST);root.add(body,BorderLayout.CENTER);root.add(btnR,BorderLayout.SOUTH);
        return root;
    }

    JPanel buildEtapes(){
        JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBackground(C_BG_PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0,0,0,1,C_BORDER),new EmptyBorder(14,10,14,10)));
        p.setPreferredSize(new Dimension(215,0));
        p.add(lbl("📋 Étapes simulation",12,Font.BOLD,C_TEXT_WHITE));p.add(vg(10));
        String[][] et={
            {"1","Ransomware détecté","Chiffrement malveillant sur réseau"},
            {"2","Alerte ROUGE","Cyberattaque confirmée"},
            {"3","Isolement réseau","Déconnexion réseau principal"},
            {"4","Robots déconnectés","Flotte isolée du réseau"},
            {"5","Mode dégradé","Basculement fonctionnement local"},
            {"6","Missions vitales","Urgences & réanimation maintenues"},
            {"7","Rapport généré","Audit trail légal disponible"},
        };
        for(String[] e:et){
            JPanel row=new JPanel(new BorderLayout(6,0));row.setBackground(C_BG_PANEL);row.setMaximumSize(new Dimension(195,44));
            row.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0,0,1,0,C_BORDER),new EmptyBorder(5,0,5,0)));
            JLabel num=new JLabel(e[0]);num.setFont(new Font("Segoe UI",Font.BOLD,13));num.setForeground(C_ROUGE);num.setPreferredSize(new Dimension(16,0));
            JPanel txt=new JPanel();txt.setLayout(new BoxLayout(txt,BoxLayout.Y_AXIS));txt.setBackground(C_BG_PANEL);
            txt.add(lbl(e[1],11,Font.BOLD,C_TEXT_WHITE));txt.add(lbl("<html><font size='2' color='#64748b'>"+e[2]+"</font></html>",9,Font.PLAIN,C_TEXT_MUTED));
            row.add(num,BorderLayout.WEST);row.add(txt,BorderLayout.CENTER);p.add(row);p.add(vg(2));
        }
        return p;
    }

    static final String[] MD_MSGS={
        "⚠  Comportement ransomware détecté — chiffrement malveillant identifié sur 3 nœuds réseau...",
        "🔴 Niveau d'alerte escaladé : VERT → ORANGE → ROUGE. Cyberattaque confirmée par l'IDS.",
        "📡 Déconnexion du réseau principal lancée...\n   Réseau principal ISOLÉ.",
        "🤖 Déconnexion forcée de la flotte non-vitale :\n   ✗ CARD-01, NEUR-01, ONCO-01 déconnectés\n   ✗ CHIR-01, CHIR-02 déconnectés\n   ✗ RAD-01, LABO-01, PHAR-01 déconnectés\n   ✗ PED-01, MAINT-01, HYG-01 déconnectés\n   ✅ SEC-01, URG-01, REA-01, REA-02 → MAINTENUS (missions vitales)",
        "🔒 MODE DÉGRADÉ ACTIVÉ — Robots non-vitaux basculent en mode local isolé.\n   Synchronisation réseau suspendue.",
        "❤  Missions vitales maintenues :\n   ✅ SEC-01 : Surveillance réseau — ACTIF\n   ✅ URG-01 : Transport médicaments & assistance critique — ACTIF\n   ✅ REA-01 : Surveillance continue réanimation — ACTIF\n   ✅ REA-02 : Réanimation 2 — MAINTENU\n   ✗ Missions secondaires (Cardiologie, Chirurgie, etc.) : SUSPENDUES\n   ✗ Synchronisation BDD centrale : SUSPENDUE",
        "📋 Rapport d'incident généré automatiquement.\n   Audit trail légal disponible pour enquête.\n   Hash SHA-256 de l'incident calculé.\n✅ Système STABILISÉ — En attente de restauration par ADMIN.",
    };
    static final String[] MD_STATUTS={
        "🔍 Ransomware détecté…","🔴 Alerte CRITIQUE","📡 Isolement réseau…",
        "🤖 Déconnexion robots…","🔒 MODE DÉGRADÉ ACTIF","❤  Missions vitales maintenues","✅ Incident stabilisé"
    };

    void lancerMD(){
        if(mdTimer!=null)mdTimer.cancel();
        mdLog.setText("");mdStep=0;modeDegrade=false;mdProgress.setValue(0);
        mdLog.append("═══ SIMULATION MODE DÉGRADÉ — "+LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+" ═══\n\n");
        log("MODE_DÉGRADÉ","🔴 Simulation lancée par "+sessionUser);audit(sessionUser,sessionRole,"SIMULATION_MD_START","");
        mdTimer=new Timer(true);
        mdTimer.scheduleAtFixedRate(new TimerTask(){@Override public void run(){
            SwingUtilities.invokeLater(()->{
                if(mdStep>=MD_MSGS.length){if(mdTimer!=null){mdTimer.cancel();mdTimer=null;}return;}
                String ts=LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
                mdLog.append("["+ts+"] "+MD_MSGS[mdStep]+"\n\n");
                mdLog.setCaretPosition(mdLog.getDocument().getLength());
                mdStatut.setText(MD_STATUTS[mdStep]);
                mdProgress.setValue(mdStep+1);mdProgress.setString(MD_STATUTS[mdStep]);
                if(mdStep>=3){modeDegrade=true;mdCartePanel.md=true;refreshTable();}
                if(mdStep>=4)showAlert("🔴 MODE DÉGRADÉ ACTIF — Réseau hospitalier isolé !",C_ROUGE);
                mdCartePanel.repaint();
                if(cartePanel!=null){cartePanel.md=modeDegrade;cartePanel.repaint();}
                mdStep++;
            });
        }},500,1900);
    }
    void stopMD(){if(mdTimer!=null){mdTimer.cancel();mdTimer=null;}mdLog.append("\n[SIMULATION STOPPÉE]\n");log("MODE_DÉGRADÉ","⏹ Stoppé.");}
    void resetMD(){
        stopMD();modeDegrade=false;mdStep=0;mdCartePanel.md=false;
        for(int i=0;i<etats.length;i++)etats[i]=(Integer)RD[i][4];
        for(int i=0;i<energies.length;i++)energies[i]=(Integer)RD[i][3];
        refreshTable();mdCartePanel.repaint();if(cartePanel!=null){cartePanel.md=false;cartePanel.repaint();}
        mdLog.setText("");mdStatut.setText("État : En attente");mdProgress.setValue(0);mdProgress.setString("Simulation non démarrée");
        alertBanner.setVisible(false);log("MODE_DÉGRADÉ","🔄 Réinitialisé.");
    }

    // ═══════ TAB 4 : CONSOLE ══════════════════════════════════
    JPanel buildConsoleTab(){
        JPanel root=new JPanel(new BorderLayout(10,10));root.setBackground(C_BG_DARK);root.setBorder(new EmptyBorder(14,14,14,14));
        consoleArea=new JTextArea();consoleArea.setEditable(false);consoleArea.setBackground(new Color(5,10,20));
        consoleArea.setForeground(new Color(160,255,160));consoleArea.setFont(new Font("Courier New",Font.PLAIN,12));consoleArea.setBorder(new EmptyBorder(8,10,8,10));
        JScrollPane sc=new JScrollPane(consoleArea);sc.setBorder(new LineBorder(C_BORDER));
        JPanel tb=new JPanel(new FlowLayout(FlowLayout.LEFT,10,5));tb.setBackground(C_BG_PANEL);tb.setBorder(new MatteBorder(0,0,1,0,C_BORDER));
        tb.add(lbl("📋 Console de Monitoring",13,Font.BOLD,C_TEXT_WHITE));
        JButton bE=sbtn("Effacer",C_BG_CARD),bD=sbtn("⬇ Télécharger Logs",C_ACCENT_BLUE),bA=sbtn("📜 Audit Trail (Admin)",C_GOLD);
        bA.setForeground(Color.BLACK);bE.addActionListener(e->consoleArea.setText(""));bD.addActionListener(e->dlLogs());bA.addActionListener(e->exportAudit());
        tb.add(bE);tb.add(bD);tb.add(bA);root.add(tb,BorderLayout.NORTH);root.add(sc,BorderLayout.CENTER);return root;
    }
    void dlLogs(){
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("logs_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt"));
        
        if(fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION){
            try {
                String content = "=== LOGS ===\nExport : " + LocalDateTime.now() + "\nUtilisateur : " + sessionUser + " [" + sessionRole + "]\n" + consoleArea.getText();
                // Remplacement compatible Java 8
                Files.write(fc.getSelectedFile().toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                
                showInfo("Logs exportés.");
            } catch(IOException ex) {
                showError("Erreur : " + ex.getMessage());
            }
        }
    }

    void exportAudit(){
        if(!"ADMIN".equals(sessionRole)){
            showError("ADMIN requis.");
            return;
        }
        
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("audit_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt"));
        
        if(fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION){
            try {
                StringBuilder sb = new StringBuilder("=== AUDIT TRAIL LÉGAL ===\nExport : " + LocalDateTime.now() + "\nEntrées : " + auditL.size() + "\n\n");
                for(String e : auditL) {
                    sb.append(e).append("\n");
                }
                
                // Remplacement compatible Java 8
                Files.write(fc.getSelectedFile().toPath(), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                
                showInfo("Audit trail exporté.");
            } catch(IOException ex) {
                showError("Erreur : " + ex.getMessage());
            }
        }
    }

    // ═══════ TAB 5 : HONEYPOT ULTRA-VISUEL ════════════════════
    JPanel buildHoneypotTab(){
        JPanel root=new JPanel(new BorderLayout(10,10));root.setBackground(C_BG_DARK);root.setBorder(new EmptyBorder(12,12,12,12));
        // Top : radar gauche + infos droite
        JPanel top=new JPanel(new BorderLayout(12,0));top.setBackground(C_BG_DARK);
        RadarPanel radar=new RadarPanel();radar.setPreferredSize(new Dimension(290,290));hpRadar=radar;
        top.add(radar,BorderLayout.WEST);
        // Infos + sim
        JPanel info=new JPanel(new BorderLayout(0,10));info.setBackground(C_BG_DARK);
        // Bannière
        JPanel banner=new JPanel(new BorderLayout());banner.setBackground(new Color(80,50,0,120));
        banner.setBorder(BorderFactory.createCompoundBorder(new LineBorder(C_GOLD,2,true),new EmptyBorder(12,16,12,16)));
        JLabel hTit=new JLabel("🍯 HONEYPOT — SYSTÈME DE PIÈGE À INTRUS");hTit.setFont(new Font("Segoe UI",Font.BOLD,16));hTit.setForeground(C_GOLD);
        JLabel hDesc=new JLabel("<html><font color='#94a3b8'>Ressource piège : <b style='color:#fbbf24'>DOSSIER_PATIENT_VIP_CONFIDENTIEL</b><br>Aucun utilisateur légitime n'accède jamais à cette ressource fictive.<br>Tout accès = <b style='color:#ef4444'>intrusion confirmée à 100%</b>. Technique utilisée dans les vrais SOC.</font></html>");
        hDesc.setFont(new Font("Segoe UI",Font.PLAIN,12));
        hpCount=new JLabel("0 intrusion détectée",SwingConstants.RIGHT);hpCount.setFont(new Font("Segoe UI",Font.BOLD,22));hpCount.setForeground(C_GOLD);
        banner.add(hTit,BorderLayout.NORTH);banner.add(hDesc,BorderLayout.CENTER);banner.add(hpCount,BorderLayout.EAST);
        // Sim panel
        JPanel sim=new JPanel();sim.setLayout(new BoxLayout(sim,BoxLayout.Y_AXIS));sim.setBackground(C_BG_PANEL);
        sim.setBorder(BorderFactory.createCompoundBorder(new LineBorder(C_BORDER,1,true),new EmptyBorder(14,14,14,14)));
        sim.add(lbl("🧪 Simuler un accès à une ressource :",12,Font.BOLD,C_TEXT_WHITE));sim.add(vg(10));
        JPanel rRow=new JPanel(new BorderLayout(10,0));rRow.setBackground(C_BG_PANEL);
        JLabel lR=lbl("Ressource demandée :",11,Font.BOLD,C_TEXT_MUTED);lR.setPreferredSize(new Dimension(155,28));
        JTextField tfR=stf();tfR.setText("DOSSIER_PATIENT_VIP_CONFIDENTIEL");rRow.add(lR,BorderLayout.WEST);rRow.add(tfR,BorderLayout.CENTER);
        JPanel aRow=new JPanel(new BorderLayout(10,0));aRow.setBackground(C_BG_PANEL);
        JLabel lA=lbl("Acteur simulé :",11,Font.BOLD,C_TEXT_MUTED);lA.setPreferredSize(new Dimension(155,28));
        JTextField tfA=stf();tfA.setText("ROBOT_COMPROMIS_01");aRow.add(lA,BorderLayout.WEST);aRow.add(tfA,BorderLayout.CENTER);
        // Scénarios rapides
        JPanel scRow=new JPanel(new FlowLayout(FlowLayout.LEFT,7,3));scRow.setBackground(C_BG_PANEL);
        scRow.add(lbl("Scénarios rapides :",10,Font.BOLD,C_TEXT_MUTED));
        String[][] sc={
            {"🤖 Robot compromis","DOSSIER_PATIENT_VIP_CONFIDENTIEL","ROBOT_REA-02"},
            {"👤 Accès légitime","DOSSIER_CARDIOLOGIE_001","DR_MARTIN"},
            {"💻 Attaquant ext.","DOSSIER_PATIENT_VIP_CONFIDENTIEL","ATTAQUANT_EXT"},
            {"🔑 Accès normal","LABO_ANALYSES_2024","INF_SOPHIE"},
        };
        for(String[] s:sc){JButton b=new JButton(s[0]);b.setBackground(C_BG_CARD);b.setForeground(C_TEXT_WHITE);b.setFont(new Font("Segoe UI",Font.PLAIN,11));b.setBorderPainted(false);b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.setBorder(new EmptyBorder(5,10,5,10));b.addActionListener(e->{tfR.setText(s[1]);tfA.setText(s[2]);});scRow.add(b);}
        JButton btnTest=sbtn("🪤 Tester l'Accès",C_GOLD);btnTest.setForeground(Color.BLACK);btnTest.setFont(new Font("Segoe UI",Font.BOLD,13));btnTest.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnTest.addActionListener(e->testHoneypot(tfR.getText().trim(),tfA.getText().trim()));
        sim.add(rRow);sim.add(vg(8));sim.add(aRow);sim.add(vg(10));sim.add(scRow);sim.add(vg(12));sim.add(btnTest);
        info.add(banner,BorderLayout.NORTH);info.add(sim,BorderLayout.CENTER);
        top.add(info,BorderLayout.CENTER);
        // Journal bas
        JPanel jPanel=new JPanel(new BorderLayout(0,5));jPanel.setBackground(C_BG_DARK);jPanel.setBorder(new MatteBorder(2,0,0,0,C_BORDER));jPanel.setPreferredSize(new Dimension(0,185));
        JLabel jTit=lbl("📜 Journal des Intrusions Honeypot",12,Font.BOLD,C_GOLD);jTit.setBorder(new EmptyBorder(5,4,5,0));
        hpDetails=new JTextArea();hpDetails.setEditable(false);hpDetails.setBackground(new Color(15,10,0));hpDetails.setForeground(C_GOLD);hpDetails.setFont(new Font("Courier New",Font.PLAIN,12));hpDetails.setBorder(new EmptyBorder(6,10,6,10));hpDetails.setText("En attente d'intrusion…\n");
        JScrollPane dS=new JScrollPane(hpDetails);dS.setBorder(new LineBorder(new Color(100,70,0),1));
        jPanel.add(jTit,BorderLayout.NORTH);jPanel.add(dS,BorderLayout.CENTER);
        root.add(top,BorderLayout.CENTER);root.add(jPanel,BorderLayout.SOUTH);
        return root;
    }

    void testHoneypot(String res,String acteur){
        String ts=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        if("DOSSIER_PATIENT_VIP_CONFIDENTIEL".equals(res)){
            hpEvents.add(new HpEvent(acteur,res,ts,true));hpIntrusions.add("["+ts+"] 🚨 "+acteur);
            hpDetails.append("═══════════════════════════════════════════\n");
            hpDetails.append("🚨 PIÈGE DÉCLENCHÉ — "+ts+"\n");
            hpDetails.append("   Acteur   : "+acteur+"\n");
            hpDetails.append("   Ressource: "+res+"\n");
            hpDetails.append("   Verdict  : INTRUSION CONFIRMÉE À 100%\n");
            hpDetails.append("   Actions  : Alerte ROUGE | Audit trail | Blocage\n");
            hpDetails.append("═══════════════════════════════════════════\n\n");
            hpDetails.setCaretPosition(hpDetails.getDocument().getLength());
            long cnt=hpEvents.stream().filter(HpEvent::confirmed).count();
            hpCount.setText(cnt+" intrusion"+(cnt>1?"s":"")+" détectée"+(cnt>1?"s":""));hpCount.setForeground(C_ROUGE);
            log("HONEYPOT","🚨 INTRUSION par "+acteur);audit(acteur,"INTRUS","HONEYPOT_DECLENCHE",res);
            showAlert("🍯 HONEYPOT DÉCLENCHÉ par "+acteur+" !",C_GOLD);
            if(hpRadar!=null)hpRadar.repaint();
            // Dialogue
            JDialog dlg=new JDialog(this,"🚨 INTRUSION HONEYPOT",true);dlg.setSize(440,240);dlg.setLocationRelativeTo(this);
            JPanel dp=new JPanel(new BorderLayout());dp.setBackground(new Color(80,5,5));dp.setBorder(new LineBorder(C_ROUGE,3));
            JLabel dt=new JLabel("⛔ PIÈGE DÉCLENCHÉ !",SwingConstants.CENTER);dt.setFont(new Font("Segoe UI",Font.BOLD,22));dt.setForeground(C_ROUGE);dt.setBorder(new EmptyBorder(18,0,10,0));
            JLabel di=new JLabel("<html><center><font color='#fca5a5'>"+acteur+"<br>a tenté d'accéder à une ressource confidentielle inexistante.<br><br><b style='color:#fbbf24'>Intrusion enregistrée dans l'audit trail légal.</b><br>Niveau d'alerte : <b>ROUGE</b></font></center></html>",SwingConstants.CENTER);di.setFont(new Font("Segoe UI",Font.PLAIN,13));
            JButton ok=sbtn("Compris",C_ROUGE);JPanel okP=new JPanel();okP.setBackground(new Color(80,5,5));okP.add(ok);okP.setBorder(new EmptyBorder(0,0,14,0));ok.addActionListener(x->dlg.dispose());
            dp.add(dt,BorderLayout.NORTH);dp.add(di,BorderLayout.CENTER);dp.add(okP,BorderLayout.SOUTH);dlg.add(dp);dlg.setVisible(true);
        }else{
            hpEvents.add(new HpEvent(acteur,res,ts,false));
            hpDetails.append("["+ts+"] ✅ Accès normal — '"+res+"' par "+acteur+"\n");
            hpDetails.setCaretPosition(hpDetails.getDocument().getLength());
            log("HONEYPOT","Accès normal : '"+res+"' par "+acteur);
            if(hpRadar!=null)hpRadar.repaint();
        }
    }

    // Panneau radar animé
    class RadarPanel extends JPanel {
        float angle=0f; int pulse=0,pDir=1;
        RadarPanel(){
            setBackground(C_BG_DARK);
            new javax.swing.Timer(30,e->{angle=(angle+2f)%360f;pulse+=pDir*2;if(pulse>=80)pDir=-1;else if(pulse<=0)pDir=1;repaint();}).start();
        }
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g;g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int W=getWidth(),H=getHeight(),cx=W/2,cy=H/2,R=Math.min(W,H)/2-18;
            // Fond radar
            g2.setColor(new Color(0,20,5));g2.fillOval(cx-R,cy-R,R*2,R*2);
            // Cercles
            for(int r=R/4;r<=R;r+=R/4){g2.setColor(new Color(0,180,50,38));g2.setStroke(new BasicStroke(1f));g2.drawOval(cx-r,cy-r,r*2,r*2);}
            // Croix
            g2.setColor(new Color(0,180,50,28));g2.drawLine(cx-R,cy,cx+R,cy);g2.drawLine(cx,cy-R,cx,cy+R);
            // Balayage
            boolean hasIntrus=!hpIntrusions.isEmpty();
            Color sw=hasIntrus?new Color(220,30,30,180):new Color(0,220,80,180);
            for(int da=0;da<60;da++){
                float fa=(float)Math.toRadians(angle-da);int al=(int)(155*(1f-da/60f));
                g2.setColor(new Color(sw.getRed(),sw.getGreen(),sw.getBlue(),al));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawLine(cx,cy,(int)(cx+R*Math.cos(fa)),(int)(cy+R*Math.sin(fa)));
            }
            // Points
            for(int i=0;i<hpEvents.size();i++){
                HpEvent ev=hpEvents.get(i);Random rng=new Random(i*31337L);
                double ra=rng.nextDouble()*Math.PI*2,rr=0.3+rng.nextDouble()*0.55;
                int ex=(int)(cx+R*rr*Math.cos(ra)),ey=(int)(cy+R*rr*Math.sin(ra));
                Color dc=ev.confirmed()?C_ROUGE:C_VERT;
                g2.setColor(new Color(dc.getRed(),dc.getGreen(),dc.getBlue(),40));
                int ph=pulse+i*15;g2.fillOval(ex-ph/4-6,ey-ph/4-6,ph/4*2+12,ph/4*2+12);
                g2.setColor(dc);g2.fillOval(ex-5,ey-5,10,10);
                // Croix rouge si intrusion confirmée
                if(ev.confirmed()){g2.setColor(new Color(255,100,100,180));g2.setStroke(new BasicStroke(1.5f));g2.drawLine(ex-4,ey-4,ex+4,ey+4);g2.drawLine(ex+4,ey-4,ex-4,ey+4);}
            }
            // Label
            g2.setFont(new Font("Courier New",Font.BOLD,10));g2.setColor(new Color(0,200,50));g2.drawString("HONEYPOT RADAR",cx-54,cy+R+16);
            // Compteur centre
            long nb=hpEvents.stream().filter(HpEvent::confirmed).count();
            g2.setFont(new Font("Segoe UI",Font.BOLD,16));Color cc=nb>0?C_ROUGE:C_VERT;g2.setColor(cc);
            String cs2=nb+" intrus.";FontMetrics fm=g2.getFontMetrics();g2.drawString(cs2,cx-fm.stringWidth(cs2)/2,cy+6);
            // Bord
            g2.setColor(new Color(0,120,40,120));g2.setStroke(new BasicStroke(2f));g2.drawOval(cx-R,cy-R,R*2,R*2);
        }
    }

    // ═══════ HELPERS ══════════════════════════════════════════
    JPanel card(String t){
        JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBackground(C_BG_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(new LineBorder(C_BORDER,1,true),new EmptyBorder(18,18,18,18)));
        JLabel tl=lbl(t,14,Font.BOLD,C_TEXT_WHITE);tl.setAlignmentX(0f);
        JSeparator sep=new JSeparator();sep.setForeground(C_BORDER);sep.setMaximumSize(new Dimension(Integer.MAX_VALUE,1));
        p.add(tl);p.add(vg(4));p.add(sep);p.add(vg(14));return p;
    }
    void addR(JPanel p,String[] ls,JComponent[] fs){for(int i=0;i<ls.length;i++){JLabel l=lbl(ls[i],11,Font.BOLD,C_TEXT_MUTED);l.setAlignmentX(0f);fs[i].setAlignmentX(0f);p.add(l);p.add(vg(3));p.add(fs[i]);p.add(vg(10));}}
    JLabel lbl(String t,int sz,int st,Color c){JLabel l=new JLabel(t);l.setFont(new Font("Segoe UI",st,sz));l.setForeground(c);return l;}
    JTextField stf(){JTextField tf=new JTextField();tf.setBackground(C_BG_CARD);tf.setForeground(C_TEXT_WHITE);tf.setCaretColor(C_TEXT_WHITE);tf.setFont(new Font("Segoe UI",Font.PLAIN,13));tf.setBorder(BorderFactory.createCompoundBorder(new LineBorder(C_BORDER),new EmptyBorder(6,10,6,10)));tf.setMaximumSize(new Dimension(Integer.MAX_VALUE,36));return tf;}
    void spf(JPasswordField pf){pf.setBackground(C_BG_CARD);pf.setForeground(C_TEXT_WHITE);pf.setCaretColor(C_TEXT_WHITE);pf.setFont(new Font("Segoe UI",Font.PLAIN,13));pf.setBorder(BorderFactory.createCompoundBorder(new LineBorder(C_BORDER),new EmptyBorder(6,10,6,10)));pf.setMaximumSize(new Dimension(Integer.MAX_VALUE,36));}
    <T> JComboBox<T> scb(T[] items){JComboBox<T> cb=new JComboBox<>(items);cb.setBackground(C_BG_CARD);cb.setForeground(C_TEXT_WHITE);cb.setFont(new Font("Segoe UI",Font.PLAIN,13));cb.setMaximumSize(new Dimension(Integer.MAX_VALUE,36));cb.setBorder(new LineBorder(C_BORDER));return cb;}
    JButton sbtn(String t,Color bg){JButton b=new JButton(t);b.setBackground(bg);b.setForeground(C_TEXT_WHITE);b.setFont(new Font("Segoe UI",Font.BOLD,12));b.setFocusPainted(false);b.setBorderPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.setBorder(new EmptyBorder(9,16,9,16));b.setAlignmentX(Component.LEFT_ALIGNMENT);b.addMouseListener(new MouseAdapter(){Color o=bg;@Override public void mouseEntered(MouseEvent e){b.setBackground(bg.brighter());}@Override public void mouseExited(MouseEvent e){b.setBackground(o);}});return b;}
    Component vg(int h){return Box.createVerticalStrut(h);}
    void showAlert(String m,Color c){alertBanner.setText("  "+m+"  ");alertBanner.setBackground(c);alertBanner.setForeground(c.equals(C_GOLD)?Color.BLACK:Color.WHITE);alertBanner.setVisible(true);new Timer(true).schedule(new TimerTask(){@Override public void run(){SwingUtilities.invokeLater(()->alertBanner.setVisible(false));}},9000);}
    void showInfo(String m){JOptionPane.showMessageDialog(this,m,"Info",JOptionPane.INFORMATION_MESSAGE);}
    void showError(String m){JOptionPane.showMessageDialog(this,m,"Accès Refusé",JOptionPane.ERROR_MESSAGE);}
    void showWarn(String m){JOptionPane.showMessageDialog(this,m,"Avertissement",JOptionPane.WARNING_MESSAGE);}
    boolean canAccess(String r, String m) {
        Map<String, Integer> o = new HashMap<>();
        o.put("INFIRMIER", 1);
        o.put("TECHNICIEN", 2);
        o.put("MEDECIN", 3);
        o.put("ADMIN", 4);
        
        return o.getOrDefault(r, 0) >= o.getOrDefault(m, 0);
    }
    void log(String cat,String msg){String ts=LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));String ln="["+ts+"] ["+cat+"] "+msg;logsC.add(ln);if(consoleArea!=null)SwingUtilities.invokeLater(()->{consoleArea.append(ln+"\n");consoleArea.setCaretPosition(consoleArea.getDocument().getLength());});}
    void audit(String a,String r,String act,String d){String ts=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));auditL.add(String.format("[AUDIT|%s] ACTEUR=%s | ROLE=%s | ACTION=%s | DETAILS=%s",ts,a,r,act,d));}
    void updateSB(){if(statusBar!=null&&sessionUser!=null)statusBar.setText("  Connecté : "+sessionUser+" ["+sessionRole+"] — Dept: "+sessionDept+" — Token: "+sessionToken.substring(0,12)+"… — "+LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));}
    void deconnecter(){log("SESSION","Déconnexion : "+sessionUser+" ["+sessionRole+"]");audit(sessionUser,sessionRole,"DECONNEXION","Session terminée");sessionUser=null;sessionRole=null;sessionDept=null;sessionToken=null;if(refreshTimer!=null){refreshTimer.cancel();refreshTimer=null;}stopMD();mainCL.show(mainPanel,"LOGIN");}
    void startRefresh(){
    	refreshTimer=new Timer(true);
    	refreshTimer.scheduleAtFixedRate(new TimerTask(){@Override public void run(){SwingUtilities.invokeLater(()->{updateSB();if(cartePanel!=null)cartePanel.repaint();});}},5000,5000);}
    static String sha256(String in) throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] h=d.digest(in.getBytes("UTF-8"));StringBuilder sb=new StringBuilder();for(byte b:h)sb.append(String.format("%02x",b));return sb.toString();}

    public static void main(String[] args){
        try{UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());UIManager.put("Panel.background",new Color(8,14,28));UIManager.put("OptionPane.background",new Color(15,23,42));UIManager.put("OptionPane.messageForeground",new Color(236,242,255));}catch(Exception e){e.printStackTrace();}
        SwingUtilities.invokeLater(DashboardSecurite2::new);
    }
}