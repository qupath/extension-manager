package qupath.ext.extensionmanager.app;

import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.Stage;
import qupath.ext.extensionmanager.core.ExtensionIndexManager;
import qupath.ext.extensionmanager.core.savedentities.Registry;
import qupath.ext.extensionmanager.core.savedentities.SavedIndex;
import qupath.ext.extensionmanager.gui.ExtensionManager;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * An application that launches a {@link ExtensionManager}. A temporary directory (with
 * an empty extension JAR file inside) is used as the extension directory.
 * This <a href="https://github.com/Rylern/test-index">index</a> is used.
 */
public class ExtensionManagerApp extends Application {

    private ExtensionIndexManager extensionIndexManager;

    /**
     * Start the extension manager application.
     *
     * @param args the command line arguments passed to the application
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        extensionIndexManager = new ExtensionIndexManager(
                new SimpleObjectProperty<>(createExtensionDirectory()),
                ExtensionManagerApp.class.getClassLoader(),
                "v0.6.0-rc3",
                new Registry(List.of(new SavedIndex(
                        "QuPath index",
                        "Extensions maintained by the QuPath team",
                        URI.create("https://github.com/Rylern/test-index"),
                        URI.create("https://raw.githubusercontent.com/Rylern/test-index/refs/heads/main/index.json")
                )))
        );

        new ExtensionManager(extensionIndexManager, () -> {}).show();
    }

    @Override
    public void stop() throws Exception {
        if (extensionIndexManager != null) {
            extensionIndexManager.close();
        }
    }

    private Path createExtensionDirectory() throws IOException {
        Path extensionDirectoryPath = Files.createTempDirectory("").toFile().toPath();

        Files.createFile(extensionDirectoryPath.resolve("qupath-extension-example.jar"));

        return extensionDirectoryPath;
    }
}