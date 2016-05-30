package datacenter;

public class Node {
	private String IPAddress;

	private int nextIndex = 1;

	private int matchIndex = 0;

	private int flag = 0;

	private int id;

	public Node(String IPAddress, int id) {
		this.IPAddress = IPAddress;
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
}
