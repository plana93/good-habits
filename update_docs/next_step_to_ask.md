# Good Habits — Specifica Nuove Funzionalità (2025 Q4)

Questo documento descrive in modo esaustivo tre nuove funzionalità in fase di implementazione per l’app Good Habits.  
L’obiettivo è estendere le capacità dell’applicazione oltre il solo squat tracking, migliorare l’engagement e potenziare la dashboard con strumenti più visivi e motivanti.

---

# 1. Visualizzazione Grafica Avanzata delle Ripetizioni

## Descrizione
L’attuale dashboard mostra le ripetizioni in formato numerico o a elenco.  
Per rendere l’analisi più immediata e motivante, verrà introdotta una **visualizzazione a grafico** delle singole ripetizioni di una sessione o di più sessioni combinate.

## Dettagli Funzionali
- **Grafico lineare o scatter** con ogni ripetizione rappresentata come punto o nodo.
- Ogni punto contiene:
  - timestamp,
  - profondità raggiunta,
  - qualità della postura,
  - velocità dell’esecuzione,
  - tempo tra una ripetizione e l’altra.
- **Color coding dinamico**:
  - Verde = esecuzione corretta  
  - Giallo = borderline  
  - Rosso = esecuzione da correggere
- **Popup contestuale**: toccando un punto, l’utente vede dati dettagliati della ripetizione.
- **Heatline**: linea che varia colore in base alla qualità generale della sessione.
- **Mini replay opzionale**: animazione skeleton-based della singola ripetizione (solo se attivato).

## Obiettivo
Rendere la dashboard più leggibile, professionale e orientata al miglioramento della tecnica.

---

# 2. Aggiunta Manuale Sessioni + Recupero Sessioni Perse

## 2.1 Aggiunta Manuale
L’utente potrà creare sessioni non registrate real-time, inserendo:
- data,
- esercizio,
- ripetizioni,
- durata stimata,
- eventuali note,
- scelta se far valere la sessione per la streak.

Il sistema effettua verifiche per evitare duplicati o valori incoerenti.

## 2.2 Recupero Sessioni Perse ("Recovery Mode")
Viene introdotto il **Calendario della Costanza**, che mostra:
- Giorni completati (verde)
- Giorni mancati (rosso)
- Giorni recuperati (icona dedicata)
- Giorni futuri (grigio)

### Regola per il recupero
Per recuperare un giorno saltato:
1. L’utente seleziona il giorno rosso nel calendario.
2. Avvia una **Sessione di Recupero**.
3. Completa almeno **50 ripetizioni real-time** del relativo esercizio.
4. Il giorno viene registrato come recuperato e la streak ripristinata.

## Obiettivo
Ridurre la frustrazione derivante dalla perdita di una streak e rendere la progressione più flessibile e motivante.

---

# 3. Tracking Multi-Esercizio + Editor Personalizzato

## 3.1 Preset Esercizi
Vengono introdotti esercizi multipli con regole di rilevazione predefinite:
- Push-up  
- Affondi  
- Crunch / Sit-up  
- Jumping Jacks  
- Shoulder Press  
- Plank dinamico  

Per ogni esercizio il sistema definisce:
- Stato di riposo
- Stato massimo (momento di conteggio)
- Ritorno allo stato di riposo
- Giunti da ignorare (non sempre in camera)
- Tolleranze angolari/distanza

## 3.2 Editor Personalizzato ("Exercise Builder")
L’utente può creare nuovi esercizi caricando:
- una foto per lo **stato di riposo**,
- una foto per lo **stato massimo**,
- una foto per il **ritorno**.

Il sistema estrae automaticamente i keypoints e genera una bozza di regola che l’utente può modificare:
- Giunti da considerare / ignorare
- Variabili angolari rilevanti
- Tolleranze su distanza e angoli
- Numero di frame per la validazione di ogni stato
- Soglie personalizzabili per lo stato massimo

## 3.3 Funzione “Copia Regola”
Per facilitare la creazione assistita tramite LLM, l’app offre un bottone “📋 Copia Regola” che:
- esporta negli appunti tutte le regole dell’esercizio,
- include valori, tolleranze, giunti e stati,
- permette all’utente di incollare il contenuto in un’app LLM esterna.

L’LLM **non** è integrato nell’app.

---

# Obiettivi Generali
- Potenziare la dashboard rendendola visiva e analitica.  
- Aumentare la flessibilità con sessioni manuali + recuperi motivazionali.  
- Trasformare Good Habits in una piattaforma multi-esercizio altamente personalizzabile.  

---

SUMMARY 

1) Visualizzazione avanzata per ogni singola ripetizione

(Sostituzione elenco → grafici dinamici e leggibili)

Problema attuale

Nella dashboard, i dati relativi alle singole ripetizioni vengono mostrati in formato numerico o testuale, risultando poco intuitivi da interpretare, poco motivanti e difficili da confrontare nel tempo.

Nuova funzionalità

Introdurremo una sezione completamente rinnovata dedicata alle singole ripetizioni, trasformando l’elenco statico in una visualizzazione grafica interattiva.
L’obiettivo è rendere ogni ripetizione “viva”, visibile e interpretabile con un colpo d’occhio.

Cosa verrà mostrato

