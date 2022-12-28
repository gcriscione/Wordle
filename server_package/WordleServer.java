package server_package;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;


/**
 * Reti e laboratorio III - A.A. 2022/2023
 * WORDLE
 *
 * Classe Java che rappresenta il server del gioco Wordle
 * 
 * Il server gestisce un pool di thread ed esegue un ciclo nel quale:
 * (1) accetta richieste di connessione da parte dei vari client;
 * (2) per ogni richiesta, attiva un thread per interagire con il client;
 * 
 * Inoltre dispone di alcuni metodi sincronizzati che utilizzano i thread 
 * 
 * @author Giovanni Criscione
 *
 */
public class WordleServer {
	public static final String configFile = "server_package//ServerWordle.properties";    // Percorso del file di configurazione del client
	public static String jsonFile;                                  // Percorso del file json contenente le informazioni dei giocatori
	public static String path_wordlist;                         	// percorso file contenente le parole di gioco

	private static SelectionWord sw;                                // thread che fornisce e aggiorna la secret_word
	private static int time_word;		                            // tempo durata di una secret_word prima che venga cambiata (in ms)
	
	private static ServerSocket serverSocket;						// serversocket per l'instaurazioni di connessioni
	private static int port;                  					    // numero porta di ascolto del server WordleServer
	
	private static ExecutorService pool;    						// thread-pool per la gestione dei client
	private static int num_thread;									// numero di thread per il thread-pool
	private static int maxDelay;                             		// tempo di attesa (in ms) per la terminazione del server dopo l'avvio di chiusura
	
    private static String multicast_addr;                            // indirizzo ip di multicast
	private static int multicast_port;                               // porta di multicast
	private static InetAddress multicast_inet;                                  


	// mappa condivisa tra i thread WordleServerTask contenente le informazioni degli account salvati
	private static ConcurrentHashMap<Integer, AccountPlayer> map = new ConcurrentHashMap<Integer, AccountPlayer>();

	// set contenente gli hashCode degli account loggati
	private static HashSet<Integer> set = new HashSet<Integer>();   //gestire sincronizzazione,
	


	public static void main(String[] args) {

		try{
			//acquisizione dati dal file di configurazione
			readConfig(configFile);

			// controllo indirizzo di multicast
			multicast_check();
			
			// carica in memoria (nella HashMap) tutti gli account registrati sul file jsonFile
			read_json();

			
			// avvio thread SelectionWord
			sw = new SelectionWord(path_wordlist, time_word);
			Thread thread = new Thread (sw);   
			thread.start();
			
			
			// Creazione del threadpool e Socket
			pool = Executors.newFixedThreadPool(num_thread);
			serverSocket = new ServerSocket(port);
			
			// setta handler per la terminazione del WordleServer
			Runtime.getRuntime().addShutdownHook(new TerminationHandler(serverSocket, pool, maxDelay));
			


			//   ---- inizio fase di ascolto del server
			System.out.printf("\n\n[SERVER] In ascolto sulla porta: %d\n", port);
			while (true) {
				Socket socket = null;
	        	
	        	// accetta le richieste dei client, quando TerminationHandler chiude la ServerSocket
				// viene sollevata una SocketException ed esce dal ciclo
	        	try {
	        		socket = serverSocket.accept();
	        	}
	        	catch(SocketException e){
	        		break;
	        	}
	        	
	        	//avvio WordleServerTask per interagire con il client
				pool.execute(new WordleServerTask(socket, map, set));
			}
			
		}
		//eccezione porta occupata
		catch(BindException ex){
			System.out.println("\n[WORDLE_SERVER]\t!-- Errore porta "+port + " occupata --!\n");
			ex.printStackTrace();
		}
		catch(Exception e) {
			System.out.println("\n[WORDLE_SERVER]\t!-- Errore --!");
			e.printStackTrace();
		}
		finally {
			// chiude SelectionWord
			sw.stop();

			// chiude ServerSocket, threadpool, ServerMulticast
			try {
				if(serverSocket!=null) 		{  serverSocket.close();  }
				if(pool!=null) 				{  pool.shutdownNow();  }
			}
			catch(Exception e) {
				e.printStackTrace();
			}

			// salvataggio degli AccountPlayer sul file jsonFile
			write_json();
			
			System.out.println("\n[WORDLE_SERVER] Terminato");
		}
	}
	
	
	

	// Metodi privati --------------------------------------------------------------------------
	// estrae i dati dal file di configurazione e sette hostname e port
	private static void readConfig(String configFile) throws Exception {
		InputStream input = new FileInputStream(configFile);
		Properties prop = new Properties();
		prop.load(input);
		
		port = Integer.parseInt(prop.getProperty("port"));
		num_thread = Integer.parseInt(prop.getProperty("num_thread"));
		maxDelay = Integer.parseInt(prop.getProperty("maxDelay"));
		path_wordlist = prop.getProperty("path_wordlist");
		time_word = Integer.parseInt(prop.getProperty("time_word"));
		jsonFile = prop.getProperty("jsonFile");
		multicast_addr = prop.getProperty("multicast_addr");
		multicast_port = Integer.parseInt(prop.getProperty("multicast_port"));
		
		input.close();
	}


