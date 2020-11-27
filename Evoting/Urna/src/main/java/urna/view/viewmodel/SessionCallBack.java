package urna.view.viewmodel;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import urna.view.SessionSelection;

public class SessionCallBack implements Callback<TableColumn.CellDataFeatures<SessionViewModel, CheckBox>, ObservableValue<CheckBox>> {
    private SessionSelection scene;

    public SessionCallBack(SessionSelection scene){
        this.scene = scene;
    }

    @Override
    public ObservableValue<CheckBox> call(TableColumn.CellDataFeatures<SessionViewModel, CheckBox> param) {
    	SessionViewModel s = param.getValue();
        CheckBox checkBox = new CheckBox();
        checkBox.selectedProperty().setValue(s.isSelected());
        checkBox.selectedProperty().addListener((ov, old_val, new_val) -> {
                scene.chooseSession(s);
        });

        return new SimpleObjectProperty<>(checkBox);
    }
}
