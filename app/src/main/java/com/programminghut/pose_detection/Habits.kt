package com.programminghut.pose_detection


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text

import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

class Habits : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Pose_detectionTheme {
                // Use a column layout for better appearance
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Display a welcome message
                    Text(
                        text = "Welcome to Pose Detection App!",
                        color = Color.Gray
                     )
                    
                    // Add some space between the text and the buttons
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Bottone SQUAT (flusso originale)
                    CustomButton(
                        text = "SQUAT COUNTER",
                        onClick = {
                            val intent = Intent(this@Habits, Squat::class.java)
                            startActivity(intent)
                        },
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Bottone RECORD SKELETON (nuovo flusso)
                    CustomButton(
                        text = "RECORD SKELETON",
                        onClick = {
                            val intent = Intent(this@Habits, RecordingCameraSelectionActivity::class.java)
                            startActivity(intent)
                        },
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Bottone URBAN CAMERA (flusso urban street art)
                    CustomButton(
                        text = "URBAN CAMERA",
                        onClick = {
                            val intent = Intent(this@Habits, UrbanCameraSelectionActivity::class.java)
                            startActivity(intent)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun CustomButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Adjust padding as needed
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = text)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HabitsPreview() {
    Pose_detectionTheme {
        Habits()
    }
}
