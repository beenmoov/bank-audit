#!/bin/bash
# ============================================================
#  build.sh — Compilation et lancement de BankAuditApp
#  Prérequis : JDK 17+, sqlite-jdbc.jar dans ./lib/
# ============================================================

set -e

# ── Couleurs terminal ────────────────────────────────────────
GREEN='\033[0;32m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   BankAuditApp — Build & Run Script      ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════╝${NC}"

# ── Vérification JDK ────────────────────────────────────────
if ! command -v javac &> /dev/null; then
    echo -e "${RED}[ERREUR] javac introuvable. Installez JDK 17+.${NC}"
    exit 1
fi

JAVA_VER=$(javac -version 2>&1 | awk '{print $2}' | cut -d'.' -f1)
if [ "$JAVA_VER" -lt 17 ]; then
    echo -e "${RED}[ERREUR] JDK 17 minimum requis (détecté : $JAVA_VER).${NC}"
    exit 1
fi
echo -e "${GREEN}[OK] JDK $JAVA_VER détecté.${NC}"

# ── Vérification du driver SQLite ──────────────────────────
SQLITE_JAR="lib/sqlite-jdbc.jar"
if [ ! -f "$SQLITE_JAR" ]; then
    echo -e "${RED}[ERREUR] $SQLITE_JAR introuvable.${NC}"
    echo "  Téléchargez-le depuis :"
    echo "  https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.3.0/sqlite-jdbc-3.45.3.0.jar"
    echo "  et placez-le dans le dossier lib/"
    exit 1
fi
echo -e "${GREEN}[OK] Driver SQLite trouvé.${NC}"

# ── Création des dossiers de sortie ────────────────────────
mkdir -p out

# ── Collecte des sources Java ──────────────────────────────
echo -e "${CYAN}[1/3] Collecte des sources...${NC}"
find src -name "*.java" > sources.txt
echo "      $(wc -l < sources.txt) fichiers .java trouvés."

# ── Compilation ────────────────────────────────────────────
echo -e "${CYAN}[2/3] Compilation...${NC}"
javac -encoding UTF-8 \
      -source 17 -target 17 \
      -cp "$SQLITE_JAR" \
      -d out \
      @sources.txt

rm -f sources.txt
echo -e "${GREEN}[OK] Compilation réussie.${NC}"

# ── Création du JAR exécutable ────────────────────────────
echo -e "${CYAN}[3/3] Création du JAR...${NC}"

# Manifest
cat > out/MANIFEST.MF << EOF
Manifest-Version: 1.0
Main-Class: bank.Main
Class-Path: ../lib/sqlite-jdbc.jar
EOF

jar cfm BankAuditApp.jar out/MANIFEST.MF -C out .
echo -e "${GREEN}[OK] BankAuditApp.jar créé.${NC}"

# ── Lancement ─────────────────────────────────────────────
echo ""
echo -e "${CYAN}══════════════════════════════════════════${NC}"
echo -e "${GREEN}  Lancement de l'application...${NC}"
echo -e "${CYAN}══════════════════════════════════════════${NC}"
echo ""

java -cp "BankAuditApp.jar:$SQLITE_JAR" bank.Main
