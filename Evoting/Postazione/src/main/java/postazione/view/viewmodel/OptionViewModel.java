package postazione.view.viewmodel;

import javafx.beans.property.SimpleStringProperty;

public class OptionViewModel {

    private final SimpleStringProperty option;
    private boolean selected;

    public OptionViewModel(String option, boolean selected){
        this.option = new SimpleStringProperty(option);
        this.selected = selected;
    }

    public String getOption(){
        return option.getValue();
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }
}
