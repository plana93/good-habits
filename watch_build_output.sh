#!/bin/bash

# Watcher che monitora automaticamente i cambiamenti nei file di build
# Avvia questo script in background mentre lavori con Android Studio

BUILD_LOG_DIR="/Users/mirco/AndroidStudioProjects/realtime_pose_detection_android-main/build_logs"
WATCH_FILE="$BUILD_LOG_DIR/live_build_output.log"

echo "🔄 WATCHER ATTIVO - Monitoraggio build Android Studio" > "$WATCH_FILE"
echo "Avviato: $(date)" >> "$WATCH_FILE"
echo "=========================================" >> "$WATCH_FILE"

# Funzione per processare i log
process_logs() {
    echo "" >> "$WATCH_FILE"
    echo "📅 $(date): Nuovo output rilevato" >> "$WATCH_FILE"
    echo "----------------------------------------" >> "$WATCH_FILE"
    
    # Cattura errori di compilazione Kotlin
    find . -name "*.log" -newer "$WATCH_FILE" -exec grep -H "error\|Error\|ERROR" {} \; >> "$WATCH_FILE" 2>/dev/null
    
    # Cattura output build recente
    find ./app/build -name "*.log" -newer "$WATCH_FILE" -exec tail -n 20 {} \; >> "$WATCH_FILE" 2>/dev/null
}

echo "👀 Watcher avviato. Log in tempo reale: $WATCH_FILE"
echo "💡 Usa: tail -f $WATCH_FILE (per vedere in tempo reale)"
echo "🛑 Ctrl+C per fermare"

# Monitora cambiamenti nella directory di build
fswatch -o ./app/build 2>/dev/null | while read num
do
    process_logs
done &

# Backup: monitora ogni 30 secondi
while true; do
    sleep 30
    process_logs
done