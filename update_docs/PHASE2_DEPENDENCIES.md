# Phase 2 Dashboard - Dependencies Summary

**Data**: 6 dicembre 2025  
**Fase**: Phase 2 - Dashboard Core Implementation

## Configurazione Build Completa

### Build Tools
| Componente | Versione | Note |
|------------|----------|------|
| Gradle | 8.0 | Richiesto da AGP 8.x |
| Android Gradle Plugin | 8.0.2 | Ultima versione compatibile con Android Studio corrente |
| Kotlin | 1.9.20 | Richiesto da Vico Charts 1.13.1 |

### Android SDK
| Componente | Versione | Note |
|------------|----------|------|
| compileSdk | 34 | Android 14 API |
| targetSdk | 34 | Opt-in Android 14 runtime |
| minSdk | 24 | Android 7.0+ (invariato) |

### Core Dependencies

#### Jetpack Compose
| Libreria | Versione | Note |
|----------|----------|------|
| Compose BOM | 2022.10.00 | Material3 |
| Compose Compiler | 1.5.4 | Compatibile con Kotlin 1.9.20 |

#### Database & Persistence
| Libreria | Versione | Motivo Aggiornamento |
|----------|----------|----------------------|
| Room Runtime | 2.6.1 | Supporto Kotlin 1.9.20, fix kapt |
| Room KTX | 2.6.1 | Coroutines integration |
| Room Compiler (kapt) | 2.6.1 | Annotation processing |
| Gson | 2.10.1 | JSON serialization (TypeConverters) |

#### Coroutines
| Libreria | Versione | Motivo Aggiornamento |
|----------|----------|----------------------|
| kotlinx-coroutines-android | 1.7.3 | Compatibilità Kotlin 1.9.20 |
| kotlinx-coroutines-core | 1.7.3 | Flow support |

#### Charts & Visualization (NEW - Phase 2)
| Libreria | Versione | Utilizzo |
|----------|----------|----------|
| Vico Compose | 1.13.1 | Line charts, bar charts |
| Vico Compose M3 | 1.13.1 | Material3 theming |
| Vico Core | 1.13.1 | Core charting logic |

#### Lifecycle & ViewModel
| Libreria | Versione |
|----------|----------|
| lifecycle-viewmodel-ktx | 2.5.1 |
| lifecycle-livedata-ktx | 2.5.1 |
| lifecycle-runtime-ktx | 2.5.1 |
| lifecycle-viewmodel-compose | 2.5.1 |

#### Other AndroidX
| Libreria | Versione |
|----------|----------|
| core-ktx | 1.7.0 |
| appcompat | 1.5.1 |
| material | 1.7.0 |
| constraintlayout | 2.1.4 |

#### CameraX (Pose Detection)
| Libreria | Versione |
|----------|----------|
| camera-core | 1.2.3 |
| camera-camera2 | 1.2.3 |
| camera-lifecycle | 1.2.3 |
| camera-video | 1.2.3 |
| camera-view | 1.2.3 |

#### TensorFlow Lite
| Libreria | Versione |
|----------|----------|
| tensorflow-lite-support | 0.1.0 |
| tensorflow-lite-gpu | 2.3.0 |
| tensorflow-lite-gpu-api | 2.3.0 |
| tensorflow-lite-metadata | 0.1.0 |

## Catena di Dipendenze - Phase 2

```
Vico Charts 1.13.1 (richiede Kotlin 1.9.x)
    ↓
Kotlin 1.9.20
    ↓
├── Compose Compiler 1.5.4 (compatibilità Kotlin 1.9.20)
├── Room 2.6.1 (supporto Kotlin 1.9.20)
└── Coroutines 1.7.3 (compatibilità Kotlin 1.9.20)
```

## Breaking Changes Affrontati

### 1. Vico Charts → Kotlin 1.9.20
**Problema**: Vico compilato con Kotlin 1.9.0, progetto usava 1.7.20  
**Soluzione**: Upgrade Kotlin 1.7.20 → 1.9.20

### 2. Kotlin 1.9.20 → Compose Compiler
**Problema**: Compose Compiler 1.3.2 non compatibile con Kotlin 1.9.20  
**Soluzione**: Upgrade Compose Compiler 1.3.2 → 1.5.4

### 3. Kotlin 1.9.20 → Room
**Problema**: Room 2.5.2 kapt stub generation falliva con Kotlin 1.9.20  
**Errore**: `error: Not sure how to convert a Cursor to this method's return type`  
**Soluzione**: Upgrade Room 2.5.2 → 2.6.1

### 4. Kotlin 1.9.20 → Coroutines
**Problema**: Potenziali incompatibilità Coroutines 1.6.4  
**Soluzione**: Upgrade preventivo 1.6.4 → 1.7.3

## Gradle Properties Aggiunte

```properties
# Suppress compileSdk warning for AGP 8.0.2 with SDK 34
android.suppressUnsupportedCompileSdk=34
```

## Kapt Arguments (Room)

```groovy
kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}
```

## File di Configurazione Modificati

1. `build.gradle` (root) - AGP, Kotlin version
2. `app/build.gradle` - SDK versions, dependencies, Compose Compiler, kapt args
3. `gradle/wrapper/gradle-wrapper.properties` - Gradle 8.0
4. `gradle.properties` - suppressUnsupportedCompileSdk

## Testing Checklist

- [ ] `./gradlew clean` completato
- [ ] `./gradlew build` completato senza errori kapt
- [ ] Sync Gradle in Android Studio successful
- [ ] Build APK debug successful
- [ ] Installazione su device fisico
- [ ] Test Dashboard UI (KPI cards, charts)
- [ ] Test Session tracking (Phase 1 compatibility)
- [ ] Test Database queries (Room 2.6.1)

## Riferimenti

- [Kotlin 1.9.20 Release Notes](https://kotlinlang.org/docs/whatsnew1920.html)
- [Room 2.6.0 Release Notes](https://developer.android.com/jetpack/androidx/releases/room#2.6.0)
- [Compose-Kotlin Compatibility](https://developer.android.com/jetpack/androidx/releases/compose-kotlin)
- [Coroutines 1.7.3 Release](https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.7.3)
- [Vico Charts Documentation](https://github.com/patrykandpatrick/vico)
