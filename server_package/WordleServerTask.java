package server_package;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reti e laboratorio III - A.A. 2022/2023
 * WORDLE
 *
 * Thread che gestisce la singola connessione con il WordleClient
 * 
 * 
 * @author Giovanni Criscione
 *
 */
public class WordleServerTask implements Runnable{

	// Socket e stream per la comunicazione con il client.
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	
	// informazioni giocatore
	private AccountPlayer player = null;               // oggetto che contiene tutti i dati del giocatore
	boolean player_lose = false;                       // indica se il giocatore è in uno stato di sconfitta
	                                                   // server per segnare la partita come persa del giocatore nel caso esegua il logout
													   // senza aver concluso tutti i tentativi o aver vinto
	
	
	// strutture condivisa tra i thread WordleServerTask
	private ConcurrentHashMap<Integer, AccountPlayer> map;   // contenente le informazioni degli account salvati
	private HashSet<Integer> set;                            // contiene hashcode degli account che sono in sessione

	// numero e parola segreta
	private int number_secret_word;
	private String secret_word;
	
	/**
	 * Costruttore WordleServerTask
	 * @param socket   socket aperta con il client
	 * @param m        mappa condivisa
	 * @param s        set condiviso
	 */
	public WordleServerTask(Socket socket, ConcurrentHashMap<Integer, AccountPlayer> m, HashSet<Integer> s) {
		this.socket = socket;
		this.number_secret_word = 0;
		this.secret_word = "";  
		this.map = m;
		this.set = s;
	}


	public void run() {
		System.out.println("\n[ip: "+this.socket.getRemoteSocketAddress()+"]  Connected");
		
		try {
			// apre gli stream sulla socket
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
			
			// Riceve i comandi dal client, esegue le operazione, risponde al client
			// si rimette in ascolto di altri comandi
			// termina quando il client invia il comando di logout e run viene settato a false
			Boolean run = true;
			while(run) {
				switch(in.readLine()) {
				
					case "LOGIN":
						System.out.println("\n[ip: "+this.socket.getRemoteSocketAddress()+"]  login");
						login();
					break;
					
					case "REGISTER":
						System.out.println("\n[ip: "+this.socket.getRemoteSocketAddress()+"]  registration");
						registration();
					break;
					
					case "PLAY_WORDLE":
						System.out.println("\n[ip: "+this.socket.getRemoteSocketAddress()+"]  PLAY_WORDLE");
						play_wordle();
					break;

					case "SEND_WORD":
						System.out.println("\n[ip: "+this.socket.getRemoteSocketAddress()+"]  SEND_WORD");
						recived_word();
					break;
					
					case "SEND_STATISTICS":
						System.out.println("\n[ip: "+this.socket.getRemoteSocketAddress()+"]  SEND_STATISTIC");
						send_statistics();
					break;
					
					case "SHARE":
						System.out.println("\n[ip: "+this.socket.getRemoteSocketAddress()+"]  SHERE");
						share();
					break;
					
					case "LOGOUT":
						System.out.println("\n[ip: "+this.socket.getRemoteSocketAddress()+"]  logout");
						logout();
						run = false;
					break;
					
					case "EXIT":
						System.out.println("\n[ip: "+this.socket.getRemoteSocketAddress()+"]  EXIT");
						exit();
						run = false;
					break;
	
					default:
						throw new CommunicationErrorServerClient("\n\tErrore nella fase di ricezione del comando da parte del client");
				}		
			}
	    }
		catch (NullPointerException|SocketException e) {
			System.out.println("[ip: "+this.socket.getRemoteSocketAddress()+"]  !Errore interruzzione imporvvisa della connessione con il client!");
	        e.printStackTrace();
	    }
		catch (Exception e) {
			System.out.println("[ip: "+this.socket.getRemoteSocketAddress()+"]  !Errore!");
	        e.printStackTrace();
	    }
		finally {
			try {
				//chiusura socket e stream
				if(socket!=null) {  socket.close(); }
				if(in!=null)     {  in.close();     }
				if(out!=null)    {  out.close();    }
			}
			catch(Exception e) {
				System.out.println("[ip: "+this.socket.getRemoteSocketAddress()+"]  !Errore nel finally!");
				e.printStackTrace();
			}
			
			// Salvataggio dei dati
			WordleServer.synchronized_write_json();
			
			// logout dell'utente
			logout(); 
			
			System.out.println("[ip: "+this.socket.getRemoteSocketAddress()+"]  Connected Close");
		}
	}



