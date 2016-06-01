package datacenter;

public class RequestVoteRPCResponse extends Message {
	private int term;
	private boolean voteGranted;
	private int nodeId;

	public RequestVoteRPCResponse(int term, boolean voteGranted, int nodeId) {
		super(MessageType.REQUEST_VOTE_RESPONSE);
		this.term = term;
		this.voteGranted = voteGranted;
		this.nodeId = nodeId;
	}

	public int getTerm() {
		return term;
	}

	public boolean isVoteGranted() {
		return voteGranted;
	}

	public int getNodeId() {
		return nodeId;
	}
}
