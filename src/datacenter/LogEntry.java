package datacenter;

public class LogEntry {
	private int term;
	private String command;
	
	public int getTerm(){
		return this.term;
	}
	
	public String getCommand(){
		return this.command;
	}
}
