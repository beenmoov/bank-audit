package bank.ui;

import bank.dao.UtilisateurDAO;
import bank.model.Utilisateur;
import bank.util.Session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Fenetre 1 - Authentification avec username + mot de passe.
 * Role "admin"       -> acces complet (Gestion + Audit).
 * Role "utilisateur" -> acces Gestion uniquement.
 */
public class LoginFrame extends JFrame {

    // ── Palette et polices partagees ─────────────────────────────────────────
    static final Color COLOR_BG         = new Color(15, 23, 42);
    static final Color COLOR_CARD       = new Color(30, 41, 59);
    static final Color COLOR_ACCENT     = new Color(59, 130, 246);
    static final Color COLOR_ACCENT2    = new Color(16, 185, 129);
    static final Color COLOR_DANGER     = new Color(239, 68, 68);
    static final Color COLOR_WARNING    = new Color(245, 158, 11);
    static final Color COLOR_TEXT       = new Color(248, 250, 252);
    static final Color COLOR_TEXT_MUTED = new Color(148, 163, 184);

    static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD, 24);
    static final Font FONT_LABEL  = new Font("Segoe UI", Font.PLAIN, 14);
    static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 14);
    static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 12);

    // ── Composants ────────────────────────────────────────────────────────────
    private JTextField     txtUsername;
    private JPasswordField txtPassword;
    private JLabel         lblError;
    private int            tentatives = 0;
    private static final int MAX_TENTATIVES = 5;

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();

    public LoginFrame() {
        super("Authentification - Gestion Bancaire");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        buildUI();
        pack();
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(40, 60, 40, 60));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx  = 0;
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);

        // Logo
        JLabel lblIcon = new JLabel("[BANQUE]", SwingConstants.CENTER);
        lblIcon.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblIcon.setForeground(COLOR_ACCENT);
        lblIcon.setOpaque(true);
        lblIcon.setBackground(new Color(30, 58, 95));
        lblIcon.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_ACCENT, 2),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        gbc.gridy = 0; root.add(lblIcon, gbc);

        // Titre
        JLabel lblTitle = makeLabel("Gestion Bancaire", FONT_TITLE, COLOR_TEXT);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 1; gbc.insets = new Insets(4, 0, 2, 0);
        root.add(lblTitle, gbc);

        // Sous-titre
        JLabel lblSub = makeLabel("Connexion securisee", FONT_SMALL, COLOR_TEXT_MUTED);
        lblSub.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 20, 0);
        root.add(lblSub, gbc);

        // Separateur
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(51, 65, 85));
        gbc.gridy = 3; gbc.insets = new Insets(0, 0, 16, 0);
        root.add(sep, gbc);

        // Champ utilisateur
        gbc.gridy = 4; gbc.insets = new Insets(0, 0, 4, 0);
        root.add(makeLabel("Nom d'utilisateur", FONT_LABEL, COLOR_TEXT_MUTED), gbc);
        txtUsername = new JTextField(22);
        styleTextField(txtUsername);
        gbc.gridy = 5; gbc.insets = new Insets(0, 0, 10, 0);
        root.add(txtUsername, gbc);

        // Champ mot de passe
        gbc.gridy = 6; gbc.insets = new Insets(0, 0, 4, 0);
        root.add(makeLabel("Mot de passe", FONT_LABEL, COLOR_TEXT_MUTED), gbc);
        txtPassword = new JPasswordField(22);
        styleTextField(txtPassword);
        txtPassword.setEchoChar('*');
        gbc.gridy = 7; gbc.insets = new Insets(0, 0, 4, 0);
        root.add(txtPassword, gbc);

        // Ligne "Afficher le mot de passe"
        JCheckBox chkShow = new JCheckBox("Afficher le mot de passe");
        chkShow.setBackground(COLOR_BG);
        chkShow.setForeground(COLOR_TEXT_MUTED);
        chkShow.setFont(FONT_SMALL);
        chkShow.setFocusPainted(false);
        chkShow.addActionListener(e ->
            txtPassword.setEchoChar(chkShow.isSelected() ? (char) 0 : '*'));
        gbc.gridy = 8; gbc.insets = new Insets(0, 0, 6, 0);
        root.add(chkShow, gbc);

        // Hint comptes par defaut
        JLabel lblHint = makeLabel("Comptes : admin/admin123  ou  user/user123", FONT_SMALL,
                                   new Color(71, 85, 105));
        lblHint.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 9; gbc.insets = new Insets(0, 0, 4, 0);
        root.add(lblHint, gbc);

        // Message d'erreur
        lblError = makeLabel("", FONT_SMALL, COLOR_DANGER);
        lblError.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 10; gbc.insets = new Insets(0, 0, 6, 0);
        root.add(lblError, gbc);

        // Bouton connexion
        JButton btnLogin = makeButton("Se Connecter", COLOR_ACCENT);
        btnLogin.addActionListener(e -> doLogin());
        gbc.gridy = 11; gbc.insets = new Insets(4, 0, 0, 0);
        root.add(btnLogin, gbc);

        // Connexion sur Entree
        txtPassword.addActionListener(e -> doLogin());
        txtUsername.addActionListener(e -> txtPassword.requestFocusInWindow());

        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) {
                txtUsername.requestFocusInWindow();
            }
        });

        setContentPane(root);
    }

    // ── Logique de connexion ─────────────────────────────────────────────────

    private void doLogin() {
        if (tentatives >= MAX_TENTATIVES) {
            lblError.setText("Compte bloque - trop de tentatives.");
            return;
        }

        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Veuillez remplir tous les champs.");
            return;
        }

        Utilisateur u = utilisateurDAO.authentifier(username, password);

        if (u == null) {
            tentatives++;
            int restants = MAX_TENTATIVES - tentatives;
            lblError.setText("Identifiants incorrects. Tentatives restantes : " + restants);
            txtPassword.setText("");
            txtPassword.requestFocusInWindow();
            return;
        }

        // Connexion reussie
        Session.Role role = "admin".equalsIgnoreCase(u.getRole())
            ? Session.Role.ADMIN
            : Session.Role.UTILISATEUR;
        Session.getInstance().login(u.getUsername(), role);

        MainFrame mainFrame = new MainFrame();
        mainFrame.setVisible(true);
        dispose();
    }

    // ── Helpers de style partages ─────────────────────────────────────────────

    static JLabel makeLabel(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    static void styleTextField(JTextField tf) {
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setBackground(new Color(51, 65, 85));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(71, 85, 105), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        tf.setPreferredSize(new Dimension(tf.getPreferredSize().width, 38));
    }

    static JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed()  ? bg.darker()   :
                            getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BUTTON);
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 40));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
