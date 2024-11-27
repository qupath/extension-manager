package qupath.ext.extensionmanager.app;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.Stage;
import qupath.ext.extensionmanager.core.ExtensionIndexManager;
import qupath.ext.extensionmanager.core.savedentities.Registry;
import qupath.ext.extensionmanager.core.savedentities.SavedIndex;
import qupath.ext.extensionmanager.gui.ExtensionManager;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        new ExtensionManager(new ExtensionIndexManager(
                new SimpleStringProperty(Files.createTempDirectory("").toFile().getAbsolutePath()),
                "v0.6.0-rc3",
                new Registry(List.of(new SavedIndex(
                        "QuPath index",
                        "Extensions maintained by the QuPath team",
                        URI.create("https://raw.githubusercontent.com/Rylern/test-index/refs/heads/main/index.json")
                )))
        )).show();
    }

    public static void main(String[] args) {
        launch();
    }
}