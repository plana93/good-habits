# Gradle e Android SDK Upgrade Summary

**Data**: 6 dicembre 2025  
**Motivo**: Risolvere conflitti AAR metadata con dipendenze AndroidX

## Problema Originale

Le dipendenze AndroidX più recenti richiedevano `compileSdk 34`, ma il progetto utilizzava:
- Android Gradle Plugin 7.4.2 (max supportato: SDK 33)
- compileSdk 33
- targetSdk 32

### Errori AAR Metadata

4 dipendenze causavano errori:
1. `androidx.emoji2:emoji2-views-helper:1.4.0` → richiede compileSdk 34+
2. `androidx.emoji2:emoji2:1.4.0` → richiede compileSdk 34+
3. `androidx.core:core:1.12.0` → richiede compileSdk 34+
4. `androidx.core:core-ktx:1.12.0` → richiede compileSdk 34+

## Modifiche Applicate

### 1. Gradle Wrapper (`gradle/wrapper/gradle-wrapper.properties`)
```diff
- distributionUrl=https\://services.gradle.org/distributions/gradle-7.5-bin.zip
+ distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip
```

**Rationale**: Android Gradle Plugin 8.x richiede Gradle 8.0+

### 2. Android Gradle Plugin (`build.gradle` root)
```diff
plugins {
-   id 'com.android.application' version '7.4.2' apply false
-   id 'com.android.library' version '7.4.2' apply false
+   id 'com.android.application' version '8.0.2' apply false
+   id 'com.android.library' version '8.0.2' apply false
-   id 'org.jetbrains.kotlin.android' version '1.7.20' apply false
+   id 'org.jetbrains.kotlin.android' version '1.9.20' apply false
}
```

**Rationale**: 
- AGP 8.0.2 supporta compileSdk 34 ed è compatibile con Android Studio corrente
- Kotlin 1.9.20 richiesto dalle dipendenze Vico Charts

### 3. Android SDK Versions (`app/build.gradle`)
```diff
android {
-   compileSdk 33
+   compileSdk 34

    defaultConfig {
        applicationId "com.programminghut.pose_detection"
        minSdk 24
-       targetSdk 32
+       targetSdk 34
        versionCode 1
        versionName "1.0"
        
+       // Room schema export configuration
+       kapt {
+           arguments {
+               arg("room.schemaLocation", "$projectDir/schemas")
+           }
+       }
    }
    
    composeOptions {
-       kotlinCompilerExtensionVersion '1.3.2'
+       kotlinCompilerExtensionVersion '1.5.4'
    }
```

**Rationale**: 
- `compileSdk 34`: Permette uso API Android 14
- `targetSdk 34`: Opt-in comportamento runtime Android 14
- `minSdk 24`: Invariato (Android 7.0+)
- Compose Compiler 1.5.4: Compatibile con Kotlin 1.9.20
- Room schema: Configurato per export schemi database

### 4. Gradle Properties (`gradle.properties`)
```diff
+ # Suppress compileSdk warning for AGP 8.0.2 with SDK 34
+ android.suppressUnsupportedCompileSdk=34
```

**Rationale**: Sopprime warning di AGP 8.0.2 con compileSdk 34

## Compatibilità

### Versioni Finali
| Componente | Vecchia | Nuova |
|------------|---------|-------|
| Gradle | 7.5 | 8.0 |
| Android Gradle Plugin | 7.4.2 | 8.0.2 |
| Kotlin | 1.7.20 | 1.9.20 |
| Compose Compiler | 1.3.2 | 1.5.4 |
| compileSdk | 33 | 34 |
| targetSdk | 32 | 34 |
| minSdk | 24 | 24 (invariato) |

### Device Compatibility
- **Minimo**: Android 7.0 (API 24) - invariato
- **Target**: Android 14 (API 34) - aggiornato
- **Build**: Android 14 SDK (API 34) - aggiornato

### Java/JDK Requirements
- AGP 8.x richiede **Java 17** minimum
- Android Studio Giraffe+ include Java 17 embedded
- Build da terminale: assicurarsi JAVA_HOME punti a JDK 17+

## Breaking Changes Potenziali

### AGP 8.x Changes
1. **Namespace obbligatorio**: Già presente in `build.gradle` (`namespace 'com.programminghut.pose_detection'`)
2. **BuildConfig default disabled**: Già abilitato (`android.defaults.buildfeatures.buildconfig=true`)
3. **Non-transitive R classes**: Già abilitato (`android.nonTransitiveRClass=true`)

### Android 14 (API 34) Behavior Changes
1. **Foreground Service Types**: Dichiarazioni già corrette nel manifest
2. **Runtime Permissions**: Gestione camera già implementata
3. **Storage Access**: Permission READ_MEDIA già specificate

## Testing Checklist

- [x] Sincronizzazione Gradle completata
- [ ] Build APK debug success
- [ ] Installazione su device fisico
- [ ] Test funzionalità:
  - [ ] Camera e pose detection
  - [ ] Database Room (sessioni/rep)
  - [ ] UI Compose (dashboard, storico)
  - [ ] Permission camera
  - [ ] Storage media (recording)

## Riferimenti

- [Android Gradle Plugin 8.0 Release Notes](https://developer.android.com/studio/releases/gradle-plugin#8-0-0)
- [Gradle 8.0 Release Notes](https://docs.gradle.org/8.0/release-notes.html)
- [Android 14 Behavior Changes](https://developer.android.com/about/versions/14/behavior-changes-14)
- [AGP/Gradle Compatibility Matrix](https://developer.android.com/studio/releases/gradle-plugin#updating-gradle)

## Note

- L'upgrade è **backward compatible** con device Android 7.0+
- AGP 8.0.2 è l'ultima versione compatibile con Android Studio corrente
- Il cambio `targetSdk 32 → 34` richiede test runtime su Android 14
- Build in Android Studio: utilizzare "Sync Project with Gradle Files"
- Build terminale: assicurarsi Java 17+ configurato
