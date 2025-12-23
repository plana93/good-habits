#!/bin/bash

# Script per configurare Java 11 per Android Studio
echo "🔧 CONFIGURAZIONE JAVA 11 PER ANDROID STUDIO"
echo "============================================="

# Controlla se Homebrew è installato
if ! command -v brew &> /dev/null; then
    echo "❌ Homebrew non trovato. Installazione necessaria per Java 11."
    echo "💡 Installa Homebrew con:"
    echo '/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"'
    exit 1
fi

echo "📦 Installando OpenJDK 11 via Homebrew..."
brew install openjdk@11

echo "🔗 Configurando link simbolico..."
sudo ln -sfn /opt/homebrew/opt/openjdk@11/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-11.jdk

echo "⚙️  Configurando JAVA_HOME..."
JAVA_11_HOME="/opt/homebrew/opt/openjdk@11"

# Aggiunge la configurazione al profilo shell
SHELL_PROFILE=""
if [ -f ~/.zshrc ]; then
    SHELL_PROFILE=~/.zshrc
elif [ -f ~/.bash_profile ]; then
    SHELL_PROFILE=~/.bash_profile
else
    SHELL_PROFILE=~/.profile
fi

echo "" >> $SHELL_PROFILE
echo "# Java 11 Configuration for Android Studio" >> $SHELL_PROFILE
echo "export JAVA_HOME=$JAVA_11_HOME" >> $SHELL_PROFILE
echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> $SHELL_PROFILE

echo "✅ Configurazione completata!"
echo ""
echo "📋 PROSSIMI PASSI:"
echo "1. Riavvia il terminale o esegui: source $SHELL_PROFILE"
echo "2. Verifica con: java -version"
echo "3. In Android Studio: File → Project Structure → SDK Location → JDK Location: $JAVA_11_HOME"
echo ""
echo "🎯 Comandi di verifica:"
echo "java -version"
echo "echo \$JAVA_HOME"