package server_package;

/**
 * Reti e laboratorio III - A.A. 2022/2023
 * WORDLE
 * 
 * Questo thread si occupa di leggere dal file contenente le parole di gioco, 
 * estrarre una parola casuale e settarla come secret_word. 
 * Ripete questa operazione ad intervalli regolari definiti da un parametro.
 * Fornisce anche un metodo sincronizzato per effettuare la ricerca binaria di una
 * parola all'interno di un file di parole (es. words.txt).
 * 
 * @author Giovanni Criscione
 *
 */
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ThreadLocalRandom;

public class SelectionWord implements Runnable{

	private String secret_word;      // parola da indovinare 
	private int number_sw;           // numero di sequenza della secret_word
	private String file_wordlist;    // file che contiene tutte le parole di gioco
	private int time_word;           // tempo di durata della secret word (allo scadere viene cambiata)
	private volatile boolean exit;   // variabile che server a far terminare il thread
	private RandomAccessFile file;   // stream per la lettura del file

	/**
     *  Costruttore della classe SelectionWord
     *  @param file_wordlist   percorso del file
     *  @param time_word       tempo in millisecondi
     */
	public SelectionWord(String file_wordlist, Integer time_word) {
		this.file_wordlist = file_wordlist;
		this.time_word = time_word;
		this.secret_word = "";
		this.number_sw = 0;
		exit = false;
	}
	
	public void run() {
		
		System.out.println("[SELECTION_WORD] attivo");
		try {	
			this.file = new RandomAccessFile(this.file_wordlist, "r");
			
			
			while( !this.exit ) {
				// estrae dal file una parola random e la setta come secret_word,
				// effettua una sleep per attendere un tempo fissato prima di
				// aggiornare nuovamente la parola
				// esce da ciclo quando exit viene settato a true (nel metodo public void stop())
				this.secret_word = random_word();
				this.number_sw++;
				System.out.println("[SELECTION_WORD] secret_word: "+this.number_sw+")  "+this.secret_word);
				Thread.sleep(this.time_word);
			}
		}
		catch(Exception e){
			System.out.println("[SELECTION_WORD] Errore!");
			e.printStackTrace();
		}
		finally{
			// chiude lo stream per la lettura del file
			if( this.file!=null ) { 
				try {
					this.file.close(); 
				}
				catch(Exception e){
					System.out.println("[SELECTION_WORD] Errore!");
					e.printStackTrace();
				}
			}
			
			System.out.println("\n[SELECTION_WORD] terminato");
		}
	}
	
	
	
	
	//---------------------------------------------------------------------------------------
	
	/**
	 * Ritorna una parola casuale letta dal file
	 * @return ritorna la parola trovata
	 * @throws IOException in caso di errore di lettura dal file
	 */
	public String random_word() throws IOException {
		// numero di byte per ogni stringa formata da 10 caratteri + "\n" (totale 11 caratteri)
		final int StringByte = (11); 			
		// calcola il numero di parole presenti nel file
		final int numElements =( (int) this.file.length()) / StringByte;
		
		// calcola una posizione casuale compresa tra 0 e numero massimo di elementi
		int casual = ThreadLocalRandom.current().nextInt(0, numElements + 1);
		
		//legge e ritorna la stringa
		this.file.seek(casual * (StringByte));
		return this.file.readLine();

	}
	
	/**
	 * Esegue una ricerca binaria della chiave specificata nel file in input
	 * @note  mettere '\n' anche nell'ultima parola del file words.txt (altrimenti ne conta una in meno)
	 * @param key parola da cercare
	 * @return ritorna true se la parola è stata trovata, altrimenti false
	 * @throws IOException in caso di errore di lettura dal file
	 */
	private boolean binarySearch(String key) throws IOException {
		// numero di byte per ogni stringa formata da 10 caratteri + "\n" (totale 11 caratteri)
		final int StringByte = (11); 			
		// calcola il numero di parole presenti nel file
		final int numElements =( ( (int) this.file.length()) / StringByte);
		
		// variabili per il limite inferiore, superiore e metà
		int lower = 0; 
		int upper = numElements - 1;
		int mid = 0;
		
		// ricerca binaria
		while (lower <= upper) {
			mid = (lower + upper) / 2;
			
			// lettura parola in posizione mid
			this.file.seek(mid * (StringByte));
			String value = this.file.readLine();

			if (key.compareTo(value)==0) {
				return true;
			}
			if (key.compareTo(value)<0) {
				upper = mid - 1;
			}
			else {
				lower = mid + 1;
			}
		}
		return false;
	}

	/**
	 * Metodo sincronizzato per eseguire una ricerca binaria di una parola su un file di parole
	 * @param key parola da cercare
	 * @return ritorna true se la parola è stata trovata, altrimenti false
	 * @throws IOException in caso di errore di lettura dal file
	 */
	public boolean binarySearch_synchronized(String key) throws IOException {

		boolean res = false;
		synchronized(this.file){
			res = this.binarySearch(key);
		}
		return res;
	}


	public String getSecretWord() {
		return this.secret_word;
	}
	
	public int getNumberSecretWord() {
		return this.number_sw;
	}
	
	public String getFileWordlist() {
		return this.file_wordlist;
	}
	
	public int getTimer() {
		return this.time_word;
	}

	// funzione per terminare il ciclo while
	public void stop(){
		exit = true;

		try {
			Thread.sleep(3000);
		} catch (Exception e) {
			;
		}finally{
			System.exit(1);
		}
	}
}

