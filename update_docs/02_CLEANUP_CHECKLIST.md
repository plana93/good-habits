# 🗑️ Cleanup Checklist - Good Habits

**Data**: 5 Dicembre 2025  
**Obiettivo**: Rimuovere tutto il codice relativo a Urban Camera

---

## ✅ Step-by-Step Cleanup Process

### Phase 1: Activity Files Removal
- [ ] UrbanCameraActivity.kt
- [ ] UrbanCameraActivityNew.kt
- [ ] UrbanCameraActivityRefactored.kt
- [ ] UrbanCameraSelectionActivity.kt
- [ ] MediaUploadPreviewActivity.kt

### Phase 2: Effects System Removal
- [ ] effects/ folder completo (~40 files)
  - [ ] FilterManager.kt
  - [ ] AdaptiveFilter.kt
  - [ ] SkeletonFilter.kt
  - [ ] GlowDotsFilter.kt
  - [ ] ColorAdjustmentFilter.kt
  - [ ] BlurFilter.kt
  - [ ] ConnectedLineCenterSobel.kt
  - [ ] UrbanEffectsManager.kt
  - [ ] RandomProvider.kt
  - [ ] FrameClock.kt
  - [ ] Tutte le config classes

### Phase 3: Adapters & UI Components
- [ ] adapters/AvailableFiltersAdapter.kt
- [ ] adapters/ActiveFiltersAdapter.kt
- [ ] ui/FilterParamsBottomSheet.kt

### Phase 4: Layout Files
- [ ] activity_urban_camera.xml
- [ ] activity_urban_camera_new.xml
- [ ] activity_urban_camera_simple.xml
- [ ] activity_urban_camera_refactored.xml
- [ ] dialog_photo_preview.xml
- [ ] filter_item.xml
- [ ] active_filter_item.xml
- [ ] bottom_sheet_filter_params.xml
- [ ] activity_media_upload_preview.xml

### Phase 5: Drawable Resources
- [ ] ic_urban_boxes.xml
- [ ] Altri drawable urban-related (se presenti)

### Phase 6: Documentation
- [ ] URBAN_CAMERA_GUIDE.md
- [ ] URBAN_CAMERA_REDESIGN.md
- [ ] NUOVO_FLUSSO.md
- [ ] VIDEO_EXPORT_FIX_SUMMARY.md
- [ ] EXPORT_DIAGNOSIS.md

### Phase 7: Code Modifications
- [ ] Habits.kt - Remove Urban Camera button
- [ ] AndroidManifest.xml - Remove Urban activities
- [ ] strings.xml - Update app_name to "GoodHabits"
- [ ] build.gradle - Review dependencies (optional)

### Phase 8: Testing
- [ ] App builds successfully
- [ ] Squat counter works
- [ ] Recording mode works
- [ ] No urban camera imports errors
- [ ] No missing resource errors

### Phase 9: Git Commit
- [ ] Stage all changes
- [ ] Commit with message: "feat: split project - remove urban camera features"
- [ ] Push to good-habits repo

---

## 📋 Files to Remove (Complete List)

### Kotlin Files (Activities)
```
app/src/main/java/com/programminghut/pose_detection/
├── UrbanCameraActivity.kt
├── UrbanCameraActivityNew.kt
├── UrbanCameraActivityRefactored.kt
├── UrbanCameraSelectionActivity.kt
└── MediaUploadPreviewActivity.kt
```

### Kotlin Files (Effects - Complete Folder)
```
app/src/main/java/com/programminghut/pose_detection/effects/
├── AdaptiveFilter.kt
├── AdaptiveFilterConfig.kt
├── BlackWhiteConfig.kt
├── BlurFilter.kt
├── BlurFilterConfig.kt
├── ColorAdjustmentFilter.kt
├── ColorAdjustmentFilterConfig.kt
├── ConnectedLineCenterSobel.kt
├── FilterConfig.kt
├── FilterManager.kt
├── FrameClock.kt
├── GlowDotsFilter.kt
├── GlowDotsFilterConfig.kt
├── PixelatedConfig.kt
├── RandomProvider.kt
├── SkeletonFilter.kt
├── SkeletonFilterConfig.kt
├── SobelConfig.kt
├── UrbanConfig.kt
├── UrbanEffectsManager.kt
└── ... (altri file nella cartella)
```

### Kotlin Files (Adapters)
```
app/src/main/java/com/programminghut/pose_detection/adapters/
├── AvailableFiltersAdapter.kt
├── ActiveFiltersAdapter.kt
```

### Kotlin Files (UI)
```
app/src/main/java/com/programminghut/pose_detection/ui/
├── FilterParamsBottomSheet.kt
```

### Layout Files
```
app/src/main/res/layout/
├── activity_urban_camera.xml
├── activity_urban_camera_new.xml
├── activity_urban_camera_simple.xml
├── activity_urban_camera_refactored.xml
├── dialog_photo_preview.xml
├── filter_item.xml
├── active_filter_item.xml
├── bottom_sheet_filter_params.xml
├── activity_media_upload_preview.xml
```

### Drawable Files
```
app/src/main/res/drawable/
├── ic_urban_boxes.xml
```

### Documentation Files
```
root/
├── URBAN_CAMERA_GUIDE.md
├── URBAN_CAMERA_REDESIGN.md
├── NUOVO_FLUSSO.md
├── VIDEO_EXPORT_FIX_SUMMARY.md
├── EXPORT_DIAGNOSIS.md
```

---

## ⚠️ Files to Modify (DO NOT DELETE)

