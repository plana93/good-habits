# Pose Detection App

This Android application demonstrates real-time pose detection using the Movenet Singlepose Lightning model. The app captures video from the device's camera, processes the frames through the pose detection model, and overlays keypoints and connections on the camera preview. Additionally, it displays emojis corresponding to key facial and body points.

## Getting Started

Before running the app, ensure that the necessary camera permissions are granted. The app will request camera permissions if not already granted.

## Dependencies

- TensorFlow Lite: The app uses the TensorFlow Lite library for running the Movenet Singlepose Lightning model.
- Movenet Singlepose Lightning model: The pose detection model is included in the `ml` package as `LiteModelMovenetSingleposeLightningTfliteFloat164`.

## Usage

1. Launch the app on an Android device.
2. Grant camera permissions if prompted.
3. The camera preview will show keypoints and connections based on the detected pose in real-time.
4. Emojis corresponding to key facial and body points will be overlaid on the camera preview.
5. The app also displays the current count of repetitions detected.

## Features

- Real-time pose detection: The app uses the Movenet Singlepose Lightning model to detect the user's pose.
- Keypoints and connections: The detected keypoints and connections are visualized on the camera preview.
- Emoji overlay: Emojis corresponding to key facial and body points are overlaid on the camera preview.
- Repetition counter: The app counts and displays the number of repetitions detected.

## Code Structure

The main functionality of the app is implemented in the `MainActivity.kt` file. The key components include:

- **Camera Initialization**: The camera is initialized using the Camera2 API.
- **Pose Detection**: The Movenet Singlepose Lightning model is used to detect keypoints and connections in each video frame.
- **Visualization**: The detected keypoints, connections, and emojis are overlaid on the camera preview.
- **Repetition Counter**: The app counts and displays the number of repetitions detected.

## Cleanup

Ensure to release the resources and close the model when the app is destroyed.

```kotlin
override fun onDestroy() {
    super.onDestroy()
    model.close()
}
```

## Notes

- The app uses TensorFlow Lite for efficient on-device machine learning inference.
- Adjust the confidence threshold (`score > 0.45`) for keypoints to control the visibility of keypoints and connections.

Feel free to explore and modify the code to suit your needs. Happy coding!