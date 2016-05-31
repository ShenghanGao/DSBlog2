package datacenter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ConnectException;
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

	private static final boolean DEBUG = true;

	List<String> posts;

	ServerState state;

	private int currentTerm;

	private int votedFor;

	private boolean isVoting;

	private List<LogEntry> log;

	private int commitIndex;

	private int lastApplied;

	private int timeoutElapsed;

	private List<Node> nodes;

	private int id = -1;

	private int currentLeader;

	public static void printLog() {
		for (LogEntry e : appServer.log) {
			System.out.println(e.getCommand());
		}
		System.out.println("\n");
	}

	public static void printEntries(List<LogEntry> entries) {
		System.out.println("Entries: ");
		for (LogEntry e : entries) {
			System.out.println(e.getCommand());
		}
		System.out.println("End of entries\n");
	}

	private AppServer() {
		log = new ArrayList<>();
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

	public static LogEntry getEntryFromIndex(int i) {
		if (i >= appServer.log.size())
			return null;
		return appServer.log.get(i);
	}

	public static List<LogEntry> getEntriesFromIndex(int i) {
		List<LogEntry> entries = new ArrayList<>();
		if (i >= appServer.log.size())
			return entries;
		return appServer.log.subList(i, appServer.log.size());
	}

	public static void deleteEntriesFromIndex(int i) {
		int lastIndex = appServer.log.size() - 1;
		for (; lastIndex >= i; --lastIndex) {
			appServer.log.remove(lastIndex);
		}
	}

	public static void handleClientsReq(String req, Socket socket) {
		String[] ss = req.split(" ", 2);
		if (ss[0].compareTo("p") == 0) {
			recvEntry(ss[1]);
		} else if (ss[0].compareTo("l") == 0) {
			lookup(socket);
		}
	}

	public static void recvEntry(String command) {
		// TODO: cfg_change

		if (DEBUG)
			System.out.println("recvEntry, command = " + command);

		if (appServer.state != ServerState.LEADER) {
			if (DEBUG)
				System.out.println("I am not the leader and I need to redirect this entry.");
			sendMessage(appServer.nodes.get(appServer.currentLeader), command);
			return;
		}

		LogEntry entry = new LogEntry(appServer.currentTerm, command);
		appServer.log.add(entry);

		for (Node node : appServer.nodes) {
			if (node.getId() == appServer.id)
				continue;

			// if (node.getNextIndex() == appServer.log.size() - 1) {
			// AppendEntriesRPC ae = genAppendEntriesRPC(node);
			// sendMessage(node, ae);
			// }

			AppendEntriesRPC ae = genAppendEntriesRPC(node);

			if (DEBUG) {
				System.out.println("recvEntry, AppendEntries: term = " + ae.getTerm() + ", leaderId = "
						+ ae.getLeaderId() + ", prevLogIndex = " + ae.getPrevLogIndex() + ", prevLogTerm = "
						+ ae.getPrevLogTerm() + ", leaderCommit = " + ae.getLeaderCommit());
				printEntries(ae.entries);
				System.out.println("recvEntry, send RPC to node " + node.getId() + ".");
			}

			sendMessage(node, ae);
		}

		// TODO: Response the client
	}

	private static void lookup(Socket socket) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(appServer.posts);
			oos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static AppendEntriesRPC genAppendEntriesRPC(Node node) {
		int nextIndex = node.getNextIndex();
		int prevLogIndex = -1, prevLogTerm = -1;
		if (nextIndex >= 1) {
			prevLogIndex = nextIndex - 1;
			prevLogTerm = getEntryFromIndex(prevLogIndex).getTerm();
		}
		List<LogEntry> entries = getEntriesFromIndex(nextIndex);
		AppendEntriesRPC ae = new AppendEntriesRPC(appServer.currentTerm, appServer.id, prevLogIndex, prevLogTerm,
				entries, appServer.commitIndex);
		return ae;
	}

	public static void sendMessage(Node node, Serializable message) {
		InetAddress address = null;
		try {
			address = InetAddress.getByName(node.getIPAddress());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		try {
			Socket socket = new Socket(address, DC_PORT);
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(message);
			oos.flush();
			socket.close();
			if (DEBUG)
				System.out.println(
						"Message sent to node " + node.getId() + " (" + node.getIPAddress() + ":" + DC_PORT + ").");
		} catch (ConnectException e) {
			System.out.println(
					e.getMessage() + ", possibly no process is listening on " + node.getIPAddress() + ":" + DC_PORT);
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
		// appServer.timeElapsed = 0;
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
				sendMessage(appServer.nodes.get(i), rpc);
			}
		}
	}

	public static void becomeFollower() {
		appServer.state = ServerState.FOLLOWER;
	}

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
	// if (!appServer.nodes.get(i).equals(appServer.myIPAddress)) {
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
			// sendAppendEntriesToAll();
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
		String IPAddressesFile = "./IPAddresses2";

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

		System.out.println("My IP address is " + myIPAddress + ", my id is " + appServer.id + ".");

		becomeFollower();

		// Fix leader
		if (appServer.id == 0) {
			appServer.state = ServerState.LEADER;
		}

		appServer.currentLeader = 0;
		appServer.currentTerm = 1;

		Thread listenToClientsThread = new Thread(new ListenToClientsThread());
		listenToClientsThread.start();

		Thread listenToDCThread = new ListenToDCThread();
		listenToDCThread.start();

		while (true) {
			try {
				serverPeriodic();
				Thread.sleep(period);
				printLog();
				Thread.sleep(period);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
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
				Socket socket = null;
				try {
					socket = listenToClientsSocket.accept();
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}

				String req = null;
				try {
					InputStreamReader isr = new InputStreamReader(socket.getInputStream());
					BufferedReader br = new BufferedReader(isr);
					req = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}

				handleClientsReq(req, socket);

				try {
					socket.close();
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
						sendMessage(appServer.nodes.get(appServer.currentLeader), response);
						break;
					}
					case APPEND_ENTRIES_RESPONSE: {
						recvAppendEntriesResponse(message);
						break;
					}
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
			appServer.timeoutElapsed = 0;

			AppendEntriesRPC ae = (AppendEntriesRPC) message;

			Message response;

			if (DEBUG) {
				System.out.println("recvAppendEntries: term = " + ae.getTerm() + ", leaderId = " + ae.getLeaderId()
						+ ", prevLogIndex = " + ae.getPrevLogIndex() + ", prevLogTerm = " + ae.getPrevLogTerm()
						+ ", leaderCommit = " + ae.getLeaderCommit());
			}

			boolean success = true;
			if (appServer.state == ServerState.CANDIDATE && ae.getTerm() == appServer.currentTerm) {
				becomeFollower();
			} else if (ae.getTerm() > appServer.currentTerm) {
				appServer.currentTerm = ae.getTerm();
				becomeFollower();
			} else if (ae.getTerm() < appServer.currentTerm) {
				/* 1. Reply false if term < currentTerm (5.1) */
				success = false;
			}

			int prevLogIndex = ae.getPrevLogIndex();
			if (success && prevLogIndex >= 0) {
				/*
				 * 2. Reply false if log doesn't contain an entry at
				 * prevLogIndex whose term matches prevLogTerm (5.3)
				 */
				LogEntry entryAtPrevLogIndex = getEntryFromIndex(prevLogIndex);
				if (entryAtPrevLogIndex == null || entryAtPrevLogIndex.getTerm() != ae.getPrevLogTerm()) {
					success = false;
					deleteEntriesFromIndex(ae.getPrevLogIndex());
				}
			}
			if (!success) {
				response = new AppendEntriesRPCResponse(appServer.currentTerm, success, appServer.id,
						appServer.log.size() - 1);
				return response;
			}

			/*
			 * 3. If an existing entry conflicts with a new one (same index but
			 * different terms), delete the existing entry and all that follow
			 * it (5.3)
			 */
			for (int i = 0; i < ae.getEntries().size(); ++i) {
				LogEntry aeEntry = ae.getEntries().get(i);
				int existingEntryIndex = ae.getPrevLogIndex() + 1 + i;
				LogEntry existingEntry = getEntryFromIndex(existingEntryIndex);
				if (existingEntry == null)
					break;
				else if (existingEntry.getTerm() != aeEntry.getTerm()) {
					deleteEntriesFromIndex(existingEntryIndex);
					// Now existingEntry is null, so break the loop.
					break;
				}
			}

			/* 4. Append any new entries not already in the log */
			// TODO: Check
			for (LogEntry e : ae.getEntries()) {
				appServer.log.add(e);
			}

			/*
			 * 5. If leaderCommit > commitIndex, set commitIndex =
			 * min(leaderCommit, index of last new entry)
			 */
			if (ae.getLeaderCommit() > appServer.commitIndex) {
				appServer.commitIndex = Math.min(ae.getLeaderCommit(), appServer.log.size() - 1);
			}

			response = new AppendEntriesRPCResponse(appServer.currentTerm, success, appServer.id,
					appServer.log.size() - 1);
			return response;
		}

		private static void recvAppendEntriesResponse(Message message) {
			AppendEntriesRPCResponse aer = (AppendEntriesRPCResponse) message;

			if (DEBUG) {
				System.out.println("recvAppendEntriesResponse: term = " + aer.getTerm() + ", success = "
						+ aer.isSuccess() + ", nodeId = " + aer.getNodeId());
			}

			if (appServer.state != ServerState.LEADER) {
				return;
			}

			if (aer.getTerm() > appServer.currentTerm) {
				appServer.currentTerm = aer.getTerm();
				becomeFollower();
				return;
			}

			// if (aer.getTerm() != appServer.currentTerm) {
			// return;
			// }

			Node node = appServer.nodes.get(aer.getNodeId());

			if (aer.isSuccess()) {
				// TODO: To update
				node.setNextIndex(aer.getLogIndex() + 1);
				node.setMatchIndex(aer.getLogIndex());
			} else {
				node.setNextIndex(node.getNextIndex() - 1);
				AppendEntriesRPC ae = genAppendEntriesRPC(node);
				sendMessage(node, ae);
				return;
			}

			// TODO: cfg_change

			/*
			 * If there exists an N such that N > commitIndex, a majority of
			 * matchIndex[i] >= N, and log[N].term == currentTerm: set
			 * commitIndex = N (5.3, 5.4).
			 */
			int lastLogIndex = appServer.log.size() - 1;
			for (int N = lastLogIndex; N > appServer.commitIndex
					&& appServer.log.get(N).getTerm() == appServer.currentTerm; --N) {
				int cnt = 1;
				for (Node n : appServer.nodes) {
					if (n.getId() == appServer.id)
						continue;

					// TODO: Check
					if (n.getMatchIndex() >= N)
						++cnt;

					// TODO: There may be a problem with the number of nodes
					if (cnt >= appServer.nodes.size() / 2) {
						appServer.commitIndex = N;
					}
				}
			}

			// Send remaining entries, is this needed?
			// AppendEntriesRPC ae = genAppendEntriesRPC(node);
			// sendMessage(node, ae);
		}

		//
		// private Message recvRequestVote(Message message) {
		// RequestVoteRPC rv = (RequestVoteRPC) message;
		//
		// boolean voteGranted;
		//
		// // 1
		// if (rv.getTerm() < appServer.currentTerm) {
		// voteGranted = false;
		// }
		//
		// // 2
		// boolean isUpToDate = true;
		//
		// int currentIndex = appServer.log.size() - 1;
		// LogEntry e = getEntryFromIndex(currentIndex);
		//
		// if (e.getTerm() > rv.getLastLogTerm()
		// || (e.getTerm() == rv.getLastLogTerm() && currentIndex >
		// rv.getLastLogIndex())) {
		// isUpToDate = false;
		// }
		//
		// if ((appServer.votedFor == null ||
		// appServer.votedFor
		// == rv.getCandidateId())
		// && isUpTodate) {
		// voteGranted = true;
		// }
		//
		// Message response = new
		// RequestVoteRPCResponse(appServer.currentTerm, voteGranted);
		// return response;
		// }
		//
		// private void recvRequestVoteResponse(Message message) {
		// RequestVoteRPCResponse rvr = (RequestVoteRPCResponse) message;
		//
		// if (appServer.state != ServerState.CANDIDATE) {
		// return;
		// } else if (appServer.currentTerm < rvr.getTerm()) {
		// appServer.currentTerm = rvr.getTerm();
		// becomeFollower();
		// return;
		// } else if (appServer.currentTerm != rvr.getTerm()) {
		// return;
		// }
		//
		// if (rvr.isVoteGranted()) {
		// if (is_majority) {
		// becomeLeader();
		// }
		// }
		// }
		//

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
