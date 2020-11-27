package seggioAusiliario;

import common.TestView;
import common.TestServer;
import exceptions.PEException;
import model.DummyPost;
import model.Terminals;
import seggio.aux.controller.Controller;
import seggio.aux.controller.Factory;
import seggio.aux.model.SeggioAusiliario;

import java.net.InetAddress;

public class ControllerSubS extends Controller {
	public ControllerSubS(TestView view, SeggioAusiliario seggio, InetAddress ipThisTest) {
		super(new TestServer(seggio.getNumConnections(), new Factory(), Terminals.Type.SubStation, ipThisTest), view, seggio);
	}

	@Override
	public void updateSubStation(DummyPost[] posts) throws PEException {
		super.updateSubStation(posts);
	}
}
