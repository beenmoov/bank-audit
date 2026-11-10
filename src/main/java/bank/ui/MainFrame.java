package bank.ui;

import bank.util.Session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Fenetre principale.
 *
 * Roles et acces :
 *   utilisateur -> onglet "Gestion des Comptes" uniquement (CRUD)
 *   admin       -> onglet "Gestion" en LECTURE SEULE + onglet "Journal d'Audit"
 *
 * L'admin ne touche pas a la base : il observe uniquement.
 */
public class MainFrame extends JFrame {

    private GestionPanel gestionPanel;
    private AuditPanel   auditPanel;

    private JButton    btnTabGestion;
    private CardLayout cardLayout;
    private JPanel     cardPanel;

    private static final Color TAB_ACTIVE_BG   = new Color(59, 130, 246);
    private static final Color TAB_INACTIVE_BG = new Color(22, 32, 51);
    private static final Color TAB_HOVER_BG    = new Color(40, 56, 82);
    public MainFrame() {
        String role = Session.getInstance().isAdmin() ? "Supervision" : "Gestion";
        super("Banque — " + role + " | " + Session.getInstance().getUsername());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 680));
        buildUI();
        pack();
        setLocationRelativeTo(null);
    }

    // ── Construction ─────────────────────────────────────────────────────────

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(LoginFrame.COLOR_BG);
        root.add(buildHeader(),  BorderLayout.NORTH);
        root.add(buildContent(), BorderLayout.CENTER);
        setContentPane(root);
    }

    // ── En-tete ───────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(15, 23, 42));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(51, 65, 85)),
            new EmptyBorder(10, 20, 10, 20)));

        String titleText = Session.getInstance().isAdmin()
            ? "Supervision Bancaire — Journal d'Audit"
            : "Systeme de Gestion Bancaire";
        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(LoginFrame.COLOR_TEXT);

        // Badge role
        Session s = Session.getInstance();
        Color   roleColor = s.isAdmin() ? new Color(34, 197, 94) : LoginFrame.COLOR_ACCENT;
        String  roleTxt   = s.isAdmin() ? "ADMIN" : "UTILISATEUR";

        JLabel lblRole = new JLabel(" " + roleTxt + " ");
        lblRole.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblRole.setForeground(roleColor);
        lblRole.setOpaque(true);
        lblRole.setBackground(new Color(roleColor.getRed(),
                                        roleColor.getGreen(),
                                        roleColor.getBlue(), 25));
        lblRole.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(roleColor, 1),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)));

        JLabel lblUser = new JLabel("  " + s.getUsername()
                + "   Machine : " + s.getHostname());
        lblUser.setFont(LoginFrame.FONT_SMALL);
        lblUser.setForeground(LoginFrame.COLOR_TEXT_MUTED);

        JButton btnLogout = buildLogoutButton();

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(new Color(15, 23, 42));
        right.add(lblRole);
        right.add(lblUser);
        right.add(btnLogout);

        panel.add(title, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private JButton buildLogoutButton() {
        JButton btn = new JButton("Deconnexion") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = LoginFrame.COLOR_DANGER;
                g2.setColor(getModel().isRollover() ? c.brighter() : c.darker());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(LoginFrame.FONT_SMALL);
        btn.setForeground(Color.WHITE);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(100, 26));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> doLogout());
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.repaint(); }
            public void mouseExited (MouseEvent e) { btn.repaint(); }
        });
        return btn;
    }

    // ── Contenu ───────────────────────────────────────────────────────────────

    private JPanel buildContent() {
        boolean isAdmin = Session.getInstance().isAdmin();

        if (isAdmin) {
            // Admin : interface épurée — uniquement le journal d'audit, sans onglets
            auditPanel = new AuditPanel();
            auditPanel.refresh();
            return auditPanel;
        }

        // Utilisateur : un seul onglet "Gestion des Comptes" (CRUD complet)
        btnTabGestion = buildTabBtn("Gestion des Comptes");
        btnTabGestion.addActionListener(e -> activateTab("GESTION"));

        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabBar.setBackground(new Color(15, 23, 42));
        tabBar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0,
                new Color(51, 65, 85)));
        tabBar.add(btnTabGestion);

        gestionPanel = new GestionPanel(this, false);

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setBackground(LoginFrame.COLOR_BG);
        cardPanel.add(gestionPanel, "GESTION");
        activateTab("GESTION");

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(LoginFrame.COLOR_BG);
        wrapper.add(tabBar,    BorderLayout.NORTH);
        wrapper.add(cardPanel, BorderLayout.CENTER);
        return wrapper;
    }

    // ── Onglet custom ─────────────────────────────────────────────────────────

    private JButton buildTabBtn(String label) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                boolean active  = Boolean.TRUE.equals(getClientProperty("active"));
                boolean hovered = getModel().isRollover();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(active ? TAB_ACTIVE_BG : (hovered ? TAB_HOVER_BG : TAB_INACTIVE_BG));
                g2.fillRect(0, 0, getWidth(), getHeight());
                if (active) {
                    g2.setColor(Color.WHITE);
                    g2.fillRect(0, getHeight() - 3, getWidth(), 3);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.putClientProperty("active", false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(240, 40));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.repaint(); }
            public void mouseExited (MouseEvent e) { btn.repaint(); }
        });
        return btn;
    }

    private void activateTab(String name) {
        if (btnTabGestion != null) {
            btnTabGestion.putClientProperty("active", "GESTION".equals(name));
            btnTabGestion.repaint();
        }
        if (cardLayout != null) cardLayout.show(cardPanel, name);
    }

    // ── Deconnexion ───────────────────────────────────────────────────────────

    private void doLogout() {
        int ok = JOptionPane.showConfirmDialog(this,
            "Se deconnecter ?", "Deconnexion",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            Session.getInstance().logout();
            new LoginFrame().setVisible(true);
            dispose();
        }
    }

    // ── Callback depuis GestionPanel ──────────────────────────────────────────

    public void notifyAuditRefresh() {
        if (auditPanel != null) auditPanel.refresh();
    }
}
