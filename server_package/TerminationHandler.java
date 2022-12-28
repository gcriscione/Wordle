package server_package;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Reti e laboratorio III - A.A. 2022/2023
 * WORDLE
 * 
 * Classe che implementa l'handler di terminazione del server
 * Questo thread viene avviato al momento della pressione dei tasti CTRL+C.
 * Lo scopo e' quello di far terminare il main del server bloccato sulla accept()
 * in attesa di nuove connessioni e chiudere il pool di thread.
 * 
 * @author Giovanni Criscione
 *
 */
public class TerminationHandler extends Thread {
	private ServerSocket serverSocket;     // socket utilizzata dal server 
	private ExecutorService pool;          // threadpool utilizzato dal server
	private int maxDelay;                  // tempo di attesa per la terminazione del threadpool
	
	/**
     *  Costruttore della classe TerminationHandler
     *  @param serverSocket   riferimento al socket
     *  @param pool           riferimento al threadpool
	 *  @param maxDelay       tempo in millisecondi
     */
	public TerminationHandler(ServerSocket serverSocket, ExecutorService pool, int maxDelay) {
		this.maxDelay = maxDelay;
		this.pool = pool;
		this.serverSocket = serverSocket;
	}
	
	public void run() {
		
		//avvio procedura di terminazione del server
        System.out.println("[TERMINATION_HANDLER] Avvio terminazione WordleServer...");
        
        //chisura ServerSocket
        try{
        	serverSocket.close();
        }
        catch (IOException e) {
        	e.printStackTrace();
        }
        
        //terminazione del threadpool
        pool.shutdown();
	    try {
	        if (!pool.awaitTermination(maxDelay, TimeUnit.MILLISECONDS)){
	        	pool.shutdownNow();
	        }
	    } 
	    catch (InterruptedException e){
	    	pool.shutdownNow();
	    	e.printStackTrace();
	    }
        System.out.println("[TERMINATION_HANDLER] Terminato");
	}
}

