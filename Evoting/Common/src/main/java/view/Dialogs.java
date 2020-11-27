package view;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import exceptions.PEException;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

@SuppressWarnings("rawtypes")
public class Dialogs implements Callable {
	private final AlertType type;
	private final String title;
	private final String header;
	private final String content;

	private Dialogs(Alert.AlertType type, String title, String header, String content) {
		this.type = type;
		this.title = title;
		this.header = header;
		this.content = content;
	}
	
	@Override
	public Object call() throws Exception {
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.showAndWait();
		
		Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
		stage.setAlwaysOnTop(true);
		stage.toFront();
		
		return null;
	}
	
	public static void printError(String title, String msg, String content) {
    	@SuppressWarnings("unchecked")
		FutureTask<String> futureTask = new FutureTask<>(
				new Dialogs(AlertType.ERROR, title, msg, content)
		);
		Platform.runLater(futureTask);
    }
	
	public static void printException(PEException pee) {
		printError("Eccezione "+pee.getCode()+" Lanciata", pee.getGeneric(), pee.getSpecific());
        pee.printStackTrace();
	}
	
	public static void printSuccess(String title, String msg, String content) {
		@SuppressWarnings("unchecked")
		FutureTask<String> futureTask = new FutureTask<>(
				new Dialogs(AlertType.INFORMATION, title, msg, content)
		);
		Platform.runLater(futureTask);
	}
	
	public static void printWarning(String title, String msg, String content) {
		@SuppressWarnings("unchecked")
		FutureTask<String> futureTask = new FutureTask<>(
				new Dialogs(AlertType.WARNING, title, msg, content)
		);
		Platform.runLater(futureTask);
	}
	
	public static boolean printConfirmation(String title, String msg, String content) {
		if(Platform.isFxApplicationThread()) {
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle(title);
			alert.setHeaderText(msg);
			alert.setContentText(content);
			Optional<ButtonType> button = alert.showAndWait();
			return button.get() == ButtonType.OK;
		}
		
		return false;
	}
	
	public static String printTextConfirmation(String title, String msg, String content) {
		if(Platform.isFxApplicationThread()) {
			TextInputDialog dialog = new TextInputDialog();
			dialog.setTitle(title);
			dialog.setHeaderText(msg);
			dialog.setContentText(content);
	
			Optional<String> result = dialog.showAndWait();
			return result.isPresent() ? result.get() : "<<abort>>";
		}
		
		return "<<error>>";
	}
}
