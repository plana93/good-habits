# 📸 Implementazione Image Picker per Esercizi

## 🎯 Funzionalità Implementata

### ✅ **Dialog Selezione Immagine**
Quando si clicca sull'icona camera nell'`ExerciseDetailDialog`, ora si apre un dialog con due opzioni:

1. **📷 Scegli dalla Galleria**
   - Apre il picker nativo Android
   - Seleziona immagini da galleria/foto
   - Gestisce URI dell'immagine selezionata

2. **📸 Scatta Foto** *(preparato per futuro)*
   - Placeholder per implementazione camera
   - UI preparata con icona e descrizione

## 🔧 Implementazione Tecnica

### **Activity Result Launchers**
```kotlin
// Image picker per galleria
val imagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
) { uri ->
    uri?.let {
        // TODO: Salvare l'URI dell'immagine nel database
        Log.d("IMAGE_PICKER", "Immagine selezionata: $uri")
    }
}

// Camera launcher (per future implementazioni)
val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture()
) { success ->
    if (success) {
        Log.d("CAMERA", "Foto scattata con successo")
    }
}
```

### **UI Dialog Opzioni**
```kotlin
AlertDialog(
    title = { Text("Cambia Immagine") },
    text = {
        Column {
            // Card Galleria
            Card(onClick = { imagePickerLauncher.launch("image/*") }) {
                Row {
                    Icon(Icons.Default.Photo)
                    Text("Scegli dalla Galleria")
                }
            }
            
            // Card Camera  
            Card(onClick = { /* TODO: Camera */ }) {
                Row {
                    Icon(Icons.Default.PhotoCamera)
                    Text("Scatta Foto (presto)")
                }
            }
        }
    }
)
```

## 🎨 User Experience

### **Flow Utente**
1. **Long press** su esercizio nella griglia → Apre ExerciseDetailDialog
2. **Click su immagine** → Mostra dialog opzioni
3. **"Scegli dalla Galleria"** → Apre picker nativo Android
4. **Selezione immagine** → Log dell'URI (ready per salvataggio)

### **Design Pattern**
- **Card interattive**: Feedback visivo chiaro
- **Icone intuitive**: Photo per galleria, Camera per scatto
- **Colori tematici**: Primary per galleria, Secondary per camera
- **Descrizioni esplicative**: "Seleziona immagine esistente" vs "Scatta nuova foto"

## 📱 Stato Attuale

### ✅ **Funziona**
- Dialog si apre correttamente
- Image picker launcher configurato
- UI responsiva e accessibile
- Log dell'URI selezionato

### 🔄 **Da Completare** (TODO per prossime iterazioni)
1. **Salvataggio Database**: Salvare URI nel campo `imagePath` di `ExerciseTemplate`
2. **Caricamento Immagini**: Mostrare immagine custom invece di ExerciseThumbnail
3. **Camera Integration**: Implementare scatto foto con permessi
4. **Image Processing**: Resize e ottimizzazione immagini
5. **Storage Management**: Gestione spazio e cleanup immagini

## 🧩 Architettura Preparata

### **Database Schema** (già esistente)
```kotlin
@Entity(tableName = "exercise_templates")
data class ExerciseTemplate(
    // ...altri campi...
    val imagePath: String? = null  // ← Ready per URI immagini
)
```

### **Permissions** (da aggiungere al Manifest)
```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.CAMERA" />
```

### **Dependencies Aggiunte**
```kotlin
// Activity Result API
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
```

## 🚀 Come Testare

1. **Apri app** → Naviga a Exercise Library
2. **Long press** su qualsiasi esercizio
3. **Click area immagine** (grande card sopra)
4. **Scegli "Galleria"** → Dovrebbe aprire image picker nativo
5. **Seleziona immagine** → Check log per URI

### **Log Expected**
```
D/IMAGE_PICKER: Immagine selezionata: content://media/external/images/media/1234
```

## 🔮 Next Steps

1. **Integrazione Database**: Salvare URI in `ExerciseTemplate.imagePath`
2. **Custom Thumbnail**: Sostituire ExerciseThumbnail quando `imagePath != null`
3. **Camera Permissions**: Richiedere permessi e implementare scatto
4. **Image Optimization**: Ridimensionare immagini per performance
5. **Sync & Backup**: Gestione cloud storage immagini

---
*Implementato il: 24 dicembre 2025*
*Image picker funzionale, pronto per integrazione database*