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
import javafx.scene.layout.HBox;
import org.controlsfx.control.PopOver;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.ExtensionIndexManager;
import qupath.ext.extensionmanager.core.ExtensionInstallationInformation;
import qupath.ext.extensionmanager.core.indexmodel.Extension;
import qupath.ext.extensionmanager.gui.ExtensionIndexModel;
import qupath.ext.extensionmanager.gui.UiUtils;

import java.io.IOException;
import java.util.Optional;

/**
 * A container that displays information and controls of an extension.
 */
class ExtensionLine extends HBox {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionLine.class);
    private static final GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");
    private final Extension extension;
    private final ExtensionIndexModel model;
    @FXML
    private Label name;
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
     * @param extension the extension to display
     * @param model the model to use when accessing data
     * @throws IOException when an error occurs while creating the container
     */
    public ExtensionLine(Extension extension, ExtensionIndexModel model) throws IOException {
        this.extension = extension;
        this.model = model;

        UiUtils.loadFXML(this, ExtensionLine.class.getResource("extension_line.fxml"));

        ReadOnlyObjectProperty<Optional<ExtensionInstallationInformation>> installedExtension = model.getInstalledExtension(extension);
        if (installedExtension.get().isPresent()) {
            name.setText(String.format("%s %s", extension.name(), installedExtension.get().get().version()));
        } else {
            name.setText(extension.name());
        }
        installedExtension.addListener((p, o, n) -> Platform.runLater(() -> {
            if (n.isPresent()) {
                name.setText(String.format("%s %s", extension.name(), n.get().version()));
            } else {
                name.setText(extension.name());
            }
        }));

        add.setGraphic(fontAwesome.create(FontAwesome.Glyph.PLUS_CIRCLE));
        settings.setGraphic(fontAwesome.create(FontAwesome.Glyph.GEAR));
        delete.setGraphic(fontAwesome.create(FontAwesome.Glyph.MINUS_CIRCLE));
        info.setGraphic(fontAwesome.create(FontAwesome.Glyph.INFO_CIRCLE));

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
            new ExtensionModificationWindow(extension, model.getInstalledExtension(extension).get().orElse(null)).show();
        } catch (IOException e) {
            logger.error("Error when creating extension modification window", e);
        }
    }

    @FXML
    private void onSettingsClicked(ActionEvent ignored) {
        try {
            new ExtensionModificationWindow(extension, model.getInstalledExtension(extension).get().orElse(null)).show();
        } catch (IOException e) {
            logger.error("Error when creating extension modification window", e);
        }
    }

    @FXML
    private void onDeleteClicked(ActionEvent ignored) {
        var confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                String.format(
                        "Remove %s?",
                        extension.name()
                )
        ).showAndWait();

        if (confirmation.isPresent() && confirmation.get().equals(ButtonType.OK)) {
            ExtensionIndexManager.removeExtension(extension);

            new Alert(
                    Alert.AlertType.INFORMATION,
                    String.format(
                            "%s removed.",
                            extension.name()
                    )
            ).show();
        }
    }

    @FXML
    private void onInfoClicked(ActionEvent ignored) {
        try {
            PopOver popOver = new PopOver(new ExtensionDetails(extension));
            popOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
            popOver.show(info);
        } catch (IOException e) {
            logger.error("Error when creating extension detail pane", e);
        }
    }
}
