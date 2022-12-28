package client_package;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Reti e laboratorio III - A.A. 2022/2023
 * WORDLE
 * 
 * Thread che viene avviato da client e si occupa di ricevere e salvare
 * i messaggi UDP mandati sull'indirizzo di multicast dal server
 * 
 * @author Giovanni Criscione
 *
 */
public class WordleClientMulticast implements Runnable {
    
	//dichiarazione variabili principali
    private String INET_ADDR; 						// indirizzo di multicast
    private int PORT;                               // porta dove ricevere i pacchetti
    private InetAddress address = null;             // conversione dell'indirizzo ip
	MulticastSocket multicastSocket = null;         // socket multicast
	private static HashMap<Integer, String> message_server;   // mappa contenente tutti i messaggi ricevuti dal server
    
    
    public WordleClientMulticast(String inet_addr, int port) {
    	this.INET_ADDR = inet_addr;
        this.PORT = port;
        this.message_server = new HashMap<Integer, String>();
    }
    
 
    public void run() {
    	System.out.println("\n\n[WordleClientMulticast] Avviato");
    	
    	try {
	    	// prende l'indirizzo ip
	    	address = InetAddress.getByName(INET_ADDR);
    	}
    	catch(Exception ex) {
    		System.out.println("[WordleClientMulticast] !Errore indirizzo di multicast!");
    		ex.printStackTrace();
    	}
    	

    	//ricezione pacchetti multi-cast
    	try{
			multicastSocket = new MulticastSocket(PORT);
    		
			// crea un buffer di byte, che verrà utilizzato per memorizzare
	        // i byte in entrata contenenti le informazioni dal server.
	        // poiché qui il messaggio è piccolo, 256 byte dovrebbero essere sufficienti.
	        byte[] buf = new byte[512];
	         
	        // crea un nuovo socket multi-cast
	        multicastSocket.joinGroup(address);
	        //multicastSocket.setSoTimeout(300000);
	        
	        System.out.println("[WordleClientMulticast] Connesso al gruppo multicast "+address.getHostAddress()+", porta "+multicastSocket.getLocalPort()+"]");
	        System.out.println("[WordleClientMulticast] in attesa di pacchetti...");
	        
	        // attesa pacchetti
			String in = "";
			String user = "";
            while(true){
                // ricezione pacchetto e salvataggio nell'arraylist
                DatagramPacket msgPacket = new DatagramPacket(buf, buf.length);
                
				// riceve messaggi sull'indirizzo di multicast, quando il client chiude la multicastSocket
				// viene sollevata una SocketException ed esce dal ciclo
                //gestione ritardo ricezione pacchetto
                try {
                	multicastSocket.receive(msgPacket);
                }
                catch (SocketTimeoutException ex) {  
                	System.out.println("[WordleClientMulticast] !Timeout ricezione pacchetto scaduto!");
        			ex.printStackTrace();
        			System.exit(1);
        		}
	        	catch(SocketException e){
	        		break;
	        	}
 
				in = new String(buf, 0, buf.length).replace("/n", "\n").trim();
				user = in.substring(0, in.indexOf("\n"));
				message_server.put(user.hashCode(), in);
            }
        }
    	catch (Exception ex) {
    		System.out.println("[WordleClientMulticast] !Errore!\n");
            ex.printStackTrace();
        }
    	finally {
			if (multicastSocket!=null){ multicastSocket.close(); }
    		System.out.println("[WordleClientMulticast] terminato\n");
    	}
    }
    
    public ArrayList<String> getMessageServer() {
    	
    	ArrayList<String> list = new ArrayList<String>();

		for (HashMap.Entry<Integer, String> set : message_server.entrySet()){
			list.add(set.getValue());
		}
    	
    	
    	return list;
    }

	public void stop(){
		multicastSocket.close();
	}
}
