package client_package;
/**
 * Reti e laboratorio III - A.A. 2022/2023
 * WORDLE
 * 
 * Tipo enumerato che descrive le operazione del gioco 
 * 
 * @author Giovanni Criscione
 *
 */

public enum Command {
	LOGIN,
	REGISTER,
	PLAY_WORDLE,
	SEND_WORD,
	SEND_STATISTICS,
	SHARE,
	LOGOUT,
	FAILURE,
	ERRORMESSAGE,
	EXIT,
	WIN,
	LOSE,
}
