package datacenter;

public class RequestVoteRPCResponse extends Message {
	private int term;
	private boolean voteGranted;

	public int getTerm() {
		return term;
	}

	public boolean isVoteGranted() {
		return voteGranted;
	}

	public RequestVoteRPCResponse(int term, boolean voteGranted) {
		super(MessageType.REQUEST_VOTE_RESPONSE);
		this.term = term;
		this.voteGranted = voteGranted;
	}
}
