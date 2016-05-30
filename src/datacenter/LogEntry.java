package datacenter;

public class LogEntry {
	private int term;
	// private String command;

	// public String getCommand(){
	// return this.command;

	public LogEntry(int term) {
		this.term = term;
	}

	public int getTerm() {
		return term;
	}
}
