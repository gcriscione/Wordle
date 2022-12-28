package server_package;

/**
 * Reti e laboratorio III - A.A. 2022/2023
 * WORDLE
 * 
 * Classe che rappresenta un giocatore
 * e tiene traccia dei suoi dati
 * 
 * @author Giovanni Criscione
 *
 */
public class AccountPlayer {
	// informazioni account giocatore
    private String user;               		// nome utente
    private String password;           		// password di accesso
    private int games_played;          		// numero partite giocate
    private int games_won;             		// numero partite vinte
    private int streak;                		// lunghezza dell’ultima sequenza continua di vincite
    private int max_streak;            		// lunghezza della massima sequenza continua  di vincite
    private int guess_distribution[];    	// vettore di interi, dove la posizione k-esima del vettore contiene il numero di parole
                                            // indovinate dall'utente con esattamente k tentativi
    private int last_SecretWord;         	// numero dell'ultima SecretWord con cui il giocatore ha giocato
    private int attempts_used;              // tentativi utilizzati (riferiti a last_SeCretWord)
    private String last_game_stats[];       // statistiche relative all'ultima partita giocata
    
    
    /**
     *  Costruttore della classe AccountPlayer
     *  @param user nome utente
     *  @param password password
     */
    public AccountPlayer(String user, String password) {
    	this.user = user;
    	this.password = password;

    	this.games_played = 0;
    	this.games_won = 0;
    	this.streak = 0;
    	this.max_streak = 0;
    	this.guess_distribution = new int[]{ 0,0,0,0,0,0,0,0,0,0,0,0 };
        this.last_SecretWord = 0;
        this.attempts_used = 0;
        this.last_game_stats = new String[]{ "","","","","","","","","","","","" };
    }



    // --- metodi di controllo ---------------------------------
    
    // controlla se user e password forniti corrispondono con quelli dell'oggetto
    public boolean check_credential(String u, String p){
        return ( (user.compareTo(u)==0) && (password.compareTo(p)==0));
    }

    // controlla se la password fornita corrisponde con quella dell'oggetto
    public boolean check_password(String p){
        return (password.compareTo(p)==0);
    }

    // controlla se l'utente ha esaurito i tentativi relativi a quella secret_word
    public boolean remaing_attempt(int secretW){
    	
    	if(last_SecretWord==secretW && attempts_used>=12) {
			return false;
		}
    	
    	return true;
    }


    // --- metodi per aggiornare il valore dei dati ------------
    // incrementa di +1 i tentativi utilizzati
    public void use_attempt() {  
        this.attempts_used += 1;
    }
    
    // aggiorna i dati considerando una vittoria
    public void wins() {  
        // incrementa il numero di partite_giocate, partite_vinte, streak
        // aggiorna max_streak e guess_distribution
        this.games_played  += 1;  
        this.games_won     += 1;
        this.streak        += 1;
        if(this.streak > this.max_streak){
            this.max_streak = this.streak;
        }

        // aggiorna guess_distribution
        this.guess_distribution[attempts_used-1] += 1;

        // esaurisce tutti i tentati per non far rigiocare
        this.attempts_used = 12;
    }

    // aggiorna i dati considerando una sconfitta
    public void lose(){
        // incrementa il numero di partite_giocate e azzera streak
        this.games_played  += 1;  
        this.streak        = 0;
        
        // esaurisce tutti i tentati per non far rigiocare
        this.attempts_used = 12;
    }

    // --- metodi Setter ---------------------------------------
    // setta last_SecretWord e inizializza il numero di tentativi usati
    public void setLastSecretWord(int sw){
        // controlla se aveva esaurito tutti i tentativi per l'ultima SecretWord per cui ha giocato,
        // nel caso segna la partita come persa
        if(this.attempts_used<12){
            this.lose();
        }


        this.last_SecretWord = sw;
        this.attempts_used = 0;
        this.last_game_stats = new String[]{ "","","","","","","","","","","","" };
    }
    
    // setta le statistiche di un tentativo per la Secret_Word giocata
    public void setGameStats(String s){
        this.last_game_stats[this.attempts_used-1] = s;
    }

    // --- metodi Getter ---------------------------------------
    
    // restituisce il nome utente
    public String getUser() {  return this.user;  }
    
    // restituisce la password
    public String getPassword() {  return this.password;  }
    
    // restituisce in numero di partite giocate
    public int getGamesPlayed() {  return this.games_played;  }
    
    // restituisce il numero di partite vinte
    public int getGamesWon() {  return this.games_won;  }

    // restituisce streak 
    public int getStreak () {  return this.streak ;  }

    // restituisce max_streak
    public int getMaxStreak() {  return this.max_streak;  }

    // restituisce last_SecretWord
    public int getLastWordle() {  return this.last_SecretWord; }
 
    // restituisce il numero di tentativi usati
    public int getAttemptsUsed() {  return this.attempts_used;  }

    // restituisce una stringa che contenente le statistiche dell'utente
    public String getStatistics(){
        // Utilizza '/n' al posto di '\n' per permette al server di effettuare una lettura come unica stringa 
        
        String s = new String("");
        s += "\tUtente: "+user+"/n";
        s += "Partite giocate: "+games_played+"/n";
        if(games_played != 0) {        	
        	s += "Partite vinte:    "+( (games_won*100)/games_played )+"%/n";
        }else {
        	s += "Partite vinte:    0%/n";
        }
        s += "Ultimo streak:    "+streak+"/n";
        s += "Max streak:       "+max_streak+"/n";
        s += "Guess distribution:/n";
        for(int i=0; i<guess_distribution.length; i++){
            s += " - ["+(i+1)+"]\t  "+guess_distribution[i]+"/n";
        }
        s += "/n";

        return s;
    }
    
    // 
    public String getlast_game_stats(){
        String s = "";
        s += this.user + "/n";
        s += "Secret_word:\t"+this.last_SecretWord+"/n";
        for(int i=0; i<this.attempts_used; i++){
            s += "Tentativo "+(i+1)+"\t\t"+this.last_game_stats[i]+"/n";
        }
        return s;
    }


    // --- Override dei metodi ------------------------------
    // ritorna l'hashCode della stringa user (user valore unico per ogni oggetto)
    @Override
    public int hashCode() {
    	return this.user.hashCode();
    }
    
    // rappresentazione in stringa dell'oggetto
    @Override
    public String toString() {
    	String s = "["+user+", "+password+", "+games_played+", "+games_won+", "+streak+", ";
    	s += max_streak+", "+toString_guessDis()+", ";
    	s += last_SecretWord+", "+attempts_used+" ]";
    	return s;
    }

    // rappresentazione in stringa della guess_distribution
    private String toString_guessDis() {
        String s = "";
        
        s += "["+guess_distribution[0];
        for(int i=1; i<guess_distribution.length; i++){
            s += " ,"+guess_distribution[i];
        }
        s += "]";

        return s;
    }
}