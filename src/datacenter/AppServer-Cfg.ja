package datacenter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
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
	private static AppServer appServer = new AppServer();

	private static final int period = 2000;

	private static final int DC_LISTEN_TO_CLIENTS_PORT = 8887;

	private static final int DC_LISTEN_TO_DC_PORT = 8888;

	private static final int CLIENT_LISTEN_TO_DC_PORT = 8888;

	private static final int requestTimeout = 100; ////

	private static final int electionTimeout = 5 * period; ///

	private static final boolean DEBUG = true;

	List<String> posts;

	ServerState state;

	private int currentTerm;

	private int votedFor = -1;

	private boolean isVoting;

	private List<LogEntry> log;

	private int commitIndex;

	private int lastApplied;

	private int timeoutElapsed;

	private List<Node> nodes;

	private int id = -1;

	private int currentLeader;

	private int cfgChangeLogIdx; /* CFG change: configuration change entry index */
	
	public static void printState() {
		if (appServer.state == ServerState.FOLLOWER) {
			System.out.println("state: FOLLOWER.");
		} else if (appServer.state == ServerState.CANDIDATE) {
			System.out.println("state: CANDIDATE.");
		} else if (appServer.state == ServerState.LEADER) {
			System.out.println("state: LEADER.");
		}
	}

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
		state = ServerState.FOLLOWER;
		currentTerm = 1;
		votedFor = -1;
		commitIndex = -1;
		lastApplied = -1;
		id = -1;
		currentLeader = -1;

		log = new ArrayList<>();
		nodes = new ArrayList<>();
		posts = new ArrayList<>();
		posts.add("init1");
		posts.add("init2");
		posts.add("init3");
		posts.add("init4");
	}

	public static AppServer getAppServer() {
		return appServer;
	}

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
		/* CFG change: if a configuration entry has been deleted, update cfg_change_log_idx */
		if(appServer.cfgChangeLogIdx >= i) 
			appServer.cfgChangeLogIdx = -1;
		/* CFG change */
	}

	public static void handleClientsReq(String req, InetAddress inetAddress) {
		String[] ss = req.split(" ", 2);

		if (ss[0].compareTo("p") == 0) {
			if (appServer.state != ServerState.LEADER) {
				if (DEBUG)
					System.out.println("I am not the leader and I need to redirect this req.");
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

	public static void recvEntry(String command, String content) {

		if (DEBUG)
			System.out.println("recvEntry, command = " + command);

		LogEntry entry = new LogEntry(appServer.currentTerm, command);
		appServer.log.add(entry);

		/* CFG change 1:*/
		if(command == "C_old,new") {
			List<Node> newServers = parse(content); // list of servers in the new configuration.
			for(Node newNode : newServers) {
				if(newNode not in appServer.nodes() ) {
					appServer.nodes.add(newNode);
				}
			}
			for(Node newNode : newServers) {
				for(Node node : appServer.nodes) {
					if(newNode == node) {
						int oldFlag = node.getFlag();
						node.setFlag(oldFlag | (1<<1) );  //update newNode's state
					}
				}
			}
			
			cfgChangeLogIdx = appServer.log.size()-1;
		}
		
		/* CFG change 2: */
		if(command == "C_new") {
			List<Node> newServers = parse(content);
			for(Node newNode : newServers) {
				for(Node node : appServer.nodes) {
					if(newNode == node) {
						node.setFlag(1); //set newNode's state to be 1
					} else {
						node.setFlag(0);
					} //set other nodes state to be zero
				}
			}
			
			cfgChangeLogIdx = appServer.log.size()-1;
		}
		/* there are two types of configuration change: C_{old,new} and C_{new} */
		
		for (Node node : appServer.nodes) if(node.getFlag() > 0){ 
			if (node.getId() == appServer.id) /* CFG change: also check if node.Flag > 0 */
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
			Socket socket = new Socket(address, DC_LISTEN_TO_DC_PORT);
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(message);
			oos.flush();
			socket.close();
			if (DEBUG)
				System.out.println("Message sent to node " + node.getId() + " (" + node.getIPAddress() + ":"
						+ DC_LISTEN_TO_DC_PORT + ").");
		} catch (ConnectException e) {
			System.out.println(e.getMessage() + ", possibly no process is listening on " + node.getIPAddress() + ":"
					+ DC_LISTEN_TO_DC_PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void sendAppendEntriesToAll() {
		int num = appServer.nodes.size();
		for (int i = 0; i < num; ++i) if(appServer.nodes.get(i).getFlag() > 0) { /* CFG CHANGE: send to a server only if the server's flag > 0 */
			if (appServer.id != appServer.nodes.get(i).getId() ) { 
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
				sendMessage(appServer.nodes.get(i), ae);
			}
		}
	}

	public static void becomeFollower() {
		if (DEBUG)
			System.out.println("becomeFollower");

		appServer.votedFor = -1;
		appServer.state = ServerState.FOLLOWER;
	}

	public static void becomeCandidate() {
		if (DEBUG) {
			System.out.println("becomeCandidate");
			System.out.println("currentTerm = " + appServer.currentTerm);
		}

		appServer.currentTerm += 1;

		for (Node node : appServer.nodes) { 
			if (node.getId() == appServer.id)/*CFG change: might also check that appServer's flag > 0 */
				node.setVotedForMe(true);
			else
				node.setVotedForMe(false);
		}

		/* vote for self */
		appServer.votedFor = appServer.id;
		appServer.currentLeader = -1;
		appServer.state = ServerState.CANDIDATE;

		Random rn = new Random();
		appServer.timeoutElapsed = (electionTimeout - 2 * (rn.nextInt(5 * electionTimeout) % electionTimeout)) / 2;

		/* request vote */
		for (Node node : appServer.nodes) if(node.getFlag() > 0) { /*CFG change: only requestVote from those servers whose flag > 0 */
			if (node.getId() == appServer.id)
				continue;
			int term = appServer.currentTerm;
			int candidateId = appServer.id;
			int lastLogIndex = appServer.log.size() - 1;
			int lastLogTerm = -1;
			if (lastLogIndex >= 0)
				lastLogTerm = appServer.log.get(lastLogIndex).getTerm();
			RequestVoteRPC rpc = new RequestVoteRPC(term, candidateId, lastLogIndex, lastLogTerm);
			sendMessage(node, rpc);
		}
	}

	public static void becomeLeader() {
		if (DEBUG)
			System.out.println("becomeLeader");

		appServer.state = ServerState.LEADER;

		int lastLogIndex = appServer.log.size() - 1;
		for (Node node : appServer.nodes) if(node.getFlag() > 0) { /*CFG change: only for those servers whose flag > 0 */
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
		// //////////////////////////////////////////////
		// sendAppendEntriesRPC(this, target, rpc);
		// flag = true;
		// this.nextIndex.set(i, lastLogIndex + 1);
		// this.matchIndex.set(i, lastLogIndex);
		// } else {
		// nextIndex.set(i, prevLogIndex);
		// }
		///////////////////////////////////////////////

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
		while (appServer.commitIndex > appServer.lastApplied) {
			++appServer.lastApplied;
			String cmd = appServer.log.get(appServer.lastApplied).getCommand();
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
		appServer.currentTerm = 1;

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
						Message response = recvAppendEntries(message);
						sendMessage(appServer.nodes.get(appServer.currentLeader), response);
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
					deleteEntriesFromIndex(prevLogIndex);
				}
			}
			if (!success) {
				response = new AppendEntriesRPCResponse(appServer.currentTerm, success, appServer.id, -1);
				return response;
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
				node.setNextIndex(aer.getLogIndex() + 1);
				node.setMatchIndex(aer.getLogIndex());
			} else {
				node.setNextIndex(node.getNextIndex() - 1);
				AppendEntriesRPC ae = genAppendEntriesRPC(node);
				sendMessage(node, ae);
				return;
			}

			// TODO: cfg_change
			/*CFG change: we should set a non-voting new server to be isVoting if it receives sufficient logs, 
			  but here we just assume the new server is "isVoting" from the beginning, thus we need to do nothing here */
			
			/*
			 * If there exists an N such that N > commitIndex, a majority of
			 * matchIndex[i] >= N, and log[N].term == currentTerm: set
			 * commitIndex = N (5.3, 5.4).
			 */
			int lastLogIndex = appServer.log.size() - 1;
			for (int N = lastLogIndex; N > appServer.commitIndex
					&& appServer.log.get(N).getTerm() == appServer.currentTerm; --N) {
				
				/* CFG change: there should be cnt1 and cnt2, one for the old configuration and one for the new configuration*/
				int cntOld = 0;
				int cntNew = 0;
				int numOld = 0;
				int numNew = 0;
				for (Node n : appServer.nodes) if (n.getFlag() > 0) { //CFG change: compute numOld, numNew
					if( (n.getFlag() | 1) > 0) numOld++;
					if( (n.getFlag() | 2) > 0) numNew++; 
				}
				
				for (Node n : appServer.nodes) if (n.getFlag() > 0) { //only consider the server whose flag > 0 
					if (n.getId() == appServer.id) { //add the vote myself
						if( (n.getFlag() | 1) > 0) cntOld++;
						if( (n.getFlag() | 2) > 0) cntNew++; 
						continue;
					}
					// TODO: Check
					if (n.getMatchIndex() >= N) { //check both old and new configurations
						if( (n.getFlag() | 1) > 0) cntOld++;
						if( (n.getFlag() | 2) > 0) cntNew++; 
					}

					// TODO: There may be a problem with the number of nodes
					if (cntOld >= numOld / 2 + 1) {
						if(numNew == 0 || cntNew >= numNew / 2 + 1) //it's possible that numNew = 0
							appServer.commitIndex = N;
					}
				}
				/*CFG change */
			}
			
			// Send remaining entries, is this needed?
			// AppendEntriesRPC ae = genAppendEntriesRPC(node);
			// sendMessage(node, ae);
		}

		private static void recvRequestVote(Message message) {
			RequestVoteRPC rv = (RequestVoteRPC) message;
			
			/*CFG change: if the receiver has received a heartbeat recently, he won't grant the vote */
			boolean voteGranted = false;
			if (appServer.timeoutElapsed < electionTimeout / 3) {
				Message response = new RequestVoteRPCResponse(appServer.currentTerm, voteGranted, appServer.id);
				sendMessage(appServer.nodes.get(rv.getCandidateId()), response);
				return;
			}
			/* CFG change */
			
			if (rv.getTerm() > appServer.currentTerm) {
				appServer.currentTerm = rv.getTerm();
				becomeFollower();
			}


			/* 1. Reply false if term < currentTerm (5.1) */
			if (rv.getTerm() < appServer.currentTerm) {
				voteGranted = false;
			}

			/*
			 * 2. If votedFor is null or candidateId, and candidate's log is at
			 * least as up-to-date as receiver's log, grant vote (5.2, 5.4)
			 */
			boolean isUpToDate = true;

			int currentIndex = appServer.log.size() - 1;

			if (currentIndex != -1) {
				LogEntry e = getEntryFromIndex(currentIndex);

				if (e.getTerm() > rv.getLastLogTerm()
						|| (e.getTerm() == rv.getLastLogTerm() && currentIndex > rv.getLastLogIndex())) {
					isUpToDate = false;
				}
			}

			if ((appServer.votedFor == -1 || appServer.votedFor == rv.getCandidateId()) && isUpToDate) {
				voteGranted = true;
				appServer.votedFor = rv.getCandidateId();
			}

			Message response = new RequestVoteRPCResponse(appServer.currentTerm, voteGranted, appServer.id);
			sendMessage(appServer.nodes.get(rv.getCandidateId()), response);
		}

		private static void recvRequestVoteResponse(Message message) {
			RequestVoteRPCResponse rvr = (RequestVoteRPCResponse) message;

			if (appServer.state != ServerState.CANDIDATE) {
				return;
			} else if (appServer.currentTerm < rvr.getTerm()) {
				appServer.currentTerm = rvr.getTerm();
				becomeFollower();
				return;
			} else if (appServer.currentTerm != rvr.getTerm()) {
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

		private static boolean isMajority() { /* CFG change: consider c_old and c_new */
			int cntOld = 0, cntNew = 0;
			int numOld = 0, numNew = 0;
			for (Node n : appServer.nodes) if (n.getFlag() > 0) { //CFG change: compute numOld, numNew
				if( (n.getFlag() | 1) > 0) numOld++;
				if( (n.getFlag() | 2) > 0) numNew++; 
			}
			
			for (Node node : appServer.nodes) if (node.getFlag() > 0){
				if (node.isVotedForMe()) {
					if( (node.getFlag() | 1) > 0) cntOld++;
					if( (node.getFlag() | 2) > 0) cntNew++; 
				}
			}
			if (cntOld >= numOld / 2 + 1) {
				if(numNew == 0 || cntNew >= numNew / 2 + 1) { //it's possible that numNew = 0
					return true;
				}
			}
			return false;
		}
	}
}
