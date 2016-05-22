package datacenter;

import java.util.ArrayList;
import java.util.List;

public class AppendEntriesRPC extends Message {
	private int term;

	private int leaderId;

	private int prevLogIndex;

	private int prevLogTerm;

	List<LogEntry> entries;

	private int leaderCommit;

	public int getTerm() {
		return term;
	}

	public int getLeaderId() {
		return leaderId;
	}

	public int getPrevLogIndex() {
		return prevLogIndex;
	}

	public int getPrevLogTerm() {
		return prevLogTerm;
	}

	public List<LogEntry> getEntries() {
		return entries;
	}

	public int getLeaderCommit() {
		return leaderCommit;
	}

	public AppendEntriesRPC(int term, int leaderId, int prevLogIndex, int prevLogTerm, List<LogEntry> entries,
			int leaderCommit) {
		super(MessageType.APPEND_ENTRIES);
		this.term = term;
		this.leaderId = leaderId;
		this.prevLogIndex = prevLogIndex;
		this.prevLogTerm = prevLogTerm;
		this.entries = new ArrayList<>(entries);
		this.leaderCommit = leaderCommit;
	}
}
