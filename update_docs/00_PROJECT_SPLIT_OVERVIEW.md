# 📋 Project Split Overview - Good Habits & TheDrop

**Data**: 5 Dicembre 2025  
**Autore**: Mirco

## 🎯 Obiettivo

Dividere il progetto originale in due applicazioni separate:

1. **Good Habits** (questa repository) - Focus su fitness e squat tracking
2. **TheDrop** (nuova repository) - Focus su urban camera ed effetti creativi

---

## 🏗️ Architettura Originale

Il progetto originale conteneva tre funzionalità principali accessibili dalla schermata `Habits.kt`:

```
┌─────────────────────────────────┐
│         Habits Screen           │
├─────────────────────────────────┤
│  1. Squat Counter               │ → MainActivity + SquatCounter
│  2. Track Skeleton Points       │ → Recording Mode
│  3. Urban Camera                │ → UrbanCamera* Activities
└─────────────────────────────────┘
```

---

## 🔄 Split Strategy

### Good Habits (Current Repo)
**Repository**: `good-habits`  
**Package**: `com.programminghut.pose_detection` → `com.goodhabits.fitness`  
**App Name**: `GoodHabits`

**Funzionalità Mantenute**:
- ✅ Squat Counter con persistenza
- ✅ Track Skeleton Points (recording mode)
- ✅ Pose detection base (MoveNet)
- ✅ Camera selection

**Funzionalità Rimosse**:
- ❌ Urban Camera Activities (tutte le varianti)
- ❌ Filter system (FilterManager, AdaptiveFilter, etc.)
- ❌ Urban effects (effects/ folder completo)
- ❌ Media upload & playback
- ❌ Video export con filtri

---

### TheDrop (New Repo)
**Repository**: `TheDrop` (già creata)  
**Package**: `com.thedrop.urban`  
**App Name**: `TheDrop`

**Funzionalità Migrate**:
- ✅ Urban Camera (tutte le varianti)
- ✅ Filter system completo
- ✅ Effects engine
- ✅ Media upload & playback
- ✅ Video export
- ✅ Filter parameters UI

---

## 📦 Files to Remove from Good Habits

### Activities (7 files)
```
app/src/main/java/com/programminghut/pose_detection/
├── UrbanCameraActivity.kt                  ❌
├── UrbanCameraActivityNew.kt               ❌
├── UrbanCameraActivityRefactored.kt        ❌
├── UrbanCameraSelectionActivity.kt         ❌
└── MediaUploadPreviewActivity.kt           ❌
```

### Effects System (entire folder)
```
app/src/main/java/com/programminghut/pose_detection/effects/
├── FilterManager.kt                        ❌
├── AdaptiveFilter.kt                       ❌
├── FilterConfig.kt                         ❌
├── SkeletonFilter.kt                       ❌
├── GlowDotsFilter.kt                       ❌
├── ColorAdjustmentFilter.kt                ❌
├── BlurFilter.kt                           ❌
├── RandomProvider.kt                       ❌
├── FrameClock.kt                           ❌
├── ConnectedLineCenterSobel.kt             ❌
├── UrbanEffectsManager.kt                  ❌
└── ... (tutte le altre config)             ❌
```

### Adapters
```
app/src/main/java/com/programminghut/pose_detection/adapters/
├── AvailableFiltersAdapter.kt              ❌
├── ActiveFiltersAdapter.kt                 ❌
```

### UI Components
```
app/src/main/java/com/programminghut/pose_detection/ui/
├── FilterParamsBottomSheet.kt              ❌
```

### Layouts (10+ files)
```
app/src/main/res/layout/
├── activity_urban_camera.xml               ❌
├── activity_urban_camera_new.xml           ❌
├── activity_urban_camera_simple.xml        ❌
├── activity_urban_camera_refactored.xml    ❌
├── dialog_photo_preview.xml                ❌
├── filter_item.xml                         ❌
├── active_filter_item.xml                  ❌
├── bottom_sheet_filter_params.xml          ❌
├── activity_media_upload_preview.xml       ❌
```

### Drawables
```
app/src/main/res/drawable/
├── ic_urban_boxes.xml                      ❌
└── ... (urban-related icons)               ❌
```

### Documentation
```
root/
├── URBAN_CAMERA_GUIDE.md                   ❌
├── URBAN_CAMERA_REDESIGN.md                ❌
├── NUOVO_FLUSSO.md                         ❌
├── VIDEO_EXPORT_FIX_SUMMARY.md             ❌
├── EXPORT_DIAGNOSIS.md                     ❌
├── IMPLEMENTAZIONE_RECORDING.md            (keep - squat related)
└── SQUAT_COUNTER_GUIDE.md                  (keep - squat related)
```

---

## 📦 Files to Keep in Good Habits

### Core Activities
```
app/src/main/java/com/programminghut/pose_detection/
├── Habits.kt                               ✅ (modified - remove Urban button)
├── MainActivity.kt                         ✅ (squat counter)
├── CameraSelectionActivity.kt              ✅ (for squat)
├── RecordingCameraSelectionActivity.kt     ✅ (skeleton tracking)
```

### Support Classes
```
app/src/main/java/com/programminghut/pose_detection/
├── SquatCounter.kt                         ✅
├── PoseLogger.kt                           ✅
├── CameraAspectRatioHelper.kt              ✅
```

### Layouts
```
app/src/main/res/layout/
├── activity_main.xml                       ✅ (squat counter UI)
├── activity_habits.xml                     ✅
└── ... (squat-related layouts)             ✅
```

---

## 🔧 Modifications Required

### 1. Habits.kt
- ❌ Remove "URBAN CAMERA" button
- ✅ Keep only "SQUAT COUNTER" and "TRACK SKELETON POINTS"

### 2. AndroidManifest.xml
- ❌ Remove Urban Camera activities
- ❌ Remove MediaUploadPreviewActivity
- ✅ Keep MainActivity, CameraSelectionActivity, RecordingCameraSelectionActivity

### 3. build.gradle
- Review dependencies (keep only what's needed for pose detection & squat)
- ❌ Remove CameraX if only used for urban camera

### 4. strings.xml
- Update `app_name` to "GoodHabits"
- Remove urban-related strings

---

## 📊 Statistics

| Metric | Before | After Good Habits | Migrated to TheDrop |
|--------|--------|-------------------|---------------------|
| Activities | 9 | 4 | 5 |
| Kotlin Files | ~60 | ~15 | ~45 |
| Layout XMLs | ~25 | ~8 | ~17 |
| Dependencies | Full | Minimal | Full |

---

## ✅ Success Criteria

### Good Habits
- [x] App builds successfully
- [ ] Squat counter works
- [ ] Recording mode works
- [ ] No urban camera references
- [ ] Clean git history

### TheDrop
- [ ] All urban features migrated
- [ ] Filter system intact
- [ ] Video export working
- [ ] Independent from Good Habits

---

## 🚀 Next Steps

1. ✅ Create `update_docs/` folder
2. ✅ Document project split strategy
3. ⏳ Remove urban camera files
4. ⏳ Update AndroidManifest.xml
5. ⏳ Update Habits.kt
6. ⏳ Update app name & package
7. ⏳ Test build
8. ⏳ Commit changes to git
9. ⏳ Verify TheDrop has everything needed

---

## 📝 Notes

- Keep ML model (MoveNet) in both projects
- Camera2 API used by both projects
- Consider shared library for common pose detection code in future

