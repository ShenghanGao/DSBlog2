package datacenter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class AppServer {
	private static AppServer appServer = new AppServer();

	private static final int period = 4000;

	private static final int CLIENTS_PORT = 8887;

	private static final int DC_PORT = 8888;

	private static final int requestTimeout = 100; ////

	private static final int electionTimeout = 3000; ///

	List<String> posts;

	ServerState state;

	private int currentTerm;

	private int votedFor;

	private boolean isVoting;

	List<LogEntry> log;

	private int commitIndex;

	private int lastApplied;

	private int timeElapsed;

	private List<Node> nodes;

	private int id = -1;

	private int currentLeader;

	private AppServer() {
		nodes = new ArrayList<>();
	}

	public static AppServer getAppServer() {
		return appServer;
	}

	// public boolean sendAppendEntriesRPC(String targetServerIPAddress,
	// AppendEntriesRPC rpc) {
	// Socket socket = new Socket(targetServerIPAddress, PORT);
	// ObjectOutputStream oos = new
	// ObjectOutputStream(socket.getOutputStream());
	// oos.writeObject((Message) rpc);
	//
	// ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
	// AppendEntriesRPCResponse aer = (AppendEntriesRPCResponse)
	// ois.readObject();
	//
	// return true;
	// }

	public static void sendMessage(Node node, Message rpc) {
		InetAddress address = null;
		try {
			address = InetAddress.getByName(node.getIPAddress());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		try {
			Socket socket = new Socket(address, DC_PORT);
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(rpc);
			oos.flush();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// public boolean sendAppendEntriesRPC(String targetServerIPAddress,
	// RequestVoteRPC rpc) {
	// Socket socket = new Socket(targetServerIPAddress, PORT);
	// ObjectOutputStream oos = new
	// ObjectOutputStream(socket.getOutputStream());
	// oos.writeObject((Message) rpc);
	//
	// return true;
	// }

	// public boolean SendRequestVoteRPC(AppServer server, AppServer
	// targetServer, RequestVoteRPC rpc) {
	// return true;
	// }

	public static void sendAppendEntriesToAll() {
		appServer.timeElapsed = 0;
		int num = appServer.nodes.size();
		for (int i = 0; i < num; ++i) {
			if (appServer.id != appServer.nodes.get(i).getId()) {
				int term = appServer.currentTerm;
				int leaderId = appServer.id;
				int prevLogIndex = -1;
				int prevLogTerm = -1;
				List<LogEntry> entries = new ArrayList<>();
				int leaderCommit = appServer.commitIndex;
				AppendEntriesRPC rpc = new AppendEntriesRPC(term, leaderId, prevLogIndex, prevLogTerm, entries,
						leaderCommit);
				sendMessage(AppServer.appServer.nodes.get(i), rpc);
			}
		}
	}
	//
	// public static void becomeFollower() {
	// appServer.state = ServerState.FOLLOWER;
	// }
	//
	// public static void becomeCandidate() {
	// appServer.currentTerm += 1;
	//
	// /* vote for self */
	// appServer.votedFor = appServer.index;
	//
	// appServer.currentLeader = -1;
	// appServer.state = ServerState.CANDIDATE;
	//
	// Random rn = new Random();
	// appServer.timeElapsed = (electionTimeout - 2 * (rn.nextInt(10000) %
	// electionTimeout)) / 10;
	//
	// /* request vote */
	// int num = appServer.nodes.size();
	// for (int i = 0; i < num; ++i)
	// if (!appServer.nodes.get(i).equals(AppServer.appServer.myIPAddress)) {
	// int term =
	// !appServer.nodes.get(i).equals(appServer.myIPAddress).currentTerm;
	// int candidateId = appServer.index;
	// int lastLogIndex = appServer.log.size() - 1;
	// int lastLogTerm = appServer.log.get(lastLogIndex).getTerm();
	// RequestVoteRPC rpc = new RequestVoteRPC(term, candidateId, lastLogIndex,
	// lastLogTerm);
	// SendRequestVoteRPC(this, appServer.nodes.get(i),
	// rpc); /* server to server */
	// }
	// }
	//
	// public static void electionStart() {
	// becomeCandidate();
	// }
	//
	// public static void becomeLeader() {
	// appServer.state = ServerState.LEADER;
	// int lastLogIndex = appServer.log.size() - 1;
	// int num = appServer.nodes.size();
	//
	// sendAppendEntriesToAll();

	/* leader appends entries, then updates nextIndex[] and matchIndex[] */

	// for (int i = 0; i < num; ++i)
	// if (appServer.nodes.get(i) != this && lastLogIndex >=
	// appServer.nextIndex.get(i)) { //
	// Node target = appServer.nodes.get(i);
	//
	//
	// boolean flag = false;
	// while (!flag) {
	// int term = appServer.currentTerm;
	// int leaderId = appServer.index;
	// int prevLogIndex = appServer.nodes.get(i) - 1;
	// int prevLogTerm = appServer.log.get(prevLogIndex).getTerm();
	// List<LogEntry> entries = new
	// ArrayList<>(appServer.log.subList(prevLogIndex + 1, lastLogIndex +
	// 1));
	// int leaderCommit = appServer.commitIndex;
	// AppendEntriesRPC rpc = new AppendEntriesRPC(term, leaderId,
	// prevLogIndex, prevLogTerm, entries,
	// leaderCommit);
	// //////////////////////////////////////////////
	// if (SendAppendEntriesRPC(this, target, rpc)) {
	// flag = true;
	// this.nextIndex.set(i, lastLogIndex + 1);
	// this.matchIndex.set(i, lastLogIndex);
	// } else {
	// nextIndex.set(i, prevLogIndex);
	// }
	// ///////////////////////////////////////////////
	// }
	// }
	//
	/* update commitIndex */
	// for (int N = lastLogIndex; N >= appServer.commitIndex; --N) {
	// int counter = 0;
	// for (int i = 0; i < num; ++i)
	// if ((this.nodes.get(i) != this) && appServer.matchIndex.get(i) >= N) {
	// int term = appServer.log.get(N).getTerm();
	// if (term == appServer.currentTerm)
	// counter++;
	// }
	// if (counter + 1 > num / 2) { // +1 because we need to add the server
	// // itself
	// appServer.commitIndex = N;
	// break;
	// }
	// }
	//
	// /* note here we didn't apply the commitIndex to the state-machine */
	// }
	//
	// /* we always assume # of servers > 1 */
	public static void serverPeriodic() {
		if (appServer.state == ServerState.LEADER) {
			sendAppendEntriesToAll();
		}

		// appServer.timeElapsed += period;
		//
		// /* send heartbeat or start a new election */
		// if (appServer.state == ServerState.LEADER) {
		// if (requestTimeout <= appServer.timeElapsed) {
		// sendAppendEntriesToAll();
		// }
		// }
		// else if (electionTimeout <= appServer.timeElapsed) {
		// ElectionStart();
		// }

		/* commit idx < lastApplied */
		// if (appServer.commitIndex > appServer.lastApplied) {
		// while (appServer.lastApplied < appServer.commitIndex) {
		// appServer.lastApplied++;
		// String cmd = appServer.log.get(appServer.lastApplied).getCommand();
		// appServer.posts.add(cmd);
		// }
		// }
	}

	/*
	 * thundarr.cs.ucsb.edu: 128.111.43.40, optimus.cs.ucsb.edu: 128.111.43.41,
	 * megatron.cs.ucsb.edu: 128.111.43.42
	 */
	public static void main(String[] args) {
		String IPAddressesFile = "./IPAddresses";

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(IPAddressesFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		String line;
		int lineNo = 0;
		try {
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty())
					continue;
				Node node = new Node(line, lineNo++);
				appServer.nodes.add(node);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		InetAddress inetAddress = null;
		try {
			inetAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		String myIPAddress = inetAddress.getHostAddress();

		for (Node node : appServer.nodes) {
			if (myIPAddress.equals(node.getIPAddress())) {
				appServer.id = node.getId();
				break;
			}
		}
		if (appServer.id == -1) {
			System.out.println("The IP address of this machine (" + myIPAddress + ") is not in the IP address file!");
			return;
		}

		System.out.print("My IP address is " + myIPAddress + ", my id is " + appServer.id + ".");

		appServer.state = ServerState.FOLLOWER;

		// Fix leader
		if (appServer.id == 0) {
			appServer.state = ServerState.LEADER;
		}

		Thread listenToClientsThread = new Thread(new ListenToClientsThread());
		listenToClientsThread.start();

		Thread listenToDCThread = new ListenToDCThread();
		listenToDCThread.start();

		while (true) {
			try {
				AppServer.serverPeriodic();
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

				} catch (IOException e) {
					e.printStackTrace();
				}
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
					ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
					Message message = (Message) ois.readObject();

					switch (message.getType()) {
					case APPEND_ENTRIES: {
						Message response = recvAppendEntries(message);
						// TODO: Send response

						break;
					}
						// case APPEND_ENTRIES_RESPONSE: {
						// recvAppendEntriesResponse(message);
						// break;
						// }
						// case REQUEST_VOTE: {
						// Message response = recvRequestVote(message);
						// break;
						// }
						// case REQUEST_VOTE_RESPONSE: {
						// recvRequestVoteResponse(message);
						// break;
						// }
					default: {

					}
					}

					socket.close();
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}

				// ServerState state = appServer.state;
				// MessageType type = message.getType();
				// switch (state) {
				// case FOLLOWER: {
				// switch (type) {
				// case APPEND_ENTRIES: {
				// Message response = recvAppendEntries(message);
				// sendResponse(connectedSocket, response);
				// break;
				// }
				// case REQUEST_VOTE: {
				// break;
				// }
				// default: {
				//
				// }
				// }
				// break;
				// }
				// case CANDIDATE: {
				// switch (type) {
				// case APPEND_ENTRIES: {
				// Message response = recvAppendEntries(message);
				// sendResponse(connectedSocket, response);
				// break;
				// }
				// case REQUEST_VOTE: {
				// break;
				// }
				// default: {
				//
				// }
				// }
				// break;
				// }
				// case LEADER: {
				// switch (type) {
				// case APPEND_ENTRIES_RESPONSE: {
				// break;
				// }
				// case REQUEST_VOTE_RESPONSE: {
				// break;
				// }
				// default: {
				//
				// }
				// }
				// break;
				// }
				// default: {
				//
				// }
				// }

			}
		}

		// private static void sendResponse(Socket connectedSocket, Message
		// response) {
		// ObjectOutputStream oos;
		// try {
		// oos = new ObjectOutputStream(connectedSocket.getOutputStream());
		// oos.writeObject(response);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		//
		private static Message recvAppendEntries(Message message) {
			AppendEntriesRPC ae = (AppendEntriesRPC) message;

			System.out.println("recvAppendEntries: term = " + ae.getTerm() + ", leaderId = " + ae.getLeaderId()
					+ ", prevLogIndex = " + ae.getPrevLogIndex() + ", prevLogTerm = " + ae.getPrevLogTerm()
					+ ", leaderCommit = " + ae.getLeaderCommit());

			return null;

			// if (AppServer.appServer.state == ServerState.CANDIDATE &&
			// AppServer.appServer.currentTerm <= ae.getTerm()) {
			// AppServer.becomeFollower();
			// }
			//
			// boolean success;
			// // 1
			// if (ae.getTerm() < AppServer.appServer.currentTerm) {
			// success = false;
			// }
			// // 2
			// int prevLogIndex = ae.getPrevLogIndex();
			// if (prevLogIndex >= AppServer.appServer.log.size()) {
			// success = false;
			// } else {
			// LogEntry logEntry = AppServer.appServer.log.get(prevLogIndex);
			// if (logEntry.getTerm() != ae.getPrevLogTerm()) {
			// success = false;
			// deleteEntriesFromIndex(ae.getPrevLogIndex());
			// }
			//
			// }
			//
			// // 3
			// // heartbeat
			// if (ae.getEntries().size() == 0) {
			// deleteEntriesFromIndex(ae.getPrevLogIndex());
			// }
			//
			// for (int i = 0; i < ae.getEntries().size(); ++i) {
			// LogEntry aeEntry = ae.getEntries().get(i);
			// int existingEntryIndex = ae.getPrevLogIndex() + 1 + i;
			// LogEntry existingEntry = getEntryFromIndex(existingEntryIndex);
			// if (existingEntry == null)
			// break;
			// else if (existingEntry.getTerm() != aeEntry.getTerm()) {
			// deleteEntriesFromIndex(existingEntryIndex);
			// }
			// }
			//
			// // TODO: ???
			// for (LogEntry e : ae.getEntries()) {
			// AppServer.appServer.log.add(e);
			// }
			// // ???
			//
			// // 4
			// if (ae.getLeaderCommit() > AppServer.appServer.commitIndex) {
			// AppServer.appServer.commitIndex = Math.min(ae.getLeaderCommit(),
			// AppServer.appServer.log.size() - 1);
			// }
			// success = true;
			//
			// Message response = new
			// AppendEntriesRPCResponse(AppServer.appServer.currentTerm,
			// success);
			// return response;
		}
		//
		// private static void recvAppendEntriesResponse(Message message) {
		// AppendEntriesRPCResponse aer = (AppendEntriesRPCResponse) message;
		//
		// if (AppServer.appServer.state != ServerState.LEADER) {
		//
		// }
		//
		// if (aer.getTerm() > AppServer.appServer.currentTerm) {
		// AppServer.appServer.currentTerm = aer.getTerm();
		// AppServer.becomeFollower();
		// return;
		// }
		//
		// if (aer.isSuccess()) {
		// Node node = null;// get the node;
		// node.setNextIndex(aer.getLogIndex() + 1);
		// node.setMatchIndex(aer.getLogIndex());
		//
		// } else {
		//
		// }
		//
		// /*
		// * If there exists an N such that N > commitIndex, a majority of
		// * matchIndex[i] >= N, and log[N].term == currentTerm: set
		// * commitIndex = N (5.3, 5.4).
		// */
		// int lastLogIndex = AppServer.appServer.log.size() - 1;
		// for (int N = lastLogIndex; N > AppServer.appServer.commitIndex; --N)
		// {
		// if (AppServer.appServer.log.get(N).getTerm() ==
		// AppServer.appServer.currentTerm) {
		// int counter = 0;
		// for (Node n : AppServer.appServer.nodes) {
		// // TODO: Not self
		//
		// if (n.getMatchIndex() >= N)
		// ++counter;
		//
		// if (counter >= num) { // wrong, majority
		// AppServer.appServer.commitIndex = N;
		// }
		// }
		// }
		// }
		//
		// // Send remaining entries
		//
		// }
		//
		// private Message recvRequestVote(Message message) {
		// RequestVoteRPC rv = (RequestVoteRPC) message;
		//
		// boolean voteGranted;
		//
		// // 1
		// if (rv.getTerm() < AppServer.appServer.currentTerm) {
		// voteGranted = false;
		// }
		//
		// // 2
		// boolean isUpToDate = true;
		//
		// int currentIndex = AppServer.appServer.log.size() - 1;
		// LogEntry e = getEntryFromIndex(currentIndex);
		//
		// if (e.getTerm() > rv.getLastLogTerm()
		// || (e.getTerm() == rv.getLastLogTerm() && currentIndex >
		// rv.getLastLogIndex())) {
		// isUpToDate = false;
		// }
		//
		// if ((AppServer.appServer.votedFor == null ||
		// AppServer.appServer.votedFor
		// == rv.getCandidateId())
		// && isUpTodate) {
		// voteGranted = true;
		// }
		//
		// Message response = new
		// RequestVoteRPCResponse(AppServer.appServer.currentTerm, voteGranted);
		// return response;
		// }
		//
		// private void recvRequestVoteResponse(Message message) {
		// RequestVoteRPCResponse rvr = (RequestVoteRPCResponse) message;
		//
		// if (AppServer.appServer.state != ServerState.CANDIDATE) {
		// return;
		// } else if (AppServer.appServer.currentTerm < rvr.getTerm()) {
		// AppServer.appServer.currentTerm = rvr.getTerm();
		// AppServer.becomeFollower();
		// return;
		// } else if (AppServer.appServer.currentTerm != rvr.getTerm()) {
		// return;
		// }
		//
		// if (rvr.isVoteGranted()) {
		// if (is_majority) {
		// AppServer.becomeLeader();
		// }
		// }
		// }
		//
		// private static LogEntry getEntryFromIndex(int i) {
		// if (i >= AppServer.appServer.log.size())
		// return null;
		// return AppServer.appServer.log.get(i);
		// }
		//
		// private static void deleteEntriesFromIndex(int i) {
		// while (i <= AppServer.appServer.log.size()) {
		// AppServer.appServer.log.remove(AppServer.appServer.log.size() - 1);
		// }
		// }
		//
		// public class ListenToDCSocketHandler implements Runnable {
		// private Socket connectedSocket;
		//
		// public ListenToDCSocketHandler(Socket connectedSocket) {
		// this.connectedSocket = connectedSocket;
		// }
		//
		// @Override
		// public void run() {
		// // Message message = null;
		// // try {
		// // ObjectInputStream ois = new
		// // ObjectInputStream(connectedSocket.getInputStream());
		// // message = (Message) ois.readObject();
		// // } catch (IOException | ClassNotFoundException e) {
		// // e.printStackTrace();
		// // }
		// //
		// // switch (message.getType()) {
		// // case APPEND_ENTRIES: {
		// // Message response = recvAppendEntries(message);
		// // // TODO: Send response
		// //
		// // break;
		// // }
		// // case APPEND_ENTRIES_RESPONSE: {
		// // recvAppendEntriesResponse(message);
		// // break;
		// // }
		// // case REQUEST_VOTE: {
		// // Message response = recvRequestVote(message);
		// // break;
		// // }
		// // case REQUEST_VOTE_RESPONSE: {
		// // recvRequestVoteResponse(message);
		// // break;
		// // }
		// // default: {
		// //
		// // }
		// // }
		//
		// // ObjectOutputStream oos = new
		// // ObjectOutputStream(connectedSocket.getOutputStream());
		//
		// }

		// }
	}
}
