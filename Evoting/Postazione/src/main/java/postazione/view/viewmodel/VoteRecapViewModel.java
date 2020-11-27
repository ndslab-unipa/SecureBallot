package postazione.view.viewmodel;

import javafx.beans.property.SimpleStringProperty;

public class VoteRecapViewModel {

    private final SimpleStringProperty ballot;
    private final SimpleStringProperty list;
    private final SimpleStringProperty preference;


    public VoteRecapViewModel(String ballot, String list, String preference) {
        this.ballot = new SimpleStringProperty(ballot);
        this.list = new SimpleStringProperty(list);
        this.preference = new SimpleStringProperty(preference);
    }

    public String getBallot(){
        return ballot.getValue();
    }

    public String getList(){
        return list.getValue();
    }

    public String getPreference(){
        return preference.getValue();
    }
}
