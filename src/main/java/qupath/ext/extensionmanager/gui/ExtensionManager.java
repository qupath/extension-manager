package qupath.ext.extensionmanager.gui;

import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.IndexMetadata;
import qupath.ext.extensionmanager.core.QuPath;
import qupath.ext.extensionmanager.gui.index.IndexPane;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * A window that displays information and controls regarding indexes and their extensions
 */
public class ExtensionManager extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionManager.class);
    private final ExtensionIndexModel model = new ExtensionIndexModel();
    private IndexManager indexManager;
    @FXML
    private VBox indexes;

    /**
     * Create the window.
     *
     * @throws IOException when an error occurs while creating the container
     */
    public ExtensionManager() throws IOException {
        UiUtils.loadFXML(this, ExtensionManager.class.getResource("extension_manager.fxml"));

        setIndexes();
        model.getIndexes().addListener((ListChangeListener<? super IndexMetadata>) change ->
                setIndexes()
        );
    }

    @FXML
    private void onOpenExtensionDirectory(ActionEvent ignored) {
        String directory = QuPath.getExtensionDirectory().get();

        try {
            Desktop.getDesktop().open(new File(directory));
        } catch (Exception e) {
            logger.error(String.format("Error while opening QuPath extension directory %s", directory), e);

            new Alert(
                    Alert.AlertType.ERROR,
                    String.format("Cannot open '%s':\n%s", directory, e.getLocalizedMessage())
            ).show();
        }
    }

    @FXML
    private void onManageIndexesClicked(ActionEvent ignored) {
        if (indexManager == null) {
            try {
                indexManager = new IndexManager(model);
                indexManager.show();
            } catch (IOException e) {
                logger.error("Error while creating index manager window", e);
            }
        }

        if (indexManager != null) {
            indexManager.show();
            indexManager.requestFocus();
        }
    }

    private void setIndexes() {
        indexes.getChildren().setAll(model.getIndexes().stream()
                .map(indexMetadata -> {
                    try {
                        return new IndexPane(indexMetadata, model);
                    } catch (IOException e) {
                        logger.error("Error while creating index pane", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList()
        );
    }
}
