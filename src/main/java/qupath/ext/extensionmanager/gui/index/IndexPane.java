package qupath.ext.extensionmanager.gui.index;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.IndexFetcher;
import qupath.ext.extensionmanager.core.IndexMetadata;
import qupath.ext.extensionmanager.gui.ExtensionIndexModel;
import qupath.ext.extensionmanager.gui.UiUtils;

import java.io.IOException;
import java.util.Objects;

/**
 * A pane that displays information and controls regarding an index and its extensions.
 */
public class IndexPane extends TitledPane {

    private static final Logger logger = LoggerFactory.getLogger(IndexPane.class);
    @FXML
    private VBox extensions;

    /**
     * Create the pane.
     *
     * @param indexMetadata metadata related to the index to display
     * @throws IOException when an error occurs while loading a FXML file
     */
    public IndexPane(IndexMetadata indexMetadata, ExtensionIndexModel model) throws IOException {
        UiUtils.loadFXML(this, IndexPane.class.getResource("index_pane.fxml"));

        setText(indexMetadata.name());

        IndexFetcher.getIndex(indexMetadata.url().toString()).handle((index, error) -> {
            if (error == null) {
                Platform.runLater(() -> extensions.getChildren().addAll(index.extensions().stream()
                        .map(extension -> {
                            try {
                                return new ExtensionLine(extension, model);
                            } catch (IOException e) {
                                logger.error("Error while creating extension line", e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .toList()
                ));
            } else {
                logger.warn(String.format("Error when fetching index at %s", indexMetadata.url()), error);
            }
            return null;
        });
    }
}
