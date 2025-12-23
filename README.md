# 🏋️ Good Habits - AI Fitness Tracker

**Modern Android fitness app with AI-powered pose detection for automatic squat counting**

---

## 📱 Overview

Good Habits is a production-ready Android application that combines artificial intelligence, modern UI design, and comprehensive workout tracking. Using Google's MoveNet pose detection model, the app automatically counts squat repetitions through your device camera while providing a complete fitness management system.

### 🎯 Key Features

- **🤖 AI Squat Detection**: Automatic counting using TensorFlow Lite MoveNet
- **📅 Smart Sessions**: Daily workout tracking with temporal navigation
- **📊 Analytics Dashboard**: Real-time statistics with calendar integration
- **💾 Data Export**: Complete CSV export for external analysis
- **🎨 Material3 Design**: Modern UI with conditional navigation

---

## 🚀 Quick Start

### Prerequisites
- Android device (API 24+)
- Camera permissions
- Android Studio (for development)

### Installation
```bash
# Clone the repository
git clone <repository-url>
cd realtime_pose_detection_android-main

# Build and install
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### First Run
1. Grant camera permissions when prompted
2. Open the app and navigate to Dashboard
3. Tap the central + button and select "AI Squat"
4. Choose camera (front/back) and start squatting!

---

## 🏗️ Architecture

**Clean Architecture** with **MVVM** pattern:
- **UI Layer**: Jetpack Compose with Material3
- **Domain Layer**: ViewModels with business logic
- **Data Layer**: Room database with repositories

**Key Technologies**:
- Kotlin 100%
- Jetpack Compose
- TensorFlow Lite + MoveNet
- Room Database
- Navigation Compose

---

## 📖 Documentation

Complete documentation is available in the [`update_docs/`](update_docs/) folder:

- **[📋 Overview](update_docs/00_APP_OVERVIEW.md)** - App features and user experience
- **[🔧 Technical Architecture](update_docs/01_TECHNICAL_ARCHITECTURE.md)** - Detailed technical implementation
- **[📈 Development Roadmap](update_docs/02_DEVELOPMENT_ROADMAP.md)** - Current status and future opportunities
- **[👨‍💻 Developer Reference](update_docs/03_DEVELOPER_REFERENCE.md)** - Quick development guide
- **[🚀 Build Guide](update_docs/05_BUILD_DEPLOY_GUIDE.md)** - Build and deployment instructions

---

## ✅ Current Status

**Version 3.0 - Production Ready**

All core features are complete and fully functional:
- ✅ AI pose detection with MoveNet
- ✅ Session management system  
- ✅ Dashboard with statistics
- ✅ Calendar with streak tracking
- ✅ CSV export functionality
- ✅ Material3 design implementation

---

## 🧪 Testing

### Core Features Test
```bash
# Install and test key functionality
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk

# Open app and verify:
# 1. Dashboard loads with statistics
# 2. AI Squat detection works
# 3. Calendar shows activity
# 4. Export generates CSV
```

### Development Testing
```bash
# Run with detailed logging
adb logcat | grep -E "(TODAY_DEBUG|AI|DATABASE)"
```

---

## 🔧 Development

### Setup Environment
1. Install Android Studio
2. Install Kotlin plugin
3. Set JDK 11+ for project
4. Sync Gradle dependencies

### Project Structure
```
app/src/main/java/com/programminghut/pose_detection/
├── ui/activity/           # Main activities and screens
├── ui/viewmodel/         # ViewModels and business logic
├── data/repository/      # Data access layer
├── data/dao/            # Database access objects
├── data/model/          # Data models and entities
└── SquatCounter.kt      # AI pose detection engine
```

### Key Entry Points
- `NewMainActivity.kt` - Main app navigation and UI
- `TodayViewModel.kt` - Session management logic  
- `DailySessionRepository.kt` - Core business operations
- `SquatCounter.kt` - AI pose detection implementation

---

## 📊 Performance

- **App Size**: ~15MB (includes AI model)
- **Memory Usage**: ~150MB during AI detection
- **Battery**: Optimized for extended workout sessions
- **AI Performance**: 30+ FPS pose detection on modern devices

---

## 🤝 Contributing

1. Review the [Technical Architecture](update_docs/01_TECHNICAL_ARCHITECTURE.md)
2. Check [Development Roadmap](update_docs/02_DEVELOPMENT_ROADMAP.md) for enhancement opportunities
3. Follow existing code patterns and architecture
4. Test thoroughly before submitting changes

---

## 📄 License

This project demonstrates modern Android development practices with AI integration. Feel free to use as a reference or starting point for similar applications.

---

## 🌟 Recognition

This app showcases:
- Production-quality Android architecture
- AI/ML integration on mobile devices
- Modern Jetpack Compose UI patterns
- Comprehensive fitness tracking features
- Clean, maintainable codebase

*Good Habits represents a complete, modern Android application ready for production use or further development.*