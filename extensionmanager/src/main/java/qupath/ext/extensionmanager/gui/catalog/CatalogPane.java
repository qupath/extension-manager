package qupath.ext.extensionmanager.gui.catalog;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.ExtensionCatalogManager;
import qupath.ext.extensionmanager.core.catalog.CatalogFetcher;
import qupath.ext.extensionmanager.core.savedentities.SavedCatalog;
import qupath.ext.extensionmanager.gui.ExtensionCatalogModel;
import qupath.ext.extensionmanager.gui.UiUtils;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * A pane that displays information and controls regarding a catalog and its extensions.
 */
public class CatalogPane extends TitledPane {

    private static final Logger logger = LoggerFactory.getLogger(CatalogPane.class);
    @FXML
    private VBox extensions;

    /**
     * Create the pane.
     *
     * @param extensionCatalogManager the extension catalog manager this pane should use
     * @param savedCatalog the catalog to display
     * @throws IOException when an error occurs while loading a FXML file
     */
    public CatalogPane(ExtensionCatalogManager extensionCatalogManager, SavedCatalog savedCatalog, ExtensionCatalogModel model) throws IOException {
        UiUtils.loadFXML(this, CatalogPane.class.getResource("catalog_pane.fxml"));

        setText(savedCatalog.name());

        CatalogFetcher.getCatalog(savedCatalog.rawUri()).handle((fetchedCatalog, error) -> {
            if (error == null) {
                Platform.runLater(() -> {
                    setExpanded(true);

                    extensions.getChildren().addAll(IntStream.range(0, fetchedCatalog.extensions().size())
                            .mapToObj(i -> {
                                try {
                                    ExtensionLine extensionLine = new ExtensionLine(
                                            extensionCatalogManager,
                                            model,
                                            savedCatalog,
                                            fetchedCatalog.extensions().get(i)
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
                    );
                });
            } else {
                logger.warn("Error when fetching catalog at {}", savedCatalog.rawUri(), error);
            }
            return null;
        });
    }
}
