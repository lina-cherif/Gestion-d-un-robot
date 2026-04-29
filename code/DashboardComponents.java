package projetrobot;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

// ═══════════════════════════════════════════════════════════════════════
//  HEADER PANEL
// ═══════════════════════════════════════════════════════════════════════

class HeaderPanel extends JPanel {
    private final JLabel lblClock = new JLabel("00:00:00");
    private final JLabel lblAlert = new JLabel("Système nominal");
    private final JLabel dotAlert;
    private boolean alertBlink = false;

    HeaderPanel(Dashboard ui) {
        setBackground(new Color(13,22,38));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,Dashboard.BORDER),
            BorderFactory.createEmptyBorder(6,12,6,12)));
        setLayout(new BorderLayout(8,0));
        setPreferredSize(new Dimension(0,46));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));
        left.setOpaque(false);
        JLabel ico = new JLabel("⬡");
        ico.setFont(new Font("Courier New",Font.BOLD,20)); ico.setForeground(Dashboard.C_CYAN);
        JLabel title = new JLabel("Système Robot Hospitalier — Tableau de Bord | Urgences");
        title.setFont(Dashboard.FT_TITLE); title.setForeground(Dashboard.C_CYAN);
        left.add(ico); left.add(title);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,14,0));
        right.setOpaque(false);
        JLabel net = new JLabel("🤖 RT-ALPHA actif | WIFI-HOPITAL-5G");
        net.setFont(Dashboard.FT_SMALL); net.setForeground(Dashboard.C_GREEN);
        lblClock.setFont(new Font("Courier New",Font.BOLD,14)); lblClock.setForeground(new Color(74,138,181));
        dotAlert = new JLabel("• "); dotAlert.setFont(Dashboard.FT_SMALL); dotAlert.setForeground(Dashboard.C_GREEN);
        lblAlert.setFont(Dashboard.FT_SMALL); lblAlert.setForeground(Dashboard.TEXT_MAIN);
        right.add(net); right.add(lblClock); right.add(dotAlert); right.add(lblAlert);
        add(left,BorderLayout.WEST); add(right,BorderLayout.EAST);

        new javax.swing.Timer(500, e -> {
            if (alertBlink) {
                boolean on=(System.currentTimeMillis()/500)%2==0;
                dotAlert.setForeground(on?Dashboard.C_RED:new Color(80,0,0));
            }
        }).start();
    }

    void tickClock(){ lblClock.setText(LocalDateTime.now().format(Dashboard.HH_MM_SS)); }

    void update(NiveauESI esi){
        if(esi==null){resetAlert();return;}
        switch(esi){
            case ESI_1: alertBlink=true; dotAlert.setForeground(Dashboard.C_RED);
                        lblAlert.setText("🚨 CODE BLUE ACTIF"); lblAlert.setForeground(Dashboard.C_RED); break;
            case ESI_2: alertBlink=false; dotAlert.setForeground(Dashboard.C_AMBER);
                        lblAlert.setText("⚠ Urgence niveau 2"); lblAlert.setForeground(Dashboard.C_AMBER); break;
            default: resetAlert();
        }
    }
    private void resetAlert(){
        alertBlink=false; dotAlert.setForeground(Dashboard.C_GREEN);
        lblAlert.setText("Système nominal"); lblAlert.setForeground(Dashboard.TEXT_MAIN);
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  SALLE PANEL – carte temps réel
// ═══════════════════════════════════════════════════════════════════════

class SallePanel extends JPanel {

    private SalleState state = new SalleState();
    private int blinkPhase = 0;
    private final SimulationEngine engine;
    private String selectedPatientId = null;

    // Position normalisée de la porte soins (droite de la zone attente)
    static final float PORTE_SOINS_X = 0.78f;
    static final float PORTE_SOINS_Y = 0.50f;

    SallePanel(SimulationEngine engine) {
        this.engine = engine;
        setBackground(Dashboard.BG_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Dashboard.BORDER),
            BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                "  SALLE D'URGENCES — Vue temps réel  |  Cliquer un patient pour détails",
                TitledBorder.LEFT, TitledBorder.TOP, Dashboard.FT_TINY, Dashboard.TEXT_DIM)));

        new javax.swing.Timer(80, e -> { blinkPhase=(blinkPhase+1)%20; repaint(); }).start();

        addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){ handleClick(e.getX(),e.getY()); }
        });
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void handleClick(int mx, int my){
        int W=getWidth(),H=getHeight();
        int pad=28,gw=W-pad*2,gh=H-pad*2-16;
        int ox=pad,oy=pad+10;
        for(PatientMarker pm:state.patients){
            int px=ox+(int)(pm.x*gw), py=oy+(int)(pm.y*gh);
            if(Math.abs(mx-px)<18&&Math.abs(my-py)<18){
                selectedPatientId=pm.id; state.selectedPatientId=pm.id;
                repaint();
                engine.onPatientClicked(pm.id);
                return;
            }
        }
    }

    void setState(SalleState s){ this.state=s; repaint(); }
    void setSelectedPatient(String id){ selectedPatientId=id; state.selectedPatientId=id; repaint(); }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int W=getWidth(),H=getHeight();
        int pad=28,gw=W-pad*2,gh=H-pad*2-16;
        int ox=pad,oy=pad+10;

        drawFloor(g2,ox,oy,gw,gh);
        drawChemins(g2,ox,oy,gw,gh);
        drawChaises(g2,ox,oy,gw,gh);
        drawObstacles(g2,ox,oy,gw,gh);
        drawPatients(g2,ox,oy,gw,gh);
        drawRobot(g2,ox,oy,gw,gh);
        drawInfirmiers(g2,ox,oy,gw,gh);
        drawLegend(g2,W,H);
        g2.dispose();
    }

    // ── Sol + zones ───────────────────────────────────────────────

    private void drawFloor(Graphics2D g2, int ox, int oy, int gw, int gh){
        g2.setColor(Dashboard.BG_DEEP);
        g2.fillRoundRect(ox,oy,gw,gh,6,6);

        // 3 zones : ACCUEIL | ZONE ATTENTE | ADMIN (pas de zone SOINS)
        // La zone attente occupe 60% de la largeur
        int accW = gw/7;
        int attW = (int)(gw*0.62f);
        int admW = gw - accW - attW;

        // Accueil
        g2.setColor(new Color(20,35,55));
        g2.fillRect(ox, oy, accW, gh);
        // Attente
        g2.setColor(new Color(15,26,44));
        g2.fillRect(ox+accW, oy, attW, gh);
        // Admin
        g2.setColor(new Color(10,30,20));
        g2.fillRect(ox+accW+attW, oy, admW, gh);

        // Étiquettes zones
        g2.setFont(new Font("Courier New",Font.BOLD,11));
        FontMetrics fm=g2.getFontMetrics();
        String[] zones={"ACCUEIL","ZONE ATTENTE","ADMIN"};
        int[] xstarts={ox, ox+accW, ox+accW+attW};
        int[] widths={accW, attW, admW};
        Color[] zcols={new Color(80,140,200), new Color(60,110,170), new Color(60,150,80)};
        for(int i=0;i<3;i++){
            g2.setColor(zcols[i]);
            g2.drawString(zones[i], xstarts[i]+(widths[i]-fm.stringWidth(zones[i]))/2, oy+14);
        }

        // Séparateurs de zones
        g2.setColor(new Color(40,70,100,120));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(ox+accW, oy, ox+accW, oy+gh);
        g2.drawLine(ox+accW+attW, oy, ox+accW+attW, oy+gh);

        // Grille légère
        g2.setColor(new Color(30,58,95,40));
        g2.setStroke(new BasicStroke(0.5f));
        for(int x=0;x<=10;x++) g2.drawLine(ox+x*gw/10,oy,ox+x*gw/10,oy+gh);
        for(int y=0;y<=8;y++)  g2.drawLine(ox,oy+y*gh/8,ox+gw,oy+y*gh/8);

        // Bordure salle
        g2.setColor(Dashboard.C_CYAN);
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRoundRect(ox,oy,gw,gh,6,6);

        // ── ENTRÉE (bord gauche, zone ACCUEIL) ───────────────────
        g2.setColor(Dashboard.C_GREEN);
        g2.setStroke(new BasicStroke(4f));
        g2.drawLine(ox, oy+gh*2/5, ox, oy+gh*3/5);
        g2.setFont(new Font("Courier New",Font.BOLD,11));
        g2.setColor(Dashboard.C_GREEN);
        g2.drawString("🚪 ENTRÉE", ox+3, oy+gh*2/5-4);

        // ── PORTE SOINS (droite de la zone attente) ───────────────
        int porteSoinsX = ox + accW + attW;
        int porteSoinsY1 = oy + gh*2/5;
        int porteSoinsY2 = oy + gh*3/5;
        g2.setColor(Dashboard.C_RED);
        g2.setStroke(new BasicStroke(4f));
        g2.drawLine(porteSoinsX, porteSoinsY1, porteSoinsX, porteSoinsY2);
        // Arche de porte
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(239,68,68,180));
        g2.drawArc(porteSoinsX-15, porteSoinsY1, 30, (porteSoinsY2-porteSoinsY1), -90, 180);
        g2.setFont(new Font("Courier New",Font.BOLD,11));
        g2.setColor(Dashboard.C_RED);
        g2.drawString("🚪 PORTE SOINS", porteSoinsX+4, porteSoinsY1-4);

        // ── Zone de recharge (bas gauche) ───────────────────────
        int rzx=ox+(int)(SimulationEngine.RECHARGE_X*gw)-22;
        int rzy=oy+(int)(SimulationEngine.RECHARGE_Y*gh)-10;
        g2.setColor(new Color(59,143,212,25));
        g2.fillRoundRect(rzx-8,rzy-6,72,26,6,6);
        g2.setColor(Dashboard.C_BLUE);
        g2.setStroke(new BasicStroke(1.2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1f,new float[]{4,3},0));
        g2.drawRoundRect(rzx-8,rzy-6,72,26,6,6);
        g2.setFont(new Font("Courier New",Font.PLAIN,10));
        g2.setColor(Dashboard.C_BLUE);
        g2.drawString("⚡ RECHARGE",rzx,rzy+10);

        // ── Zone Admin : bureau + robot sécurité ─────────────────
        int adminCX = ox+accW+attW+admW/2;
        int adminCY = oy+gh/2;
        g2.setColor(new Color(20,45,30));
        g2.fillRoundRect(adminCX-20,adminCY-32,40,22,4,4);
        g2.setColor(new Color(50,120,70));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(adminCX-20,adminCY-32,40,22,4,4);
        g2.setFont(new Font("Courier New",Font.PLAIN,9));
        g2.setColor(new Color(50,180,80,180));
        g2.drawString("BUREAU",adminCX-14,adminCY-19);

        // Robot sécurité fixe (bouclier)
        drawSecurityRobot(g2,adminCX,adminCY+20);
    }

    private void drawSecurityRobot(Graphics2D g2, int cx, int cy){
        // Halo
        g2.setColor(new Color(34,100,50,50));
        g2.fillOval(cx-16,cy-16,32,32);
        // Corps (bouclier)
        int[] sx={cx-8,cx+8,cx+8,cx,cx-8};
        int[] sy={cy-10,cy-10,cy+4,cy+12,cy+4};
        g2.setColor(Dashboard.C_GREEN);
        g2.fillPolygon(sx,sy,5);
        g2.setColor(Dashboard.C_GREEN.darker());
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawPolygon(sx,sy,5);
        // Croix sur le bouclier
        g2.setColor(new Color(0,200,80,180));
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(cx,cy-6,cx,cy+4); g2.drawLine(cx-4,cy-1,cx+4,cy-1);
        // Antenne
        g2.setColor(Dashboard.C_GREEN);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(cx,cy-10,cx,cy-18); g2.fillOval(cx-2,cy-20,4,4);
        // Scan tournant
        double angle=(System.currentTimeMillis()/900.0)%(2*Math.PI);
        int scanR=20;
        int scanX=(int)(cx+scanR*Math.cos(angle)), scanY=(int)(cy+scanR*Math.sin(angle));
        g2.setColor(new Color(34,197,94,50));
        g2.setStroke(new BasicStroke(0.8f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1f,new float[]{3,3},0));
        g2.drawOval(cx-scanR,cy-scanR,scanR*2,scanR*2);
        g2.setColor(new Color(34,197,94,200));
        g2.fillOval(scanX-2,scanY-2,4,4);
        // Label
        g2.setFont(new Font("Courier New",Font.BOLD,8));
        g2.setColor(Dashboard.C_GREEN);
        g2.drawString("🛡 SÉCURITÉ",cx-16,cy+28);
    }

    // ── Chemins ──────────────────────────────────────────────────

    private void drawChemins(Graphics2D g2, int ox, int oy, int gw, int gh){
        if(state.chemin1!=null&&state.chemin1.size()>=2){
            g2.setColor(new Color(126,207,255,70));
            g2.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1f,new float[]{5f,4f},0f));
            for(int i=1;i<state.chemin1.size();i++){
                float[] a=state.chemin1.get(i-1),b=state.chemin1.get(i);
                g2.drawLine(ox+(int)(a[0]*gw),oy+(int)(a[1]*gh),ox+(int)(b[0]*gw),oy+(int)(b[1]*gh));
            }
        }
    }

    // ── Chaises ──────────────────────────────────────────────────

    private void drawChaises(Graphics2D g2, int ox, int oy, int gw, int gh){
        if(state.chaises==null)return;
        for(ChairMarker ch:state.chaises){
            int px=ox+(int)(ch.x*gw), py=oy+(int)(ch.y*gh);
            drawChaise(g2,px,py,ch);
        }
    }

    private void drawChaise(Graphics2D g2, int px, int py, ChairMarker ch){
        // Siège
        g2.setColor(new Color(35,62,95));
        g2.fillRoundRect(px-8,py-5,16,11,3,3);
        // Dossier
        g2.fillRoundRect(px-8,py-10,16,5,2,2);
        // Contour
        g2.setColor(new Color(60,100,150));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(px-8,py-5,16,11,3,3);
        g2.drawRoundRect(px-8,py-10,16,5,2,2);
        // Pieds
        g2.setColor(new Color(40,70,100));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawLine(px-7,py+6,px-7,py+10);
        g2.drawLine(px+7,py+6,px+7,py+10);

        if(ch.occupee && ch.occupantId!=null){
            boolean isPatient = ch.occupantEstPatient;
            Color figColor = isPatient
                ? (ch.occupantId.equals(state.selectedPatientId) ? Color.WHITE : new Color(200,230,255,200))
                : new Color(180,180,255,160);
            // Tête
            g2.setColor(figColor);
            g2.fillOval(px-5,py-22,10,10);
            // Corps assis
            g2.fillRoundRect(px-5,py-12,10,10,3,3);
            if(isPatient){
                // Badge "PAT" en petit
                g2.setFont(new Font("Courier New",Font.BOLD,7));
                g2.setColor(Dashboard.C_CYAN);
                g2.drawString("P",px-3,py-12);
            } else {
                // Accompagnant : icône différente
                g2.setFont(new Font("Courier New",Font.BOLD,7));
                g2.setColor(new Color(180,180,255,200));
                g2.drawString("A",px-3,py-12);
            }
        } else {
            // Chaise vide
            g2.setFont(new Font("Courier New",Font.PLAIN,7));
            g2.setColor(new Color(60,100,140,130));
            g2.drawString("_",px-2,py-2);
        }
    }

    // ── Obstacles ─────────────────────────────────────────────────

    private void drawObstacles(Graphics2D g2, int ox, int oy, int gw, int gh){
        if(state.obstacles==null)return;
        for(ObstacleItem ob:state.obstacles){
            int px=ox+(int)(ob.x*gw)-8, py=oy+(int)(ob.y*gh)+6;
            if(ob.mobile){
                // Urgentiste en mouvement : silhouette courante améliorée
                g2.setColor(new Color(34,197,94,30));
                g2.fillOval(px-5,py-22,26,26);
                g2.setColor(Dashboard.C_GREEN);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(px+1,py-18,8,8);   // tête
                g2.drawLine(px+5,py-10,px+5,py); // corps
                g2.drawLine(px-1,py-7,px+11,py-7); // bras
                int legOff=(blinkPhase%8<4)?3:-3;
                g2.drawLine(px+5,py,px+2+legOff,py+9);
                g2.drawLine(px+5,py,px+8-legOff,py+9);
                g2.setFont(new Font("Courier New",Font.BOLD,8));
                g2.setColor(Dashboard.C_GREEN);
                g2.drawString("🏃 URGENTISTE",px-12,py+20);
            } else {
                // Chariot statique : symbole croix médicale
                g2.setColor(new Color(245,158,11,35));
                g2.fillRoundRect(px-2,py-14,20,18,4,4);
                g2.setColor(Dashboard.C_AMBER);
                g2.setStroke(new BasicStroke(2f));
                // Croix
                g2.drawLine(px+8,py-12,px+8,py+2);
                g2.drawLine(px+2,py-5,px+14,py-5);
                // Roues
                g2.setStroke(new BasicStroke(1.5f));
                g2.fillOval(px,py+2,5,5); g2.fillOval(px+11,py+2,5,5);
                g2.setFont(new Font("Courier New",Font.BOLD,8));
                g2.setColor(Dashboard.C_AMBER);
                g2.drawString("🛒 CHARIOT",px-10,py+16);
            }
        }
    }

    // ── Patients ──────────────────────────────────────────────────

    private void drawPatients(Graphics2D g2, int ox, int oy, int gw, int gh){
        if(state.patients==null)return;
        for(PatientMarker pm:state.patients){
            int px=ox+(int)(pm.x*gw), py=oy+(int)(pm.y*gh);
            Color col=pm.esi==null?Dashboard.TEXT_DIM:Dashboard.esiColor(pm.esi);

            // Halo ESI-1 pulsant
            if(pm.esi==NiveauESI.ESI_1){
                float pulse=0.5f+0.5f*(float)Math.sin(blinkPhase*Math.PI/10);
                int hr=(int)(16+8*pulse);
                g2.setColor(new Color(239,68,68,(int)(70*pulse)));
                g2.fillOval(px-hr,py-hr,hr*2,hr*2);
            }
            // Halo sélection
            if(pm.id.equals(selectedPatientId)){
                g2.setColor(new Color(255,255,255,45));
                g2.fillOval(px-20,py-20,40,40);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1f,new float[]{3,2},0));
                g2.drawOval(px-18,py-18,36,36);
            }

            drawPatientSymbol(g2,px,py,pm,col);

            // Accompagnant
            if(pm.accompagnant>=1){
                int ax=px+16,ay=py;
                g2.setColor(new Color(180,180,255,160));
                g2.fillOval(ax-4,ay-14,8,8);
                g2.fillRoundRect(ax-5,ay-6,10,12,3,3);
                g2.setColor(new Color(180,180,255,80));
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(px+2,py,ax-2,ay);
                g2.setFont(new Font("Courier New",Font.BOLD,8));
                g2.setColor(new Color(180,180,255,200));
                g2.drawString("acc",ax+2,ay+4);
            }

            // ID + badge ESI
            g2.setFont(Dashboard.FT_TINY);
            g2.setColor(col);
            String lbl=pm.id.replace("PAT-","P");
            FontMetrics fm=g2.getFontMetrics();
            g2.drawString(lbl,px-fm.stringWidth(lbl)/2,py+24);

            if(pm.esi!=null){
                String esiTxt=pm.esi.name().replace("ESI_","E");
                g2.setFont(new Font("Courier New",Font.BOLD,9));
                g2.setColor(Dashboard.esiBg(pm.esi));
                g2.fillRoundRect(px-10,py-36,20,12,4,4);
                g2.setColor(col);
                g2.drawString(esiTxt,px-8,py-26);
            }
        }
    }

    /**
     * Symboles ESI très distincts :
     *  ESI 1 → ✚ croix rouge (critique)
     *  ESI 2 → ▲ triangle orange (urgent)
     *  ESI 3 → ◆ losange jaune (semi-urgent)
     *  ESI 4 → ● rond teal (peu urgent)
     *  ESI 5 → ○ cercle bleu (non urgent)
     *  inconscient → corps allongé
     */
    private void drawPatientSymbol(Graphics2D g2, int px, int py, PatientMarker pm, Color col){
        boolean inconscient = pm.accompagnant==2;
        g2.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));

        if(inconscient){
            // Corps allongé
            Color fc=new Color(col.getRed(),col.getGreen(),col.getBlue(),90);
            g2.setColor(fc);
            g2.fillRoundRect(px-15,py-4,30,9,5,5);
            g2.setColor(col);
            g2.drawRoundRect(px-15,py-4,30,9,5,5);
            g2.fillOval(px-7,py-14,14,12); // tête
            return;
        }

        int r=pm.enCours?12:9;
        NiveauESI esi=pm.esi;

        if(esi==NiveauESI.ESI_1){
            // Grande croix rouge
            g2.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),55));
            g2.fillOval(px-r-2,py-r-2,(r+2)*2,(r+2)*2);
            g2.setColor(col);
            g2.setStroke(new BasicStroke(3f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g2.drawLine(px,py-r,px,py+r);
            g2.drawLine(px-r,py,px+r,py);
        } else if(esi==NiveauESI.ESI_2){
            // Triangle orange vers le haut
            int[] xs={px,px-r,px+r};
            int[] ys={py-r,py+r/2,py+r/2};
            g2.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),55));
            g2.fillPolygon(xs,ys,3);
            g2.setColor(col);
            g2.drawPolygon(xs,ys,3);
            // Point d'exclamation
            g2.setFont(new Font("Courier New",Font.BOLD,9));
            g2.drawString("!",px-2,py+3);
        } else if(esi==NiveauESI.ESI_3){
            // Losange jaune
            int[] xs={px,px+r,px,px-r};
            int[] ys={py-r,py,py+r,py};
            g2.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),50));
            g2.fillPolygon(xs,ys,4);
            g2.setColor(col);
            g2.drawPolygon(xs,ys,4);
        } else if(esi==NiveauESI.ESI_4){
            // Cercle plein teal
            g2.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),80));
            g2.fillOval(px-r,py-r,r*2,r*2);
            g2.setColor(col);
            g2.drawOval(px-r,py-r,r*2,r*2);
        } else {
            // ESI_5 ou non classé : petit cercle vide
            g2.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),40));
            g2.fillOval(px-r,py-r,r*2,r*2);
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1f,new float[]{3,2},0));
            g2.drawOval(px-r,py-r,r*2,r*2);
        }
        // Tête par-dessus le symbole
        if(!inconscient){
            Color headCol=pm.traite?new Color(col.getRed(),col.getGreen(),col.getBlue(),100):col;
            g2.setColor(headCol);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(px-4,py-r-12,9,9);
        }
    }

    // ── Robot ─────────────────────────────────────────────────────

    private void drawRobot(Graphics2D g2, int ox, int oy, int gw, int gh){
        if(!state.robot1EnMarche && state.robotStatus1.equals("ARRÊTÉ")) {
            // Robot éteint : affiché grisé
        }
        float rx=state.robotX1, ry=state.robotY1;
        int rpx=ox+(int)(rx*gw), rpy=oy+(int)(ry*gh);
        Color robCol = state.codeBlue ? Dashboard.C_RED : Dashboard.C_CYAN;

        // Halo animé
        float pulse=0.5f+0.5f*(float)Math.sin(blinkPhase*Math.PI/10);
        int hr=(int)(22+6*pulse);
        g2.setColor(new Color(robCol.getRed(),robCol.getGreen(),robCol.getBlue(),(int)(18*pulse)));
        g2.fillOval(rpx-hr,rpy-hr,hr*2,hr*2);

        int rs=13;
        // Ombre
        g2.setColor(new Color(0,0,0,70));
        g2.fillRoundRect(rpx-rs+2,rpy-rs+2,rs*2,rs*2,6,6);
        // Corps
        Color bodyCol=state.robot1EnMarche?robCol:new Color(robCol.getRed(),robCol.getGreen(),robCol.getBlue(),110);
        g2.setColor(bodyCol);
        g2.fillRoundRect(rpx-rs,rpy-rs,rs*2,rs*2,6,6);
        // Croix médicale
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g2.drawLine(rpx,rpy-6,rpx,rpy+6);
        g2.drawLine(rpx-6,rpy,rpx+6,rpy);
        // Bordure
        g2.setColor(bodyCol.brighter());
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRoundRect(rpx-rs,rpy-rs,rs*2,rs*2,6,6);
        // Indicateur marche
        if(state.robot1EnMarche){
            boolean on=(blinkPhase%4)<2;
            g2.setColor(on?Dashboard.C_GREEN:new Color(0,60,30));
            g2.fillOval(rpx+rs-3,rpy-rs-1,6,6);
        } else {
            g2.setColor(Dashboard.C_AMBER);
            g2.setFont(new Font("Courier New",Font.BOLD,8));
            g2.drawString("■",rpx-rs,rpy+rs+20);
        }
        // Label
        g2.setFont(Dashboard.FT_TINY);
        g2.setColor(bodyCol);
        g2.drawString("RT-ALPHA",rpx-rs-2,rpy+rs+12);
        // Status
        if(state.robotStatus1!=null&&!state.robotStatus1.isEmpty()){
            g2.setFont(new Font("Courier New",Font.PLAIN,10));
            g2.setColor(new Color(robCol.getRed(),robCol.getGreen(),robCol.getBlue(),200));
            g2.drawString(state.robotStatus1,rpx-rs,rpy-rs-5);
        }

        // ── FLÈCHE DE DIRECTION ───────────────────────────────────
        // Direction = vers le premier point du chemin ou vers le patient
        if(state.chemin1!=null&&state.chemin1.size()>=2){
            float[] next=state.chemin1.get(1);
            int nx=ox+(int)(next[0]*gw), ny=oy+(int)(next[1]*gh);
            double angle=Math.atan2(ny-rpy,nx-rpx);
            drawArrow(g2,rpx,rpy,angle,robCol,18);
        } else if(state.robotPatientId!=null){
            // Chercher le patient
            for(PatientMarker pm:state.patients){
                if(pm.id.equals(state.robotPatientId)){
                    int tx=ox+(int)(pm.x*gw), ty=oy+(int)(pm.y*gh);
                    double angle=Math.atan2(ty-rpy,tx-rpx);
                    drawArrow(g2,rpx,rpy,angle,robCol,18);
                    break;
                }
            }
        }
    }

    private void drawArrow(Graphics2D g2, int cx, int cy, double angle, Color col, int len){
        int ex=(int)(cx+Math.cos(angle)*(len+14));
        int ey=(int)(cy+Math.sin(angle)*(len+14));
        g2.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),200));
        g2.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g2.drawLine(cx+(int)(Math.cos(angle)*15),cy+(int)(Math.sin(angle)*15),ex,ey);
        // Pointe
        double a1=angle+Math.toRadians(145), a2=angle-Math.toRadians(145);
        int[] xs={ex,(int)(ex+Math.cos(a1)*9),(int)(ex+Math.cos(a2)*9)};
        int[] ys={ey,(int)(ey+Math.sin(a1)*9),(int)(ey+Math.sin(a2)*9)};
        g2.fillPolygon(xs,ys,3);
    }

    // ── Infirmiers en mouvement ───────────────────────────────────

    private void drawInfirmiers(Graphics2D g2, int ox, int oy, int gw, int gh){
        if(state.infirmiers==null)return;
        for(InfirmierMarker inf:state.infirmiers){
            if(!inf.actif)continue;
            int ix=ox+(int)(inf.x*gw), iy=oy+(int)(inf.y*gh);
            drawInfirmier(g2,ix,iy,inf);
        }
    }

    private void drawInfirmier(Graphics2D g2, int ix, int iy, InfirmierMarker inf){
        inf.animPhase=(inf.animPhase+1)%6;
        g2.setColor(new Color(251,146,60,35));
        g2.fillOval(ix-10,iy-20,20,20);
        // Tête blanche (blouse)
        g2.setColor(new Color(255,255,255,200));
        g2.fillOval(ix-5,iy-24,10,10);
        // Corps bleu infirmier
        g2.setColor(new Color(59,196,198,200));
        g2.fillRoundRect(ix-5,iy-14,10,14,3,3);
        // Jambes animées
        int legA=(inf.animPhase<3)?6:-6;
        g2.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(200,220,255,200));
        g2.drawLine(ix-2,iy,ix-4+legA,iy+10);
        g2.drawLine(ix+2,iy,ix+4-legA,iy+10);
        // Croix rouge
        g2.setColor(Dashboard.C_RED);
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawLine(ix+7,iy-19,ix+7,iy-13);
        g2.drawLine(ix+4,iy-16,ix+10,iy-16);
        g2.setFont(new Font("Courier New",Font.BOLD,8));
        g2.setColor(Dashboard.C_ORANGE);
        String dest=inf.target!=null?(inf.target.startsWith("PAT")?"→ "+inf.target:"→ SOINS"):"";
        g2.drawString("🏃 INF. "+dest,ix-14,iy+18);
    }

    // ── Légende ───────────────────────────────────────────────────

    private void drawLegend(Graphics2D g2, int W, int H){
        String[] labels={"✚ ESI 1 – Code Blue","▲ ESI 2 – Urgent","◆ ESI 3","● ESI 4","○ ESI 5",
                         "🤖 Robot RT-ALPHA","🏃 Infirmier","🛒 Chariot","🪑 Chaise occ.",
                         "P=Patient  A=Accompagnant"};
        Color[] cols={Dashboard.C_RED,Dashboard.C_AMBER,Dashboard.C_YELLOW,Dashboard.C_TEAL,
                      new Color(91,200,245),Dashboard.C_CYAN,Dashboard.C_ORANGE,
                      Dashboard.C_AMBER,Dashboard.TEXT_DIM,new Color(150,180,220)};
        g2.setFont(new Font("Courier New",Font.PLAIN,10));
        int lx=34,ly=H-8;
        for(int i=0;i<labels.length;i++){
            g2.setColor(cols[i]);
            g2.drawString(labels[i],lx,ly);
            lx+=g2.getFontMetrics().stringWidth(labels[i])+10;
            if(lx>W-120){lx=34;ly-=13;}
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  CAMERA PANEL – synchronisée avec la consultation
// ═══════════════════════════════════════════════════════════════════════

class CameraPanel extends JPanel {
    private CameraState camState = new CameraState();
    private int tick = 0;
    private float scanLineY = 0f;

    CameraPanel(){
        setBackground(Dashboard.BG_DEEP);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Dashboard.BORDER),
            BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                "  CAMÉRA RGB — Vue Robot RT-ALPHA  |  Consultation en temps réel",
                TitledBorder.LEFT,TitledBorder.TOP,Dashboard.FT_TINY,Dashboard.TEXT_DIM)));
        new javax.swing.Timer(55, e -> { tick++; scanLineY=(scanLineY+0.010f)%1.0f; repaint(); }).start();
    }

    void setState(CameraState s){ this.camState=s; }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        int W=getWidth(),H=getHeight();
        int cy=20,ch=H-cy-42,cx=8,cw=W-16;

        g2.setColor(new Color(3,8,16));
        g2.fillRoundRect(cx,cy,cw,ch,6,6);
        drawCameraContent(g2,cx,cy,cw,ch);

        // Ligne de scan
        GradientPaint sg=new GradientPaint(cx,0,new Color(59,143,212,0),cx+cw/2,0,new Color(126,207,255,90),true);
        g2.setPaint(sg);
        int sly=cy+(int)(scanLineY*ch);
        g2.fillRect(cx,sly,cw,1);

        drawCameraCorners(g2,cx,cy,cw,ch);

        // Info bas de caméra
        g2.setFont(Dashboard.FT_TINY);
        g2.setColor(new Color(74,138,181,200));
        g2.drawString(getPhaseLabel(),cx+6,cy+ch+14);

        // Si patient en cours : afficher signes vitaux en overlay
        if(camState.signes!=null && (camState.phase==CameraState.Phase.MEASURING||camState.phase==CameraState.Phase.ESI_RESULT)){
            drawVitauxOverlay(g2,cx+cw-120,cy+ch-60);
        }

        g2.dispose();
    }

    private void drawVitauxOverlay(Graphics2D g2, int x, int y){
        SignesVitaux sv=camState.signes;
        if(sv==null)return;
        g2.setColor(new Color(5,15,30,190));
        g2.fillRoundRect(x-6,y-14,126,72,6,6);
        g2.setColor(new Color(59,143,212,120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x-6,y-14,126,72,6,6);
        g2.setFont(new Font("Courier New",Font.BOLD,10));
        Color pc=sv.getPouls()>120||sv.getPouls()<50?Dashboard.C_RED:Dashboard.C_TEAL;
        Color sc=sv.getSpo2()<94?Dashboard.C_RED:Dashboard.C_TEAL;
        Color tc=sv.getTemperature()>39||sv.getTemperature()<35?Dashboard.C_RED:Dashboard.C_TEAL;
        Color tec=sv.getTensionSystole()>140||sv.getTensionSystole()<90?Dashboard.C_RED:Dashboard.C_TEAL;
        g2.setColor(pc);  g2.drawString("♥ "+sv.getPouls()+" bpm",x,y);
        g2.setColor(sc);  g2.drawString("○ SpO2 "+sv.getSpo2()+"%",x,y+14);
        g2.setColor(tc);  g2.drawString("⊛ "+String.format("%.1f",sv.getTemperature())+"°C",x,y+28);
        g2.setColor(tec); g2.drawString("⊞ "+sv.getTensionSystole()+"/"+sv.getTensionDiastole(),x,y+42);
        if(camState.esiResult!=null){
            Color ec=Dashboard.esiColor(camState.esiResult);
            g2.setColor(ec);
            g2.drawString("ESI "+camState.esiResult.name().replace("ESI_",""),x,y+56);
        }
    }

    private void drawCameraContent(Graphics2D g2,int cx,int cy,int cw,int ch){
        int mx=cx+cw/2,my=cy+ch/2;
        switch(camState.phase){
            case WAITING:         drawWaiting(g2,mx,my);break;
            case MOVING_CLEAR:    drawMoveArrow(g2,mx,my,Dashboard.C_CYAN);break;
            case MOVING_OBSTACLE: drawMoveArrow(g2,mx,my,Dashboard.C_CYAN);drawObstacleWarning(g2,mx,my);break;
            case THUMB_SCAN:      drawThumbScan(g2,mx,my);break;
            case BODY_SCAN:       drawBodyCamera(g2,mx,my,cw,ch);break;
            case MEASURING:       drawMeasuring(g2,mx,my);break;
            case ESI_RESULT:      drawESIResult(g2,mx,my,cw,ch);break;
            case RECHARGING:      drawRecharging(g2,mx,my);break;
            case STOPPED:         drawStopped(g2,mx,my);break;
        }
    }

    private void drawWaiting(Graphics2D g2,int mx,int my){
        g2.setColor(new Color(59,143,212,45));
        g2.fillOval(mx-24,my-24,48,48);
        g2.setColor(Dashboard.TEXT_DIM);
        g2.setFont(Dashboard.FT_SMALL);
        g2.drawString("EN ATTENTE",mx-30,my+4);
        // Radar animé
        double angle=(tick*0.04)%(2*Math.PI);
        g2.setColor(new Color(59,143,212,80));
        g2.setStroke(new BasicStroke(1f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1f,new float[]{4,4},0));
        g2.drawOval(mx-40,my-40,80,80);
        g2.setColor(new Color(59,143,212,180));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(mx,my,(int)(mx+40*Math.cos(angle)),(int)(my+40*Math.sin(angle)));
    }

    private void drawMoveArrow(Graphics2D g2,int mx,int my,Color c){
        int offset=(tick/5)%12;
        g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),110+offset*8));
        g2.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        for(int i=0;i<3;i++){int yOff=my-22+i*18-offset;g2.drawLine(mx-14,yOff+10,mx,yOff);g2.drawLine(mx,yOff,mx+14,yOff+10);}
        if(!camState.patientId.isEmpty()){g2.setFont(Dashboard.FT_SMALL);g2.setColor(c);g2.drawString("→ "+camState.patientId,mx-28,my+38);}
    }

    private void drawObstacleWarning(Graphics2D g2,int mx,int my){
        boolean blink=(tick/6)%2==0;
        g2.setColor(blink?Dashboard.C_AMBER:new Color(100,60,0));
        int[] xs={mx,mx-24,mx+24};int[]ys={my-24,my+14,my+14};
        g2.fillPolygon(xs,ys,3);
        g2.setFont(new Font("Courier New",Font.BOLD,16));
        g2.setColor(Dashboard.BG_DEEP);g2.drawString("!",mx-5,my+10);
        g2.setFont(Dashboard.FT_SMALL);g2.setColor(Dashboard.C_AMBER);
        g2.drawString("⚠ "+(camState.obstacleType.isEmpty()?"OBSTACLE":camState.obstacleType.toUpperCase()),mx-32,my+34);
    }

    private void drawThumbScan(Graphics2D g2,int mx,int my){
        g2.setColor(new Color(59,143,212,65));
        g2.fillRoundRect(mx-11,my-24,22,35,11,11);
        g2.setColor(Dashboard.C_BLUE);g2.setStroke(new BasicStroke(1.8f));
        g2.drawRoundRect(mx-11,my-24,22,35,11,11);
        g2.setColor(new Color(126,207,255,80));g2.setStroke(new BasicStroke(0.8f));
        for(int i=0;i<7;i++)g2.drawLine(mx-8,my-18+i*4,mx+8,my-18+i*4);
        int scanY=my-24+(tick*3%35);
        g2.setColor(Dashboard.C_CYAN);g2.setStroke(new BasicStroke(2f));
        g2.drawLine(mx-13,scanY,mx+13,scanY);
        g2.setFont(Dashboard.FT_SMALL);g2.setColor(Dashboard.C_CYAN);
        g2.drawString("EMPREINTE...",mx-36,my+24);
        if(!camState.patientId.isEmpty()){g2.setColor(Dashboard.C_GREEN);g2.drawString("ID: "+camState.patientId,mx-30,my+38);}
    }

    private void drawBodyCamera(Graphics2D g2,int mx,int my,int cw,int ch){
        // Corps silhouette avec données du scénario réel
        Color bodyCol=Dashboard.C_BLUE;
        if(camState.scenario!=null && camState.scenario.vision!=null){
            ResultatVision v=camState.scenario.vision;
            if(v.isSaignementMassif()||v.isAmputationDetectee()) bodyCol=Dashboard.C_RED;
            else if(v.isInconscient()) bodyCol=Dashboard.C_AMBER;
        }
        g2.setColor(new Color(bodyCol.getRed(),bodyCol.getGreen(),bodyCol.getBlue(),80));
        g2.fillOval(mx-11,my-38,22,22);
        g2.fillRect(mx-11,my-16,22,30);
        g2.fillRect(mx-22,my-14,11,22);
        g2.fillRect(mx+11,my-14,11,22);
        g2.fillRect(mx-11,my+14,10,22);
        g2.fillRect(mx+1,my+14,10,22);
        g2.setColor(bodyCol);g2.setStroke(new BasicStroke(1.2f));
        g2.drawOval(mx-11,my-38,22,22);
        g2.drawRect(mx-11,my-16,22,30);

        // Annotations vision réelles
        ResultatVision v=camState.vision!=null?camState.vision:(camState.scenario!=null?camState.scenario.vision:null);
        if(v!=null){
            if(v.isSaignementMassif()){g2.setColor(new Color(239,68,68,110));g2.fillOval(mx-16,my-6,14,9);g2.setColor(Dashboard.C_RED);g2.setFont(Dashboard.FT_TINY);g2.drawString("HSV⚠",mx+14,my);}
            if(v.isAmputationDetectee()){g2.setColor(Dashboard.C_RED);g2.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1f,new float[]{4,3},0));g2.drawLine(mx-11,my+24,mx-17,my+40);g2.setFont(Dashboard.FT_TINY);g2.drawString("AMPUT.",mx-48,my+32);}
            if(v.isInconscient()){g2.setColor(Dashboard.C_RED);g2.setFont(Dashboard.FT_TINY);g2.drawString("EAR<0.2",mx-34,my-42);}
            if(v.isAgite()){g2.setColor(Dashboard.C_AMBER);g2.setFont(Dashboard.FT_TINY);g2.drawString("AGITATION",mx+24,my-22);}
        }
        // Anneau scan
        float scanA=(tick*0.05f)%(float)(2*Math.PI);
        g2.setColor(new Color(59,143,212,80));g2.setStroke(new BasicStroke(1.8f));
        g2.drawOval(mx-36,my-22,72,64);
        int scanPx=(int)(mx+36*Math.cos(scanA)),scanPy=(int)((my-22+32)+32*Math.sin(scanA));
        g2.setColor(Dashboard.C_CYAN);g2.fillOval(scanPx-4,scanPy-4,8,8);

        if(!camState.patientId.isEmpty()){g2.setFont(Dashboard.FT_SMALL);g2.setColor(Dashboard.C_CYAN);g2.drawString(camState.patientId,mx-26,my-48);}
    }

    private void drawMeasuring(Graphics2D g2,int mx,int my){
        // Utilise les vraies données du scénario
        SignesVitaux sv=camState.signes!=null?camState.signes:(camState.scenario!=null?camState.scenario.signes:null);
        String[] labels={"♥ POULS","○ SpO2","⊛ TEMP","⊞ TENSION"};
        String[] vals=sv!=null?new String[]{
            sv.getPouls()+" bpm",
            sv.getSpo2()+"%",
            String.format("%.1f",sv.getTemperature())+"°C",
            sv.getTensionSystole()+"/"+sv.getTensionDiastole()
        }:new String[]{"---","---","---","---"};
        int active=(tick/12)%labels.length;
        for(int i=0;i<labels.length;i++){
            boolean isA=i==active;
            g2.setColor(isA?Dashboard.C_CYAN:Dashboard.TEXT_DIM);
            g2.setFont(isA?new Font("Courier New",Font.BOLD,12):new Font("Courier New",Font.PLAIN,10));
            int bx=mx-80+i*52;
            g2.drawString(labels[i],bx-8,my-10);
            g2.setFont(isA?new Font("Courier New",Font.BOLD,11):new Font("Courier New",Font.PLAIN,9));
            g2.setColor(isA?Dashboard.C_GREEN:Dashboard.TEXT_DIM);
            g2.drawString(vals[i],bx-8,my+6);
            if(isA){int prog=(tick*4)%100;g2.setColor(new Color(126,207,255,60));g2.fillRect(bx-8,my+12,44,5);g2.setColor(Dashboard.C_CYAN);g2.fillRect(bx-8,my+12,prog*44/100,5);}
        }
    }

    private void drawESIResult(Graphics2D g2,int mx,int my,int cw,int ch){
        NiveauESI esi=camState.esiResult;
        if(esi==null)return;
        Color c=Dashboard.esiColor(esi);
        int num=Integer.parseInt(esi.name().replace("ESI_",""));
        boolean pulse=esi==NiveauESI.ESI_1&&(tick/6)%2==0;
        int r=pulse?36:30;
        g2.setColor(Dashboard.esiBg(esi));g2.fillOval(mx-r,my-r,r*2,r*2);
        g2.setColor(c);g2.setStroke(new BasicStroke(3f));g2.drawOval(mx-r,my-r,r*2,r*2);
        g2.setFont(new Font("Courier New",Font.BOLD,32));g2.setColor(c);g2.drawString(String.valueOf(num),mx-10,my+12);
        g2.setFont(Dashboard.FT_SMALL);g2.setColor(c);
        String lbl=esi==NiveauESI.ESI_1?"CODE BLUE":esi==NiveauESI.ESI_2?"URGENCE":esi==NiveauESI.ESI_3?"SURVEILLANCE":"STABLE";
        g2.drawString(lbl,mx-g2.getFontMetrics().stringWidth(lbl)/2,my+r+18);
        // Nom patient
        if(!camState.patientId.isEmpty()){g2.setFont(Dashboard.FT_TINY);g2.setColor(Dashboard.C_CYAN);g2.drawString(camState.patientId,mx-20,my-r-6);}
    }

    private void drawRecharging(Graphics2D g2,int mx,int my){
        g2.setColor(new Color(59,143,212,80));g2.fillOval(mx-26,my-26,52,52);
        g2.setFont(new Font("Courier New",Font.BOLD,22));g2.setColor(Dashboard.C_CYAN);g2.drawString("⚡",mx-10,my+8);
        g2.setFont(Dashboard.FT_SMALL);g2.setColor(Dashboard.C_BLUE);g2.drawString("RECHARGE...",mx-30,my+28);
        int prog=(tick*2)%100;
        g2.setColor(Dashboard.BORDER);g2.fillRect(mx-32,my+36,64,6);
        g2.setColor(Dashboard.C_CYAN);g2.fillRect(mx-32,my+36,prog*64/100,6);
    }

    private void drawStopped(Graphics2D g2,int mx,int my){
        g2.setColor(new Color(100,80,0,80));g2.fillOval(mx-24,my-24,48,48);
        g2.setFont(new Font("Courier New",Font.BOLD,18));g2.setColor(Dashboard.C_AMBER);g2.drawString("■",mx-8,my+7);
        g2.setFont(Dashboard.FT_SMALL);g2.setColor(Dashboard.C_AMBER);g2.drawString("ARRÊTÉ",mx-22,my+26);
    }

    private void drawCameraCorners(Graphics2D g2,int cx,int cy,int cw,int ch){
        g2.setColor(new Color(59,143,212,190));g2.setStroke(new BasicStroke(2f));int cs=12;
        g2.drawLine(cx+1,cy+1,cx+1+cs,cy+1);g2.drawLine(cx+1,cy+1,cx+1,cy+1+cs);
        g2.drawLine(cx+cw-1,cy+1,cx+cw-1-cs,cy+1);g2.drawLine(cx+cw-1,cy+1,cx+cw-1,cy+1+cs);
        g2.drawLine(cx+1,cy+ch-1,cx+1+cs,cy+ch-1);g2.drawLine(cx+1,cy+ch-1,cx+1,cy+ch-1-cs);
        g2.drawLine(cx+cw-1,cy+ch-1,cx+cw-1-cs,cy+ch-1);g2.drawLine(cx+cw-1,cy+ch-1,cx+cw-1,cy+ch-1-cs);
    }

    private String getPhaseLabel(){
        switch(camState.phase){
            case WAITING:return "En attente de patient";
            case MOVING_CLEAR:return "Déplacement → "+camState.patientId;
            case MOVING_OBSTACLE:return "⚠ Obstacle: "+camState.obstacleType+" — Contournement";
            case THUMB_SCAN:return "Authentification biométrique empreinte...";
            case BODY_SCAN:return "Analyse YOLO + HSV | Patient: "+camState.patientId;
            case MEASURING:return "Mesure constantes vitales (Tension / Temp / SpO2 / Pouls)";
            case ESI_RESULT:return "ESI calculé: "+(camState.esiResult!=null?camState.esiResult.name():"–")+" | "+camState.patientId;
            case RECHARGING:return "⚡ Zone de recharge — réapprovisionnement kit";
            case STOPPED:return "■ Robot arrêté — tâches complétées";
            default:return "";
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  BODY SCAN PANEL
// ═══════════════════════════════════════════════════════════════════════

class BodyScanPanel extends JPanel {
    private ScenarDef scenario;
    private NiveauESI esi;
    private int tick=0;

    BodyScanPanel(){
        setBackground(Dashboard.BG_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Dashboard.BORDER),
            BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                "  ANALYSE CORPORELLE — YOLO Pose + HSV",
                TitledBorder.LEFT,TitledBorder.TOP,Dashboard.FT_TINY,Dashboard.TEXT_DIM)));
        new javax.swing.Timer(100,e->{tick++;repaint();}).start();
    }

    void update(ScenarDef sc,NiveauESI e){this.scenario=sc;this.esi=e;}

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        int W=getWidth(),H=getHeight();
        int cx=W/2,cy=H/2+10;
        Color bodyColor=esi!=null?Dashboard.esiColor(esi):Dashboard.C_BLUE;
        ResultatVision v=scenario!=null?scenario.vision:null;
        drawBody(g2,cx,cy,bodyColor,v);
        drawESIBar(g2,W,H,bodyColor);
        float sy=(float)(tick%40)/40f;
        GradientPaint sp=new GradientPaint(8,0,new Color(59,143,212,0),W/2,0,new Color(126,207,255,70),true);
        g2.setPaint(sp);
        g2.fillRect(8,18+(int)(sy*(H-40)),W-16,1);
        if(scenario!=null){g2.setFont(Dashboard.FT_TINY);g2.setColor(Dashboard.C_CYAN);g2.drawString(scenario.patientId+" — "+scenario.nom,8,14);}
        g2.dispose();
    }

    private void drawBody(Graphics2D g2,int cx,int cy,Color col,ResultatVision v){
        boolean amput=v!=null&&v.isAmputationDetectee();
        boolean saign=v!=null&&v.isSaignementMassif();
        boolean incon=v!=null&&v.isInconscient();
        boolean agite=v!=null&&v.isAgite();
        int jx=agite?(int)((tick%4)-2):0,jy=agite?(int)((tick%3)-1):0;
        g2.translate(jx,jy);
        g2.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),30));
        g2.fillOval(cx-14,cy-56,28,28);g2.fillRect(cx-13,cy-28,26,38);
        g2.fillRect(cx-13,cy+10,11,30);if(!amput)g2.fillRect(cx+2,cy+10,11,30);
        g2.setColor(col);g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(cx-14,cy-56,28,28);g2.drawLine(cx,cy-28,cx,cy+10);
        g2.drawLine(cx-13,cy-18,cx-24,cy+8);g2.drawLine(cx+13,cy-18,cx+24,cy+8);
        g2.drawLine(cx-7,cy+10,cx-7,cy+40);
        if(amput){g2.setColor(Dashboard.C_RED);g2.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1f,new float[]{4,3},0));g2.drawLine(cx+7,cy+10,cx+7,cy+20);g2.setFont(new Font("Courier New",Font.BOLD,9));g2.drawString("AMPUT.",cx+12,cy+22);}
        else{g2.setColor(col);g2.setStroke(new BasicStroke(1.5f));g2.drawLine(cx+7,cy+10,cx+7,cy+40);}
        if(saign){g2.setColor(new Color(239,68,68,65));g2.fillOval(cx-18,cy-10,16,10);g2.setColor(Dashboard.C_RED);g2.setStroke(new BasicStroke(0.8f));g2.drawOval(cx-18,cy-10,16,10);g2.setFont(Dashboard.FT_TINY);g2.drawString("HSV⚠",cx-28,cy+6);}
        if(incon){g2.setColor(new Color(239,68,68,110));g2.fillOval(cx-10,cy-44,8,3);g2.fillOval(cx+2,cy-44,8,3);g2.setColor(Dashboard.C_RED);g2.setFont(Dashboard.FT_TINY);g2.drawString("EAR<0.2",cx-22,cy-62);}
        if(agite){g2.setColor(Dashboard.C_AMBER);g2.setFont(Dashboard.FT_TINY);g2.drawString("AGITATION",cx+22,cy-22);}
        g2.translate(-jx,-jy);
    }

    private void drawESIBar(Graphics2D g2,int W,int H,Color col){
        if(esi==null)return;
        int num=Integer.parseInt(esi.name().replace("ESI_",""));
        float pct=(5-num)/4f;
        int bx=10,by=H-22,bw=W-20,bh=7;
        g2.setColor(Dashboard.BORDER);g2.fillRoundRect(bx,by,bw,bh,3,3);
        g2.setColor(col);g2.fillRoundRect(bx,by,(int)(bw*pct),bh,3,3);
        g2.setFont(new Font("Courier New",Font.BOLD,10));g2.setColor(col);
        g2.drawString("ESI "+num+" — "+esi.getDescription(),bx,by-3);
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  VITAUX PANEL
// ═══════════════════════════════════════════════════════════════════════

class VitauxPanel extends JPanel {
    private final JLabel lblPatientId=Dashboard.mkLabel("—",Dashboard.TEXT_DIM,Dashboard.FT_SMALL);
    private final JLabel lblPouls=mkVal("—",Dashboard.C_CYAN);
    private final JLabel lblTension=mkVal("—",Dashboard.C_CYAN);
    private final JLabel lblTemp=mkVal("—",Dashboard.C_CYAN);
    private final JLabel lblSpo2=mkVal("—",Dashboard.C_CYAN);
    private final JLabel lblESI=mkVal("—",Dashboard.C_CYAN);
    private final JLabel lblEsiDesc=Dashboard.mkLabel("—",Dashboard.TEXT_DIM,Dashboard.FT_TINY);
    private final JLabel lblVision=Dashboard.mkLabel("Vision : —",Dashboard.TEXT_DIM,Dashboard.FT_TINY);
    private final JLabel lblDossier=Dashboard.mkLabel("Dossier : —",Dashboard.TEXT_DIM,Dashboard.FT_TINY);

    VitauxPanel(){
        setBackground(Dashboard.BG_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Dashboard.BORDER),
            BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                "  PATIENT EN COURS — Constantes vitales  ♥ SpO2 Tension Temp",
                TitledBorder.LEFT,TitledBorder.TOP,Dashboard.FT_TINY,Dashboard.TEXT_DIM)));
        setLayout(new BorderLayout(0,4));
        JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);
        top.add(Dashboard.mkLabel("Patient:",Dashboard.TEXT_DIM,Dashboard.FT_TINY),BorderLayout.WEST);
        top.add(lblPatientId,BorderLayout.CENTER);
        JPanel grid=new JPanel(new GridLayout(1,5,4,0));grid.setOpaque(false);
        grid.add(vitalCard("♥ Pouls",lblPouls,"bpm"));
        grid.add(vitalCard("⊞ Tension",lblTension,"mmHg"));
        grid.add(vitalCard("⊛ Temp.",lblTemp,"°C"));
        grid.add(vitalCard("○ SpO2",lblSpo2,"%"));
        JPanel ec=vitalCard("ESI",lblESI,"");ec.add(lblEsiDesc,BorderLayout.SOUTH);grid.add(ec);
        JPanel infos=new JPanel(new GridLayout(2,1,0,2));infos.setOpaque(false);
        infos.add(lblVision);infos.add(lblDossier);
        add(top,BorderLayout.NORTH);add(grid,BorderLayout.CENTER);add(infos,BorderLayout.SOUTH);
    }

    void update(ScenarDef sc,NiveauESI esi){
        SignesVitaux s=sc.signes;
        Color pCol=s.getPouls()>120||s.getPouls()<50?Dashboard.C_RED:Dashboard.C_TEAL;
        Color tCol=s.getTensionSystole()>140||s.getTensionSystole()<90?Dashboard.C_RED:Dashboard.C_TEAL;
        Color tpCol=s.getTemperature()>39||s.getTemperature()<35?Dashboard.C_RED:Dashboard.C_TEAL;
        Color sCol=s.getSpo2()<94?Dashboard.C_RED:Dashboard.C_TEAL;
        lblPatientId.setText(sc.patientId+"  —  "+sc.nom);lblPatientId.setForeground(Dashboard.C_CYAN);
        lblPouls.setText(String.valueOf(s.getPouls()));lblPouls.setForeground(pCol);
        lblTension.setText(s.getTensionSystole()+"/"+s.getTensionDiastole());lblTension.setForeground(tCol);
        lblTemp.setText(String.format("%.1f",s.getTemperature()));lblTemp.setForeground(tpCol);
        lblSpo2.setText(String.valueOf(s.getSpo2()));lblSpo2.setForeground(sCol);
        if(esi!=null){Color ec=Dashboard.esiColor(esi);lblESI.setText("ESI "+esi.name().replace("ESI_",""));lblESI.setForeground(ec);lblEsiDesc.setText(esi.getDescription());lblEsiDesc.setForeground(ec);}
        ResultatVision v=sc.vision;
        String vt="Vision : ";
        if(v.isSaignementMassif())vt+="⚠ Saignement ";if(v.isAmputationDetectee())vt+="⚠ Amputation ";
        if(v.isInconscient())vt+="⚠ Inconscient ";if(v.isAgite())vt+="~ Agitation ";
        if(vt.equals("Vision : "))vt+="✓ Normal";
        lblVision.setText(vt);lblVision.setForeground(v.isSaignementMassif()||v.isAmputationDetectee()||v.isInconscient()?Dashboard.C_RED:Dashboard.TEXT_DIM);
        if(sc.dossier!=null){lblDossier.setText("Dossier: "+sc.dossier.getAge()+" ans — "+sc.dossier.getCategories());lblDossier.setForeground(Dashboard.C_CYAN);}
        else{lblDossier.setText(sc.modeDegrade?"Dossier: MODE DÉGRADÉ":"Dossier: Nouveau patient");lblDossier.setForeground(sc.modeDegrade?Dashboard.C_AMBER:Dashboard.TEXT_DIM);}
    }

    private static JPanel vitalCard(String lbl,JLabel val,String unit){
        JPanel p=new JPanel(new BorderLayout(0,1));p.setBackground(Dashboard.BG_DEEP);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Dashboard.BORDER),BorderFactory.createEmptyBorder(4,6,4,6)));
        JLabel l=Dashboard.mkLabel(lbl,Dashboard.TEXT_DIM,Dashboard.FT_TINY);l.setHorizontalAlignment(SwingConstants.CENTER);
        val.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel u=Dashboard.mkLabel(unit,Dashboard.TEXT_DIM,Dashboard.FT_TINY);u.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(l,BorderLayout.NORTH);p.add(val,BorderLayout.CENTER);p.add(u,BorderLayout.SOUTH);return p;
    }
    private static JLabel mkVal(String t,Color c){JLabel l=new JLabel(t,SwingConstants.CENTER);l.setFont(new Font("Courier New",Font.BOLD,15));l.setForeground(c);return l;}
}

