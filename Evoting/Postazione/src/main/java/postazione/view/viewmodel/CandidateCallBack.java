package postazione.view.viewmodel;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import postazione.view.MainScene;
import view.ViewAbstrController;

public class CandidateCallBack implements Callback<TableColumn.CellDataFeatures<CandidateViewModel, CheckBox>, ObservableValue<CheckBox>> {
    private final MainScene mainScene;

    public CandidateCallBack(MainScene scene){
        this.mainScene = scene;
    }

    @Override
    public ObservableValue<CheckBox> call(TableColumn.CellDataFeatures<CandidateViewModel, CheckBox> param) {
        CandidateViewModel candidate = param.getValue();
        CheckBox checkBox = new CheckBox();
        if(candidate != null){
            checkBox.selectedProperty().setValue(candidate.isSelected());
            checkBox.selectedProperty().addListener((ov, old_val, new_val) -> mainScene.selectCandidate(candidate));
        }
        else{
            ViewAbstrController.showElement(checkBox, false);
        }
        return new SimpleObjectProperty<>(checkBox);
    }
}
