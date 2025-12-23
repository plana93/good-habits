# CONFIGURAZIONE ANDROID STUDIO PER LOG AUTOMATICI
# ===================================================

## METODO 1: Build Output Redirect (Manuale)
# In Android Studio:
# 1. View → Tool Windows → Build
# 2. Durante il build, fai clic destro nella finestra Build
# 3. "Copy All" → Incolla in un file di testo

## METODO 2: Gradle Console Output
# In Android Studio:
# 1. File → Settings → Build → Gradle
# 2. Spunta "Use Gradle from: gradle-wrapper.properties file"
# 3. Command-line Options: --info --stacktrace
# 4. Build → Clean Project → Rebuild Project

## METODO 3: Script Automatico (Raccomandato)
# 
# ISTRUZIONI:
# 1. Apri Terminale in VS Code
# 2. Esegui: ./capture_build_output.sh
# 3. Poi compila in Android Studio
# 4. Il log sarà salvato automaticamente in build_logs/
#
# LETTURA DEI LOG:
# - Tutti i log: ls build_logs/
# - Ultimo log: cat build_logs/build_output_*.log | tail
# - Solo errori: grep -i "error\|exception" build_logs/build_output_*.log
# - Log in tempo reale: tail -f build_logs/live_build_output.log

## METODO 4: Android Studio IDE Log
# Posizione log Android Studio:
# - macOS: ~/Library/Logs/Google/AndroidStudio*/idea.log
# - Per vedere: tail -f ~/Library/Logs/Google/AndroidStudio*/idea.log

## TRUCCO RAPIDO:
# Crea un alias nel tuo ~/.zshrc:
# alias buildlog='tail -f /Users/mirco/AndroidStudioProjects/realtime_pose_detection_android-main/build_logs/live_build_output.log'
# Poi usa semplicemente: buildlog

## INTEGRAZIONE VSCODE:
# Aggiungi questa task in .vscode/tasks.json per leggere i log direttamente da VS Code:
# {
#     "label": "Read Android Studio Build Log",
#     "type": "shell", 
#     "command": "cat",
#     "args": ["build_logs/build_output_*.log"],
#     "group": "build",
#     "presentation": {
#         "echo": true,
#         "reveal": "always",
#         "panel": "new"
#     }
# }