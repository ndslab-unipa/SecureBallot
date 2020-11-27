package poll.view.viewmodel;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import poll.view.ProcedureSelection;

/**
 * Classe CallBack utilizzata per gestire l'interazione dell'utente con CheckBox presenti all'interno di Tabelle JavaFX.
 */
public class ProcedureCallBack implements Callback<TableColumn.CellDataFeatures<ProcedureViewModel, CheckBox>, ObservableValue<CheckBox>> {
    private ProcedureSelection scene;

    /**
     * Costruttore con parametri che inizializza la scena in cui è utilizzata la CallBack.
     * @param scene Riferimento alla scena in cui utilizzare la CallBack, di tipo {@link poll.view.ProcedureSelection ProcedureSelection}.
     */
    public ProcedureCallBack(ProcedureSelection scene){
        this.scene = scene;
    }

    /**
     * Lega il valore della checkbox alla proprietà "is selected" di {@link ProcedureViewModel}. Inoltre, aggiunge un listener alla checkbox,
     * la funzione {@link ProcedureSelection#chooseProcedure(ProcedureViewModel)}.
     * @param param Cella di una tabella JavaFX all'interno della quale inserire una checkbox
     * @return Oggetto SimbleObjectProperty costruito sulla checkbox
     */
    @Override
    public ObservableValue<CheckBox> call(TableColumn.CellDataFeatures<ProcedureViewModel, CheckBox> param) {
    	ProcedureViewModel p = param.getValue();
        CheckBox checkBox = new CheckBox();
        checkBox.selectedProperty().setValue(p.isSelected());
        checkBox.selectedProperty().addListener((ov, old_val, new_val) -> {
                scene.chooseProcedure(p);
        });

        return new SimpleObjectProperty<>(checkBox);
    }
}
