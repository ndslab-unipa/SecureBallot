package postazione.view.viewmodel;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import postazione.view.MainScene;
import view.ViewAbstrController;

public class OptionCallBack implements Callback<TableColumn.CellDataFeatures<OptionViewModel, CheckBox>, ObservableValue<CheckBox>> {
    private final MainScene mainScene;

    public OptionCallBack(MainScene scene){
        this.mainScene = scene;
    }

    @Override
    public ObservableValue<CheckBox> call(TableColumn.CellDataFeatures<OptionViewModel, CheckBox> param) {
    	OptionViewModel option = param.getValue();
        CheckBox checkBox = new CheckBox();
        if(option != null) {
            checkBox.selectedProperty().setValue(option.isSelected());
            checkBox.selectedProperty().addListener((ov, old_val, new_val) -> mainScene.selectOption(option));
        }
        else{
            ViewAbstrController.showElement(checkBox, false);
        }
        return new SimpleObjectProperty<>(checkBox);
    }
}
