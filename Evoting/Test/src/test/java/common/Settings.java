package common;

public class Settings {
    public final static TestView.Behaviour viewBehaviour = TestView.Behaviour.QUIET;
    public final static boolean printTestName = viewBehaviour != TestView.Behaviour.QUIET;
    public final static boolean testDB = true;
}
