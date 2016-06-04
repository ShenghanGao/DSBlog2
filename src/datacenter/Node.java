package datacenter;

public class Node {
	private String IPAddress;

	private int nextIndex = 0;

	private int matchIndex = -1;

	private boolean votedForMe = false;

	private int flag;

	private int id;

	public Node(String IPAddress, int id) {
		this.IPAddress = IPAddress;
		this.flag = 0;
		this.id = id;
	}

	public String getIPAddress() {
		return IPAddress;
	}

	public void setNextIndex(int nextIndex) {
		this.nextIndex = nextIndex;
	}

	public void setMatchIndex(int matchIndex) {
		this.matchIndex = matchIndex;
	}

	public void setFlag(int flag) {
		this.flag = flag;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getNextIndex() {
		return nextIndex;
	}

	public int getMatchIndex() {
		return matchIndex;
	}

	public int getFlag() {
		return flag;
	}

	public int getId() {
		return id;
	}

	public boolean isVotedForMe() {
		return votedForMe;
	}

	public void setVotedForMe(boolean votedForMe) {
		this.votedForMe = votedForMe;
	}
}
