# WORDLE
----
## Comandi per la compilazione da terminale

### Server
```
compilare   --->     javac -cp ".;gson-2.8.6.jar" .\server_package\*.java
eseguire    --->     java -cp ".;gson-2.8.6.jar" server_package/WordleServer
```

### Client
```
compilare    --->     javac  .\client_package\*.java
eseguire     --->     java  client_package/WordleClient
```
---
## Descrizione Gioco
>gioco di parole web-based, divenuto virale alla fine del 2021.  Il gioco consiste nel trovare una parola inglese formata da 5 lettere, impiegando un numero massimo di 6 tentativi. WORDLE dispone di un vocabolario di parole di 5 lettere, da cui estrae casualmente una parola SW (Secret Word), che gli utenti devono indovinare. Ogni giorno viene selezionata una nuova SW, che rimane invariata fino al giorno successivo e che viene proposta a tutti gli utenti che si collegano al sistema durante quel giorno. Quindi esiste una sola parola per ogni giorno e tutti gli utenti devono indovinarla, questo attribuisce al gioco un aspetto sociale. L’utente propone una parola GW (Guessed Word) e il sistema inizialmente verifica se la parola è presente nel vocabolario. In caso negativo avverte l’utente che deve immettere un’altra parola. In caso la parola sia presente, il sistema fornisce all’utente alcuni indizi utili per indovinare la parola.
----
## Scelte implementative
> La struttura principale è composta da un client e un server. Il server utilizza un thradpool (con numero di thread massimo fissato) per delegare la gestione di una singola connessione con un client a un thread task indipendente. La comunicazione tra client e server avviene utilizzando una connessione TCP. Il client propone una lista di comandi e richiede l'inserimento di dati all'utente, tali comandi e dati inseriti (dopo aver superato i controlli) vengono inoltrati al server che gestisce le richieste e risponde con un messaggio. Eventuali comunicazioni non riconosciute o scorrette riscontrate dal client o server vengono comunicate attraverso un'eccezione definita.
Le informazioni dei giocatori vengono rappresentati da una classe. All'avvio del server vi è la lettura di tali informazioni da un file json, si occupa di salvare tutti i dati dei giocatori come oggetti in una hashmap condivisa tra i vari thread task. In questo modo i thread task accedono alle informazioni di tutti i giocatori registrati, possono modificare i dati e alla chiusa di ogni connessione con un client tutte le informazioni della hashmap vengono sovrascritti nel file json. Per evitare che più thread task agiscano sullo stesso oggetto giocatore e per evitare anche che lo stesso utente da due client diversi affetti il login in contemporanea, viene utilizzato un set che tiene traccia di tutti gli oggetti giocatore che sono stati occupati.
Il server contiene diversi metodi statici sincronizzati con l'utilizzo dei monitor per permette ai thread di eseguire operazioni concorrenti su dati condivisi.
Il server dispone di un handler per la gestione dei segnali, in particolare il comando 'CRTL+C' ne permette la terminazione.
Per la gestione della secret_word che viene scelta casualmente da un elenco di parole di gioco salvate su un file e aggiornata ad intervalli di tempo definiti viene avviato un thread che utilizza una variabile per salvare la secret_word generata e una sleep per aspettare il tempo settato prima di estrarne una nuova. La lettura viene effettuata eseguendo un accesso puntuale, difatti conoscendo la struttura del file contenente le parole di gioco è possibile ricavare il numero di parole presenti e leggere una parola scelta casualmente senza sanzionare l'intero file. Tale thread offre dei metodi che permettono di leggere il valore delle secret_word ed in oltre presenta un metodo per la ricerca di una parola all'interno del file di parole di gioco (utilizzato per vedere se l'utente ha inserito una parola lecita). Data la struttura ordinata del file viene sfittata la ricerca binaria.
Per quanto riguarda il client, esso avvia un thread che si occupa di ricevere restando in ascolto e salvare tutti i messaggi inviati dal server all'indirizzo di multicast (usato per condividere le informazioni di un giocatore con gli altri) e tramite metodi get il client accede ai messaggi memorizzati.
---

## Struttura del programma
> Il server è implementato utilizzando:
> --> **`WordleServer`** classe principale che resta in ascolto di nuove connessioni provenienti dai client
> --> **`WordleServerTask`** thread che gestisce la signola connessione con il client
> --> **`TerminationHandler`** thread che si occupa di terminare il server quando vengono premuti i tasti `CRTL+C`
> --> **`SelectionWord`** thread che gestisce la secret_word.

> Il client è implementato utilizzando:
> --> **`WordleClient`** classe principale che interagisce con il server inviando i comandi indicati dall'utente
> --> **`WordleClientMethond`** classe che raggruppa le funzioni del `WordleClient`
> --> **`WordleClientMulticast`** thread che si occupa di ricevere e salvare i messaggi UDP inviati dal server sull'indirizzo di multicast

> Altre classi:
> --> **`AccountPlayer`**, classe che modella un giocatore
> --> **`Command`** , tipo enumerato che descrive i comandi scambiati tra Client e Server
> --> **`CommunicationErrorServerClient`**, classe che ridefinisce un'eccezzione 



### File esterni utilizzati
> --> **`words.txt`**, file contente tutte le parole di gioco. Importante: tutte le parole devono essere ordinate in ordine alfabetico; ogni parola presente deve essere formata da esattamente 10 caratteri e terminare con `'\n'` 
> --> **`list_accounts.json`**, file dove vengono salvati i dati di ogni giocatore in formato json seguendo la struttura della classe `AccountPlayer`
> --> **`ServerWordle.properties`**, file di configurazione che contiene i valori con cui settare determinate variabili del Server (porta_di_ascolto, path_words.txt, path_lista_accounts.json, ...)
> --> **`ClientWordle.properties`**, file di configurazione che contiene i valori con cui settare determinate variabili del Client (indirzzo_ip_server, porta_server, ...)

---
## Classi Utilizzate
### AccountPlayer
> Classe che rappresneta un giocatote e tiene traccia di tutti i suoi dati. Dispone di metodi getter e setter, metodi di confronto, metodi @override per ridefinire il toString e HashCode.
> **Campi presenti**
> -> String `user`, nome utente
> -> String `password`, password di accesso
> -> int `games_played`, numero partite giocate
> -> int `games_won`, numero partite vinte
> -> int `strak`, lunghezza dell’ultima sequenza continua di vincite
> -> int `max_streak`, lunghezza della massima sequenza continua  di vincite
> -> int `last_secretWord`, numero di sequenza dell'ultima SecretWord con cui il giocatore ha giocato
> -> int `attempts_used`, tentativi utilizzati (riferiti a last_SecretWord)
> -> int[] `guess_distribution`, vettore di interi, dove la posizione k-esima del vettore contiene il numero di parole indovinate dall'utente con esattamente k tentativi
> -> int[] `last_game_stats`, vettore di stringhe che salva per ogni tentativo utilizzato dal giocatore nell'ultima partita i colori delle lettere della parola proposta

### Command
> Tipo enumerato che descrive le operazione di gioco, usato per scambaire messaggi durante la comunicazione tra client e server
> -> `LOGIN`
> -> `REGISTER` 
> -> `PLAY_WORDLE` 
> -> `SEND_WORD`
> -> `SEND_STATISTICS`
> -> `SHARE`
> -> `LOGOUT`
> -> `FAILURE`
> -> `ERRORMESSAGE`
> -> `EXIT`
> -> `WIN`
> -> `LOSE`

### CommunicationErrorServerClient
> Classe che ridefinisce un'eccezione. Viene l'anciata queste eccezione nei casi di comunicazione anomala tra Client-Server (ricezione di messaggi non aspettati).

### WordleServer
> Classe Java che rappresenta il server del gioco Wordle.
Resta in ascolto di nuove connessioni provenienti dai client e utilizza un threadpool (con numero thread fissato) per delegare la gestione di una singola connessione (task) ad un thread indipendente `WordleServerTask`. Nella fase iniziale legge il file di configurazione `ServerWordle.properties` e avvia un thread `TerminationHandler` che si occupa di terminare il server quando vengono premuti i tasti `CRTL+C` e un thread `SelectionWord` che gestisce la secret_word.  Legge il file json contenete le informazioni salvate dei vari giocatori e salva tali dati su una CuncurrenthashMap che viene condivisa tra i vari thread `WordleServerTask`. Il `WordleServer` contiene metodi sincronizzati attraverso l'utilizzo di monitor per scrivere sul file json, cercare una parola all'interno del file contenente le parole di gioco, oppure inviare una stringa come messaggio UDP sull'indirizzo di multicast, metodi utilizzati dai thread `WordleServerTask`.

### TerminationHandler
> Classe che implementa l'handler di terminazione del server. Questo thread viene avviato al momento della pressione dei tasti `CTRL+C` e lo scopo e' quello di far terminare il main del server bloccato sulla accept() in attesa di nuove connessioni e terminare il pool di thread. Per fare ciò chiude la socket del server  (questo genererà un'eccezione nel server che sarà gestita e porterà alla chiusura) e invia il segnale di terminazione ai thread del threadpool, aspettanto un termpo stabilito prima i terminare definitamente i thread.
> **Parametri**
> -> ServerSocket `serverSocket`, riferimento alla socket del server
> -> ExecutorService `pool`, riferimento al threadpool del server
> -> int `maxDelay`, tempo di attesa in millisencodi per la terminazione dei thread nel threadpool

### SelectionWord
> Questo thread si occupa di leggere dal file contenente le parole di gioco, estrarre una parola casuale e settarla come secret_word. Ripete questa operazione ad intervalli regolari definiti dal parametro `time_word`. La classe è implementata utilizzando una variabile che salva la secret_word, un metodo run che legge dal file, modifica il valore di tale variabile e si mette in sleep e un metodo pubblico che ritona la secret_word. Per estrarre una parola casuale da file usa lo stream RandomAccessFile, che permtette di leggere direttamente la parola scelta casualmente. Dispone di un metodo sincronizzato che esegue la ricerca binaria di una parola all'interno di una file di parole ordinate (words.txt). Tale metodo è utilizzato per verificare se un utente ha immesso come guessed_word una parola presente nel sistema.
> **Parametri**
> -> String `file_wordlist`, percoso del file contenete le parole di gioco
> -> int `time_word`, tempo (ms) che indica la durata di una parola prima di essere cambiata

### WordleServerTask
> Classe che gestisce la singola connessione con un client. Attende comandi da parte del client, per ogni comando ricevuto esegue l'operazione associata e invia una risposta al client. Nella fase di login utilizza una CuncurrentHashMap condivisa per estrarre le informazioni del giocatore (oggetto dalla classe `AccountPlayer`) che accede. Salva l'hashcode dell'oggetto che rappresenta il player gestito su un set condiviso per notificare agli altri `WordleServerTask` che l'account in questione è attualmente loggato. Termina quando il client invia il comando il logout o exit, effettuando una scrittura nel file json per salvare le informazioni del giocatore eventualmente cambiate.

### WordleClient
> Classe Java che rappresenta il client del gioco.
> Nella fase iniziale acquisisce i dati di configurazione dal file `"ClientWordle.properties"` e avvia un thread `WordleClientMulticast` che si occupa della ricezione dei messaggi UDP inviati all'indirizzo ip di multicast dal server.
> Durante il gioco il client legge l'input da tastiera dell'utente, effettua una parte di controlli su di essi, e invia i messaggi al server specificando l'operazione richiesta e attendendo i messaggi di risposta che notificano il successo/fallimento dell'operazione. 
La classe è' suddivisa in 3 parti principale
> -> **Fase di accesso/registrazione**
> Permette all'untete di loggarsi oppure di registrarsi, non permette di andare avanti finché l'utente non si è loggato correttamente.
> -> **Menu principale**
> Esegue un loop dove chiede all'untete di eseguere una delle operazioni disponibili, come giocare, visualizzare le proprie statistiche, effettuare il logout ecc. Esce dal loop quando viene effettuato il logout.
> -> **Fase di gioco**
>  Viene eseguto quando l'utente seleziona di giocare nel menu principale. Chiede all'untete di inserire una parola (guessed_word), invia la parola al server e attende la risposta. Termina nel caso in cui il giocatore non può giocare (ha terminato i tentativi) o ha indovinato la parola.
Prima di terminare si occupa di far terminare il `WordleClientMulticast`.

### WordleClientMethond
> Classe che contiene i metodi per gestire le singole operazioni (login, register...) e le stampe su terminale. Tali metodi sono utilizzati dal `WordleClient`.

### WordleClientMulticast
> Thread che viene avviato dalla classe `WordleClient` e si occupa di ricevere e salvare i messaggi UDP mandati sull'indirizzo di multicast dal server. Viene implementato usando la classe `MulticastSocket` che permette di unirsi ad un gruppo multicast. Il thread resta in un loop di attesa di pacchetti. Appena riceve un pacchetto salva l'informazione in un una `Hashmap` e si rimette in ascolto. All'interto dell`Hashmap` le informazioni sono salvati con la chiave "nome_utente" a cui è associato il messaggio contentenente le statistiche, questo metoto permette di tenere in memoria un solo messaggio per utente. Dispone di due metodi, uno che restituisce la lista contenente lo storico di tutti i pacchetti ricevuti fino a quel momento e un metodo per arrestare il thread.
> **Parametri**
> -> String `inet_addr`, indirizzo di multicast
> -> int `port`, porta per la ricezione dei pacchetti

