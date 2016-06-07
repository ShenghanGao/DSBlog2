package api;

import java.io.Serializable;

public class EntryResponse implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2610868522087379671L;
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
