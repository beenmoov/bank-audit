package bank;

import bank.ui.LoginFrame;
import bank.util.DatabaseManager;

import javax.swing.*;

/**
 * Point d'entree principal de l'application de Gestion et d'Audit Bancaire.
 */
public class Main {

    public static void main(String[] args) {
        // Nimbus respecte les couleurs custom contrairement au LAF systeme Windows
        // qui ecrase setBackground/setForeground sur JTabbedPane et JTable.
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    // Surcharger les couleurs Nimbus pour notre theme sombre
                    UIManager.put("control",          new java.awt.Color(30, 41, 59));
                    UIManager.put("info",             new java.awt.Color(30, 41, 59));
                    UIManager.put("nimbusBase",       new java.awt.Color(15, 23, 42));
                    UIManager.put("nimbusBlueGrey",   new java.awt.Color(51, 65, 85));
                    UIManager.put("nimbusLightBackground", new java.awt.Color(30, 41, 59));
                    UIManager.put("text",             new java.awt.Color(248, 250, 252));
                    break;
                }
            }
        } catch (Exception e) {
            // Garder le LAF par defaut si Nimbus absent
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) {}
        }

        // Initialisation de la base de donnees (tables + triggers)
        DatabaseManager.getInstance().initialize();

        // Lancement de l'interface sur l'EDT
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
