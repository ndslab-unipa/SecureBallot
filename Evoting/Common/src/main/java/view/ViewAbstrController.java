package view;

import controller.AbstrController;
import javafx.scene.Node;
import javafx.scene.control.TextFormatter;
import utils.FileUtils;

import java.util.function.UnaryOperator;

public abstract class ViewAbstrController {
    protected AbstrController controller;

    public void setMainController(AbstrController controller){
        this.controller = controller;
    }

    /**
     * Restrizione sui caratteri per i campi "data":
     * Solo cifre e "/", Max lunghezza 10 caratteri.
     * Il testo diventa rosso se il formato non viene riconosciuto come data valida
     * (giorno compreso tra 1 e 31 "/" mese tra 1 e 12 "/" anno tra 0000 e 9999)
     */
    public static UnaryOperator<TextFormatter.Change> dateFilter = change -> {
        String text = change.getText().replaceAll("[^0-9/]", "");
        String newText = change.getControlNewText();

        if(newText.length() > 10){
            int avaiableChars = Math.max(10 - newText.length(), 0);
            change.setText( text.substring(0, avaiableChars));
        }
        else{
            change.setText(text);
        }

        newText = newText.substring(0, Math.min(newText.length(), 10));

        if(FileUtils.isDate(newText))
            change.getControl().setStyle("-fx-text-inner-color: black;");
        else
            change.getControl().setStyle("-fx-text-inner-color: red;");
        return change;
    };

    /**
     * Restrizione sui caratteri per i campi "orario":
     * Solo cifre e ":", Max lunghezza 8 caratteri.
     * Il testo diventa rosso se il formato non viene riconosciuto come orario valido
     */
    public static UnaryOperator<TextFormatter.Change> timeFilter = change -> {
        String text = change.getText().replaceAll("[^0-9:]", "");
        String newText = change.getControlNewText();

        if(newText.length() > 8){
            int avaiableChars = Math.max(8 - newText.length(), 0);
            change.setText( text.substring(0, avaiableChars));
        }
        else{
            change.setText(text);
        }

        newText = newText.substring(0, Math.min(newText.length(), 8));

        if(FileUtils.isTime(newText))
            change.getControl().setStyle("-fx-text-inner-color: black;");
        else
            change.getControl().setStyle("-fx-text-inner-color: red;");
        return change;
    };

    /**
     * Restrizione sui caratteri per i campi "nome" e "cognome".
     * Solo lettere, spazi e "-", prima lettera forzatamente maiuscola.
     */
    public static UnaryOperator<TextFormatter.Change> noDigitsFilter = change -> {
        String text = change.getText().replaceAll("[^a-zA-Z\\-]","");
        change.setText(change.getControlNewText().length() == 1 ? text.toUpperCase() : text);
        return change;
    };

    /**
     * Restrizione sui campi numerici.
     * Solo numeri.
     */
    public static UnaryOperator<TextFormatter.Change> onlyDigitsFilter = change -> {
        String newChar = change.getText().replaceAll("[^0-9]", "");
        change.setText(newChar);
        return change;
    };
    
    /**
     * Nasconde e disabilita la gestione o mostra e abilita la gestione, dell'elemento della GUI passato per argomento.
     * @param n         L'elemento da mostrare/nascondere.
     * @param flag      True per mostrare, false per nascondere.
     */
    public static void showElement(Node n, boolean flag) {
    	n.setVisible(flag);
    	n.setManaged(flag);
    }
}
