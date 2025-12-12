package qupath.ext.extensionmanager.gui.catalog;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.ExtensionCatalogManager;
import qupath.ext.extensionmanager.core.catalog.Catalog;
import qupath.ext.extensionmanager.gui.UiUtils;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.IntStream;

/**
 * A pane that displays information and controls regarding a catalog and its extensions.
 * <p>
 * An instance of this class must be {@link #close() closed} once no longer used.
 */
public class CatalogPane extends TitledPane implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(CatalogPane.class);
    private static final ResourceBundle resources = UiUtils.getResources();
    @FXML
    private VBox extensions;

    /**
     * Create the pane.
     *
     * @param extensionCatalogManager the extension catalog manager this pane should use
     * @param catalog the catalog to display
     * @param onInvalidExtensionDirectory a function that will be called if an operation needs to access the extension
     *                                    directory (see {@link ExtensionCatalogManager#getExtensionDirectory()}) but this
     *                                    directory is currently invalid. It lets the possibility to the user to define
     *                                    and create a valid directory before performing the operation (which would fail
     *                                    if the directory is invalid). This function is guaranteed to be called from the
     *                                    JavaFX Application Thread
     * @throws IOException if an error occurs while creating the pane
     */
    public CatalogPane(
            ExtensionCatalogManager extensionCatalogManager,
            Catalog catalog,
            Runnable onInvalidExtensionDirectory
    ) throws IOException {
        UiUtils.loadFXML(this, CatalogPane.class.getResource("catalog_pane.fxml"));

        setText(catalog.getName());

        catalog.getExtensions().handle((extensions, error) -> {
            if (error != null) {
                logger.error("Error when getting extensions of {}", catalog, error);

                Platform.runLater(() -> {
                    setExpanded(true);

                    this.extensions.getChildren().add(new Label(MessageFormat.format(
                            resources.getString("Catalog.CatalogPane.errorFetchingExtensions"),
                            error.getLocalizedMessage()
                    )));
                });

                return null;
            }

            Platform.runLater(() -> {
                setExpanded(true);

                this.extensions.getChildren().addAll(IntStream.range(0, extensions.size())
                        .mapToObj(i -> {
                            try {
                                ExtensionLine extensionLine = new ExtensionLine(
                                        extensionCatalogManager,
                                        catalog,
                                        extensions.get(i),
                                        onInvalidExtensionDirectory
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
            return null;
        });
    }

    @Override
    public void close() {
        for (Node child: extensions.getChildren()) {
            if (child instanceof ExtensionLine extensionLine) {
                extensionLine.close();
            }
        }
    }
}
