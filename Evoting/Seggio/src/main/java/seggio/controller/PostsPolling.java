package seggio.controller;

public class PostsPolling extends Thread {
    private final Controller controller;
    private final int delta;
    private boolean running = true;
    
    public PostsPolling(Controller controller, int delta){
        this.controller = controller;
        this.delta = delta;
    }

    @Override
    public void run(){
    	running = true;
    	
        while (running) {
            try {
                Thread.sleep(delta);
            } catch (InterruptedException e) {
                return;
            }

            controller.checkForUnreachablePosts();
        }
    }

    public void shutDown(){
        try {
        	running = false;
        }
        catch (Exception ignored) { };
    }
}
