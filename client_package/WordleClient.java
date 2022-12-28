package client_package;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;

/**
 * Reti e laboratorio III - A.A. 2022/2023
 * WORDLE
 * 
 *  Classe Java che rappresenta il client del gioco.
 * 
 * @author Giovanni Criscione
 *
 */
public class WordleClient {
	// variabili pubbliche
	public static final String configFile = "client_package//ClientWordle.properties";   	// percorso del file di configurazione del client.
	public static String hostname = "";        						// indirizzo ip del server WordleServer 
	public static int port = 0;                  					// numero porta del server WordleServer
    public static String multicast_addr;                            // indirizzo ip di multicast
	public static int multicast_port;                               // porta di multicast
	
	// variabili private 
	private static Socket socket = null;							// socket per la comunicazione
	private static BufferedReader input_term = null;             	// stream per la lettura dal terminale
	private static BufferedReader input_socket = null;             	// stream per la lettura dal socket
	private static PrintWriter output_socket = null;                // stream per la scrittura nel socket

	private static WordleClientMethod wcm;                  		// classe che contiene metodi per la gestione delle
	                                                                // stampe su terminale e delle operazioni (login, logout ...)
	private static WordleClientMulticast wMulticast;                // thread che resta in ascolto di notifiche UDP da parte del server

	public static void main(String[] args) {

		//-----------------------------------------------------------------------------------------------------------

		try{
			//  ---- Fase di inizializzazione delle risorse -----------------------------
			//acquisizione dati dal file di configurazione
			readConfig(configFile);

			// apertura socket e stream di input e output relativi ad esso
			socket = new Socket(hostname, port);
			input_socket = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			output_socket = new PrintWriter(socket.getOutputStream(), true);
			
			//classe contenente i metodi utilizzati dal WordleClient
			input_term = new BufferedReader(new InputStreamReader(System.in));
			wcm = new WordleClientMethod(input_term, input_socket, output_socket);
			
			// avvio thread che sta in ascolto su un indirizzo di broadcast di messaggi UDP inviati dal server
			wMulticast = new WordleClientMulticast(multicast_addr, multicast_port);
			Thread thread = new Thread (wMulticast);   
			thread.start();
			
			// stampa informazioni
			Thread.sleep(1000);          // aspetta avvio del thread WordleClientMulticast
			System.out.println("\n[WordleClient] avviato");
			System.out.println("[WordleClient] Connesso al Server ["+socket.getLocalSocketAddress()+"]");
			System.out.println("Premere un tasto per proseguire");
			input_term.readLine();



			//  ---- Fase di accesso / registrazione -------------------------------------
			Boolean login = true;
			int result = 0;
			while(login) {		
				// stampa menu principale e ritorna:
				//   1: se l'utente vuole fare il login		
				//   2: se l'utente vuole registrarsi
				//   3: se l'utente vuole uscire
				switch(wcm.menu_log_reg()) {
					case 1:
						//login, ritorna 1: se l'operazione di login si è conclusa con successo
						result = wcm.login();

						// se effettua correttamente il login non ripete la fase di accesso/registrazione
						if(result==1) {  login = false;  }     
					break;
					
					case 2:
						// registrazione
						wcm.register();
					break;

					case 3:
						// uscita
						wcm.exit();
						return;
				}

				System.out.print("\nPremere un tasto per proseguire");
				input_term.readLine();
			}
			
			
			//  ---- Menu principale  -------------------------------------
			wcm.ClearConsole();
			System.out.println("\n\tBenvenuto su WORDLE <Client>");
			Boolean logout = false;    // se settato a false chiude il client
			Boolean playing = true;    // se settato a false esce dalla fase di inserimento guessed_word
			String guessed_word = "";				
			while( !logout ) {		

				// stampa menu principale, ritorna:
				//   1: se l'utente vuole iniziare il gioco			
				//   2: se l'utente richiede le proprie statistiche
				//   3: se l'utente richiede le notifiche riguardo le partite degli altri utenti
				//   4: se l'utente vuole effettuare il logout
				switch(wcm.menu_main()) {
					case 1:
						// playWORDLE() richiesta di inizio della fase di gioco, ritorna 
						//   1: se il player può giocare
						//  -1: se ha esaurito tutti i tentativi
						if ( wcm.playWORDLE()==1 ){
							System.out.print("Premere invio per smettere di inviare parole");
							playing  = true;
							while(playing){
								//ritorna la stringa che rappresentato la guessed word
								guessed_word = wcm.menu_play();
								if(guessed_word.compareTo("")==0)
								{
									System.out.println("\n\n------- fine -------");
									playing = false;
								}
								else {
									// sendWord() invia la guessed_word al server, ritorna 
									//  1: se l'utente non può inserire un'altra guessed_word
									// -1: altrimenti
									if( wcm.sendWord(guessed_word)==-1 ){
										playing = false;
									}
								}	
							}
						}
					break;
					
					case 2:
						// sendMeStatistics() richiede al server le proprie statistiche
						wcm.sendMeStatistics();
					break;
					
					case 3:
						// showMeSharing() mostra tutte le notifiche ricevute dal client sull'indirizzo di multicast
						wcm.showMeSharing(wMulticast);
					break;
					
					case 4:
						// logout() chiede al server di fare il logout dell'account
						wcm.logout();

						// esce da ciclo
						logout = true;
					break;
						
				}
			}
			
		}
		catch(Exception e) {
			System.out.println("[WordleClient] errore imprevisto!\n");
			e.printStackTrace();
		}
		finally {
			System.out.println("\n\n\n/---------- Terminazione ----------\\");	
			// stop WordleClientMulticast
			wMulticast.stop();

			// chiusura socket e stream
			try {
				if(socket!=null) 		{  socket.close(); }
				if(input_socket!=null)  {  input_socket.close(); }
				if(output_socket!=null) {  output_socket.close(); }
			}
			catch(Exception e) {
				System.out.println("[WordleClient] errore terminazione!\n\n");
				e.printStackTrace();
			}
			System.out.println("[WordleClient] terminato\n\n");
		}
	}
	
	

	//******************************************************************************
	// estrae i dati dal file di configurazione
	public static void readConfig(String configFile) throws Exception {
		InputStream input = new FileInputStream(configFile);
	    Properties prop = new Properties();
	    prop.load(input);

		hostname = prop.getProperty("hostname");
		port = Integer.parseInt(prop.getProperty("port"));
		multicast_addr = prop.getProperty("multicast_addr");
		multicast_port = Integer.parseInt(prop.getProperty("multicast_port"));
		
	    input.close();
	}
}
