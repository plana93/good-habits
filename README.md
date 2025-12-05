# 🏋️ Good Habits - AI Fitness Tracker# Pose Detection App



**Personal AI fitness assistant using pose detection for automatic squat counting**This Android application demonstrates real-time pose detection using the Movenet Singlepose Lightning model. The app captures video from the device's camera, processes the frames through the pose detection model, and overlays keypoints and connections on the camera preview. Additionally, it displays emojis corresponding to key facial and body points.



---## Getting Started



## 📱 AboutBefore running the app, ensure that the necessary camera permissions are granted. The app will request camera permissions if not already granted.



**Good Habits** is an Android fitness app that uses AI-powered pose detection to automatically count your squat repetitions. No wearables, no manual counting - just your phone's camera and machine learning magic!## Dependencies



The app uses Google's **MoveNet** model (TensorFlow Lite) to track 17 body keypoints in real-time, providing accurate squat detection and persistent workout tracking.- TensorFlow Lite: The app uses the TensorFlow Lite library for running the Movenet Singlepose Lightning model.

- Movenet Singlepose Lightning model: The pose detection model is included in the `ml` package as `LiteModelMovenetSingleposeLightningTfliteFloat164`.

---

## Usage

## ✨ Features

1. Launch the app on an Android device.

### 🔢 Automatic Squat Counter2. Grant camera permissions if prompted.

- **Real-time pose detection** using MoveNet AI model3. The camera preview will show keypoints and connections based on the detected pose in real-time.

- **Automatic repetition counting** - no manual input needed4. Emojis corresponding to key facial and body points will be overlaid on the camera preview.

- **Visual feedback** with color-coded borders (red/yellow/green)5. The app also displays the current count of repetitions detected.

- **Persistent data** - total squats saved across sessions

- **Smooth animations** with temporal smoothing (reduces flickering)## Features

- **Dual counter display**: session count + lifetime total

- Real-time pose detection: The app uses the Movenet Singlepose Lightning model to detect the user's pose.

### 🎬 Skeleton Recording Mode- Keypoints and connections: The detected keypoints and connections are visualized on the camera preview.

- **Export pose data** to CSV for analysis- Emoji overlay: Emojis corresponding to key facial and body points are overlaid on the camera preview.

- **17 keypoint tracking** with confidence scores- Repetition counter: The app counts and displays the number of repetitions detected.

- **Timestamp logging** for every frame

- **Debug and research** capabilities## Code Structure



### 📊 Smart Detection AlgorithmThe main functionality of the app is implemented in the `MainActivity.kt` file. The key components include:

- **Adaptive calibration** based on your body proportions

- **Position validation** (standing → squat → standing)- **Camera Initialization**: The camera is initialized using the Camera2 API.

- **Foot parallelism check** for proper form- **Pose Detection**: The Movenet Singlepose Lightning model is used to detect keypoints and connections in each video frame.

- **75% keypoint threshold** (reduces false negatives)- **Visualization**: The detected keypoints, connections, and emojis are overlaid on the camera preview.

- **Repetition Counter**: The app counts and displays the number of repetitions detected.

---

## Cleanup

## 🎯 How It Works

Ensure to release the resources and close the model when the app is destroyed.

1. **Position Detection Phase**

   - Stand in front of camera```kotlin

   - Wait for green borders (8 stable frames)override fun onDestroy() {

   - App calibrates to your body proportions    super.onDestroy()

    model.close()

2. **Squat Counting Phase**}

   - Perform squats normally```

   - Counter increments automatically

   - Visual feedback on form quality## Notes



3. **Data Persistence**- The app uses TensorFlow Lite for efficient on-device machine learning inference.

   - Session count resets each launch- Adjust the confidence threshold (`score > 0.45`) for keypoints to control the visibility of keypoints and connections.

   - Total count accumulates forever

   - Data saved automaticallyFeel free to explore and modify the code to suit your needs. Happy coding!

---

## 🚀 Installation

### Prerequisites
- Android 7.0 (API 24) or higher
- Camera permission
- ~150MB free RAM

### Build from Source
```bash
# Clone repository
git clone https://github.com/plana93/good-habits.git

# Open in Android Studio
cd good-habits
# Open with Android Studio → Run on device
```

---

## 🎮 Usage

### Basic Workout
1. Launch app → Select "SQUAT COUNTER"
2. Choose camera (front/back)
3. Position yourself in frame
4. Wait for green borders
5. Start squatting!

### Recording Mode (Advanced)
1. Launch app → Select "RECORD SKELETON"
2. Choose camera
3. Perform movements
4. Export CSV data via "Exit & Copy" button

---

