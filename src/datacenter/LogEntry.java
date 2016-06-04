package datacenter;

import java.io.Serializable;

public class LogEntry implements Serializable {
	private int term;
	private LogEntryType type;
	private String contents;

	public LogEntry(int term, LogEntryType type, String contents) {
		this.term = term;
		this.type = type;
		this.contents = contents;
	}

	public LogEntryType getType() {
		return type;
	}

	public String getContents() {
		return contents;
	}

	public int getTerm() {
		return term;
	}
}
