package qupath.ext.extensionmanager.gui.index;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.ExtensionIndexManager;
import qupath.ext.extensionmanager.core.index.IndexFetcher;
import qupath.ext.extensionmanager.core.savedentities.SavedIndex;
import qupath.ext.extensionmanager.gui.ExtensionIndexModel;
import qupath.ext.extensionmanager.gui.UiUtils;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.IntStream;

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
     * @param extensionIndexManager the extension index manager this pane should use
     * @param savedIndex the index to display
     * @throws IOException when an error occurs while loading a FXML file
     */
    public IndexPane(ExtensionIndexManager extensionIndexManager, SavedIndex savedIndex, ExtensionIndexModel model) throws IOException {
        UiUtils.loadFXML(this, IndexPane.class.getResource("index_pane.fxml"));

        setText(savedIndex.name());

        IndexFetcher.getIndex(savedIndex.rawUri()).handle((fetchedIndex, error) -> {
            if (error == null) {
                Platform.runLater(() -> extensions.getChildren().addAll(IntStream.range(0, fetchedIndex.extensions().size())
                        .mapToObj(i -> {
                            try {
                                ExtensionLine extensionLine = new ExtensionLine(
                                        extensionIndexManager,
                                        model,
                                        savedIndex,
                                        fetchedIndex.extensions().get(i)
                                );

                                if (i % 2 == 0) {
                                    extensionLine.getStyleClass().add(UiUtils.getClassName(UiUtils.CssClass.ODD_ROW));
                                }

                                return extensionLine;
                            } catch (IOException e) {
                                logger.error("Error while creating extension line", e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .toList()
                ));
            } else {
                logger.warn("Error when fetching index at {}", savedIndex.rawUri(), error);
            }
            return null;
        });
    }
}
