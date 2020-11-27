package postazione.controller;

import controller.Link;
import controller.ServiceFactory;
import controller.TerminalController;

public class Factory implements ServiceFactory {

	@Override
	public Runnable createService(TerminalController controller, Link link, String name) {
		return new Service(controller, link, name);
	}

	/*public Runnable createService(TerminalController controller, Socket socket) {
		return new Service((Controller) controller, socket);
	}
	
	public Runnable createService(TerminalController controller, Scanner in, PrintWriter out, InetAddress ip){
		return new Service((Controller) controller, in, out, ip);
	}*/
	
}