	// termina il server se l'indirizzo di multicast non è corretto
	private static void multicast_check(){
		try{		    		
			// prende l'indirizzo ip di multicast
			multicast_inet = InetAddress.getByName(multicast_addr);
			
			// controlla l'indirizzo di multicast
			if(!multicast_inet.isMulticastAddress()) {
				System.out.println("!-- Errore indirizzo ip di dategroup ("+multicast_addr+") --!");
				System.out.println("!-- Non è un indirizzo di multicast --!\n\n");
				System.exit(1);
			}
		}
		catch(UnknownHostException e){
			System.out.println("!-- Errore indirizzo ip di dategroup ("+multicast_addr+") --!\n\n");
			System.out.println("[WORDLE_SERVER] Terminato");
			System.exit(1);
		}
	}



	// legge il file jsonFile e salva tutti gli oggetti AccountPlayer nella HashMap
	private static void read_json() {
		Gson gson = new Gson();
		
		// lettura file
		try (JsonReader reader = new JsonReader(
				new InputStreamReader(
						new FileInputStream(jsonFile)));
		) 
		{
			// GSON legge la parentesi quadra aperta "["
			reader.beginArray();
			
			// itera su tutti gli oggetti contenuti nella lista
			while (reader.hasNext()) {
				
				// deserializza il singolo AccountPlayer e lo salva nella HashMap
				AccountPlayer account = gson.fromJson(reader, AccountPlayer.class);
				map.put(account.hashCode(), account);
			}

			reader.endArray();
		}
		
		// crea il file nel caso non esiste
		catch(FileNotFoundException e) {
			System.out.println("[WORDLE_SERVER] File json non trovato:\t"+jsonFile);
			PrintWriter writer = null;
			
			try {
				// crea un file json compatibile per la lettura con beginArray() e endArray();
				writer = new PrintWriter(jsonFile, "UTF-8");
				writer.print("[]");
				System.out.println("[WORDLE_SERVER] Creato file json:\t\t"+jsonFile);
				writer.close();
			} 
			catch (Exception e2) {
				if(writer!=null) {  writer.close();  }
				System.out.println("[WORDLE_SERVER] Errore creazione file json:\t\t"+jsonFile);
				e2.printStackTrace();
				System.out.println("\n[WORDLE_SERVER] Terminato");
				System.exit(1);
			}
			finally{
				if(writer!=null) {  writer.close();  }
			}
		}
		catch(Exception e){
			System.out.println("[WORDLE_SERVER] Errore con il file json:\t\t"+jsonFile);
			e.printStackTrace();
			System.out.println("\n[WORDLE_SERVER] Terminato");
			System.exit(1);
		}
	}


	// scrive nel file jsonFile tutti gli oggetti AccountPlayer salvati nella HashMap
	private static void write_json() {
		// Inizializzazione GSON.
		GsonBuilder builder = new GsonBuilder();
		Gson gson = builder.setPrettyPrinting().create();

		try (PrintWriter out = new PrintWriter(jsonFile)) {

			out.print(gson.toJson(map.values()));
			
			System.out.println("File json scritto correttamente in: "+ new File(jsonFile).getAbsolutePath());
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
		
	
	
	// Metodi Pubblici Sincronizzati utilizzati dai thread WordleServerTask --------------------------
	// scrittura del file json sincronizzata
	public static void synchronized_write_json() {
		synchronized(map){
			write_json();
		}
	}

	// restituisce la secret_word
	public static String getSecretWord() {
		return sw.getSecretWord();
	}
	
	// restituisce il numero della secret_word
	public static int getNumberSecretWord() {
		return sw.getNumberSecretWord();
	}

	// ricerca binaria di una parola all'interno del file contente le parole di gioco ordinate (words.txt)
	public static boolean synchronized_find_word(String key) throws IOException{
		return sw.binarySearch_synchronized(key);
	}

	// invia le statistiche ricevute come stringa nell'indirizzo di multicast
	public static void shareStatistics(String statistics) {
		DatagramSocket serverMulticast = null;    
		try {
				// apre un DatagramPacket dove saranno inviati i messaggi
				serverMulticast = new DatagramSocket();
				DatagramPacket msgPacket = new DatagramPacket(statistics.getBytes("US-ASCII"), statistics.getBytes().length, multicast_inet, multicast_port);
				serverMulticast.send(msgPacket);
		
		} 
		catch (Exception ex) {
			System.out.println("\n[WORDLE_SERVER] !-- Errore invio statistiche su indirizzo multicast --!");
			ex.printStackTrace();
		}
		finally{
			if(serverMulticast !=null)	{  serverMulticast.close(); }
		}
	}
}
