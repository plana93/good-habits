# Changelog

All notable changes to Good Habits app will be documented in this file.

## [1.1.0] - 2026-01-19

### ✨ Added
- **83 new exercise templates** - Total library now includes 90 exercises
  - 20 LEGS exercises (lunges, glutes, calf raises, squats variations)
  - 18 UPPER BODY PUSH (push-ups variations, triceps, planks)
  - 10 UPPER BODY PULL (back, shoulders, core)
  - 25 CORE/ABS (crunches, planks, rotational exercises)
  - 10 CARDIO (jumps, high knees, burpees, skaters)
  - 14 STRETCHING (yoga poses, mobility exercises)
- Detailed Italian descriptions with biomechanics notes for each exercise
- Material Design icons for each exercise
- Realistic defaultReps/defaultTime values for each exercise

### 🎨 Changed
- **Squat always appears first** in exercise selection lists
- Improved exercise ordering (Squat priority, then alphabetical)
- Zero Dopamine design refinements (removed flame emojis from streak displays)

### 🔧 Technical
- Exercise templates loaded from JSON files in assets
- Optimized template loading with sorting in ExerciseTemplateFileManager
- Added architecture analysis documentation (ARCHITECTURE_ANALYSIS.md)
- Added migration plan documentation (MIGRATION_PLAN_JSON_ONLY.md)

### 📦 Files
- APK Size: 52MB (debug build)
- Min SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)

---

## [1.0.0] - 2026-01-XX

### Initial Release
- Basic exercise tracking with AI-powered squat detection
- Daily session management
- Wellness tracker integration
- Streak calendar
- Manual exercise logging
- 7 preset exercises (squat, push-up, burpee, jumping jacks, mountain climbers, plank, test)
