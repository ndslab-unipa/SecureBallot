package common;

import exceptions.PEException;
import model.Terminals;
import view.ViewInterface;

public class TestView implements ViewInterface {

    public enum Behaviour{
        QUIET,
        ONLY_ERROR,
        ALL
    }
    
    private final Behaviour behaviour;
    private final String tag;

    public TestView(Behaviour behaviour){
        this.behaviour = behaviour;
        tag = "";
    }

    public TestView(Behaviour behaviour, Terminals.Type type){
        this.behaviour = behaviour;
        tag = "[" + type + "] >> ";
    }

    @Override
    public void update() {}

    @Override
    public void updateFromView() {}

    @Override
    public void println(String message) {
        if(behaviour == Behaviour.ALL)
            System.out.println(tag + message);
    }

    @Override
    public void printMessage(String message) {
        if(behaviour == Behaviour.ALL)
            System.out.println(tag + message);
    }

    @Override
    public void printSuccess(String message, String content) {
        if(behaviour == Behaviour.ALL)
            System.out.println(tag + message);
    }

    @Override
    public void printError(String message, String content) {
        if(behaviour != Behaviour.QUIET)
            System.out.println(tag + message + "\n" + content);
    }

    @Override
    public void printError(PEException pee) {
        if(behaviour != Behaviour.QUIET) {
            System.out.println(tag + pee.getSpecific());
            pee.printStackTrace();
        }
    }

    @Override
    public void printWarning(String message, String content) {
        if(behaviour != Behaviour.QUIET)
            System.out.println(tag + message + "\n" + content);
    }

    @Override
    public boolean printConfirmation(String message, String content) {
        if(behaviour == Behaviour.ALL){
            System.out.println(tag + message + "\n" + content);
            System.out.println("Rispondendo automaticamente si alla richiesta.");
        }
        return true;
    }
}
