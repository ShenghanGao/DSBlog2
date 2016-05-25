package datacenter;

public class AppendEntriesRPCResponse extends Message {
	private int term;
	private boolean success;

	public int getTerm() {
		return term;
	}

	public boolean isSuccess() {
		return success;
	}

	public AppendEntriesRPCResponse(int term, boolean success) {
		super(MessageType.APPEND_ENTRIES_RESPONSE);
		this.term = term;
		this.success = success;
	}
}
