package bank.ui;

import bank.dao.AuditDAO;
import bank.model.AuditLog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.util.List;

/**
 * Panneau Supervision / Audit -- vue "journal de logs" style terminal.
 * Lecture seule : l'admin observe, il n'agit pas sur la base.
 *
 * Format d'une ligne de log :
 *   [2024-05-12 14:32:07]  INSERT  | CPT-001 | Alice Martin | +1500.00 EUR | koto@DESKTOP
 */
public class AuditPanel extends JPanel {

    private final AuditDAO auditDAO = new AuditDAO();

    // ── Couleurs log ─────────────────────────────────────────────────────────
    private static final Color LOG_BG        = new Color(10, 14, 26);   // fond quasi-noir
    private static final Color LOG_TIMESTAMP = new Color(100, 116, 139);// gris
    private static final Color LOG_INSERT    = new Color(34, 197, 94);  // vert vif
    private static final Color LOG_UPDATE    = new Color(59, 130, 246); // bleu
    private static final Color LOG_DELETE    = new Color(239, 68, 68);  // rouge
    private static final Color LOG_FIELD     = new Color(148, 163, 184);// gris clair
    private static final Color LOG_VALUE     = new Color(248, 250, 252);// blanc
    private static final Color LOG_PROMPT    = new Color(52, 211, 153); // vert terminal
    private static final Font  FONT_MONO     = new Font("Consolas", Font.PLAIN, 13);
    private static final Font  FONT_MONO_B   = new Font("Consolas", Font.BOLD,  13);

    // ── Composants ──────────────────────────────────────────────────────────
    private JTextPane       logPane;
    private StyledDocument  doc;
    private JComboBox<String> cboFilter;
    private JTextField       txtSearch;
    private JCheckBox        chkAutoScroll;

    // ── Stats ────────────────────────────────────────────────────────────────
    private JLabel lblTotal, lblInserts, lblUpdates, lblDeletes;

