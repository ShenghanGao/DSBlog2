package api;

import java.io.Serializable;

public class EntryResponse implements Serializable {
	private int id;
	private int term;

	public EntryResponse(int id, int term) {
		this.id = id;
		this.term = term;
	}

	public int getId() {
		return id;
	}

	public int getTerm() {
		return term;
	}
}
