package datacenter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AppServer {
	private static AppServer appServer;

	private static final int period = 100;
	
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

	List<Integer> nextIndex;

	List<Integer> matchIndex;

	private int timeElapsed;
	
	/* */
	private List<AppServer> nodes;
	
	private int index;
	
	private int currentLeader;
	
	private List<Integer> recvAppendEntriesRPC(AppendEntriesRPC rpc) {

	}

	private List<Integer> recvRequestVoteRPC(AppendEntriesRPC rpc) {

	}

	public boolean SendAppendEntriesRPC(AppServer server, AppServer targetServer, AppendEntriesRPC rpc){
		
		return true;
	}
	
	public boolean SendRequestVoteRPC(AppServer server, AppServer targetServer, RequestVoteRPC rpc){
		
		return true;
	}

	public void SendAppendEntriesToAll(){
		this.timeElapsed = 0;
		int num = nodes.size();
		for(int i = 0; i < num; ++i) 
		if(nodes.get(i) != this) {
			int term = this.currentTerm;
			int leaderId = this.index;
			int prevLogIndex = -1;
			int prevLogTerm = -1;
			List<LogEntry> entries = new ArrayList<>();
			int leaderCommit = this.commitIndex;
			AppendEntriesRPC rpc = new AppendEntriesRPC(term, leaderId, prevLogIndex, prevLogTerm, entries, leaderCommit);
			SendAppendEntriesRPC(this, this.nodes.get(i), rpc); 
		}
	}
	
	public void BecomeCandidate() {
		this.currentTerm += 1;
		
		/* vote for self */
		this.votedFor = this.index;
		
		this.currentLeader = -1;
		this.state = ServerState.CANDIDATE;
		
		Random rn = new Random();
		this.timeElapsed = (electionTimeout - 2 * (rn.nextInt(10000) % electionTimeout)) / 10;
		
		/*request vote*/
		int num = nodes.size();
		for(int i = 0; i < num; ++i) 
		if(this != this.nodes.get(i) ) {
			int term = this.currentTerm;
			int candidateId = this.index;
			int lastLogIndex = this.log.size()-1;
			int lastLogTerm = this.log.get(lastLogIndex).getTerm();
			RequestVoteRPC rpc = new RequestVoteRPC(term, candidateId, lastLogIndex, lastLogTerm);
			SendRequestVoteRPC(this, this.nodes.get(i), rpc); /*server to server*/
		}
	}
	
	public void ElectionStart() {
		BecomeCandidate();
	}
	
	public void BecomeLeader() {
		this.state = ServerState.LEADER;
		int lastLogIndex = this.log.size()-1;
		int num = nodes.size();
		
		/* leader appends entries, then updates nextIndex[] and matchIndex[] */ 
		
		for(int i = 0; i < num; ++i) 
		if(this.nodes.get(i) != this && lastLogIndex >= this.nextIndex.get(i)){ //
			AppServer target = this.nodes.get(i);
			boolean flag = false;
			while(!flag) {
				int term = this.currentTerm;
				int leaderId = this.index;
				int prevLogIndex = this.nextIndex.get(i)-1;
				int prevLogTerm = this.log.get(prevLogIndex).getTerm();
				List<LogEntry> entries = new ArrayList<>(this.log.subList(prevLogIndex+1, lastLogIndex+1));
				int leaderCommit = this.commitIndex;
				AppendEntriesRPC rpc = new AppendEntriesRPC(term, leaderId, prevLogIndex, prevLogTerm, entries, leaderCommit);
				//////////////////////////////////////////////
				if(SendAppendEntriesRPC(this, target, rpc)) {
					flag = true;
					this.nextIndex.set(i, lastLogIndex+1);
					this.matchIndex.set(i, lastLogIndex);
				} else {
					nextIndex.set(i, prevLogIndex);
				}
				///////////////////////////////////////////////
			}
		}
		
		/* update commitIndex */
		for(int N = lastLogIndex; N >= this.commitIndex; --N) {
			int counter = 0;
			for(int i = 0; i < num; ++i) 
			if( (this.nodes.get(i) != this) && this.matchIndex.get(i) >= N) {
				int term = this.log.get(N).getTerm();
				if(term == this.currentTerm) counter++;
			}
			if(counter+1 > num/2) { //+1 because we need to add the server itself
				this.commitIndex = N;
				break;
			}
		}
		
		/* note here we didn't apply the commitIndex to the state-machine */
	}
	
	/* we always assume # of servers > 1*/
	public void ServerPeriodic() {
		this.timeElapsed += period;
		
		/* send heartbeat or start a new election */
		if(this.state == ServerState.LEADER) {
			if(requestTimeout <= this.timeElapsed) {
				SendAppendEntriesToAll();
			}
		}
		else if (electionTimeout <= this.timeElapsed) {
			ElectionStart();
		}
		
		/* commit idx < lastApplied */
		if(this.commitIndex > this.lastApplied) {
			while(this.lastApplied < this.commitIndex) {
				this.lastApplied++;
				String cmd = this.log.get(this.lastApplied).getCommand();
				this.posts.add(cmd);
			}
		}
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
