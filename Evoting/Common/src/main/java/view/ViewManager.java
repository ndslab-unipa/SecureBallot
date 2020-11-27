package view;

import controller.AbstrController;
import exceptions.PEException;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public abstract class ViewManager implements ViewInterface {
    protected Stage stage;
    protected AbstrController mainController;

    public ViewManager(Stage stage){
        this.stage = stage;
    }
    
    public void setControllerAndShowStage(AbstrController controller){
        this.mainController = controller;
        stage.show();
    }

    @Override
    public void update(){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                updateFromView();
            }
        });
    }

    @Override
    public void println(String msg) {
        Dialogs.printSuccess("Operazione Riuscita", "Messaggio Generico", msg);
    }

    @Override
    public void printMessage(String message) {
        System.out.println(message);
    }

    public void printSuccess(String message, String content) {
        Dialogs.printSuccess("Operazione Riuscita", message, content);
    }

    @Override
    public void printError(String message, String content) {
        Dialogs.printError("Operazione Fallita", message, content);
    }

    @Override
    public void printError(PEException pee) {
        Dialogs.printException(pee);
    }
    
    @Override
    public void printWarning(String message, String content) {
    	Dialogs.printWarning("Attenzione", message, content);
    }
    
    @Override
    public boolean printConfirmation(String msg, String content) {
    	return Dialogs.printConfirmation("Conferma Operazione", msg, content);
    }
    
    protected ViewAbstrController loadScene(URL xmlUrl, AbstrController controller) {
        FXMLLoader loader = new FXMLLoader(xmlUrl);

        try {
            Parent sceneRoot = loader.load();
            ViewAbstrController viewController = loader.getController();
            viewController.setMainController(controller);
            setScene(sceneRoot);

            return viewController;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setScene(Parent root) {
        if (stage.getScene() == null) {
            Scene scene = new Scene(root);
            stage.setScene(scene);
        }
        else {
            stage.getScene().setRoot(root);
        }
    }
}
