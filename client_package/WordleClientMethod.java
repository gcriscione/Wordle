package client_package;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.regex.Pattern;
import java.util.ArrayList;

/**
 * Reti e laboratorio III - A.A. 2022/2023
 * WORDLE
 * 
 * Classe che contiene i metodi legati alle stampe su terminale e
 * alle operazioni (login, logout, registrazione...)
 * 
 * @author Giovanni Criscione
 *
 */
public class WordleClientMethod {

	private BufferedReader input;
	private BufferedReader in;
	private PrintWriter out;
	
	// ---- costruttore della classe
	public WordleClientMethod(BufferedReader input_term, BufferedReader input_socket, PrintWriter output_socket) {
		this.input = input_term;
		this.in = input_socket;
		this.out = output_socket;
	}
	
	
	
	//******************************************************************************
	// ----funzioni di stampa su terminale e gestione degli input dell'utente
	
	// pulisce la console del terminale
	public void ClearConsole() throws Exception{
    	//Controlla il sistema operativo del dispositivo
        String operatingSystem = System.getProperty("os.name"); 
          
        //esegue i comandi per pulire la console in base al sistema operativo
        if(operatingSystem.contains("Windows")){        
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "cls");
            Process startProcess = pb.inheritIO().start();
            startProcess.waitFor();
        } else {
            ProcessBuilder pb = new ProcessBuilder("clear");
            Process startProcess = pb.inheritIO().start();
            startProcess.waitFor();
        } 
    }
	
	// stampa menu di accesso/registrazione e gestisce l'input dell'utente ritornando il valore che indica la scelta
	public int menu_log_reg() throws Exception{
		String in = "";
		while(in.compareTo("1")!=0 && in.compareTo("2")!=0 && in.compareTo("3")!=0) {
			this.ClearConsole();
			System.out.println("\n\tBenvenuto su WORDLE <Client>");
			System.out.println("\n/---------- Fase di accesso/registrazione ----------\\");	
			System.out.println("Scegli un'operazione");
			System.out.println(" 1) Login");
			System.out.println(" 2) Registrazione");
			System.out.println(" 3) Exit");
			in = input.readLine();
		}
		return Integer.valueOf(in);
	}
	
	// stampa menu principale e gestisce l'input dell'utente ritornando il valore che indica la scelta
	public int menu_main() throws Exception{
		String in = "";
		boolean first = true;
		
		// pattern per controllare che la stringa sia un numero 
		Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");

		do {
			if(!first){ 
				this.ClearConsole(); 
				System.out.println("\n\tBenvenuto su WORDLE <Client>");
			}
			System.out.println("\n\n/---------- Menu Principale ----------\\");	
			System.out.println("Scegli un'operazione");
			System.out.println(" 1) Iniziare il gioco	");
			System.out.println(" 2) Richiedere le proprie statistiche");
			System.out.println(" 3) mostrare le notifiche riguardo le partite degli altri utenti");
			System.out.println(" 4) Logout");
			in = input.readLine();
			first = false;
		} while( !(pattern.matcher(in).matches()) || (Integer.valueOf(in)<1) || (Integer.valueOf(in)>4) );
		
		return Integer.valueOf(in);
	}
	
	// stampa menu di gioco e gestisce l'input dell'utente ritornando il valore la stringa che indica la guessed_word
	public String menu_play() throws Exception{
		String in = "";
		Boolean run = true;

		while(run) {
			System.out.print("\nInserire parola:\t");
			in = input.readLine();
			
			if(( ( in.length()!=10 && in.length()!=0) || in.contains(" ")) ){
				System.out.println("\t Inserire una parola di 10 caratteri\n\n");
			}
			else{
				run = false;
			}
		}
		return in;
	}
	
	// stampa menu di gioco e gestisce l'input dell'utente ritornando il valore la stringa che indica la guessed_word
	public void menu_share() throws Exception{
		String in = "";

		System.out.println("\n\n------- Share -------");
		System.out.println("Inserire 1 per condividere con altri utenti le statistiche di questa partita: ");
		in = input.readLine();
		if(in.compareTo("1")==0){
			System.out.println("\n\nCondivisione delle statistiche:");
			this.share();
		}
		else{
			System.out.println("\n\tStatistiche non condivise");
		}
		
		
	}
	
	

	//******************************************************************************
	// ----operazioni di interazione con WordleServer
	
	/**
	 * Operazione di Login
	 * @return  1    se l'operazione è andata a buon fine
	 * @return -1    altrimenti
	 */
	public int login() throws Exception{
		String msg = "";
		String user = "";
		String password = "";
		
		System.out.println("\n\n---- Login ----");

		// inserimento user
		System.out.print("Inserisci user:     ");
		user = input.readLine();            
                                   
		// richiesta password
		System.out.print("Inserisci password: ");
		password = input.readLine();
		
		// controlli su user e password
		if( user.contains(" ") || password.contains(" ")) {
			System.out.print("\n --> User e password non possono contenere spazi");
			return -1;
		}
		if( user.length()>25 || password.length()>25) {
			System.out.print("\n --> User e password non possono essere più lunghi di 25 caratteri");
			return -1;
		}
		if( user.length()==0 || password.length()==0) {
			System.out.print("\n --> User e password non possono essere nulli");
			return -1;
		}
		

		// creazione messaggio strutturato nel seguente modo: lunghezza_stringa_user+"-"+stringa_user+stringa_password
		msg = String.valueOf(user.length()) + "-" + user + password;

		//Invio comando di Login
		out.println(String.valueOf(Command.LOGIN));
		
		//invio dati al WordleServer
		out.println(msg);
		

		//controllo esito operazione ( lancia eccezione se non riceve un messaggio aspettato )
		try{
			switch(in.readLine()) {
			case "LOGIN":
				System.out.println("\n --> Client loggato correttamente");
				return 1;
				
			case "ERRORMESSAGE":
				System.out.println("\n --> "+in.readLine());
				return -1;
				
			default:				
				throw new CommunicationErrorServerClient("\n\tErrore di comunicazione con il Server nella fase di Login\nMessaggio ricevuto: '"+msg+"'\n");
			}
		}
		catch(NullPointerException e ){
			throw new CommunicationErrorServerClient("\n\tErrore di comunicazione con il Server nella fase di Login\nMessaggio ricevuto: '"+msg+"'\n");
		}
	}
	

	/**
	 * Operazione di Registrazione
	 */
	public void register() throws Exception{
		String msg = "";
		String user = "";
		String password = "";
		
		System.out.println("\n\n---- Registrazione ----");

		// inserimento user
		System.out.print("Inserisci user:     ");
		user = input.readLine();            
                                   
		// richiesta password
		System.out.print("Inserisci password: ");
		password = input.readLine();
		
		// controlli su user e password
		if( user.contains(" ") || password.contains(" ")) {
			System.out.println("\n --> User e password non possono contenere spazi");
			return ;
		}
		if( user.length()>25 || password.length()>25) {
			System.out.println("\n --> User e password non possono essere più lunghi di 25 caratteri");
			return ;
		}
		if( user.length()==0 || password.length()==0) {
			System.out.println("\n --> User e password non possono essere vuoti");
			return ;
		}
		

		// creazione messaggio strutturato nel seguente modo: lunghezza_stringa_user+"-"+stringa_user+stringa_password
		msg = String.valueOf(user.length()) + "-" + user + password;
		
		//Invio comando di registrazione
		out.println(String.valueOf(Command.REGISTER));
		
		//invio dati al WordleServer
		out.println(msg);
		

		//controllo esito operazione ( lancia eccezione se non riceve un messaggio aspettato )
		try{
			switch(in.readLine()) {
				case "REGISTER":
					System.out.println("\n --> Utente registrato correttamente, effettuare il login per accedere");
					return ;
				
				case "ERRORMESSAGE":
					System.out.println("\n --> "+in.readLine());
					return ;

				default:				
					throw new CommunicationErrorServerClient("\n\tErrore di comunicazione con il Server nella fase di Registrazione\nMessaggio ricevuto: '"+msg+"'\n");
			}
		}
		catch(NullPointerException e ){
			throw new CommunicationErrorServerClient("\n\tErrore di comunicazione con il Server nella fase di Registrazione\nMessaggio ricevuto: '"+msg+"'\n");
		}
	}
	

	/**
	 * Operazione di Inizio gioco
	 * @return  1    se l'operazione è andata a buon fine
	 * @return -1    altrimenti
	 */
	public int playWORDLE() throws Exception{

		// invio comando
		out.println(String.valueOf(Command.PLAY_WORDLE));

		// ricezione messaggio
		String msg = in.readLine();

		// controllo esito operazione ( lancia eccezione se non riceve un messaggio aspettato )
		try{
			switch(msg) {
				case "PLAY_WORDLE":
					System.out.println("\n\n---- Play Wordle ----");
					System.out.println(in.readLine()+"\n");
					return 1;
				
				case "ERRORMESSAGE":
					System.out.println("\n"+in.readLine().replace("/n", "\n"));
					return -1;
	
				default:				
					throw new CommunicationErrorServerClient("\n\tErrore di comunicazione con il Server nella fase di playWordle\nMessaggio ricevuto: '"+msg+"'\n");
			}
		}
		catch(NullPointerException e ){
			throw new CommunicationErrorServerClient("\n\tErrore di comunicazione con il Server nella fase di playWordle\nMessaggio ricevuto: '"+msg+"'\n");
		}
	}


	/**
	 * Operazione di invio della parola
	 * @return  1    se l'utente non può inserire un'altra guessed_word
	 * @return -1    altrimenti
	 */
	public int sendWord(String guessed_word) throws Exception{

		// invio comando
		out.println(String.valueOf(Command.SEND_WORD));

		// invio parola
		out.println(String.valueOf(guessed_word));

		// ricezione messaggio
		String msg = in.readLine();

		// controllo esito operazione ( lancia eccezione se non riceve un messaggio aspettato )
		try{
			switch(msg) {
				case "SEND_WORD":
					msg = in.readLine();
					System.out.println(msg.replace("/n", "\n"));
					return 1;

				case "WIN":
					msg = in.readLine();
					System.out.println(msg.replace("/n", "\n"));
					this.menu_share();
					return -1;
				
				case "LOSE":
					msg = in.readLine();
					System.out.println(msg.replace("/n", "\n"));
					this.menu_share();
					return -1;
					
				case "ERRORMESSAGE":
					msg = in.readLine();
					System.out.println(msg.replace("/n", "\n"));
					return -1;

				default:				
					throw new CommunicationErrorServerClient("\n\tErrore di comunicazione con il Server nella fase di sendWord\nMessaggio ricevuto: '"+msg+"'\n");
			}
		}
		catch(NullPointerException e ){
			throw new CommunicationErrorServerClient("\n\tErrore di comunicazione con il Server nella fase di sendWord\nMessaggio ricevuto: '"+msg+"'\n");
		}
	}


	/**
	 * Operazione di richiesta delle statistiche al server
	 */
	public void sendMeStatistics() throws Exception{

		System.out.println("\n\n------- Statistiche -------");
		
		// invio comando
		out.println(String.valueOf(Command.SEND_STATISTICS));
		
		// ricezione messaggio
		String msg = in.readLine();

		// controllo esito operazione ( lancia eccezione se non riceve un messaggio aspettato )
		try{
			switch(msg) {
				case "SEND_STATISTICS":
					System.out.println(in.readLine().replace("/n", "\n"));
				break;
				
				/*
				case "ERRORMESSAGE":
					System.out.println("Messaggio di errore:");
					System.out.println(in.readLine());
				break;
				*/
	
				default:				
					throw new CommunicationErrorServerClient("\n\tErrore nella fase di sendMeStatistics ("+msg+")");
			}
		}
		catch(NullPointerException e ){
			throw new CommunicationErrorServerClient("\n\tErrore nella fase di sendMeStatistics ("+msg+")");
		}
	}


	/**
	 * Operazione di richiesta di condivisione delle proprie statistiche
	 */
	public void share() throws Exception{

		// invio comando
		out.println(String.valueOf(Command.SHARE));

		// ricezione messaggio
		String msg = in.readLine();

		try{
			//controllo esito operazione ( lancia eccezione se non riceve un messaggio aspettato )
			switch(msg) {
				case "SHARE":
					System.out.println("\n --> Share effettuato");
					System.out.println(in.readLine().replace("/n", "\n")+"\n");
				break;
				
				/*
				case "ERRORMESSAGE":
					System.out.println("Messaggio di errore:");
					msg = in.readLine();
					System.out.println(msg);
				break;
				*/

				default:				
					throw new CommunicationErrorServerClient("\n\tErrore di comunicazione con il Server nella fase di share\nMessaggio ricevuto: '"+msg+"'\n");
			}
		}
		catch(NullPointerException e ){
			throw new CommunicationErrorServerClient("\n\tErrore di comunicazione con il Server nella fase di share\nMessaggio ricevuto: '"+msg+"'\n");
		}
	}


	/**
	 * Operazione di visualizzare i messaggi UDP inviati dal client
	 */
	public void showMeSharing(WordleClientMulticast wMulticast) throws Exception{

		System.out.println("\n\n------- Messaggi ricevuti -------");
		ArrayList<String> list = wMulticast.getMessageServer();

		for (int i=0; i<list.size(); i++){
			System.out.println("\n\n------- ");
            System.out.println(list.get(i));
		}

		System.out.println("\n\n------- fine -------");
	}
	

	/**
	 * Operazione di logout
	 */
	public void logout() throws Exception{
		
		// invio comando
		out.println(String.valueOf(Command.LOGOUT));
	}	

	/**
	 * Operazione di Exit (senza aver eseguito il logout)
	 */
	public void exit() throws Exception{
		
		// invio comando
		out.println(String.valueOf(Command.EXIT));

		// ricezione messaggio
		String msg = in.readLine();

		//controllo esito operazione ( lancia eccezione se non riceve un messaggio aspettato )
		try{
			switch(msg) {
				case "EXIT":
					System.out.println("Exit");
				break;
				
				case "ERRORMESSAGE":
					System.out.println("Messaggio di errore:");
					msg = in.readLine();
					System.out.println(msg);
				break;

				default:				
					throw new CommunicationErrorServerClient("\n\tErrore di comunicazione con il Server nella fase di exit\nMessaggio ricevuto: '"+msg+"'\n");
			}
		}
		catch(NullPointerException e ){
			throw new CommunicationErrorServerClient("\n\tErrore di comunicazione con il Server nella fase di exit\nMessaggio ricevuto: '"+msg+"'\n");
		}
	}
}