	// Metodi privati --------------------------------------------------------------------------


	// Metodi privati --------------------------------------------------------------------------



	// Metodi privati --------------------------------------------------------------------------

	private void login() throws Exception {	
		String in_socket;
		String user = "";
		String password = "";
		int dim_num = 0;
		int dim_user = 0;
		
		
		//riceve messaggio strutturato: lunghezza_stringa_user+"-"+stringa_user+stringa_password
		in_socket = in.readLine();     
		
		// estrae user e password dal messaggio
		dim_num = in_socket.indexOf("-");
		dim_user = Integer.valueOf(in_socket.split("-")[0]) + dim_num + 1;
		user = in_socket.substring(dim_num+1, dim_user);
		password = in_socket.substring(dim_user, in_socket.length());

		// ---- Controlli
	 	
		// si assicura che il client non si sia già loggato
		if(player!=null){
			out.println(String.valueOf(Command.ERRORMESSAGE));
			out.println("Risulta già un utente loggato");
			return ;
		}


		// ricerca credenziali nella HashMap condivisa (verifica se l'utente è registrato)
		if(map.containsKey(user.hashCode())) {
			if(map.get(user.hashCode()).check_password(password)) {

				// controlla se il giocatore risulta attualmente loggato con un altro client,
				// nel caso non risulti lo inserisce nel set di giocatori in sessione attiva
				Boolean log = false;
				synchronized(set) {			
					if( set.contains(user.hashCode()) ){
						log = true;
					}
					else{
						set.add(user.hashCode());
					}
				}

				if(log){
					out.println(String.valueOf(Command.ERRORMESSAGE));
					out.println("User '"+ user +"' risulta attualmente loggato");
				}
				else{
					player = map.get(user.hashCode());
					out.println(String.valueOf(Command.LOGIN));
				}
			}
			else {
				out.println(String.valueOf(Command.ERRORMESSAGE));
				out.println("User '"+ user +"': password errata");
			}
		}
		// coso credenziali sbagliate
		else {
			out.println(String.valueOf(Command.ERRORMESSAGE));
			out.println("User '"+ user +"' non prensete tra gli utenti registrati");
		}
	}
	
	
	private void registration() throws Exception {
		String in_socket;
		String user = "";
		String password = "";
		int dim_num = 0;
		int dim_user = 0;
		
		//riceve messaggio strutturato: lunghezza_stringa_user+"-"+stringa_user+stringa_password
		in_socket = in.readLine();     
		
		// estrae user e password dal messaggio
		dim_num = in_socket.indexOf("-");
		dim_user = Integer.valueOf(in_socket.split("-")[0]) + dim_num + 1;
		user = in_socket.substring(dim_num+1, dim_user);
		password = in_socket.substring(dim_user, in_socket.length());


		// controlla se esiste già un user memorizzato con lo stesso nome
		if(map.containsKey(user.hashCode())) {
			String s = "User "+ user+" risulta gia' registrato, cambiare nome user";
			out.println(String.valueOf(Command.ERRORMESSAGE));
			out.println(s);
		}
		else {
			// crea account player e salva su HashMap 
			player = new AccountPlayer(user, password);
			map.put(player.hashCode(), player);
			player = null;
			out.println(String.valueOf(Command.REGISTER));
		}
	}
	
	
	//
	private void play_wordle() {
		// prende la secret_word (parola del gioco da indovinare)
		number_secret_word = WordleServer.getNumberSecretWord();
		
		// controlla se ha esaurito i tentativi disponibili per giocare
		if( player.remaing_attempt(number_secret_word) ){

			// memorizza nei dati del player la nuova parola per cui sta giocando
			player.setLastSecretWord(number_secret_word);

			player_lose = true;      // segna che il giocatore si trova in uno stato di potenziale di perita partia

			out.println(String.valueOf(Command.PLAY_WORDLE));
			out.println("Tentativi rimasti: "+(12-player.getAttemptsUsed()));
		}
		else{
			out.println(String.valueOf(Command.ERRORMESSAGE));
			out.println("Non hai tentativi per giocare con questa parola, aspetta la prossimi.");
		}
	}
	
	
	//
	private void recived_word() throws Exception
	{
		// leggere la secret_word attuale
		secret_word = WordleServer.getSecretWord();
		
		// prende la guessed_word inviata dall'utente
		String guessed_word = in.readLine();
		assert(guessed_word.length()==10);

		// controlla se ha ancora tentativi
		if( !(player.remaing_attempt(number_secret_word)) ){
			out.println(String.valueOf(Command.ERRORMESSAGE));
			out.println("Non hai tentativi per giocare con questa parola, aspetta la prossimi.");
			return ;
		}

		// controlla se la parola è presente nel dizionario
		if ( WordleServer.synchronized_find_word(guessed_word)==false ){
			out.println(String.valueOf(Command.SEND_WORD));
			out.println(" --> parola non presente nel dizionario");
			return ;
		}

		// incrementa tentativi
		player.use_attempt();

		// controlla se ha indovinato la parola
		if( guessed_word.compareTo(secret_word)==0){
			out.println(String.valueOf(Command.WIN));
			out.println("/n --> Complienti hai indovinato la secret_word '"+guessed_word+"'!!!/n/n");
			player.setGameStats("++++++++++");
			player.wins();
			player_lose = false;
		}
		else{
			String tmp = "";
			String s = "Suggerimento:/n";
			s += " -> guessed_word:   "+guessed_word;
			s += "/n -> secret_word:    ";
			
			// colorazione parola suggerimento
			//grigio:’X’, verde: ‘+’, giallo: ‘?’
			for(int i=0; i<guessed_word.length(); i++){
				if(guessed_word.charAt(i) == secret_word.charAt(i)){
					s += "+";
					tmp += "+";
				}
				else if( secret_word.indexOf(guessed_word.charAt(i))!=-1 ){
					s += "?";
					tmp += "?";
				}
				else{
					s += "X";
					tmp += "X";
				}
			}
			player.setGameStats(tmp);

			// controlla se ha esaurito i tentativi
			if(player.getAttemptsUsed()>=12){
				s += "/n/n\tAttenzione questo era il tuo ultimo tentativo/n";
				out.println(String.valueOf(Command.LOSE));
				out.println(s);
				player.lose();
			}
			else {
				s += "/n/n Tentativi utilizzati "+player.getAttemptsUsed()+", rimanenti: "+(12-player.getAttemptsUsed());
				out.println(String.valueOf(Command.SEND_WORD));
				out.println(s);
			}

		}
	}
	
	// 
	private void send_statistics() {
		assert(player!=null);
		
		out.println(String.valueOf(Command.SEND_STATISTICS));
		out.println(player.getStatistics());
	}
	
	//
	private void share() {
		assert(player!=null);
		
		WordleServer.shareStatistics(player.getlast_game_stats());
		
		out.println(String.valueOf(Command.SHARE));
		out.println(player.getlast_game_stats());
	}
	
	
	// notifica al client l'esecuizione del logout
	private void logout() {
		assert(player!=null);
		
		if(player_lose) {
			player.lose();
			player_lose = false;
		}
		
		// rimuove player dalla set di player attualmente connessi 
		synchronized(set) {			
			set.remove(player.hashCode());
		}
	}
	
	// notifica al client l'esecuizione del logout
	private void exit() {		
		out.println(String.valueOf(Command.EXIT));
		System.out.println("[ip: "+this.socket.getRemoteSocketAddress()+"]  EXIT");
	}
}
