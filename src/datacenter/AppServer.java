package datacenter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class AppServer {
	private static AppServer appServer;

	private static final int period = 3000;

	List<String> posts;

	ServerState state;

	private int currentTerm;

	private int votedFor;

	List<LogEntry> log;

	private int commitIndex;

	private int lastApplied;

	List<Integer> nextIndex;

	List<Integer> matchIndex;

	private List<Integer> recvAppendEntriesRPC(AppendEntriesRPC rpc) {

	}

	private List<Integer> recvRequestVoteRPC(AppendEntriesRPC rpc) {

	}

	public static void main(String[] args) {
		Thread listenToClientsThread = new Thread(new ListenToClientsThread());
		listenToClientsThread.start();

		System.out.println("Server!");
		Thread listenToDCThread = new ListenToDCThread();
		listenToDCThread.start();

		while (true) {
			try {
				Thread.sleep(period);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// try {
		// listenToDCThread.join();
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
	}

	public static class ListenToClientsThread extends Thread {
		ServerSocket listenToClientsSocket;

		@Override
		public void run() {
			try {
				listenToClientsSocket = new ServerSocket(CLIENTS_PORT, 5);
			} catch (IOException e) {
				e.printStackTrace();
			}
			while (true) {
				Socket socket;
				try {
					socket = listenToClientsSocket.accept();
					Thread t = new Thread(new ListenToClientsSocketHandler(socket));
					t.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		public class ListenToClientsSocketHandler implements Runnable {
			private Socket connectedSocket;

			public ListenToClientsSocketHandler(Socket connectedSocket) {
				this.connectedSocket = connectedSocket;
			}

			@Override
			public void run() {

			}
		}
	}

	public static class ListenToDCThread extends Thread {
		ServerSocket listenToDCSocket;

		@Override
		public void run() {
			try {
				listenToDCSocket = new ServerSocket(DC_PORT, 5);
			} catch (IOException e) {
				e.printStackTrace();
			}
			while (true) {
				Socket socket;
				try {
					socket = listenToDCSocket.accept();
					Thread t = new Thread(new ListenToDCSocketHandler(socket));
					t.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		public class ListenToDCSocketHandler implements Runnable {
			private Socket connectedSocket;

			public ListenToDCSocketHandler(Socket connectedSocket) {
				this.connectedSocket = connectedSocket;
			}

			@Override
			public void run() {
				Message message = null;
				try {
					ObjectInputStream ois = new ObjectInputStream(connectedSocket.getInputStream());
					message = (Message) ois.readObject();
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}

				switch (message.getType()) {
				case APPEND_ENTRIES: {
					Message response = recvAppendEntries(message);
					// TODO: Send response

					break;
				}
				case APPEND_ENTRIES_RESPONSE: {
					recvAppendEntriesResponse(message);
					break;
				}
				case REQUEST_VOTE: {
					Message response = recvRequestVote(message);
					break;
				}
				case REQUEST_VOTE_RESPONSE: {
					recvRequestVoteResponse(message);
					break;
				}
				default: {

				}
				}

				// ObjectOutputStream oos = new
				// ObjectOutputStream(connectedSocket.getOutputStream());

			}

			private Message recvAppendEntries(Message message) {
				AppendEntriesRPC ae = (AppendEntriesRPC) message;
				boolean success;
				// 1
				if (ae.getTerm() < AppServer.appServer.currentTerm) {
					success = false;
				}
				// 2
				int prevLogIndex = ae.getPrevLogIndex();
				if (prevLogIndex >= AppServer.appServer.log.size()) {
					success = false;
				} else {
					LogEntry logEntry = AppServer.appServer.log.get(prevLogIndex);
					if (logEntry.getTerm() != ae.getPrevLogTerm()) {
						success = false;
					}
				}
				// 3

				Message response = new AppendEntriesRPCResponse();
				return response;
			}

			private void recvAppendEntriesResponse(Message message) {

			}

			private Message recvRequestVote(Message message) {
				RequestVoteRPC rv = (RequestVoteRPC) message;

				Message response = new RequestVoteRPCResponse();
				return response;
			}

			private void recvRequestVoteResponse(Message message) {

			}
		}
	}
}
