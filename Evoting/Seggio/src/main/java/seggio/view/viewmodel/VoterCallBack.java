package seggio.view.viewmodel;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import seggio.view.MainScene;
import view.viewmodel.VoterViewModel;

public class VoterCallBack implements Callback<TableColumn.CellDataFeatures<VoterViewModel, CheckBox>, ObservableValue<CheckBox>> {
    private MainScene mainScene;

    public VoterCallBack(MainScene mainScene){
        this.mainScene = mainScene;
    }

    @Override
    public ObservableValue<CheckBox> call(TableColumn.CellDataFeatures<VoterViewModel, CheckBox> param) {
        VoterViewModel voter = param.getValue();
        CheckBox checkBox = new CheckBox();
        checkBox.selectedProperty().setValue(voter.isSelected());
        checkBox.selectedProperty().addListener((ov, old_val, new_val) -> {
                mainScene.selectVoter(voter);
        });

        return new SimpleObjectProperty<>(checkBox);
    }
}
