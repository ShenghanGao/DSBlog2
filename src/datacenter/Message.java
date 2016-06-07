package datacenter;

import java.io.Serializable;

public class Message implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -80672554444069830L;
	private MessageType type;

	public Message(MessageType type) {
		this.type = type;
	}

	public MessageType getType() {
		return type;
	}
}
