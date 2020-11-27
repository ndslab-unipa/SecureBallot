package postazione.view.viewmodel;

import javafx.beans.property.SimpleStringProperty;

public class BallotRecapViewModel {

    private final SimpleStringProperty ballot;
    private final int numPrefs;
    private final int maxPrefs;


    public BallotRecapViewModel(String ballot, int numPrefs, int maxPrefs) {
        this.ballot = new SimpleStringProperty(ballot);
        this.numPrefs = numPrefs;
        this.maxPrefs = maxPrefs;
    }

    public String getBallot(){
        return ballot.getValue();
    }

    public String getPrefString() {
        if(numPrefs == 0)
            return "Scheda Bianca";

        return numPrefs + "/" + maxPrefs;
    }
}
