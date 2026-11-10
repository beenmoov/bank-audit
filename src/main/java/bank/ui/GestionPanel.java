package bank.ui;

import bank.dao.CompteDAO;
import bank.model.Compte;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;

/**
 * Fenêtre 3 – Gestion des Comptes.
 * Formulaire d'ajout, modification et suppression de comptes.
 */
public class GestionPanel extends JPanel {

    private final CompteDAO compteDAO = new CompteDAO();
    private final MainFrame parent;

    // ── Table ──────────────────────────────────────────────────────────────────
    private DefaultTableModel tableModel;
    private JTable            table;
    private TableRowSorter<DefaultTableModel> sorter;

    // ── Formulaire ─────────────────────────────────────────────────────────────
    private JTextField txtNcompte;
    private JTextField txtNomclient;
    private JTextField txtSolde;
    private JTextField txtSearch;

    // ── Boutons ────────────────────────────────────────────────────────────────
    private JButton btnAjouter;
    private JButton btnModifier;
    private JButton btnSupprimer;
    private JButton btnVider;

    public GestionPanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout(12, 12));
        setBackground(LoginFrame.COLOR_BG);
        setBorder(new EmptyBorder(16, 16, 16, 16));
        buildUI();
        loadData();
    }

    // Constructeur de compatibilite (le boolean est ignore)
    public GestionPanel(MainFrame parent, boolean ignored) {
        this(parent);
    }

    private void buildUI() {
        JPanel formPanel = buildFormPanel();
        formPanel.setPreferredSize(new Dimension(300, 0));
        add(formPanel,          BorderLayout.WEST);
        add(buildTablePanel(),  BorderLayout.CENTER);
    }

    // ── Formulaire ─────────────────────────────────────────────────────────────

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(LoginFrame.COLOR_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
            new EmptyBorder(20, 20, 20, 20)));

        JLabel title = LoginFrame.makeLabel("Formulaire Compte",
                LoginFrame.FONT_TITLE, LoginFrame.COLOR_TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(20));

        txtNcompte   = addFormField(panel, "N° de Compte *");
        txtNomclient = addFormField(panel, "Nom du Client *");
        txtSolde     = addFormField(panel, "Solde Initial (Ar) *");

        panel.add(Box.createVerticalStrut(20));

        btnAjouter   = makeActionButton("+ Ajouter",  LoginFrame.COLOR_ACCENT2);
        btnModifier  = makeActionButton("Modifier",    LoginFrame.COLOR_ACCENT);
        btnSupprimer = makeActionButton("Supprimer",   LoginFrame.COLOR_DANGER);
        btnVider     = makeActionButton("Vider",       new Color(71, 85, 105));

        for (JButton btn : new JButton[]{btnAjouter, btnModifier, btnSupprimer, btnVider}) {
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            panel.add(btn);
            panel.add(Box.createVerticalStrut(8));
        }
        btnAjouter.addActionListener(  e -> doAjouter());
        btnModifier.addActionListener( e -> doModifier());
        btnSupprimer.addActionListener(e -> doSupprimer());
        btnVider.addActionListener(    e -> viderFormulaire());

        return panel;
    }

    private JTextField addFormField(JPanel parent, String labelText) {
        JLabel lbl = LoginFrame.makeLabel(labelText, LoginFrame.FONT_SMALL, LoginFrame.COLOR_TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(lbl);
        parent.add(Box.createVerticalStrut(4));

        JTextField tf = new JTextField();
        LoginFrame.styleTextField(tf);
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        parent.add(tf);
        parent.add(Box.createVerticalStrut(12));
        return tf;
    }

    private JButton makeActionButton(String text, Color bg) {
        return LoginFrame.makeButton(text, bg);
    }

    // ── Table ──────────────────────────────────────────────────────────────────

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(LoginFrame.COLOR_BG);

        // Barre de recherche
        JPanel searchBar = buildSearchBar();

        // Table
        String[] cols = {"N° Compte", "Nom Client", "Solde (Ar)"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        styleTable(table);

        // Renderer custom pour fond sombre uniforme (resout le bug fond blanc Nimbus)
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                setOpaque(true);
                if (isSelected) {
                    setBackground(new Color(59, 130, 246));
                    setForeground(Color.WHITE);
                } else {
                    setBackground(row % 2 == 0 ? LoginFrame.COLOR_CARD : new Color(24, 34, 52));
                    setForeground(LoginFrame.COLOR_TEXT);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });

        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Remplissage du formulaire au clic
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) remplirFormulaire();
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(LoginFrame.COLOR_CARD);
        scroll.getViewport().setBackground(LoginFrame.COLOR_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85)));

        panel.add(searchBar, BorderLayout.NORTH);
        panel.add(scroll,    BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSearchBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(LoginFrame.COLOR_BG);

        JLabel lbl = LoginFrame.makeLabel("Recherche :", LoginFrame.FONT_LABEL, LoginFrame.COLOR_TEXT_MUTED);

        txtSearch = new JTextField();
        LoginFrame.styleTextField(txtSearch);
        txtSearch.setToolTipText("Filtrer par n° compte ou nom client");
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { filtrer(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { filtrer(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrer(); }
        });

        JButton btnRefresh = LoginFrame.makeButton("Actualiser", new Color(71, 85, 105));
        btnRefresh.addActionListener(e -> loadData());

        bar.add(lbl,        BorderLayout.WEST);
        bar.add(txtSearch,  BorderLayout.CENTER);
        bar.add(btnRefresh, BorderLayout.EAST);
        return bar;
    }

    private void styleTable(JTable t) {
        t.setBackground(LoginFrame.COLOR_CARD);
        t.setForeground(LoginFrame.COLOR_TEXT);
        t.setGridColor(new Color(51, 65, 85));
        t.setSelectionBackground(new Color(59, 130, 246));
        t.setSelectionForeground(Color.WHITE);
        t.setFont(LoginFrame.FONT_LABEL);
        t.setRowHeight(32);
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setOpaque(true);
        // En-tête forcé opaque avec renderer custom (résout le bug header blanc sous Nimbus/Windows)
        t.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            {
                setOpaque(true);
                setBackground(new Color(15, 23, 42));
                setForeground(LoginFrame.COLOR_TEXT_MUTED);
                setFont(LoginFrame.FONT_SMALL);
                setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(51, 65, 85)),
                    BorderFactory.createEmptyBorder(0, 8, 0, 0)
                ));
            }
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                setText(value != null ? value.toString() : "");
                return this;
            }
        });
        t.getTableHeader().setBackground(new Color(15, 23, 42));
        t.getTableHeader().setForeground(LoginFrame.COLOR_TEXT_MUTED);
        t.getTableHeader().setFont(LoginFrame.FONT_SMALL);
        t.getTableHeader().setOpaque(true);
        t.getTableHeader().setReorderingAllowed(false);
    }

    // ── Chargement ─────────────────────────────────────────────────────────────

    public void loadData() {
        tableModel.setRowCount(0);
        List<Compte> comptes = compteDAO.findAll();
        for (Compte c : comptes) {
            tableModel.addRow(new Object[]{
                c.getNcompte(),
                c.getNomclient(),
                String.format("%,d Ar", Math.round(c.getSolde())).replace(",", " ")
            });
        }
    }

    private void filtrer() {
        String text = txtSearch.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }

    private void remplirFormulaire() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int modelRow = table.convertRowIndexToModel(row);
        txtNcompte.setText(tableModel.getValueAt(modelRow, 0).toString());
        txtNomclient.setText(tableModel.getValueAt(modelRow, 1).toString());
        String soldeStr = tableModel.getValueAt(modelRow, 2).toString()
                            .replace(" Ar", "").replace("Ar", "").replace(" ", "")
                            .replace(",", ".").trim();
        txtSolde.setText(soldeStr);
        txtNcompte.setEditable(false); // Empêche de changer la PK
    }

    private void viderFormulaire() {
        txtNcompte.setText("");
        txtNomclient.setText("");
        txtSolde.setText("");
        txtNcompte.setEditable(true);
        table.clearSelection();
    }

    // ── Actions CRUD ───────────────────────────────────────────────────────────

    private void doAjouter() {
        if (!validerChamps()) return;
        Compte c = new Compte(
            txtNcompte.getText().trim(),
            txtNomclient.getText().trim(),
            Double.parseDouble(txtSolde.getText().trim().replace(",", "."))
        );
        if (compteDAO.insert(c)) {
            showSuccess("Compte ajouté avec succès !");
            loadData();
            viderFormulaire();
            parent.notifyAuditRefresh();
        } else {
            showError("Erreur lors de l'ajout (N° déjà existant ?)");
        }
    }

    private void doModifier() {
        if (table.getSelectedRow() < 0) { showError("Sélectionnez un compte à modifier."); return; }
        if (!validerChamps()) return;
        Compte c = new Compte(
            txtNcompte.getText().trim(),
            txtNomclient.getText().trim(),
            Double.parseDouble(txtSolde.getText().trim().replace(",", "."))
        );
        if (compteDAO.update(c)) {
            showSuccess("Compte modifié avec succès !");
            loadData();
            viderFormulaire();
            parent.notifyAuditRefresh();
        } else {
            showError("Erreur lors de la modification.");
        }
    }

    private void doSupprimer() {
        if (table.getSelectedRow() < 0) { showError("Sélectionnez un compte à supprimer."); return; }
        String ncompte = txtNcompte.getText().trim();
        int confirm = JOptionPane.showConfirmDialog(this,
            "Supprimer le compte " + ncompte + " ?",
            "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            if (compteDAO.delete(ncompte)) {
                showSuccess("Compte supprimé.");
                loadData();
                viderFormulaire();
                parent.notifyAuditRefresh();
            } else {
                showError("Erreur lors de la suppression.");
            }
        }
    }

    // ── Validation ─────────────────────────────────────────────────────────────

    private boolean validerChamps() {
        if (txtNcompte.getText().trim().isEmpty()) {
            showError("Le numéro de compte est obligatoire."); return false;
        }
        if (txtNomclient.getText().trim().isEmpty()) {
            showError("Le nom du client est obligatoire."); return false;
        }
        try {
            Double.parseDouble(txtSolde.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            showError("Le solde doit être un nombre valide."); return false;
        }
        return true;
    }

    private void showSuccess(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Succès", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erreur", JOptionPane.ERROR_MESSAGE);
    }
}
