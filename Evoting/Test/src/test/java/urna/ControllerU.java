package urna;

import java.net.InetAddress;

import common.TestServer;
import common.RPTemp;
import exceptions.PEException;
import model.Terminals;
import urna.controller.Controller;
import urna.controller.Factory;
import urna.model.Urn;
import view.ViewInterface;

public class ControllerU extends Controller {
	
	public ControllerU(ViewInterface view, Urn urna, TestDB db, InetAddress ipThisTest) {
		super(new TestServer(urna.getNumConnections(), new Factory(), Terminals.Type.Urn, ipThisTest), view, urna, db);
	}
	
	@Override
	protected boolean beforeStartOps() {
		try {
			urn.setSessionParameters(0, 0, RPTemp.getPublic(), RPTemp.getPrivate());
		} catch (PEException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