// ═══════════════════════════════════════════════════════════════════════
//  FILE PANEL – clic = détails patient + bouton Réanalyser
// ═══════════════════════════════════════════════════════════════════════

class FilePanel extends JPanel {
    private final JPanel listContainer;
    private final JPanel detailContainer;
    private final Dashboard ui;
    private PatientTriage selectedPt = null;

    FilePanel(Dashboard ui){
        this.ui=ui;
        setBackground(Dashboard.BG_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Dashboard.BORDER),
            BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                "  FILE PATIENTS — Cliquer pour détails",
                TitledBorder.LEFT,TitledBorder.TOP,Dashboard.FT_TINY,Dashboard.TEXT_DIM)));
        setLayout(new BorderLayout(0,3));

        listContainer=new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer,BoxLayout.Y_AXIS));
        listContainer.setBackground(Dashboard.BG_PANEL);

        JScrollPane scroll=new JScrollPane(listContainer);
        scroll.setBackground(Dashboard.BG_PANEL);scroll.getViewport().setBackground(Dashboard.BG_PANEL);
        scroll.setBorder(null);scroll.getVerticalScrollBar().setBackground(Dashboard.BG_DEEP);

        detailContainer=new JPanel();
        detailContainer.setLayout(new BoxLayout(detailContainer,BoxLayout.Y_AXIS));
        detailContainer.setBackground(new Color(10,20,36));
        detailContainer.setBorder(BorderFactory.createMatteBorder(1,0,0,0,Dashboard.BORDER));
        detailContainer.setVisible(false);

        add(scroll,BorderLayout.CENTER);
        add(detailContainer,BorderLayout.SOUTH);
    }

    void update(List<PatientTriage> file){
        listContainer.removeAll();
        if(file.isEmpty()){
            JLabel empty=Dashboard.mkLabel("  Aucun patient en file",Dashboard.TEXT_DIM,Dashboard.FT_TINY);
            empty.setBorder(BorderFactory.createEmptyBorder(8,0,0,0));
            listContainer.add(empty);
        } else {
            for(PatientTriage pt:file){
                JPanel row=buildRow(pt);
                row.addMouseListener(new MouseAdapter(){
                    @Override public void mouseClicked(MouseEvent e){
                        selectedPt=pt;
                        showDetail(pt);
                        ui.highlightPatient(pt.id);
                    }
                    @Override public void mouseEntered(MouseEvent e){row.setBackground(new Color(30,55,85));}
                    @Override public void mouseExited(MouseEvent e){row.setBackground(Dashboard.esiBg(pt.esi));}
                });
                row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                listContainer.add(row);listContainer.add(Box.createVerticalStrut(2));
            }
        }
        listContainer.revalidate();listContainer.repaint();
    }

    private void showDetail(PatientTriage pt){
        detailContainer.removeAll();
        detailContainer.setVisible(true);

        Color ec=Dashboard.esiColor(pt.esi);
        JLabel idLbl=Dashboard.mkLabel("  "+pt.id+" — "+pt.desc,ec,Dashboard.FT_TINY);
        idLbl.setBorder(BorderFactory.createEmptyBorder(5,4,2,4));

        // Récupérer le scénario complet
        ScenarDef sc=ui.engine.findScenario(pt.id);
        if(sc!=null){
            JLabel sv=Dashboard.mkLabel(
                "  ♥ "+sc.signes.getPouls()+"bpm  SpO2 "+sc.signes.getSpo2()+
                "%  "+String.format("%.1f",sc.signes.getTemperature())+"°C  "+
                sc.signes.getTensionSystole()+"/"+sc.signes.getTensionDiastole()+"mmHg",
                Dashboard.TEXT_DIM,new Font("Courier New",Font.PLAIN,10));
            sv.setBorder(BorderFactory.createEmptyBorder(1,4,2,4));

            ResultatVision v=sc.vision;
            String vstr="  Vision: ";
            if(v.isSaignementMassif())vstr+="⚠Saignement ";
            if(v.isAmputationDetectee())vstr+="⚠Amputation ";
            if(v.isInconscient())vstr+="⚠Inconscient ";
            if(v.isAgite())vstr+="~Agitation ";
            if(vstr.equals("  Vision: "))vstr+="✓ Normal";
            JLabel visLbl=Dashboard.mkLabel(vstr,
                v.isSaignementMassif()||v.isAmputationDetectee()||v.isInconscient()?Dashboard.C_RED:Dashboard.TEXT_DIM,
                new Font("Courier New",Font.PLAIN,10));
            visLbl.setBorder(BorderFactory.createEmptyBorder(1,4,2,4));

            String dossStr=sc.dossier!=null?"  Dossier: "+sc.dossier.getAge()+" ans — "+sc.dossier.getCategories()
                                           :(sc.modeDegrade?"  MODE DÉGRADÉ":"  Nouveau patient");
            JLabel dossLbl=Dashboard.mkLabel(dossStr,Dashboard.TEXT_DIM,new Font("Courier New",Font.PLAIN,10));
            dossLbl.setBorder(BorderFactory.createEmptyBorder(1,4,3,4));

            detailContainer.add(idLbl);detailContainer.add(sv);detailContainer.add(visLbl);detailContainer.add(dossLbl);
        } else {
            detailContainer.add(idLbl);
        }

        // Bouton Réanalyser
        JButton btnReanalyse=new JButton("🔍 Réanalyser l'état");
        btnReanalyse.setFont(new Font("Courier New",Font.BOLD,11));
        btnReanalyse.setForeground(Dashboard.C_CYAN);
        btnReanalyse.setBackground(new Color(10,30,55));
        btnReanalyse.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Dashboard.C_CYAN),
            BorderFactory.createEmptyBorder(4,8,4,8)));
        btnReanalyse.setFocusPainted(false);
        btnReanalyse.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnReanalyse.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnReanalyse.addActionListener(e->{
            ui.engine.analyserPatientSurClic(pt.id);
            ui.highlightPatient(pt.id);
            btnReanalyse.setText("✔ Ré-analyse lancée");
            btnReanalyse.setForeground(Dashboard.C_GREEN);
        });
        btnReanalyse.addMouseListener(new MouseAdapter(){
            public void mouseEntered(MouseEvent e){btnReanalyse.setBackground(new Color(20,50,90));}
            public void mouseExited(MouseEvent e){btnReanalyse.setBackground(new Color(10,30,55));}
        });
        JPanel btnWrap=new JPanel(new FlowLayout(FlowLayout.CENTER,4,4));
        btnWrap.setOpaque(false);btnWrap.add(btnReanalyse);
        detailContainer.add(btnWrap);

        detailContainer.revalidate();detailContainer.repaint();
        revalidate();repaint();
    }

    private JPanel buildRow(PatientTriage pt){
        JPanel row=new JPanel(new BorderLayout(6,0));
        row.setBackground(Dashboard.esiBg(pt.esi));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Dashboard.BORDER),
            BorderFactory.createEmptyBorder(4,8,4,8)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,46));
        JPanel left=new JPanel(new GridLayout(2,1,0,1));left.setOpaque(false);
        JLabel id=Dashboard.mkLabel(pt.id,Dashboard.C_CYAN,Dashboard.FT_SMALL);
        JLabel ds=Dashboard.mkLabel(pt.desc,Dashboard.TEXT_DIM,new Font("Courier New",Font.PLAIN,10));
        left.add(id);left.add(ds);
        Color ec=Dashboard.esiColor(pt.esi);
        JLabel badge=new JLabel(" ESI "+pt.esi.name().replace("ESI_","")+" ");
        badge.setFont(new Font("Courier New",Font.BOLD,11));badge.setForeground(ec);
        badge.setBackground(Dashboard.esiBg(pt.esi));badge.setOpaque(true);
        badge.setBorder(BorderFactory.createLineBorder(ec));
        row.add(left,BorderLayout.CENTER);row.add(badge,BorderLayout.EAST);
        return row;
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  DOSSIER PANEL
// ═══════════════════════════════════════════════════════════════════════