## 🧠 Technology Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + XML Layouts
- **ML Model**: MoveNet SinglePose Lightning (TFLite)
- **Camera**: Android Camera2 API
- **Storage**: SharedPreferences

### Model Specs
- **Input**: 192x192 RGB image
- **Output**: 17 keypoints (x, y, confidence)
- **Inference**: ~30-50ms on modern devices
- **Size**: 4.8 MB

---

## 📐 Architecture

```
┌─────────────────────────────────┐
│      Habits Activity            │
│   (Main Menu - Compose)         │
└───────────┬─────────────────────┘
            │
            ├─> Squat::class
            │   └─> CameraSelectionActivity
            │       └─> MainActivity
            │           ├─> MoveNet Model
            │           ├─> SquatCounter
            │           └─> Pose Detection
            │
            └─> RecordingCameraSelectionActivity
                └─> MainActivity (recording mode)
                    └─> PoseLogger (CSV export)
```

---

## 📊 Performance

| Metric | Value |
|--------|-------|
| FPS | 30 (modern devices) |
| Inference Time | 30-50ms |
| Memory Usage | ~150MB |
| Battery Drain | ~40% per 30min |
| Accuracy | ~95% squat detection |

---

## 🛠️ Development

### Project Structure
```
app/
├── src/main/
│   ├── java/com/programminghut/pose_detection/
│   │   ├── Habits.kt                          (Main menu)
│   │   ├── MainActivity.kt                    (Squat counter)
│   │   ├── CameraSelectionActivity.kt         (Camera picker)
│   │   ├── RecordingCameraSelectionActivity.kt
│   │   ├── SquatCounter.kt                    (Persistence)
│   │   ├── PoseLogger.kt                      (CSV export)
│   │   └── CameraAspectRatioHelper.kt
│   │
│   ├── res/
│   │   ├── layout/                            (XML layouts)
│   │   ├── values/                            (strings, colors)
│   │   └── drawable/                          (icons, images)
│   │
│   └── ml/
│       └── lite_model_movenet_*.tflite        (AI model)
│
└── build.gradle                               (Dependencies)
```

### Building from Source
```bash
./gradlew clean
./gradlew assembleDebug

# Or in Android Studio:
# Build → Build Bundle(s) / APK(s) → Build APK(s)
```

---

## 📚 Documentation

Detailed documentation available in `update_docs/`:
- [Project Split Overview](update_docs/00_PROJECT_SPLIT_OVERVIEW.md) - Why we split the project
- [App Description](update_docs/01_GOOD_HABITS_APP_DESCRIPTION.md) - Detailed feature documentation
- [Cleanup Checklist](update_docs/02_CLEANUP_CHECKLIST.md) - Files removed during split
- [Changelog](update_docs/03_CHANGELOG.md) - Complete change history

Legacy documentation (for reference):
- [Squat Counter Guide](SQUAT_COUNTER_GUIDE.md)
- [Recording Implementation](IMPLEMENTAZIONE_RECORDING.md)

---

## 🔀 Project History

This project was originally a combined app with **Urban Camera** features (creative pose-based filters and effects). On December 5, 2025, the project was split into two separate apps:

- **Good Habits** (this repo) - Focus on fitness and squat tracking
- **TheDrop** (separate repo) - Focus on creative urban camera effects

See [Project Split Overview](update_docs/00_PROJECT_SPLIT_OVERVIEW.md) for details.

---

## 🚀 Roadmap & Future Features

We have an exciting roadmap ahead! See our detailed [Feature Roadmap](update_docs/ROADMAP_FEATURES.md) for:

### Coming Soon (Q1-Q2 2026)
- 📊 **Session Management** - Track and save workout sessions
- 📈 **Rich Dashboard** - Visual analytics with charts and insights
- 🤖 **AI Coaching** - Personalized recommendations and insights
- 🏆 **Gamification** - Achievements, badges, and challenges
- 📤 **Easy Sharing** - One-tap export and social sharing

### Future Possibilities
- 🎯 Multi-exercise support (lunges, push-ups)
- 👕 Wear OS integration
- 🔊 Voice feedback
- ☁️ Cloud sync (opt-in)
- 🌐 Web dashboard

See [ROADMAP_FEATURES.md](update_docs/ROADMAP_FEATURES.md) for complete details, timelines, and technical specs.

## 🤝 Contributing

Contributions welcome! Check our [roadmap](update_docs/ROADMAP_FEATURES.md) for planned features.

---

## 📜 License

This project is licensed under the MIT License.

---

## 🙏 Acknowledgments

- **TensorFlow Lite** for MoveNet model
- **Google** for pose detection research
- **Android Camera2 API** documentation

---

**Built with ❤️ for fitness enthusiasts**