Grafico lineare o a scatter di tutte le ripetizioni della sessione (o di più sessioni), con punti rappresentanti:

velocità di esecuzione,

profondità del movimento,

qualità della postura,

tempo tra una ripetizione e l'altra.

Heatline: una linea colorata che cambia colore in base alla qualità esecutiva (verde/ok, giallo/attenzione, rosso/errata).

Modalità “micro-dettaglio”: toccando il singolo punto del grafico, l’utente può vedere un popup con:

timestamp,

angoli dei principali giunti,

profondità,

eventuali warning di postura.

Grafico “ripetizione dopo ripetizione”: un piccolo oscilloscopio visivo che mostra la dinamica del corpo durante ogni movimento.

Animazione dei keypoints (opzionale, solo per dettagli avanzati): replay stilizzato della ripetizione direttamente nella dashboard.

Beneficio per l’utente

Comprensione immediata dell’andamento della sessione.

Feedback chiaro sulla qualità, non solo sulla quantità.

Dashboard più moderna, professionale e motivante.

2) Aggiunta manuale delle sessioni + recupero sessioni perse

(Calendario smart + logica motivazionale)

Problema attuale

Le sessioni possono essere registrate solo in tempo reale. Mancano:

la possibilità di aggiungerle manualmente (es. allenamenti fatti senza telefono),

la possibilità di “recuperare” giorni saltati preservando la streak,

un calendario visivo dove gestire continuità e recuperi.

Nuova funzionalità
A) Aggiunta manuale delle sessioni

L’utente potrà:

creare una sessione manualmente scegliendo data, esercizio e ripetizioni,

aggiungere note o tag,

decidere se la sessione influisce sulla streak o è “solo informativa”.

Verranno applicate controlli di coerenza (evitare doppioni, impedire numeri irrealistici, ecc.).

B) Recupero sessioni perse (funzionalità gamificata)

Introdurremo un Calendario della Costanza, che mostra:

i giorni completati (verde),

i giorni mancati (rosso),

i giorni recuperati (icona speciale),

i giorni futuri (grigio).

Regola principale

Per recuperare un giorno saltato, l’utente deve:

cliccare sul giorno “rosso” nel calendario,

avviare la Sessione di Recupero,

completare almeno 50 ripetizioni real-time (con AI),

la sessione viene registrata automaticamente come “recuperata”.

Perché questa regola è efficace

Garantisce che il recupero sia reale, non fittizio.

Mantiene valore della streak.

Motiva l’utente ad allenarsi di più quando salta un giorno.

Aumenta l’engagement dell’app.

Extra inclusi

Possibilità di aggiungere manualmente sessioni passate, ma senza modificare streak (opzione trasparente).

Icone diverse per giorni normali / recuperati / manuali.

Animazione motivazionale quando si recupera un giorno.

Benefici

Riduce la frustrazione di perdere una streak.

Aumenta la flessibilità e la correttezza dei dati nel tempo.

Gamifica l’esperienza e incentiva costanza e volume.

3) Tracking multi-esercizio con regole personalizzabili

(Preset + Editor di esercizi intelligenti)

Problema attuale

L’app supporta un solo esercizio (squat).
Gli sportivi vogliono varietà, personalizzazione e la possibilità di definire esercizi nuovi senza aspettare aggiornamenti dell’app.

Nuova funzionalità

Introduzione del sistema multi-esercizio, composto da:

A) Preset di esercizi pronti (automatically supported)

Esempi:

Push-up

Affondi

Jumping jacks

Plank rep-based

Shoulder-press

Addominali vari (crunch, sit-up)

Per ogni esercizio saranno predefinite:

regole di “stato riposo”,

regole di “stato massimo”,

regole di ritorno al riposo,

giunti da ignorare (non visibili alla camera),

tolleranze angolari,

precisione ottimale per il conteggio.

B) Editor avanzato per creare esercizi personalizzati

L’utente potrà:

Caricare 3 foto:

posizione di riposo,

posizione di massima esecuzione,

posizione di ritorno.

L’app estrarrà i keypoints da ogni immagine.

Verrà generata una bozza automatica della regola, che l’utente può modificare:

giunti da considerare,

giunti da ignorare,

distanza minima/massima,

variazioni angolari,

range di tolleranza,

numero di frame necessari per validare una ripetizione,

optional: lato dominante (sx/dx).

L’utente potrà testare l’esercizio in real-time per verificare che il conteggio funzioni.

C) Funzione “Copia Regola”

Una volta configurato l’esercizio personalizzato, l’utente avrà:

un pulsante con icona "📋 Copia Regola",

l’app copierà negli appunti un testo completo contenente:

definizione esercizio,

tutti i parametri configurati,

valori dei giunti,

condizioni di riposo/max/ritorno,

tolleranze,

eventuali commenti.

Questo testo può essere incollato in un’app esterna (es. un LLM) per perfezionare o generare nuove idee.
L’app non integra un LLM, ma facilita l’utente ad usarne uno esternamente.

Benefici principali del sistema multi-esercizio

Apre la strada all’allenamento full-body.

Enorme scalabilità senza dover aggiornare manualmente il codice dell’app.

Sensazione di “potere creativo” per l’utente più avanzato.

Perfetto anche per coach, fisioterapisti o ricercatori.

