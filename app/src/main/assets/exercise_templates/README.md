# Exercise Templates

Questa cartella contiene i template di esercizi caricati automaticamente dall'app.

## Come aggiungere un nuovo esercizio

1. Crea un nuovo file `.json` in questa cartella
2. Usa il seguente template:

```json
{
  "id": 999,
  "name": "Nome Esercizio",
  "type": "STRENGTH|CARDIO|CORE|FLEXIBILITY|OTHER",
  "mode": "REPS|TIME|BOTH",
  "description": "Descrizione dettagliata dell'esercizio",
  "imagePath": null,
  "thumbnailPath": null,
  "defaultReps": 10,
  "defaultTime": 30,
  "createdAt": 1735062000000,
  "isCustom": false
}
```

## Campi obbligatori

- `id`: Numero univoco (usa un ID alto per evitare conflitti)
- `name`: Nome dell'esercizio
- `type`: Tipo di esercizio
- `mode`: Modalità di esecuzione
- `description`: Descrizione

## Campi opzionali

- `imagePath`: Percorso immagine custom (null per placeholder)
- `thumbnailPath`: Percorso thumbnail (generato automaticamente)
- `defaultReps`: Ripetizioni suggerite (per mode REPS/BOTH)
- `defaultTime`: Tempo suggerito in secondi (per mode TIME/BOTH)
- `createdAt`: Timestamp creazione (default: current time)
- `isCustom`: true per esercizi personalizzati, false per quelli predefiniti

## Tipi supportati

- `STRENGTH`: Esercizi di forza
- `CARDIO`: Esercizi cardiovascolari
- `CORE`: Esercizi per addominali e core
- `FLEXIBILITY`: Stretching e flessibilità
- `OTHER`: Altri tipi

## Modalità supportate

- `REPS`: Solo ripetizioni
- `TIME`: Solo tempo
- `BOTH`: Sia ripetizioni che tempo