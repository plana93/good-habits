# 🚀 Build & Deploy Guide - Good Habits

**Data**: 5 Dicembre 2025  
**Status**: Ready for testing

---

## ⚠️ Important Notes

The cleanup is COMPLETE, but building from terminal requires JAVA_HOME setup.  
**Recommended**: Use Android Studio for building and testing.

---

## 🏗️ Building the App

### Option 1: Android Studio (Recommended)

1. **Open Project**
   ```
   File → Open → Select project folder
   ```

2. **Sync Gradle**
   ```
   File → Sync Project with Gradle Files
   ```

3. **Clean Build**
   ```
   Build → Clean Project
   Build → Rebuild Project
   ```

4. **Run on Device**
   ```
   Run → Run 'app'
   ```

**Expected Result**: ✅ App builds successfully and launches

---

### Option 2: Terminal (If JAVA_HOME is configured)

```bash
cd /Users/mirco/AndroidStudioProjects/realtime_pose_detection_android-main

# Clean
./gradlew clean

# Build Debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

---

## 🧪 Testing Checklist

### Launch Test
- [ ] App launches without crashes
- [ ] Shows "GoodHabits" as app name
- [ ] Main screen loads (Habits activity)

### UI Test
- [ ] Only 2 buttons are shown:
  - [ ] "SQUAT COUNTER"
  - [ ] "RECORD SKELETON"
- [ ] No "URBAN CAMERA" button visible
- [ ] Buttons are clickable

### Squat Counter Test
- [ ] Select "SQUAT COUNTER"
- [ ] Camera selection screen appears
- [ ] Select camera (front/back)
- [ ] MainActivity loads
- [ ] Camera preview works
- [ ] Pose detection works
- [ ] Green borders appear when in position
- [ ] Squat counter increments
- [ ] Total squat persists after app restart

### Recording Mode Test
- [ ] Select "RECORD SKELETON"
- [ ] Camera selection screen appears
- [ ] Select camera
- [ ] MainActivity loads in recording mode
- [ ] Skeleton visualization works
- [ ] "Exit & Copy" button visible
- [ ] CSV export works

---

## 🐛 Expected Issues (None!)

After cleanup, there should be NO errors related to:
- ❌ Missing UrbanCamera classes
- ❌ Missing FilterManager
- ❌ Missing effects package
- ❌ Missing adapters
- ❌ Missing UI components
- ❌ Missing urban layouts

If you see any of these errors, it means cleanup was incomplete.

---

## 📊 Build Output Location

### Debug APK
```
app/build/outputs/apk/debug/app-debug.apk
```

### Release APK (if built)
```
app/build/outputs/apk/release/app-release.apk
```

---

## 🔧 Troubleshooting

### Issue: "Cannot resolve symbol UrbanCamera"
**Solution**: This shouldn't happen. If it does:
1. File → Invalidate Caches / Restart
2. Build → Clean Project
3. Build → Rebuild Project

### Issue: "Cannot find layout activity_urban_camera"
**Solution**: This shouldn't happen. Check that layout was actually deleted:
```bash
find app/src/main/res/layout -name "*urban*"
# Should return nothing
```

### Issue: "Activity not declared in manifest"
**Solution**: Check AndroidManifest.xml - should only have squat-related activities.

---

## 📱 Installation on Device

### Via Android Studio
1. Connect device via USB
2. Enable USB Debugging on device
3. Click "Run" button in Android Studio
4. Select your device from list

### Via APK
1. Build APK: `Build → Build Bundle(s) / APK(s) → Build APK(s)`
2. Copy APK to device
3. Install APK on device
4. Grant camera permission when prompted

---

## 🎯 Post-Build Steps

Once build is successful:

### 1. Git Commit
```bash
cd /Users/mirco/AndroidStudioProjects/realtime_pose_detection_android-main

git status
# Should show many deleted files and modified files

git add .

git commit -m "feat: split project - remove urban camera features

- Removed all Urban Camera activities (5 files)
- Removed effects system (40+ files)  
- Removed adapters and UI components
- Removed urban-related layouts and drawables
- Updated Habits.kt to show only 2 buttons
- Updated AndroidManifest.xml
- Changed app name to 'GoodHabits'
- Created comprehensive documentation in update_docs/
- Updated README.md for Good Habits brand

This project now focuses exclusively on fitness tracking (squat counter).
Urban Camera features have been moved to TheDrop repository.

See update_docs/00_PROJECT_SPLIT_OVERVIEW.md for details."
```

### 2. Push to Repository
```bash
git push origin master
```

### 3. Create Tag
```bash
git tag -a v2.0-good-habits -m "Version 2.0 - Good Habits (post-split)"
git push origin v2.0-good-habits
```

---

## 🔄 TheDrop Migration Verification

Before closing this phase, verify that TheDrop repository has:

- [ ] All Urban Camera activities
- [ ] Complete effects/ folder
- [ ] All adapters
- [ ] All UI components
- [ ] All urban layouts
- [ ] All urban documentation
- [ ] Working build configuration

---

## 📈 Performance Expectations

### Build Times
- **Clean Build**: 2-5 minutes
- **Incremental Build**: 30-60 seconds

### APK Size
- **Debug**: ~10-12 MB
- **Release (with ProGuard)**: ~8-10 MB

Should be **~4MB smaller** than before (no urban camera code).

---

## ✅ Success Criteria

The build is successful if:

1. ✅ No compilation errors
2. ✅ No missing class errors
3. ✅ No missing resource errors
4. ✅ App launches on device
5. ✅ Only 2 buttons shown
6. ✅ Squat counter works
7. ✅ Recording mode works
8. ✅ App name is "GoodHabits"

---

## 📞 If Build Fails

1. **Check logcat for errors**
   ```bash
   adb logcat | grep -i error
   ```

2. **Review build output**
   - Look for "error:" messages
   - Check which files are causing issues

3. **Verify cleanup was complete**
   ```bash
   # Should return nothing:
   grep -r "UrbanCamera" app/src/main/java/
   grep -r "FilterManager" app/src/main/java/
   ```

4. **Rollback if needed**
   ```bash
   git status
   git reset --hard HEAD
   # Or restore specific file:
   git checkout HEAD -- <file>
   ```

---

## 🎉 Ready to Build!

Everything is prepared. Open the project in Android Studio and click "Run"!

**Good luck! 🚀**

---

**Next Document**: See results in git commit message and TheDrop repo verification.
