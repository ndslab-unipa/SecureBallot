package urna.controller;

import controller.Link;
import controller.TerminalController;
import controller.ServiceFactory;

public class Factory implements ServiceFactory {

    @Override
    public Runnable createService(TerminalController controller, Link link, String name) {
        return new Service(controller, link, name);
    }
}
