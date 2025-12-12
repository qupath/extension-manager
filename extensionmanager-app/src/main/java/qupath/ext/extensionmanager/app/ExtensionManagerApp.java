package qupath.ext.extensionmanager.app;

import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.Stage;
import qupath.ext.extensionmanager.core.ExtensionCatalogManager;
import qupath.ext.extensionmanager.core.catalog.Catalog;
import qupath.ext.extensionmanager.gui.ExtensionManager;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * An application that launches a {@link ExtensionManager}. A temporary directory (with an empty extension JAR file inside)
 * is used as the extension directory. This <a href="https://github.com/qupath/qupath-catalog">catalog</a> is used.
 */
public class ExtensionManagerApp extends Application {

    private ExtensionCatalogManager extensionCatalogManager;

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
        extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(createExtensionDirectory()),
                ExtensionManagerApp.class.getClassLoader(),
                "v0.6.0",
                List.of(new Catalog(
                        "QuPath catalog",
                        "Extensions maintained by the QuPath team",
                        URI.create("https://github.com/qupath/qupath-catalog"),
                        URI.create("https://raw.githubusercontent.com/qupath/qupath-catalog/refs/heads/main/catalog.json")
                ))
        );

        new ExtensionManager(extensionCatalogManager, () -> {}).show();
    }

    @Override
    public void stop() throws Exception {
        if (extensionCatalogManager != null) {
            extensionCatalogManager.close();
        }
    }

    private Path createExtensionDirectory() throws IOException {
        Path extensionDirectoryPath = Files.createTempDirectory("").toFile().toPath();

        Files.createFile(extensionDirectoryPath.resolve("qupath-extension-example.jar"));

        return extensionDirectoryPath;
    }
}