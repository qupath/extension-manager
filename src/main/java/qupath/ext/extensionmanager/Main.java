package qupath.ext.extensionmanager;

import javafx.application.Application;
import javafx.stage.Stage;
import qupath.ext.extensionmanager.gui.ExtensionManager;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        new ExtensionManager().show();
    }

    public static void main(String[] args) {
        launch();
    }
}