package datacenter;

public class RequestVoteRPC extends Message {
	private int term;

	private int candidateId;

	private int lastLogIndex;

	private int lastLogTerm;

	public int getTerm() {
		return term;
	}

	public int getCandidateId() {
		return candidateId;
	}

	public int getLastLogIndex() {
		return lastLogIndex;
	}

	public int getLastLogTerm() {
		return lastLogTerm;
	}

	public RequestVoteRPC(int term, int candidateId, int lastLogIndex, int lastLogTerm) {
		super(MessageType.REQUEST_VOTE);
		this.term = term;
		this.candidateId = candidateId;
		this.lastLogIndex = lastLogIndex;
		this.lastLogTerm = lastLogTerm;
	}
}