### 1. Habits.kt
**Location**: `app/src/main/java/com/programminghut/pose_detection/Habits.kt`

**Modification**: Remove Urban Camera button (around line 71-78)

**Before**:
```kotlin
// Bottone URBAN CAMERA (flusso urban street art)
CustomButton(
    text = "URBAN CAMERA",
    onClick = {
        val intent = Intent(this@Habits, UrbanCameraSelectionActivity::class.java)
        startActivity(intent)
    },
)
```

**After**: DELETE the entire button block

---

### 2. AndroidManifest.xml
**Location**: `app/src/main/AndroidManifest.xml`

**Modifications**: Remove these activity declarations

```xml
<!-- Remove these: -->
<activity
    android:name=".UrbanCameraSelectionActivity"
    android:exported="false" />
<activity
    android:name=".UrbanCameraActivity"
    android:exported="false" />
<activity
    android:name=".UrbanCameraActivityNew"
    android:exported="false"
    android:screenOrientation="portrait" />
<activity
    android:name=".UrbanCameraActivityRefactored"
    android:exported="false"
    android:screenOrientation="portrait" />
<activity
    android:name=".MediaUploadPreviewActivity"
    android:exported="false" />
```

---

### 3. strings.xml
**Location**: `app/src/main/res/values/strings.xml`

**Modification**: Change app name

**Before**:
```xml
<string name="app_name">pose_detection</string>
```

**After**:
```xml
<string name="app_name">GoodHabits</string>
```

---

### 4. build.gradle (Optional)
**Location**: `app/build.gradle`

**Review**: Check if CameraX dependencies are only used by Urban Camera

If CameraX is only used by Urban Camera, you can optionally remove:
```groovy
// Optional cleanup (only if not used elsewhere)
implementation 'androidx.camera:camera-core:1.2.3'
implementation 'androidx.camera:camera-camera2:1.2.3'
implementation 'androidx.camera:camera-lifecycle:1.2.3'
implementation 'androidx.camera:camera-video:1.2.3'
implementation 'androidx.camera:camera-view:1.2.3'
```

**Note**: Keep Camera2 API dependencies as they're used by MainActivity

---

## 🧹 Cleanup Commands

### Terminal Commands to Remove Files

```bash
# Navigate to project root
cd /Users/mirco/AndroidStudioProjects/realtime_pose_detection_android-main

# Remove Activity files
rm app/src/main/java/com/programminghut/pose_detection/UrbanCameraActivity.kt
rm app/src/main/java/com/programminghut/pose_detection/UrbanCameraActivityNew.kt
rm app/src/main/java/com/programminghut/pose_detection/UrbanCameraActivityRefactored.kt
rm app/src/main/java/com/programminghut/pose_detection/UrbanCameraSelectionActivity.kt
rm app/src/main/java/com/programminghut/pose_detection/MediaUploadPreviewActivity.kt

# Remove entire effects folder
rm -rf app/src/main/java/com/programminghut/pose_detection/effects/

# Remove adapters
rm -rf app/src/main/java/com/programminghut/pose_detection/adapters/

# Remove UI components
rm -rf app/src/main/java/com/programminghut/pose_detection/ui/

# Remove layout files
rm app/src/main/res/layout/activity_urban_camera*.xml
rm app/src/main/res/layout/dialog_photo_preview.xml
rm app/src/main/res/layout/filter_item.xml
rm app/src/main/res/layout/active_filter_item.xml
rm app/src/main/res/layout/bottom_sheet_filter_params.xml
rm app/src/main/res/layout/activity_media_upload_preview.xml

# Remove drawable
rm app/src/main/res/drawable/ic_urban_boxes.xml

# Remove documentation
rm URBAN_CAMERA_GUIDE.md
rm URBAN_CAMERA_REDESIGN.md
rm NUOVO_FLUSSO.md
rm VIDEO_EXPORT_FIX_SUMMARY.md
rm EXPORT_DIAGNOSIS.md
```

---

## ✅ Verification Steps

### 1. Build Check
```bash
./gradlew clean
./gradlew build
```

Expected: **Success (no errors)**

### 2. Import Check
Search for remaining urban camera imports:
```bash
grep -r "UrbanCamera" app/src/main/java/
grep -r "FilterManager" app/src/main/java/
grep -r "effects\." app/src/main/java/
```

Expected: **No results** (except in comments/docs)

### 3. Resource Check
```bash
grep -r "urban" app/src/main/res/
```

Expected: **Minimal or no results**

### 4. Manifest Check
Open AndroidManifest.xml and verify:
- No Urban* activities
- No MediaUploadPreviewActivity

### 5. Functional Test
- Launch app
- Should see only 2 buttons in Habits screen
- Squat counter should work
- Recording mode should work

---

## 🐛 Common Issues & Solutions

### Issue 1: Build Fails with "Cannot resolve FilterManager"
**Solution**: Search for any remaining references to `FilterManager` or `effects` package and remove them

### Issue 2: Missing Layout Resource
**Solution**: Check if any kept activity references deleted layouts, update them

### Issue 3: App Crashes on Launch
**Solution**: Check logcat for missing class references, verify AndroidManifest.xml

---

## 📦 Next Steps After Cleanup

1. Test app thoroughly
2. Update README.md with new app description
3. Create release tag: `v2.0-good-habits-only`
4. Commit and push to git
5. Verify TheDrop repo has all urban camera files

---

## 📞 Rollback Plan

If something goes wrong, you can restore from git:
```bash
git checkout HEAD -- <file-to-restore>
# Or restore all:
git reset --hard HEAD
```

