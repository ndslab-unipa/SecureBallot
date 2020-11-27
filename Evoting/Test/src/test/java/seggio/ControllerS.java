package seggio;

import java.net.InetAddress;

import common.TestView;
import model.Terminals;
import common.TestServer;
import seggio.controller.Factory;
import seggio.controller.Controller;
import seggio.model.Station;

public class ControllerS extends Controller {
	public ControllerS(TestView view, Station seggio, InetAddress ipThisTest) {
		super(new TestServer(seggio.getNumConnections(), new Factory(), Terminals.Type.Station, ipThisTest), view, seggio);
	}
}