    public AuditPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(LoginFrame.COLOR_BG);
        buildUI();
        refresh();
    }

    // ── Construction ─────────────────────────────────────────────────────────

    private void buildUI() {
        add(buildToolbar(),  BorderLayout.NORTH);
        add(buildLogPane(),  BorderLayout.CENTER);
        add(buildStatsBar(), BorderLayout.SOUTH);
    }

    // ── Barre d'outils ───────────────────────────────────────────────────────

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        bar.setBackground(new Color(15, 23, 42));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(51, 65, 85)));

        // Titre terminal
        JLabel title = new JLabel("  JOURNAL D'AUDIT  ");
        title.setFont(new Font("Consolas", Font.BOLD, 13));
        title.setForeground(LOG_PROMPT);
        title.setOpaque(true);
        title.setBackground(new Color(20, 30, 48));
        title.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LOG_PROMPT, 1),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        bar.add(title);

        bar.add(Box.createHorizontalStrut(10));

        // Filtre type
        bar.add(label("Filtre :"));
        cboFilter = new JComboBox<>(new String[]{"TOUS", "INSERT", "UPDATE", "DELETE"});
        styleCombo(cboFilter);
        cboFilter.addActionListener(e -> refresh());
        bar.add(cboFilter);

        // Recherche
        bar.add(label("  Recherche :"));
        txtSearch = new JTextField(18);
        LoginFrame.styleTextField(txtSearch);
        txtSearch.setFont(FONT_MONO);
        txtSearch.setPreferredSize(new Dimension(160, 30));
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { refresh(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { refresh(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
        });
        bar.add(txtSearch);

        // Auto-scroll
        chkAutoScroll = new JCheckBox("Auto-scroll", true);
        chkAutoScroll.setBackground(new Color(15, 23, 42));
        chkAutoScroll.setForeground(LoginFrame.COLOR_TEXT_MUTED);
        chkAutoScroll.setFont(LoginFrame.FONT_SMALL);
        chkAutoScroll.setFocusPainted(false);
        bar.add(chkAutoScroll);

        // Bouton Actualiser
        JButton btnRefresh = LoginFrame.makeButton("Actualiser", new Color(71, 85, 105));
        btnRefresh.addActionListener(e -> refresh());
        bar.add(btnRefresh);

        // Bouton Vider affichage
        JButton btnClear = LoginFrame.makeButton("Effacer vue", new Color(55, 48, 35));
        btnClear.addActionListener(e -> clearLog());
        bar.add(btnClear);

        // Bouton exporter
        JButton btnExport = LoginFrame.makeButton("Exporter .log", new Color(30, 60, 50));
        btnExport.addActionListener(e -> exportLog());
        bar.add(btnExport);

        return bar;
    }

    // ── Zone de logs ─────────────────────────────────────────────────────────

    private JScrollPane buildLogPane() {
        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setBackground(LOG_BG);
        logPane.setCaretColor(LOG_PROMPT);
        logPane.setBorder(new EmptyBorder(10, 14, 10, 14));
        logPane.setFont(FONT_MONO);
        doc = logPane.getStyledDocument();

        JScrollPane scroll = new JScrollPane(logPane);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(30, 44, 66), 2));
        scroll.getViewport().setBackground(LOG_BG);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Scrollbar sombre
        scroll.getVerticalScrollBar().setBackground(new Color(20, 28, 44));
        scroll.getHorizontalScrollBar().setBackground(new Color(20, 28, 44));

        return scroll;
    }

    // ── Barre de statistiques ─────────────────────────────────────────────────

    private JPanel buildStatsBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 32, 8));
        bar.setBackground(new Color(10, 14, 26));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(30, 44, 66)));

        lblTotal   = statLabel("Total",       "0", LoginFrame.COLOR_TEXT);
        lblInserts = statLabel("Creations",   "0", LOG_INSERT);
        lblUpdates = statLabel("Modifications","0", LOG_UPDATE);
        lblDeletes = statLabel("Suppressions","0", LOG_DELETE);

        for (JLabel lbl : new JLabel[]{lblTotal, lblInserts, lblUpdates, lblDeletes}) {
            bar.add(lbl);
        }
        return bar;
    }

    private JLabel statLabel(String label, String val, Color color) {
        JLabel l = new JLabel(
            "<html><center><span style='font-family:Consolas;font-size:10px;color:#64748b'>"
            + label + "</span><br>"
            + "<b style='font-family:Consolas;font-size:20px;color:"
            + hex(color) + "'>" + val + "</b></center></html>",
            SwingConstants.CENTER);
        l.setPreferredSize(new Dimension(110, 52));
        return l;
    }

    // ── Rendu des logs ────────────────────────────────────────────────────────

    public void refresh() {
        String typeFilter  = cboFilter  != null ? cboFilter.getSelectedItem().toString() : "TOUS";
        String searchText  = txtSearch  != null ? txtSearch.getText().trim().toLowerCase() : "";

        List<AuditLog> logs = auditDAO.findAll();

        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException ignored) {}

        // Ligne d'en-tete terminal
        appendStyled("════════════════════════════════════════════════════════════════"
                   + "══════════════════════════════════════════\n",
                     LOG_PROMPT, FONT_MONO, false);
        appendStyled(" SYSTEME DE SUPERVISION BANCAIRE — JOURNAL D'AUDIT — LECTURE SEULE\n",
                     LOG_PROMPT, FONT_MONO_B, false);
        appendStyled("════════════════════════════════════════════════════════════════"
                   + "══════════════════════════════════════════\n\n",
                     LOG_PROMPT, FONT_MONO, false);

        int shown = 0;
        // Les logs sont deja tries du plus recent -> on inverse pour afficher chronologiquement
        List<AuditLog> ordered = new java.util.ArrayList<>(logs);
        java.util.Collections.reverse(ordered);

        for (AuditLog log : ordered) {
            // Filtrage type
            if (!"TOUS".equals(typeFilter) && !typeFilter.equals(log.getTypeAction())) continue;

            // Filtrage texte
            if (!searchText.isEmpty()) {
                String line = (log.getNcompte()    != null ? log.getNcompte()    : "") + " "
                            + (log.getNomclient()  != null ? log.getNomclient()  : "") + " "
                            + (log.getUtilisateur()!= null ? log.getUtilisateur(): "") + " "
                            + (log.getDateAction() != null ? log.getDateAction() : "");
                if (!line.toLowerCase().contains(searchText)) continue;
            }

            renderLogLine(log);
            shown++;
        }

        if (shown == 0) {
            appendStyled("\n  (Aucun evenement correspondant aux criteres de filtre)\n",
                         LOG_TIMESTAMP, FONT_MONO, true);
        }

        appendStyled("\n── Fin du journal — " + shown + " entree(s) affichee(s) ──\n",
                     LOG_TIMESTAMP, FONT_MONO, true);

        // Mise a jour stats
        int ins = auditDAO.countInserts();
        int upd = auditDAO.countUpdates();
        int del = auditDAO.countDeletes();
        updateStat(lblTotal,   "Total",         ins + upd + del, LoginFrame.COLOR_TEXT);
        updateStat(lblInserts, "Creations",     ins,             LOG_INSERT);
        updateStat(lblUpdates, "Modifications", upd,             LOG_UPDATE);
        updateStat(lblDeletes, "Suppressions",  del,             LOG_DELETE);

        // Auto-scroll vers le bas
        if (chkAutoScroll != null && chkAutoScroll.isSelected()) {
            SwingUtilities.invokeLater(() ->
                logPane.setCaretPosition(doc.getLength()));
        }
    }

    /**
     * Rendu d'une ligne de log au format :
     *
    /**
     * Rendu d'un evenement sous forme de phrase naturelle. Exemple :
     *
     * [2024-05-12 14:32:07]  CREATION   Le compte CPT-001 de Alice Martin a ete ouvert
     *                                   avec un solde initial de 1 500 000 Ar.
     *                                   Effectue par : koto  —  Machine : DESKTOP-V4NB85H
     */
    private void renderLogLine(AuditLog log) {
        Color typeColor = switch (log.getTypeAction()) {
            case "INSERT" -> LOG_INSERT;
            case "UPDATE" -> LOG_UPDATE;
            case "DELETE" -> LOG_DELETE;
            default       -> LOG_FIELD;
        };

        // ── Ligne 1 : [horodatage]  TYPE  ─────────────────────────────────
        appendStyled("[" + log.getDateAction() + "]  ", LOG_TIMESTAMP, FONT_MONO, false);

        String typeLabel = switch (log.getTypeAction()) {
            case "INSERT" -> "CREATION  ";
            case "UPDATE" -> "MODIFICATION";
            case "DELETE" -> "SUPPRESSION ";
            default       -> log.getTypeAction();
        };
        appendStyled(typeLabel, typeColor, FONT_MONO_B, false);
        appendStyled("\n", LOG_FIELD, FONT_MONO, false);

        // ── Ligne 2 : phrase descriptive ──────────────────────────────────
        String indent = "                          "; // aligne sous le type
        String phrase = buildPhrase(log);
        appendStyled(indent, LOG_FIELD, FONT_MONO, false);
        appendStyled(phrase + "\n", LOG_VALUE, FONT_MONO, false);

        // ── Ligne 3 : auteur + machine ────────────────────────────────────
        String auteur = nullSafe(log.getUtilisateur());
        String machine = nullSafe(log.getMachineHote());
        appendStyled(indent, LOG_FIELD, FONT_MONO, false);
        appendStyled("Effectue par : ", LOG_FIELD, FONT_MONO, true);
        appendStyled(auteur, new Color(167, 139, 250), FONT_MONO_B, false);
        appendStyled("   Machine : ", LOG_FIELD, FONT_MONO, true);
        appendStyled(machine + "\n", new Color(167, 139, 250), FONT_MONO, false);

        // ── Separateur ────────────────────────────────────────────────────
        appendStyled("  ─────────────────────────────────────────────────────────────\n",
                new Color(30, 44, 66), FONT_MONO, false);
    }

    /**
     * Construit une phrase lisible couvrant tous les cas :
     *
     * UPDATE - nom seul change :
     *   "Le titulaire du compte CPT-001 a ete renomme de 'Jean Dupont' en 'Jean Martin'.
     *    Le solde reste inchange : 1 500 000 Ar."
     *
     * UPDATE - solde seul change :
     *   "Le compte CPT-001 (Alice Martin) a ete credite de 500 000 Ar.
     *    Solde : 1 500 000 Ar -> 2 000 000 Ar."
     *
     * UPDATE - nom ET solde changent :
     *   "Le titulaire du compte CPT-001 a ete renomme de 'Jean Dupont' en 'Jean Martin',
     *    et le solde a ete debite de 200 000 Ar (1 500 000 Ar -> 1 300 000 Ar)."
     *
     * UPDATE - aucun changement detectable :
     *   "Le compte CPT-001 a ete mis a jour sans modification detectee."
     */
    private String buildPhrase(AuditLog log) {
        String ncompte = nullSafe(log.getNcompte());
        String client  = nullSafe(log.getNomclient());

        return switch (log.getTypeAction()) {

            case "INSERT" -> {
                double solde = log.getSoldeNouv() != null ? log.getSoldeNouv() : 0.0;
                yield "Le compte " + ncompte + " de " + client
                    + " a ete ouvert avec un solde initial de " + ar(solde) + ".";
            }

            case "UPDATE" -> {
                boolean nomChange   = log.nomClientAChange();
                boolean soldeChange = log.soldeAChange();

                double ancien = log.getSoldeAncien() != null ? log.getSoldeAncien() : 0.0;
                double nouv   = log.getSoldeNouv()   != null ? log.getSoldeNouv()   : 0.0;
                double delta  = nouv - ancien;

                String ancienNom   = nullSafe(log.getNomclientAncien());
                String nouveauNom  = nullSafe(log.getNomclient());

                if (nomChange && soldeChange) {
                    // Les deux ont change
                    String mvt = delta >= 0
                        ? "credite de " + ar(delta)
                        : "debite de "  + ar(Math.abs(delta));
                    yield "Le titulaire du compte " + ncompte
                        + " a ete renomme de '" + ancienNom + "' en '" + nouveauNom + "', "
                        + "et le solde a ete " + mvt
                        + " (" + ar(ancien) + " -> " + ar(nouv) + ").";

                } else if (nomChange) {
                    // Seulement le nom a change
                    yield "Le titulaire du compte " + ncompte
                        + " a ete renomme de '" + ancienNom + "' en '" + nouveauNom + "'. "
                        + "Le solde reste inchange : " + ar(ancien) + ".";

                } else if (soldeChange) {
                    // Seulement le solde a change
                    String mvt = delta >= 0
                        ? "credite de " + ar(delta)
                        : "debite de "  + ar(Math.abs(delta));
                    yield "Le compte " + ncompte + " (" + client + ") a ete " + mvt
                        + ". Solde : " + ar(ancien) + " -> " + ar(nouv) + ".";

                } else {
                    // Aucun changement detectable (re-sauvegarde sans modif)
                    yield "Le compte " + ncompte + " (" + client
                        + ") a ete mis a jour sans modification detectee.";
                }
            }

            case "DELETE" -> {
                double solde = log.getSoldeAncien() != null ? log.getSoldeAncien() : 0.0;
                yield "Le compte " + ncompte + " de " + client
                    + " a ete clos. Dernier solde enregistre : " + ar(solde) + ".";
            }

            default -> "Operation inconnue sur le compte " + ncompte + ".";
        };
    }

    private void appendStyled(String text, Color color, Font font, boolean italic) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, color);
        StyleConstants.setFontFamily(attrs, font.getFamily());
        StyleConstants.setFontSize(attrs, font.getSize());
        StyleConstants.setBold(attrs, font.isBold());
        StyleConstants.setItalic(attrs, italic);
        try {
            doc.insertString(doc.getLength(), text, attrs);
        } catch (BadLocationException ignored) {}
    }

    private String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private void updateStat(JLabel lbl, String label, int val, Color color) {
        lbl.setText(
            "<html><center><span style='font-family:Consolas;font-size:10px;color:#64748b'>"
            + label + "</span><br>"
            + "<b style='font-family:Consolas;font-size:20px;color:" + hex(color) + "'>"
            + val + "</b></center></html>");
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private void clearLog() {
        try { doc.remove(0, doc.getLength()); } catch (BadLocationException ignored) {}
        appendStyled("  (Vue effacee — cliquez sur Actualiser pour recharger)\n",
                     LOG_TIMESTAMP, FONT_MONO, true);
    }

    private void exportLog() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("audit_"
            + java.time.LocalDate.now() + ".log"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        java.io.File f = fc.getSelectedFile();
        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new java.io.FileWriter(f, java.nio.charset.StandardCharsets.UTF_8))) {

            pw.println("=== JOURNAL D'AUDIT BANCAIRE ===");
            pw.println("Exporte le : " + java.time.LocalDateTime.now());
            pw.println("=".repeat(70));
            pw.println();

            List<AuditLog> ordered = new java.util.ArrayList<>(auditDAO.findAll());
            java.util.Collections.reverse(ordered);

            for (AuditLog log : ordered) {
                String typeLabel = switch (log.getTypeAction()) {
                    case "INSERT" -> "CREATION";
                    case "UPDATE" -> "MODIFICATION";
                    case "DELETE" -> "SUPPRESSION";
                    default       -> log.getTypeAction();
                };
                pw.printf("[%s]  %s%n", log.getDateAction(), typeLabel);
                pw.println("  " + buildPhrase(log));
                pw.printf("  Effectue par : %s   Machine : %s%n",
                    nullSafe(log.getUtilisateur()),
                    nullSafe(log.getMachineHote()));
                pw.println("  " + "-".repeat(66));
            }

            JOptionPane.showMessageDialog(this,
                "Journal exporte : " + f.getAbsolutePath(),
                "Export reussi", JOptionPane.INFORMATION_MESSAGE);

        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Erreur export : " + ex.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Formate un montant en Ariary : 1 500 000 Ar */
    private String ar(double montant) {
        // Ariary = entier (pas de centimes), on arrondit
        long val = Math.round(montant);
        // Formatage avec separateur de milliers
        return String.format("%,d Ar", val).replace(',', ' ');
    }

    private String nullSafe(String s) { return s != null && !s.isBlank() ? s : "inconnu"; }

    // ── Petits helpers ────────────────────────────────────────────────────────

    private JLabel label(String text) {
        return LoginFrame.makeLabel(text, LoginFrame.FONT_SMALL, LoginFrame.COLOR_TEXT_MUTED);
    }

    private void styleCombo(JComboBox<String> cb) {
        cb.setFont(FONT_MONO);
        cb.setBackground(new Color(30, 41, 59));
        cb.setForeground(Color.WHITE);
        cb.setPreferredSize(new Dimension(110, 30));
    }
}
