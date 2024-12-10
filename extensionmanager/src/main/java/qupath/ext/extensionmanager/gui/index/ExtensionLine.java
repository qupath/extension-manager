package qupath.ext.extensionmanager.gui.index;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.controlsfx.glyphfont.FontAwesome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.ExtensionIndexManager;
import qupath.ext.extensionmanager.core.index.Extension;
import qupath.ext.extensionmanager.core.savedentities.InstalledExtension;
import qupath.ext.extensionmanager.core.savedentities.SavedIndex;
import qupath.ext.extensionmanager.gui.ExtensionIndexModel;
import qupath.ext.extensionmanager.gui.UiUtils;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * A container that displays information and controls of an extension.
 */
class ExtensionLine extends HBox {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionLine.class);
    private static final ResourceBundle resources = UiUtils.getResources();
    private final ExtensionIndexManager extensionIndexManager;
    private final ExtensionIndexModel model;
    private final SavedIndex savedIndex;
    private final Extension extension;
    @FXML
    private Label name;
    @FXML
    private Tooltip tooltip;
    @FXML
    private Button add;
    @FXML
    private Button settings;
    @FXML
    private Button delete;
    @FXML
    private Button info;

    /**
     * Create the container.
     *
     * @param extensionIndexManager the extension index manager this window should use
     * @param model the model to use when accessing data
     * @param savedIndex the index owning the extension to display
     * @param extension the extension to display
     * @throws IOException when an error occurs while creating the container
     */
    public ExtensionLine(
            ExtensionIndexManager extensionIndexManager,
            ExtensionIndexModel model,
            SavedIndex savedIndex,
            Extension extension
    ) throws IOException {
        this.extensionIndexManager = extensionIndexManager;
        this.model = model;
        this.savedIndex = savedIndex;
        this.extension = extension;

        UiUtils.loadFXML(this, ExtensionLine.class.getResource("extension_line.fxml"));

        ReadOnlyObjectProperty<Optional<InstalledExtension>> installedExtension = model.getInstalledExtension(
                savedIndex,
                extension
        );
        if (installedExtension.get().isPresent()) {
            name.setText(String.format("%s %s", extension.name(), installedExtension.get().get().releaseName()));
        } else {
            name.setText(extension.name());
        }
        installedExtension.addListener((p, o, n) -> Platform.runLater(() -> {
            if (n.isPresent()) {
                name.setText(String.format("%s %s", extension.name(), n.get().releaseName()));
            } else {
                name.setText(extension.name());
            }
        }));

        tooltip.setText(extension.description());

        add.setGraphic(UiUtils.getFontAwesomeIcon(FontAwesome.Glyph.PLUS_CIRCLE));
        settings.setGraphic(UiUtils.getFontAwesomeIcon(FontAwesome.Glyph.GEAR));
        delete.setGraphic(UiUtils.getFontAwesomeIcon(FontAwesome.Glyph.MINUS_CIRCLE));
        info.setGraphic(UiUtils.getFontAwesomeIcon(FontAwesome.Glyph.INFO_CIRCLE));

        add.getGraphic().getStyleClass().add("add-button");
        settings.getGraphic().getStyleClass().add("other-buttons");
        delete.getGraphic().getStyleClass().add("delete-button");
        info.getGraphic().getStyleClass().add("other-buttons");

        add.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> installedExtension.get().isEmpty(),
                installedExtension
        ));
        settings.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> installedExtension.get().isPresent(),
                installedExtension
        ));
        delete.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> installedExtension.get().isPresent(),
                installedExtension
        ));

        add.managedProperty().bind(add.visibleProperty());
        settings.managedProperty().bind(settings.visibleProperty());
        delete.managedProperty().bind(delete.visibleProperty());
    }

    @FXML
    private void onAddClicked(ActionEvent ignored) {
        try {
            new ExtensionModificationWindow(
                    extensionIndexManager,
                    savedIndex,
                    extension,
                    model.getInstalledExtension(savedIndex, extension).get().orElse(null)
            ).show();
        } catch (IOException e) {
            logger.error("Error when creating extension modification window", e);
        }
    }

    @FXML
    private void onSettingsClicked(ActionEvent ignored) {
        try {
            new ExtensionModificationWindow(
                    extensionIndexManager,
                    savedIndex,
                    extension,
                    model.getInstalledExtension(savedIndex, extension).get().orElse(null)
            ).show();
        } catch (IOException e) {
            logger.error("Error when creating extension modification window", e);
        }
    }

    @FXML
    private void onDeleteClicked(ActionEvent ignored) {
        var confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                MessageFormat.format(
                        resources.getString("Index.ExtensionLine.remove"),
                        extension.name()
                )
        ).showAndWait();

        if (confirmation.isPresent() && confirmation.get().equals(ButtonType.OK)) {
            CompletableFuture.runAsync(() -> {
                try {
                    extensionIndexManager.removeExtension(savedIndex, extension);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).handle((v, error) -> {
                if (error == null) {
                    Platform.runLater(() -> new Alert(
                            Alert.AlertType.INFORMATION,
                            MessageFormat.format(
                                    resources.getString("Index.ExtensionLine.removed"),
                                    extension.name()
                            )
                    ).show());
                } else {
                    logger.error("Error while deleting extension", error);

                    Platform.runLater(() -> new Alert(
                            Alert.AlertType.ERROR,
                            MessageFormat.format(
                                    resources.getString("Index.ExtensionLine.cannotDeleteExtension"),
                                    error.getLocalizedMessage()
                            )
                    ).show());
                }

                return null;
            });
        }
    }

    @FXML
    private void onInfoClicked(ActionEvent ignored) {
        try {
            new ExtensionDetails(extension).show();
        } catch (IOException e) {
            logger.error("Error when creating extension detail pane", e);
        }
    }
}
