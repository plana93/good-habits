# Kotlin Version Upgrade - Build Fix

**Data**: 6 dicembre 2025  
**Motivo**: Risolvere incompatibilità Kotlin tra progetto (1.7.20) e dipendenze Vico Charts (1.9.20)

## Problema

Durante la build, sono apparsi errori di incompatibilità versione Kotlin:

```
Module was compiled with an incompatible version of Kotlin. 
The binary version of its metadata is 1.9.0, expected version is 1.7.1.
```

Le librerie Vico Charts (1.13.1) sono compilate con Kotlin 1.9.0, mentre il progetto utilizzava Kotlin 1.7.20.

## Soluzione Applicata

### 1. Aggiornamento Kotlin
**File**: `build.gradle` (root)
```diff
plugins {
    id 'com.android.application' version '8.0.2' apply false
    id 'com.android.library' version '8.0.2' apply false
-   id 'org.jetbrains.kotlin.android' version '1.7.20' apply false
+   id 'org.jetbrains.kotlin.android' version '1.9.20' apply false
}
```

### 2. Aggiornamento Compose Compiler
**File**: `app/build.gradle`
```diff
composeOptions {
-   kotlinCompilerExtensionVersion '1.3.2'
+   kotlinCompilerExtensionVersion '1.5.4'
}
```

**Rationale**: Compose Compiler 1.5.4 è compatibile con Kotlin 1.9.20
- [Compatibility Matrix](https://developer.android.com/jetpack/androidx/releases/compose-kotlin)

### 3. Aggiornamento Room Database
**File**: `app/build.gradle`
```diff
- def room_version = "2.5.2"
+ def room_version = "2.6.1"
```

**Rationale**: Room 2.6.1 supporta Kotlin 1.9.20 e risolve problemi di kapt stub generation

### 4. Aggiornamento Coroutines
**File**: `app/build.gradle`
```diff
- implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4'
- implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4'
+ implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
+ implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'
```

**Rationale**: Coroutines 1.7.3 è compatibile con Kotlin 1.9.20

### 5. Configurazione Room Schema Export
**File**: `app/build.gradle`
```diff
defaultConfig {
    ...
+   kapt {
+       arguments {
+           arg("room.schemaLocation", "$projectDir/schemas")
+       }
+   }
}
```

**Rationale**: Risolve warning Room "Schema export directory is not provided"

### 4. Soppressione Warning compileSdk
**File**: `gradle.properties`
```diff
+ android.suppressUnsupportedCompileSdk=34
```

**Rationale**: AGP 8.0.2 è testato fino a SDK 33, ma supporta SDK 34

## Compatibilità Kotlin/Compose

| Kotlin Version | Compose Compiler | Room | Coroutines |
|---------------|------------------|------|------------|
| 1.7.20 | 1.3.2 | 2.5.2 | 1.6.4 |
| 1.9.20 | 1.5.4 | 2.6.1 | 1.7.3 |

## Dipendenze Aggiornate

### Richieste da Vico Charts (Kotlin 1.9.x):
- `com.patrykandpatrick.vico:compose:1.13.1`
- `com.patrykandpatrick.vico:compose-m3:1.13.1`
- `com.patrykandpatrick.vico:core:1.13.1`

### Aggiornate per compatibilità:
- `androidx.room:room-*:2.6.1` (da 2.5.2)
- `kotlinx-coroutines-*:1.7.3` (da 1.6.4)

## Verifica Post-Upgrade

- [x] Kotlin 1.9.20 installato
- [x] Compose Compiler 1.5.4 configurato
- [x] Room 2.6.1 aggiornato (risolve kapt stub generation)
- [x] Coroutines 1.7.3 aggiornato
- [x] Room schema location configurato
- [x] Warning compileSdk soppresso
- [ ] Build completato con successo
- [ ] Test su device

## Riferimenti

- [Kotlin 1.9.20 Release](https://kotlinlang.org/docs/whatsnew1920.html)
- [Compose Compiler Compatibility](https://developer.android.com/jetpack/androidx/releases/compose-kotlin)
- [Vico Charts Documentation](https://github.com/patrykandpatrick/vico)
