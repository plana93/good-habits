# Workout Templates

Questa cartella contiene i template JSON per gli allenamenti predefiniti dell'app.

## Struttura del file JSON

```json
{
  "id": 1,
  "name": "Nome del workout",
  "description": "Descrizione dettagliata", 
  "icon": "icona_material_design",
  "estimatedDuration": 25,
  "difficulty": "beginner|intermediate|advanced",
  "exercises": [
    {
      "exerciseId": 1,
      "orderIndex": 0,
      "targetReps": 15,        // null se basato su tempo
      "targetTime": null,      // null se basato su ripetizioni  
      "restTime": 30           // secondi di riposo dopo l'esercizio
    }
  ],
  "tags": ["tag1", "tag2"],
  "createdAt": "2024-12-25",
  "modifiedAt": "2024-12-25",
  "category": "strength|cardio|recovery|flexibility"
}
```

## Mapping Exercise ID

- 1: Push-up
- 2: Squat
- 3: Plank
- 4: Jumping Jacks
- 5: Mountain Climbers

## Icone disponibili (Material Design Icons)

- accessibility_new: 💪 (forza/upper body)
- directions_run: 🏃 (cardio)
- self_improvement: 🧘 (flessibilità/recovery)
- fitness_center: 🏋️ (generale fitness)
- timer: ⏱️ (HIIT/temporizzati)

## Note

- I file devono essere in formato JSON valido
- L'ID deve essere unico per ogni workout
- orderIndex determina l'ordine degli esercizi nel workout
- targetReps e targetTime sono mutuamente esclusivi