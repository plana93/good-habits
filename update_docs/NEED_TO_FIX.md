# To Fix: 
rivediamo il design delle due pagine Exercise e Workout 

mi piace avere la lista di eserci non usando la lista standard ma usando delle box a griglia 
solo che ogni esercizio ha una box rettangolare che occupa troppo spazio vorrei una box quadrata 

se clicco mi piace che mi chiede il numero di ripetizioni o numero di minuti (deve poter accettare anche numeri con la virgola)

se tengo premuto però mi deve aprire una scheda che mostra 

nome (editabile se ci clicco) 
descrizione (editabile se ci clicco)
immagine precaricata o aggiungibile (in modo da ricordarmi l'esercizio)

# To fix: 

errore nel cancellare gli esercizi del workout (ovvero se li cancello non decremento il conteggio dei squat) però funziona bene con gli esercizi singoli 
(documentato qui update_docs/BUG_WORKOUT_SQUAT_DELETION.md)

# Done:

Nell'ultimo fix per sbaglio è stata rimossa l'opportunità di aggiungere gli esercizi al giorno coorrente. I pulsanti ci sono ancora tutti ma quando li clicco non vengono aggiunti al giorno coorrente. (prima funzionava quindi forse c'è stato un aggiornamento ha compromesso questa features, attendo nel risolvela di non rompoere quella passata)

se vuoi prima di risovere ti mostro l'errore eseguendo l'app e puoi leggere dai log il problema.

# Done:

Vorrei che nei giorni passati non si può aggiungere gli esercizi 
(diciamo che sono persi) però voglio aggiungere un concetto di recupera giorno 
che consiste nel fare 50 squat con l'AI in questo modo il giorno appare come recuperato e contribuosce a migliorare la sticke dei giorni continui 

quando vado nella dashboard e po nel calendario il concetto di giorno recuperato è già presente ma mi rimando alla sessione giorno invece dovrebbe semplicemente avviare la procedura di giorno recuperato

ovviamente se poi faccio l'esercizio il giorno deve apparire recuperato anche nella activity del giorno (ovviamente alla data corretta non al giorno corrente) aggiornato la schermata dall'emoticon triste ad emoticon sorridente 

# Done:

Per quanto rigiuarda la lista degli esercizi che appare nel giorno vorrei migliorare la visualizzazione 

Vorrei quando apro la tendinza un bottono aggiuni ripetizione (ovvero ti aggiunge un esercizio uguale alla lista)

Vorrei che la possibilità di cancellare l'esercizio sia una gesture da a sinistra sul elemento 

# Done:

Quando aggiungo le cose e clicco direttametne dalla scheramta di oggi con il botono più Squat AI 
deve far partire la sessione con la camera 
se invece vado su esercizi e poi metto SquatAI allora in quel caso vuol dire che voglio contarli manualmente e quindi posso mettere direttamete il numero 


continua a non salvare il conteggio dei squat fatti con l'AI
nella scheramata delle activity

forse si perde un passaggio nella logica 

gli squat AI 
sono comunque un esericizio quindi sono presenti nella schermata esercizi 
solo un esercizio particolare 
perche presenta due diverse modaltà 

una standard manuale (vado su esercizi aggiungo squat e manualmente metto il numero fatto)

una più nuova 
ovvero vado su oggi 
faccio aggiungi sqaut AI 
parte con la camera e il conteggio

prova aggiungere una piccola X 
per poter dire fine conteggio squat e tornare sulla schermata oggi
avendo insertito 
il numero esatto di squat realizzati

poi lo squat ha anche un suo  track anche speciale 

ovvero che sulla dashboard voglio avere il numero di squat totali
che si incrementa sempre 

diciamo che lo squat è l'esercizio base 
quindi deve avere più rilevanza e più caratteristiche dell'app si basano su di esso


# TO DO 
da aggiungiungere 
Kaizen 
Ikigai
Hara Hachi BU
Focus Ancorato 
Seiri Seiton
Mentalità Kintsugi 
Wabi-sabi