package postazione.view.viewmodel;

import javafx.beans.property.SimpleStringProperty;
import model.Person;

public class CandidateViewModel {
    private final int ballot;

    private final SimpleStringProperty electoralList;
    private final SimpleStringProperty firstName;
    private final SimpleStringProperty lastName;
    private final SimpleStringProperty ID;
    private final SimpleStringProperty dateOfBirth;

    private boolean selected;

    public CandidateViewModel(int ballot, String list, Person candidate, boolean selected) {
        this.ballot = ballot;

        if(list == null)
            list = "Nessuna lista";

        this.electoralList = new SimpleStringProperty(list) ;
        this.firstName = new SimpleStringProperty(candidate.getFirstName());
        this.lastName = new SimpleStringProperty(candidate.getLastName());
        this.ID = new SimpleStringProperty(candidate.getID());

        String dateOfBirth = candidate.getBirth();
        if(dateOfBirth == null)
            dateOfBirth = "---";
        this.dateOfBirth = new SimpleStringProperty(dateOfBirth);

        this.selected = selected;
    }

    public int getBallot(){
        return ballot;
    }

    public String getElectoralList() {
        return electoralList.get();
    }

    public String getFirstName() {
        return firstName.get();
    }

    public void setFirstName(String fName) {
        firstName.set(fName);
    }

    public String getLastName() {
        return lastName.get();
    }

    public void setLastName(String fName) {
        lastName.set(fName);
    }

    public String getID() {
        return ID.get();
    }

    public String getDateOfBirth() {
        return dateOfBirth.get();
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }
}