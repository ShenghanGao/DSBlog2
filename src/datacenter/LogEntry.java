package datacenter;

import java.io.Serializable;

public class LogEntry implements Serializable {
	private int term;

	private String command;

	public LogEntry(int term, String command) {
		this.term = term;
		this.command = command;
	}

	public int getTerm() {
		return term;
	}

	public String getCommand() {
		return command;
	}
}
