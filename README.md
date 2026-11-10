# 🏦 Application de Gestion et d'Audit Bancaire

Application lourde Java (Swing + SQLite) mettant en œuvre des **triggers SQL** pour la supervision automatique des opérations bancaires.

---

## 📋 Table des matières

1. [Architecture du projet](#architecture)
2. [Prérequis](#prérequis)
3. [Installation et lancement](#installation)
4. [Structure des sources](#structure)
5. [Modèle de données et Triggers SQL](#base-de-données)
6. [Description des fenêtres](#fenêtres)
7. [Flux d'exécution](#flux)
8. [Captures d'écran (description)](#ui)

---

## 🏗 Architecture du projet <a name="architecture"></a>

```
BankAuditApp/
├── src/main/java/bank/
│   ├── Main.java                   ← Point d'entrée
│   ├── model/
│   │   ├── Compte.java             ← Entité compte bancaire
│   │   └── AuditLog.java          ← Entité log d'audit
│   ├── dao/
│   │   ├── CompteDAO.java          ← CRUD sur table `compte`
│   │   └── AuditDAO.java          ← Lecture de `audit_compte` + stats
│   ├── ui/
│   │   ├── LoginFrame.java         ← Fenêtre 1 : Authentification
│   │   ├── MainFrame.java          ← Conteneur à onglets
│   │   ├── GestionPanel.java      ← Fenêtre 3 : Gestion des comptes
│   │   └── AuditPanel.java        ← Fenêtre 2 : Supervision/Audit
│   └── util/
│       ├── DatabaseManager.java   ← Init DB, connexion JDBC, triggers
│       └── Session.java           ← Singleton utilisateur connecté
├── lib/
│   └── sqlite-jdbc.jar            ← Driver JDBC SQLite (à télécharger)
├── pom.xml                        ← Build Maven (recommandé)
├── build.sh                       ← Script de build Linux/macOS
├── build.bat                      ← Script de build Windows
└── README.md
```

**Pattern architectural :** MVC simplifié
- **Model** : `Compte`, `AuditLog`
- **DAO** : `CompteDAO`, `AuditDAO` (accès données via `PreparedStatement`)
- **View** : Packages `ui` (Swing)
- **Util** : `DatabaseManager` (connexion + triggers), `Session` (singleton)

---

## ✅ Prérequis <a name="prérequis"></a>

| Composant | Version minimale | Lien |
|-----------|-----------------|------|
| JDK       | 17              | https://adoptium.net/ |
| SQLite JDBC Driver | 3.45.x | https://github.com/xerial/sqlite-jdbc |
| Maven (optionnel) | 3.8+ | https://maven.apache.org/ |

---

## 🚀 Installation et lancement <a name="installation"></a>

### Option A — Maven (recommandé, gère automatiquement les dépendances)

```bash
# 1. Cloner / extraire le projet
cd BankAuditApp

# 2. Compiler et créer le fat JAR
mvn clean package

# 3. Lancer
java -jar target/BankAuditApp.jar
```

### Option B — Script shell (Linux/macOS)

```bash
# 1. Télécharger le driver SQLite JDBC
mkdir -p lib
curl -L https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.3.0/sqlite-jdbc-3.45.3.0.jar \
     -o lib/sqlite-jdbc.jar

# 2. Donner les droits d'exécution et lancer
chmod +x build.sh
./build.sh
```

### Option C — Script batch (Windows)

```bat
REM 1. Télécharger sqlite-jdbc-3.45.3.0.jar, le placer dans lib\
REM 2. Double-cliquer sur build.bat ou :
build.bat
```

### Option D — Compilation manuelle

```bash
# Compiler
find src -name "*.java" > sources.txt
javac -encoding UTF-8 -cp lib/sqlite-jdbc.jar -d out @sources.txt

# Lancer
java -cp "out:lib/sqlite-jdbc.jar" bank.Main   # Linux/Mac
java -cp "out;lib\sqlite-jdbc.jar" bank.Main   # Windows
```

> **Note :** La base de données `banque_audit.db` est créée automatiquement dans le répertoire courant au premier lancement.

---

## 📁 Structure des sources <a name="structure"></a>

### `Main.java`
Point d'entrée. Initialise la base de données via `DatabaseManager`, puis ouvre `LoginFrame` sur l'EDT Swing.

### `util/Session.java`
**Singleton** stockant le nom d'utilisateur connecté et le nom de la machine hôte. Utilisé par `CompteDAO` pour annoter les logs d'audit.

### `util/DatabaseManager.java`
- Gère la connexion JDBC à SQLite (`banque_audit.db`)
- Crée les tables `compte` et `audit_compte` si elles n'existent pas
- Crée les **3 triggers SQL** : `trg_insert_compte`, `trg_update_compte`, `trg_delete_compte`
- Expose `patchLastAuditUser()` pour corriger `utilisateur` et `machine_hote` après chaque trigger

### `dao/CompteDAO.java`
Toutes les opérations CRUD utilisent des `PreparedStatement` :
- `findAll()` → `SELECT`
- `insert(Compte)` → `INSERT` + patch audit
- `update(Compte)` → `UPDATE` + patch audit
- `delete(String)` → `DELETE` + patch audit

### `dao/AuditDAO.java`
- `findAll()` → tous les logs triés du plus récent
- `findByType(String)` → filtrage par INSERT/UPDATE/DELETE
- `countInserts()`, `countUpdates()`, `countDeletes()` → statistiques

---

## 🗄 Modèle de données et Triggers SQL <a name="base-de-données"></a>

### Table `compte`

```sql
CREATE TABLE IF NOT EXISTS compte (
    ncompte    TEXT PRIMARY KEY,
    nomclient  TEXT NOT NULL,
    solde      REAL NOT NULL DEFAULT 0
);
```

### Table `audit_compte`

```sql
CREATE TABLE IF NOT EXISTS audit_compte (
    id_audit     INTEGER PRIMARY KEY AUTOINCREMENT,
    type_action  TEXT    NOT NULL,        -- INSERT / UPDATE / DELETE
    date_action  TEXT    NOT NULL,        -- datetime('now', 'localtime')
    ncompte      TEXT,
    nomclient    TEXT,
    soldeAncien  REAL,                   -- NULL pour les INSERT
    soldeNouv    REAL,                   -- NULL pour les DELETE
    utilisateur  TEXT,                   -- Nom de l'utilisateur connecté
    requete_sql  TEXT,                   -- Requête SQL reconstituée
    machine_hote TEXT                    -- Hostname de la machine
);
```

### Trigger AFTER INSERT

```sql
CREATE TRIGGER trg_insert_compte
AFTER INSERT ON compte
FOR EACH ROW
BEGIN
    INSERT INTO audit_compte (
        type_action, date_action, ncompte, nomclient,
        soldeAncien, soldeNouv, utilisateur, requete_sql, machine_hote
    ) VALUES (
        'INSERT',
        datetime('now', 'localtime'),
        NEW.ncompte, NEW.nomclient,
        NULL,
        NEW.solde,
        'app_user',
        'INSERT INTO compte VALUES (' || NEW.ncompte || ', ' || NEW.nomclient || ', ' || NEW.solde || ')',
        'localhost'
    );
END;
```

### Trigger AFTER UPDATE

```sql
CREATE TRIGGER trg_update_compte
AFTER UPDATE ON compte
FOR EACH ROW
BEGIN
    INSERT INTO audit_compte (
        type_action, date_action, ncompte, nomclient,
        soldeAncien, soldeNouv, utilisateur, requete_sql, machine_hote
    ) VALUES (
        'UPDATE',
        datetime('now', 'localtime'),
        NEW.ncompte, NEW.nomclient,
        OLD.solde,          -- ← solde AVANT modification
        NEW.solde,          -- ← solde APRÈS modification
        'app_user',
        'UPDATE compte SET nomclient=' || NEW.nomclient || ', solde=' || NEW.solde || ' WHERE ncompte=' || OLD.ncompte,
        'localhost'
    );
END;
```

### Trigger AFTER DELETE

```sql
CREATE TRIGGER trg_delete_compte
AFTER DELETE ON compte
FOR EACH ROW
BEGIN
    INSERT INTO audit_compte (
        type_action, date_action, ncompte, nomclient,
        soldeAncien, soldeNouv, utilisateur, requete_sql, machine_hote
    ) VALUES (
        'DELETE',
        datetime('now', 'localtime'),
        OLD.ncompte, OLD.nomclient,
        OLD.solde,          -- ← dernier solde connu
        NULL,
        'app_user',
        'DELETE FROM compte WHERE ncompte=' || OLD.ncompte,
        'localhost'
    );
END;
```

> **Stratégie utilisateur :** Les triggers SQLite ne permettent pas d'accéder à des variables d'application (comme le nom d'utilisateur connecté). La solution retenue écrit `'app_user'` dans le trigger, puis la méthode `patchLastAuditUser()` corrige immédiatement après l'opération avec le vrai nom issu du `Session` singleton.

---

## 🖥 Description des fenêtres <a name="fenêtres"></a>

### Fenêtre 1 — Authentification (`LoginFrame`)

- Champ de saisie du nom d'utilisateur
- Validation (champ non vide)
- Stockage dans `Session.getInstance().setUsername(name)`
- Connexion sur touche `Entrée` ou bouton
- Ouverture de `MainFrame` après succès

### Fenêtre 2 — Supervision / Audit (`AuditPanel`)

- **JTable** avec 10 colonnes : #, Action, Date/Heure, N° Compte, Client, Solde Ancien, Solde Nouveau, Utilisateur, Machine, Requête SQL
- **Coloration** des lignes selon le type : vert (INSERT), bleu (UPDATE), rouge (DELETE)
- **Filtres** : ComboBox par type d'action + champ de recherche textuel
- **Tri** par clic sur les en-têtes de colonnes
- **Barre de statistiques** en bas : total, insertions, modifications, suppressions (mis à jour dynamiquement)
- Rafraîchissement automatique à chaque basculement sur l'onglet

### Fenêtre 3 — Gestion des Comptes (`GestionPanel`)

- **Formulaire** : N° Compte, Nom Client, Solde
- **Boutons** : ➕ Ajouter, ✏ Modifier, 🗑 Supprimer, ✖ Vider
- **JTable** des comptes avec tri et filtre de recherche
- Clic sur une ligne → remplissage automatique du formulaire
- Confirmation avant suppression
- Validation des champs (non vide, solde numérique)
- Notification de l'`AuditPanel` après chaque opération

---

## 🔄 Flux d'exécution <a name="flux"></a>

```
Démarrage
    │
    ▼
DatabaseManager.initialize()
    ├── CREATE TABLE compte
    ├── CREATE TABLE audit_compte
    └── CREATE TRIGGER x3
    │
    ▼
LoginFrame → saisie utilisateur → Session.setUsername()
    │
    ▼
MainFrame (onglets)
    ├── GestionPanel
    │     ├── Ajouter  → CompteDAO.insert()  → TRIGGER INSERT → patchAudit()
    │     ├── Modifier → CompteDAO.update()  → TRIGGER UPDATE → patchAudit()
    │     └── Supprimer→ CompteDAO.delete()  → TRIGGER DELETE → patchAudit()
    │
    └── AuditPanel
          ├── AuditDAO.findAll()   → SELECT * FROM audit_compte
          ├── AuditDAO.countXxx()  → Statistiques dynamiques
          └── Filtre / Recherche   → RowFilter Swing
```

---

## 🎨 Interface utilisateur <a name="ui"></a>

L'application utilise un thème sombre cohérent :

| Élément | Couleur |
|---------|---------|
| Fond principal | `#0f172a` (bleu nuit) |
| Cartes / panneaux | `#1e2941` |
| Texte principal | `#f8fafc` |
| Texte secondaire | `#94a3b8` |
| Bouton / accent | `#3b82f6` (bleu vif) |
| INSERT | `#10b981` (vert) |
| UPDATE | `#3b82f6` (bleu) |
| DELETE | `#ef4444` (rouge) |

Les boutons utilisent un rendu personnalisé (`paintComponent`) avec effets de survol et de pression pour une expérience native améliorée.
