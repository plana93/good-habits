#!/bin/bash

# Script per catturare l'output di Android Studio
# Uso: ./capture_build_output.sh

BUILD_LOG_DIR="/Users/mirco/AndroidStudioProjects/realtime_pose_detection_android-main/build_logs"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="$BUILD_LOG_DIR/build_output_$TIMESTAMP.log"

echo "🔧 CATTURA OUTPUT ANDROID STUDIO" > "$LOG_FILE"
echo "Timestamp: $(date)" >> "$LOG_FILE"
echo "=========================================" >> "$LOG_FILE"

# Monitora il file di log di Android Studio (se esiste)
AS_LOG_DIR="$HOME/Library/Logs/Google/AndroidStudio*"
AS_LOG_FILE=$(find $HOME/Library/Logs/Google -name "AndroidStudio*" -type d 2>/dev/null | head -1)

if [ -n "$AS_LOG_FILE" ]; then
    echo "📱 Android Studio Log Directory: $AS_LOG_FILE" >> "$LOG_FILE"
    
    # Copia i log più recenti
    find "$AS_LOG_FILE" -name "*.log" -mtime -1 -exec tail -n 100 {} \; >> "$LOG_FILE" 2>/dev/null
fi

# Monitora anche i log di Gradle
echo "" >> "$LOG_FILE"
echo "🔨 GRADLE BUILD LOGS:" >> "$LOG_FILE"
echo "=========================================" >> "$LOG_FILE"

# Esegue build e cattura output
cd "/Users/mirco/AndroidStudioProjects/realtime_pose_detection_android-main"
./gradlew assembleDebug --info --stacktrace >> "$LOG_FILE" 2>&1

echo "📁 Log salvato in: $LOG_FILE"
echo "📖 Per leggere: cat $LOG_FILE"
echo "🔍 Per errori: grep -i error $LOG_FILE"