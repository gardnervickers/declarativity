package jol.net.tcp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import jol.core.Runtime;
import jol.net.Address;
import jol.net.Channel;
import jol.net.IP;
import jol.net.Message;
import jol.net.Network;
import jol.net.Server;
import jol.types.basic.Tuple;
import jol.types.basic.TupleSet;
import jol.types.exception.UpdateException;
import jol.types.table.TableName;

public class TCP extends Server {
	private static final TableName ReceiveMessage = new TableName("tcp", "receive");
	
	private Runtime context;
	
	private Network manager;
	
	private ServerSocket server;
	
	private ThreadGroup threads;
	
	private Map<Address, Thread> channels;
		
	public TCP(Runtime context, Network manager, Integer port) throws IOException, UpdateException {
		super("TCP Server");
		this.context = context;
		this.manager = manager;
		this.server = new ServerSocket(port);
		this.threads = new ThreadGroup("TCP");
		this.channels = new HashMap<Address, Thread>();
		context.install("system", "jol/net/tcp/tcp.olg");
	}
	
	@Override
	public void interrupt() {
		super.interrupt();
		this.threads.interrupt();
		this.threads.destroy();
	}
	
	@Override
	public void run() {
		while (true) {
			Connection channel = null;
			try {
				Socket socket = this.server.accept();
				
				channel = new Connection(socket);
				manager.connection().register(channel);
				
				Thread thread  = new Thread(this.threads, channel);
				thread.start();
				channels.put(channel.address(), thread);
			} catch (IOException e) {
				if (channel != null) channel.close();
				e.printStackTrace();
			} catch (UpdateException e) {
				if (channel != null) channel.close();
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public Channel open(Address address) {
		try {
			Connection channel = new Connection(address);
			Thread thread = new Thread(this.threads, channel);
			thread.start();
			channels.put(channel.address(), thread);
			return channel;
		} catch (Exception e) {
            throw new RuntimeException("Failed to connect to " + address, e);
		}
	}
	
	@Override
	public void close(Channel channel) {
		if (channels.containsKey(channel.address())) {
			((Connection)channel).close();
			channels.get(channel.address()).interrupt();
			channels.remove(channel.address());
		}
	}
	
	private class Connection extends Channel implements Runnable {
		private Socket socket;
		private ObjectInputStream iss;
		private ObjectOutputStream oss;

		public Connection(Address address) 
			throws UnknownHostException, IOException {
			super("tcp", address);
			IP ip = (IP) address;
			this.socket = new Socket(ip.address(), ip.port());
			this.oss    = new ObjectOutputStream(this.socket.getOutputStream());
			this.iss    = new ObjectInputStream(this.socket.getInputStream());
		}
		
		public Connection(Socket socket) throws IOException {
			super("tcp", new IP(socket.getInetAddress(), socket.getPort()));
			this.socket = socket;
			this.oss    = new ObjectOutputStream(this.socket.getOutputStream());
			this.iss    = new ObjectInputStream(socket.getInputStream());
		}
		
		@Override
		public boolean send(Message packet) {
			try {
				if (this.socket.isClosed()) {
					return false;
				}
				this.oss.writeObject(packet);
			} catch (IOException e) {
				return false;
			}
			return true;
		}
		
		private void close() {
			try {
				socket.close();
			} catch (IOException e) { }
		}

		public void run() {
			while(true) {
				try {
					Message message = (Message) this.iss.readObject();
					IP address = new IP(this.socket.getInetAddress(), this.socket.getPort());
					Tuple tuple = new Tuple(address, message);
					context.schedule("tcp", ReceiveMessage, new TupleSet(ReceiveMessage, tuple), new TupleSet(ReceiveMessage));
				} catch (IOException e) {
					try {
						TCP.this.manager.connection().unregister(this);
					} catch (UpdateException e1) {
						e1.printStackTrace();
					}
					return;
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					System.exit(0);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(0);
				}
			}
		}
	}

}
