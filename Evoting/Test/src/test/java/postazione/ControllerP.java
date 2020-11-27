package postazione;

import java.net.InetAddress;

import common.TestView;
import model.Terminals;
import common.TestServer;
import postazione.controller.Controller;
import postazione.controller.Factory;
import postazione.model.Post;

public class ControllerP extends Controller {
	public ControllerP(TestView view, Post postazione, InetAddress ipThisTest) {
		super(new TestServer(postazione.getNumConnections(), new Factory(), Terminals.Type.Post, ipThisTest), view, postazione);
	}
}
