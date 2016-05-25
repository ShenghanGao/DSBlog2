package datacenter;

public class LogEntry {
	private int term;
<<<<<<< Updated upstream
	private String command;
	
	public int getTerm(){
		return this.term;
	}
	
	public String getCommand(){
		return this.command;
=======

	public int getTerm() {
		return term;
	}

	public LogEntry(int term) {
		this.term = term;
>>>>>>> Stashed changes
	}
}
