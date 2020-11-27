package common;

import controller.Link;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

public class Internet {
	private static Internet instance;
	private InetAddress genericIp = null;

	private final HashMap<InetAddress, ArrayList<Link>> clientUserLinks;
	private final HashMap<InetAddress, TestServer> servers;
	
	private Internet() {
		servers = new HashMap<>();

		try {
			genericIp = InetAddress.getByName("0.0.0.0");
		} catch (UnknownHostException e) {
			fail();
		}

		clientUserLinks = new HashMap<>();
		clientUserLinks.put(genericIp, new ArrayList<>());
	}
	
	public static Internet getInstance() {
		if(instance == null) {
			instance = new Internet();
		}
		
		return instance;
	}
	
	public void addServer(InetAddress ip, TestServer server) {
		servers.put(ip, server);
	}
	
	//per le comunicazioni client-server o user-server
	public Link connectTo(InetAddress ipFrom, InetAddress ipTo) {

		Link clientUserLink = getClientUserLink(ipTo);
		if(clientUserLink != null)
			return clientUserLink;
		
		TestServer server = servers.get(ipTo);
		
		if(server == null) {
			return new Link(ipTo, null, null);
		}
		
		TestSocket socket = null;
		try {
			socket = new TestSocket();
		} catch (IOException e) {
			fail();
		}
		
		boolean successfulConnection = server.testConnectToServer(socket.getInput1(), socket.getOutput1(), ipFrom);

		Link link;
		if(successfulConnection)
			link = new Link(ipTo, socket.getInput0(), socket.getOutput0());
		else
			link = new Link(ipTo, null, null);
		
		return link;
	}

	//Per le comunicazioni client-user
	public Link userRedirect(InetAddress ipFrom) {
		return userRedirect(ipFrom, genericIp);
	}

	//Per le comunicazioni client-user
	public Link userRedirect(InetAddress ipFrom, InetAddress ipTo){
		TestSocket socket = null;
		try {
			socket = new TestSocket();
		} catch (IOException e) {
			fail();
		}

		synchronized (this){
			ArrayList<Link> links = clientUserLinks.get(ipTo);
			if(links == null){
				links = new ArrayList<>();
				clientUserLinks.put(ipTo, links);
			}

			links.add(new Link(ipFrom, socket.getInput0(), socket.getOutput0()));
		}

		return new Link(ipTo, socket.getInput1(), socket.getOutput1());
	}

	public synchronized void resetUserConnections() {
		clientUserLinks.clear();
		clientUserLinks.put(genericIp, new ArrayList<>());
	}

	private Link getClientUserLink(InetAddress ip){
		ArrayList<Link> specificTerminalLinks = clientUserLinks.get(ip);

		Link link;

		synchronized(this){
			if(specificTerminalLinks == null || specificTerminalLinks.isEmpty()){
				ArrayList<Link> genericLinks = clientUserLinks.get(genericIp);

				if(genericLinks.isEmpty())
					return null;

				link = genericLinks.get(0);
				genericLinks.remove(0);

			}
			else{
				link = specificTerminalLinks.get(0);
				specificTerminalLinks.remove(0);
			}

		}

		return link;
	}
}
