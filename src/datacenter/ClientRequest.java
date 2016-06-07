package datacenter;

import java.net.InetAddress;

public class ClientRequest extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7066879258460566712L;
	private InetAddress inetAddress;
	private String req;

	public ClientRequest(InetAddress inetAddress, String req) {
		super(MessageType.CLIENT_REQUEST);
		this.req = req;
		this.inetAddress = inetAddress;
	}

	public InetAddress getInetAddress() {
		return inetAddress;
	}

	public String getReq() {
		return req;
	}
}
