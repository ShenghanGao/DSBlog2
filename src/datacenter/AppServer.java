package datacenter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AppServer {
	private static AppServer appServer;

	private static final int period = 10000;

	private static final int DC_LISTEN_TO_CLIENTS_PORT = 8887;

	private static final int DC_LISTEN_TO_DC_PORT = 8888;

	private static final int CLIENT_LISTEN_TO_DC_PORT = 8888;

	private static final int electionTimeout = 5 * period; ///

	private static final boolean DEBUG = true;

	private static String currentTermFilename;

	private static String votedForFilename;

	private static String logFilename;

	List<String> posts;

	ServerState state;

	private boolean isVoting;

	private int commitIndex;

	private int lastApplied;

	private int timeoutElapsed;

	private List<Node> nodes;

	private int id = -1;

	private int currentLeader;

	public static void genFilenames(String IPAddress) {
		StringBuilder currentTermFilenameBuilder = new StringBuilder(".currentTerm.txt");
		currentTermFilenameBuilder.insert(0, IPAddress);
		currentTermFilename = currentTermFilenameBuilder.toString();

		StringBuilder votedForFilenameBuilder = new StringBuilder(".votedFor.txt");
		votedForFilenameBuilder.insert(0, IPAddress);
		votedForFilename = votedForFilenameBuilder.toString();

		StringBuilder logFilenameBuilder = new StringBuilder(".log.txt");
		logFilenameBuilder.insert(0, IPAddress);
		logFilename = logFilenameBuilder.toString();
	}

	public static int readCurrentTermFile() {
		int currentTerm = -1;
		try {
			FileInputStream fis = new FileInputStream(currentTermFilename);
			ObjectInputStream ois = new ObjectInputStream(fis);
			currentTerm = ois.readInt();
			ois.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return currentTerm;
	}

	public static void writeCurrentTermFile(int currentTerm) {
		try {
			FileOutputStream fos = new FileOutputStream(currentTermFilename);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeInt(currentTerm);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static int readVotedForFile() {
		int votedFor = -1;
		try {
			FileInputStream fis = new FileInputStream(votedForFilename);
			ObjectInputStream ois = new ObjectInputStream(fis);
			votedFor = ois.readInt();
			ois.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return votedFor;
	}

	public static void writeVotedForFile(int votedFor) {
		try {
			FileOutputStream fos = new FileOutputStream(votedForFilename);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeInt(votedFor);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<LogEntry> readLogFile() {
		List<LogEntry> log = null;
		try {
			FileInputStream fis = new FileInputStream(logFilename);
			ObjectInputStream ois = new ObjectInputStream(fis);
			log = (List<LogEntry>) ois.readObject();
			ois.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return log;
	}

	public static void writeLogFile(List<LogEntry> log) {
		try {
			FileOutputStream fos = new FileOutputStream(logFilename);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(log);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void printState() {
		if (appServer.state == ServerState.FOLLOWER) {
			System.out.println("state: FOLLOWER.");
		} else if (appServer.state == ServerState.CANDIDATE) {
			System.out.println("state: CANDIDATE.");
		} else if (appServer.state == ServerState.LEADER) {
			System.out.println("state: LEADER.");
		}
		int currentTerm = readCurrentTermFile();
		System.out.println("currentTerm = " + currentTerm);

		System.out.println("currentLeader = " + appServer.currentLeader);
	}

	public static void printLog() {
		List<LogEntry> log = readLogFile();
		for (LogEntry e : log) {
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

	private AppServer(boolean toRecover) {
		if (DEBUG)
			System.out.println("AppServer constructor is called! toRecover = " + toRecover);

		InetAddress inetAddress = null;
		try {
			inetAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		String myIPAddress = inetAddress.getHostAddress();
		genFilenames(myIPAddress);

		state = ServerState.FOLLOWER;

		commitIndex = -1;
		lastApplied = -1;
		id = -1;
		currentLeader = -1;

		if (!toRecover) {
			writeCurrentTermFile(1);
			writeVotedForFile(-1);
			writeLogFile(new ArrayList<>());
		}

		if (toRecover) {
			System.out.println("Recovered log: ");
			printLog();
		}

		nodes = new ArrayList<>();
		posts = new ArrayList<>();
		posts.add("init1");
		posts.add("init2");
	}

	public static AppServer getAppServer() {
		return appServer;
	}

	public static LogEntry getEntryFromIndex(int i) {
		List<LogEntry> log = readLogFile();
		if (i >= log.size())
			return null;
		return log.get(i);
	}

	public static List<LogEntry> getEntriesFromIndex(int i) {
		List<LogEntry> entries = new ArrayList<>();
		List<LogEntry> log = readLogFile();
		if (i >= log.size())
			return entries;
		return log.subList(i, log.size());
	}

	public static void deleteEntriesFromIndex(int i) {
		List<LogEntry> log = readLogFile();
		int lastIndex = log.size() - 1;
		for (; lastIndex >= i; --lastIndex) {
			log.remove(lastIndex);
		}
		writeLogFile(log);
	}

	public static void handleClientsReq(String req, InetAddress inetAddress) {
		String[] ss = req.split(" ", 2);

		if (ss[0].compareTo("p") == 0) {
			if (appServer.state != ServerState.LEADER) {
				if (DEBUG)
					System.out.println("I am not the leader and I need to redirect this req.");
				if (appServer.currentLeader == -1) {
					System.out.println("But currently I do not know who the leader is. Please send the request later.");
					return;
				}
				ClientRequest cr = new ClientRequest(inetAddress, req);
				sendMessage(appServer.nodes.get(appServer.currentLeader), cr);
				return;
			} else {
				recvEntry(ss[1]);
			}
		} else if (ss[0].compareTo("l") == 0) {
			sendLookup(inetAddress);
		}
	}

	public static void recvEntry(String command) {
		// TODO: cfg_change

		if (DEBUG)
			System.out.println("recvEntry, command = " + command);

		LogEntry entry = new LogEntry(readCurrentTermFile(), command);
		List<LogEntry> log = readLogFile();
		log.add(entry);
		writeLogFile(log);

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

			// sendMessage(node, ae);
		}

		// TODO: Response the client
	}

	private static void sendLookup(InetAddress inetAddress) {
		Socket socket = null;
		try {
			if (DEBUG)
				System.out.println("Client IP: " + inetAddress.getHostAddress());
			socket = new Socket(inetAddress, CLIENT_LISTEN_TO_DC_PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			OutputStream os = socket.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);

			oos.writeChar('l');
			oos.flush();

			if (DEBUG) {
				System.out.println("sendLookup: l sent.");
			}

			oos.writeObject(appServer.posts);
			oos.flush();

			if (DEBUG) {
				System.out.println("sendLookup: posts sent.");
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			socket.close();
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
		AppendEntriesRPC ae = new AppendEntriesRPC(readCurrentTermFile(), appServer.id, prevLogIndex, prevLogTerm,
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
			Socket socket = new Socket(address, DC_LISTEN_TO_DC_PORT);
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(message);
			oos.flush();
			socket.close();
			if (DEBUG) {
				System.out.println("Message sent to node " + node.getId() + "(" + node.getIPAddress() + ":"
						+ DC_LISTEN_TO_DC_PORT + ").");
			}
		} catch (ConnectException e) {
			System.out.println(e.getMessage() + ", possibly no process is listening on " + node.getIPAddress() + ":"
					+ DC_LISTEN_TO_DC_PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void sendAppendEntriesToAll() {
		int num = appServer.nodes.size();
		for (int i = 0; i < num; ++i) {
			if (appServer.id != appServer.nodes.get(i).getId()) {
				// int term = appServer.currentTerm;
				// int leaderId = appServer.id;
				// int prevLogIndex = -1;
				// int prevLogTerm = -1;
				// List<LogEntry> entries = new ArrayList<>();
				// int leaderCommit = appServer.commitIndex;
				// AppendEntriesRPC rpc = new AppendEntriesRPC(term, leaderId,
				// prevLogIndex, prevLogTerm, entries,
				// leaderCommit);

				AppendEntriesRPC ae = genAppendEntriesRPC(appServer.nodes.get(i));

				if (DEBUG)
					System.out.println("Now send heartbeat to node " + i);

				sendMessage(appServer.nodes.get(i), ae);
			}
		}
	}

	public static void becomeFollower() {
		if (DEBUG)
			System.out.println("becomeFollower");

		appServer.timeoutElapsed = 0;
		writeVotedForFile(-1);
		appServer.state = ServerState.FOLLOWER;
	}

	public static void becomeCandidate() {
		int currentTerm = readCurrentTermFile();

		currentTerm += 1;
		writeCurrentTermFile(currentTerm);

		if (DEBUG) {
			System.out.println("becomeCandidate, currentTerm = " + currentTerm);
		}

		for (Node node : appServer.nodes) {
			if (node.getId() == appServer.id)
				node.setVotedForMe(true);
			else
				node.setVotedForMe(false);
		}

		/* vote for self */
		writeVotedForFile(appServer.id);
		appServer.currentLeader = -1;
		appServer.state = ServerState.CANDIDATE;

		Random rn = new Random();
		appServer.timeoutElapsed = (electionTimeout - 2 * (rn.nextInt(5 * electionTimeout) % electionTimeout)) / 2;

		/* request vote */
		List<LogEntry> log = readLogFile();
		for (Node node : appServer.nodes) {
			if (node.getId() == appServer.id)
				continue;
			int term = currentTerm;
			int candidateId = appServer.id;
			int lastLogIndex = log.size() - 1;
			int lastLogTerm = -1;
			if (lastLogIndex >= 0)
				lastLogTerm = log.get(lastLogIndex).getTerm();
			RequestVoteRPC rpc = new RequestVoteRPC(term, candidateId, lastLogIndex, lastLogTerm);
			sendMessage(node, rpc);
		}
	}

	public static void becomeLeader() {
		int currentTerm = readCurrentTermFile();

		if (DEBUG) {
			System.out.println("becomeLeader, currentTerm = " + currentTerm);
		}

		appServer.state = ServerState.LEADER;
		appServer.currentLeader = appServer.id;

		int lastLogIndex = readLogFile().size() - 1;
		for (Node node : appServer.nodes) {
			if (node.getId() == appServer.id)
				continue;
			node.setNextIndex(lastLogIndex + 1);
			node.setMatchIndex(-1);
		}

		sendAppendEntriesToAll();

		/* leader appends entries, then updates nextIndex[] and matchIndex[] */

		// for (int i = 0; i < num; ++i)
		// if (appServer.nodes.get(i) != this && lastLogIndex >=
		// appServer.nextIndex.get(i)) { //
		// Node target = appServer.nodes.get(i);
		//
		//
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

		/* note here we didn't apply the commitIndex to the state-machine */
	}

	/* we always assume # of servers > 1 */

	public static void electionStart() {
		becomeCandidate();
	}

	public static void serverPeriodic() {
		appServer.timeoutElapsed += period;

		/* send heartbeat or start a new election */
		if (appServer.state == ServerState.LEADER) {
			sendAppendEntriesToAll();
		} else if (appServer.timeoutElapsed >= electionTimeout) {
			electionStart();
		}

		/* commit idx < lastApplied */
		List<LogEntry> log = readLogFile();
		while (appServer.commitIndex > appServer.lastApplied) {
			++appServer.lastApplied;
			String cmd = log.get(appServer.lastApplied).getCommand();
			appServer.posts.add(cmd);
		}
	}

	/*
	 * thundarr.cs.ucsb.edu: 128.111.43.40, optimus.cs.ucsb.edu: 128.111.43.41,
	 * megatron.cs.ucsb.edu: 128.111.43.42, tygra.cs.ucsb.edu: 128.111.43.43
	 */
	public static void main(String[] args) {
		String IPAddressesFile = "./IPAddresses2";

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(IPAddressesFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		System.out.println("args.length = " + args.length);

		assert(appServer == null);

		if (args.length == 0) {
			appServer = new AppServer(false);
		} else {
			appServer = new AppServer(true);
		}

		assert(appServer != null);

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
			// appServer.state = ServerState.LEADER;
		}

		appServer.currentLeader = -1;

		Thread listenToClientsThread = new Thread(new ListenToClientsThread());
		listenToClientsThread.start();

		Thread listenToDCThread = new ListenToDCThread();
		listenToDCThread.start();

		appServer.timeoutElapsed = 0;
		while (true) {
			try {
				serverPeriodic();
				Thread.sleep(period);
				printState();
				printLog();
				// Thread.sleep(period);
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
				listenToClientsSocket = new ServerSocket(DC_LISTEN_TO_CLIENTS_PORT, 5);
			} catch (IOException e) {
				e.printStackTrace();
			}
			while (true) {
				Socket socket = null;
				try {
					socket = listenToClientsSocket.accept();

					if (DEBUG)
						System.out.println("ListenToClientsThread accepted!");

				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}

				String req = null;
				try {
					InputStreamReader isr = new InputStreamReader(socket.getInputStream());
					BufferedReader br = new BufferedReader(isr);
					req = br.readLine();

					if (DEBUG) {
						System.out.println("req = " + req);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
				InetAddress inetAddress = socket.getInetAddress();

				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				handleClientsReq(req, inetAddress);
			}
		}
	}

	public static class ListenToDCThread extends Thread {
		ServerSocket listenToDCSocket;

		@Override
		public void run() {
			try {
				listenToDCSocket = new ServerSocket(DC_LISTEN_TO_DC_PORT, 5);
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
					case CLIENT_REQUEST: {
						recvClientRequest(message);
						break;
					}
					case APPEND_ENTRIES: {
						recvAppendEntries(message);
						break;
					}
					case APPEND_ENTRIES_RESPONSE: {
						recvAppendEntriesResponse(message);
						break;
					}
					case REQUEST_VOTE: {
						recvRequestVote(message);
						break;
					}
					case REQUEST_VOTE_RESPONSE: {
						recvRequestVoteResponse(message);
						break;
					}
					default: {

					}
					}

					socket.close();
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

		private static void recvClientRequest(Message message) {
			ClientRequest cr = (ClientRequest) message;
			handleClientsReq(cr.getReq(), cr.getInetAddress());
		}

		private static void recvAppendEntries(Message message) {
			appServer.timeoutElapsed = 0;

			AppendEntriesRPC ae = (AppendEntriesRPC) message;

			Message response;

			if (DEBUG) {
				System.out.println("recvAppendEntries: term = " + ae.getTerm() + ", leaderId = " + ae.getLeaderId()
						+ ", prevLogIndex = " + ae.getPrevLogIndex() + ", prevLogTerm = " + ae.getPrevLogTerm()
						+ ", leaderCommit = " + ae.getLeaderCommit());
			}

			int currentTerm = readCurrentTermFile();

			boolean success = true;
			if (appServer.state == ServerState.CANDIDATE && ae.getTerm() == currentTerm) {
				becomeFollower();
			} else if (ae.getTerm() > currentTerm) {
				currentTerm = ae.getTerm();
				writeCurrentTermFile(currentTerm);
				becomeFollower();
			} else if (ae.getTerm() < currentTerm) {
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
					deleteEntriesFromIndex(prevLogIndex);
				}
			}
			if (!success) {
				response = new AppendEntriesRPCResponse(currentTerm, success, appServer.id, -1);
				sendMessage(appServer.nodes.get(ae.getLeaderId()), response);
				return;
			}

			appServer.currentLeader = ae.getLeaderId();

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
			List<LogEntry> log = readLogFile();
			for (LogEntry e : ae.getEntries()) {
				log.add(e);
			}
			writeLogFile(log);

			/*
			 * 5. If leaderCommit > commitIndex, set commitIndex =
			 * min(leaderCommit, index of last new entry)
			 */
			if (ae.getLeaderCommit() > appServer.commitIndex) {
				appServer.commitIndex = Math.min(ae.getLeaderCommit(), log.size() - 1);
			}

			response = new AppendEntriesRPCResponse(currentTerm, success, appServer.id, log.size() - 1);
			sendMessage(appServer.nodes.get(ae.getLeaderId()), response);
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

			int currentTerm = readCurrentTermFile();

			if (aer.getTerm() > currentTerm) {
				writeCurrentTermFile(aer.getTerm());
				becomeFollower();
				return;
			}

			// if (aer.getTerm() != appServer.currentTerm) {
			// return;
			// }

			Node node = appServer.nodes.get(aer.getNodeId());

			if (aer.isSuccess()) {
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
			List<LogEntry> log = readLogFile();
			int lastLogIndex = log.size() - 1;
			for (int N = lastLogIndex; N > appServer.commitIndex && log.get(N).getTerm() == currentTerm; --N) {
				int cnt = 1;
				for (Node n : appServer.nodes) {
					if (n.getId() == appServer.id)
						continue;

					// TODO: Check
					if (n.getMatchIndex() >= N)
						++cnt;

					// TODO: There may be a problem with the number of nodes
					if (cnt >= appServer.nodes.size() / 2 + 1) {
						appServer.commitIndex = N;
					}
				}
			}

			// Send remaining entries, is this needed?
			// AppendEntriesRPC ae = genAppendEntriesRPC(node);
			// sendMessage(node, ae);
		}

		private static void recvRequestVote(Message message) {
			RequestVoteRPC rv = (RequestVoteRPC) message;

			if (DEBUG) {
				System.out.println("recvRequestVote: term = " + rv.getTerm() + ", candidateId = " + rv.getCandidateId()
						+ ", lastLogIndex = " + rv.getLastLogIndex() + ", lastLogTerm = " + rv.getLastLogTerm());
			}

			int currentTerm = readCurrentTermFile();

			if (rv.getTerm() > currentTerm) {
				currentTerm = rv.getTerm();
				writeCurrentTermFile(currentTerm);
				becomeFollower();
			}

			boolean voteGranted = false;

			/* 1. Reply false if term < currentTerm (5.1) */
			if (rv.getTerm() < currentTerm) {
				voteGranted = false;
			}

			/*
			 * 2. If votedFor is null or candidateId, and candidate's log is at
			 * least as up-to-date as receiver's log, grant vote (5.2, 5.4)
			 */
			boolean isUpToDate = true;

			int currentIndex = readLogFile().size() - 1;

			if (currentIndex != -1) {
				LogEntry e = getEntryFromIndex(currentIndex);

				if (e.getTerm() > rv.getLastLogTerm()
						|| (e.getTerm() == rv.getLastLogTerm() && currentIndex > rv.getLastLogIndex())) {
					isUpToDate = false;
				}
			}

			int votedFor = readVotedForFile();
			if ((votedFor == -1 || votedFor == rv.getCandidateId()) && isUpToDate) {
				voteGranted = true;
				writeVotedForFile(rv.getCandidateId());
			}

			RequestVoteRPCResponse response = new RequestVoteRPCResponse(currentTerm, voteGranted, appServer.id);

			if (DEBUG) {
				System.out.println("My response to recvRequestVote: term = " + response.getTerm() + ", isVoteGranted = "
						+ response.isVoteGranted() + ", nodeId = " + response.getNodeId());
			}

			sendMessage(appServer.nodes.get(rv.getCandidateId()), response);
		}

		private static void recvRequestVoteResponse(Message message) {
			RequestVoteRPCResponse rvr = (RequestVoteRPCResponse) message;

			if (DEBUG) {
				System.out.println("recvRequestVoteResponse: term = " + rvr.getTerm() + ", isVoteGranted = "
						+ rvr.isVoteGranted() + ", nodeId = " + rvr.getNodeId());
			}

			int currentTerm = readCurrentTermFile();

			if (appServer.state != ServerState.CANDIDATE) {
				return;
			} else if (currentTerm < rvr.getTerm()) {
				writeCurrentTermFile(rvr.getTerm());
				becomeFollower();
				return;
			} else if (currentTerm != rvr.getTerm()) {
				return;
			}

			if (rvr.isVoteGranted()) {
				Node node = appServer.nodes.get(rvr.getNodeId());
				node.setVotedForMe(true);
				if (isMajority()) {
					becomeLeader();
				}
			}
		}

		private static boolean isMajority() {
			int majority = appServer.nodes.size() / 2 + 1;
			int cnt = 0;
			for (Node node : appServer.nodes) {
				if (node.isVotedForMe())
					++cnt;
			}
			return cnt >= majority;
		}
	}
}
