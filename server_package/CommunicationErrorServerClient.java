package server_package;

/**
 * Reti e laboratorio III - A.A. 2022/2023
 * WORDLE
 * 
 * Classe per ridefinire un'eccezione usata nel caso
 * di comunicazione anomala tra Client-Server 
 * 
 * @author Giovanni Criscione
 *
 */
public class CommunicationErrorServerClient extends Exception{
	public CommunicationErrorServerClient(String mess){
		super(mess);
	}
}