class DossierPanel extends JPanel {
    private final JLabel lblTitle  =Dashboard.mkLabel("Aucun dossier chargé",Dashboard.TEXT_DIM,Dashboard.FT_SMALL);
    private final JLabel lblDept   =Dashboard.mkLabel("—",Dashboard.TEXT_DIM,Dashboard.FT_TINY);
    private final JLabel lblSalle  =Dashboard.mkLabel("—",Dashboard.TEXT_DIM,Dashboard.FT_TINY);
    private final JLabel lblAge    =Dashboard.mkLabel("—",Dashboard.TEXT_DIM,Dashboard.FT_TINY);
    private final JLabel lblCat    =Dashboard.mkLabel("—",Dashboard.TEXT_DIM,Dashboard.FT_TINY);
    private final JLabel lblESI    =Dashboard.mkLabel("—",Dashboard.TEXT_DIM,Dashboard.FT_SMALL);
    private final JLabel lblEnvoye =Dashboard.mkLabel("—",Dashboard.TEXT_DIM,Dashboard.FT_TINY);

    DossierPanel(){
        setBackground(Dashboard.BG_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Dashboard.BORDER),
            BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                "  DOSSIER MÉDICAL — Envoi département",
                TitledBorder.LEFT,TitledBorder.TOP,Dashboard.FT_TINY,Dashboard.TEXT_DIM)));
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        setMaximumSize(new Dimension(270,175));
        add(lblTitle);add(Box.createVerticalStrut(3));
        JPanel grid=new JPanel(new GridLayout(3,2,2,2));grid.setOpaque(false);
        grid.add(Dashboard.mkLabel("Âge :",Dashboard.TEXT_DIM,Dashboard.FT_TINY));grid.add(lblAge);
        grid.add(Dashboard.mkLabel("Catégories :",Dashboard.TEXT_DIM,Dashboard.FT_TINY));grid.add(lblCat);
        grid.add(Dashboard.mkLabel("ESI :",Dashboard.TEXT_DIM,Dashboard.FT_TINY));grid.add(lblESI);
        add(grid);add(Box.createVerticalStrut(4));
        JPanel envRow=new JPanel(new GridLayout(2,2,2,2));envRow.setOpaque(false);
        envRow.add(Dashboard.mkLabel("→ Département :",Dashboard.TEXT_DIM,Dashboard.FT_TINY));envRow.add(lblDept);
        envRow.add(Dashboard.mkLabel("   Salle :",Dashboard.TEXT_DIM,Dashboard.FT_TINY));envRow.add(lblSalle);
        add(envRow);add(Box.createVerticalStrut(3));
        lblEnvoye.setAlignmentX(LEFT_ALIGNMENT);add(lblEnvoye);
    }

    void show(String pid,DossierMedical dossier,NiveauESI esi,String dept,String salle){
        lblTitle.setText(pid+" — Dossier médical");lblTitle.setForeground(Dashboard.C_CYAN);
        if(dossier!=null){lblAge.setText(dossier.getAge()+" ans");lblAge.setForeground(Dashboard.TEXT_MAIN);lblCat.setText(dossier.getCategories().toString());lblCat.setForeground(Dashboard.TEXT_DIM);}
        else{lblAge.setText("Nouveau patient");lblCat.setText("Aucun antécédent");lblAge.setForeground(Dashboard.TEXT_DIM);}
        if(esi!=null){Color ec=Dashboard.esiColor(esi);lblESI.setText("ESI "+esi.name().replace("ESI_","")+" — "+esi.getDescription());lblESI.setForeground(ec);}
        lblDept.setText(dept);lblDept.setForeground(Dashboard.C_AMBER);
        lblSalle.setText(salle);lblSalle.setForeground(Dashboard.C_CYAN);
        lblEnvoye.setText("✔ Envoyé à "+dept+", "+salle);lblEnvoye.setForeground(Dashboard.C_GREEN);
        revalidate();repaint();
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  KIT PANEL UI
// ═══════════════════════════════════════════════════════════════════════

class KitPanelUI extends JPanel {
    private final Map<ElementKit,JProgressBar> bars=new LinkedHashMap<>();
    private final Map<ElementKit,JLabel> stockLabels=new LinkedHashMap<>();

    KitPanelUI(String titre){
        setBackground(Dashboard.BG_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Dashboard.BORDER),
            BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                "  "+titre.toUpperCase(),TitledBorder.LEFT,TitledBorder.TOP,Dashboard.FT_TINY,Dashboard.TEXT_DIM)));
        setLayout(new GridLayout(0,1,0,2));
        for(ElementKit e:ElementKit.values()){
            JPanel row=new JPanel(new BorderLayout(4,0));row.setOpaque(false);
            String lbl=e.getLabel();if(lbl.length()>10)lbl=lbl.substring(0,8)+"..";
            JLabel name=Dashboard.mkLabel(lbl,Dashboard.TEXT_DIM,Dashboard.FT_TINY);
            name.setPreferredSize(new Dimension(60,15));
            JProgressBar bar=new JProgressBar(0,e.getStockMax());bar.setValue(e.getStockMax());
            bar.setForeground(Dashboard.C_GREEN);bar.setBackground(Dashboard.BG_DEEP);
            bar.setBorderPainted(false);bar.setStringPainted(false);bar.setPreferredSize(new Dimension(0,8));
            JLabel sl=Dashboard.mkLabel(e.getStockMax()+"/"+e.getStockMax(),Dashboard.C_GREEN,Dashboard.FT_TINY);
            sl.setPreferredSize(new Dimension(32,15));
            row.add(name,BorderLayout.WEST);row.add(bar,BorderLayout.CENTER);row.add(sl,BorderLayout.EAST);
            bars.put(e,bar);stockLabels.put(e,sl);add(row);
        }
    }

    void update(KitUrgence kit){
        for(ElementKit e:ElementKit.values()){
            int stock=kit.getStock(e);JProgressBar bar=bars.get(e);JLabel sl=stockLabels.get(e);
            if(bar==null)continue;bar.setValue(stock);
            Color c=stock==0?Dashboard.C_RED:stock<e.getStockMax()?Dashboard.C_AMBER:Dashboard.C_GREEN;
            bar.setForeground(c);sl.setText(stock+"/"+e.getStockMax());sl.setForeground(c);
        }
        repaint();
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  LOG PANEL UI
// ═══════════════════════════════════════════════════════════════════════

class LogPanelUI extends JPanel {
    private final DefaultListModel<String[]> model=new DefaultListModel<>();
    private final JList<String[]> list;

    LogPanelUI(){
        setBackground(Dashboard.BG_DEEP);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Dashboard.BORDER),
            BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                "  CONSOLE DE MONITORING — Logs système",
                TitledBorder.LEFT,TitledBorder.TOP,Dashboard.FT_TINY,Dashboard.TEXT_DIM)));
        setLayout(new BorderLayout());
        list=new JList<>(model);list.setBackground(Dashboard.BG_DEEP);
        list.setFixedCellHeight(17);list.setCellRenderer(new LogRenderer());
        JScrollPane scroll=new JScrollPane(list);scroll.setBorder(null);
        scroll.setBackground(Dashboard.BG_DEEP);scroll.getViewport().setBackground(Dashboard.BG_DEEP);
        scroll.getVerticalScrollBar().setBackground(Dashboard.BG_DEEP);
        model.addListDataListener(new javax.swing.event.ListDataListener(){
            public void intervalAdded(javax.swing.event.ListDataEvent e){int l=model.getSize()-1;if(l>=0)list.ensureIndexIsVisible(l);}
            public void intervalRemoved(javax.swing.event.ListDataEvent e){}
            public void contentsChanged(javax.swing.event.ListDataEvent e){}
        });
        JButton clear=new JButton("Effacer");clear.setFont(Dashboard.FT_TINY);
        clear.setForeground(Dashboard.TEXT_DIM);clear.setBackground(Dashboard.BG_PANEL);
        clear.setBorder(BorderFactory.createLineBorder(Dashboard.BORDER));clear.setFocusPainted(false);
        clear.addActionListener(e->model.clear());
        add(scroll,BorderLayout.CENTER);add(clear,BorderLayout.EAST);
    }

    void add(String msg,LogType type){
        String ts=LocalDateTime.now().format(Dashboard.HH_MM_SS);
        model.addElement(new String[]{ts,msg,type.name()});
        if(model.getSize()>200)model.remove(0);
    }

    static class LogRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean sel,boolean focus){
            String[] row=(String[])value;LogType type=LogType.valueOf(row[2]);
            JLabel l=(JLabel)super.getListCellRendererComponent(list,"",index,sel,focus);
            l.setText("["+row[0]+"] "+row[1]);l.setFont(Dashboard.FT_TINY);
            l.setBackground(sel?new Color(30,50,80):Dashboard.BG_DEEP);
            switch(type){case CRITICAL:l.setForeground(Dashboard.C_RED);break;case WARNING:l.setForeground(Dashboard.C_AMBER);break;case SUCCESS:l.setForeground(Dashboard.C_TEAL);break;default:l.setForeground(Dashboard.TEXT_DIM);}
            if(row[1].contains("━━━"))l.setForeground(Dashboard.C_CYAN);
            if(row[1].contains("TÂCHE :"))l.setForeground(Dashboard.C_BLUE);
            return l;
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  COMMANDE PANEL
// ═══════════════════════════════════════════════════════════════════════

class CmdPanel extends JPanel {
    private final Dashboard ui;
    private final JTextField tfCode=new JTextField(8);
    private final JLabel lblStatus;

    CmdPanel(Dashboard ui){
        this.ui=ui;
        setBackground(Dashboard.BG_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Dashboard.BORDER),
            BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                "  COMMANDES",TitledBorder.LEFT,TitledBorder.TOP,Dashboard.FT_TINY,Dashboard.TEXT_DIM)));
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        setMaximumSize(new Dimension(270,150));
        lblStatus=Dashboard.mkLabel("Prêt.",Dashboard.TEXT_DIM,Dashboard.FT_TINY);
        JPanel row1=btnRow(mkBtn("Mode dégradé",Dashboard.C_AMBER,e->ui.engine.toggleDegrade()),
                           mkBtn("⚡ Recharge",Dashboard.C_GREEN,e->ui.engine.forceRecharge()));
        JPanel row2=btnRow(mkBtn("🚨 Code Blue",Dashboard.C_RED,e->ui.engine.declencherCodeBlue()),
                           mkBtn("Effacer logs",Dashboard.TEXT_DIM,e->{}));
        JPanel secRow=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));secRow.setOpaque(false);
        tfCode.setBackground(Dashboard.BG_DEEP);tfCode.setForeground(Dashboard.C_CYAN);
        tfCode.setCaretColor(Dashboard.C_CYAN);tfCode.setFont(Dashboard.FT_TINY);
        tfCode.setBorder(BorderFactory.createLineBorder(Dashboard.BORDER));
        JLabel codeLbl=Dashboard.mkLabel("Code:",Dashboard.TEXT_DIM,Dashboard.FT_TINY);
        JButton btnDoss=mkBtn("Dossier 🔐",Dashboard.C_AMBER,e->ui.engine.consulterDossier(tfCode.getText().trim()));
        secRow.add(codeLbl);secRow.add(tfCode);secRow.add(btnDoss);
        add(row1);add(Box.createVerticalStrut(3));add(row2);add(Box.createVerticalStrut(3));add(secRow);add(lblStatus);
    }

    void setStatus(String msg,Color c){lblStatus.setText(msg);lblStatus.setForeground(c);}

    private JPanel btnRow(JButton a,JButton b){
        JPanel p=new JPanel(new GridLayout(1,2,4,0));p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,28));p.add(a);p.add(b);return p;
    }

    private JButton mkBtn(String t,Color fg,ActionListener al){
        JButton b=new JButton(t);b.setFont(Dashboard.FT_TINY);b.setForeground(fg);
        b.setBackground(Dashboard.BG_DEEP);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Dashboard.BORDER),BorderFactory.createEmptyBorder(3,6,3,6)));
        b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(al);
        b.addMouseListener(new MouseAdapter(){
            public void mouseEntered(MouseEvent e){b.setBackground(new Color(18,32,52));}
            public void mouseExited(MouseEvent e){b.setBackground(Dashboard.BG_DEEP);}
        });
        return b;
    }
}
