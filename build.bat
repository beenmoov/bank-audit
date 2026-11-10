@echo off
:: ============================================================
::  build.bat — Compilation et lancement de BankAuditApp
::  Prérequis : JDK 17+, sqlite-jdbc.jar dans .\lib\
:: ============================================================

setlocal enabledelayedexpansion

echo ============================================
echo   BankAuditApp -- Build and Run Script
echo ============================================

:: -- Verification JDK --
where javac >nul 2>&1
if errorlevel 1 (
    echo [ERREUR] javac introuvable. Installez JDK 17+.
    echo Telechargez JDK : https://adoptium.net/
    pause & exit /b 1
)
echo [OK] javac detecte.

:: -- Verification driver SQLite --
if not exist "lib\sqlite-jdbc.jar" (
    echo [ERREUR] lib\sqlite-jdbc.jar introuvable.
    echo Telechargez-le ici :
    echo https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.3.0/sqlite-jdbc-3.45.3.0.jar
    echo Placez-le dans le dossier lib\
    pause & exit /b 1
)
echo [OK] Driver SQLite trouve.

:: -- Creation dossier sortie --
if not exist out mkdir out

:: -- Collecte sources --
echo [1/3] Collecte des sources Java...
dir /s /b src\*.java > sources.txt

:: -- Compilation --
echo [2/3] Compilation...
javac -encoding UTF-8 -source 17 -target 17 ^
      -cp "lib\sqlite-jdbc.jar" ^
      -d out ^
      @sources.txt

if errorlevel 1 (
    echo [ERREUR] Echec de la compilation.
    del sources.txt
    pause & exit /b 1
)
del sources.txt
echo [OK] Compilation reussie.

:: -- Creation JAR --
echo [3/3] Creation du JAR executable...

(
echo Manifest-Version: 1.0
echo Main-Class: bank.Main
echo Class-Path: ../lib/sqlite-jdbc.jar
) > out\MANIFEST.MF

jar cfm BankAuditApp.jar out\MANIFEST.MF -C out .
echo [OK] BankAuditApp.jar cree.

:: -- Lancement --
echo.
echo ============================================
echo   Lancement de l'application...
echo ============================================
echo.

java -cp "BankAuditApp.jar;lib\sqlite-jdbc.jar" bank.Main

pause
