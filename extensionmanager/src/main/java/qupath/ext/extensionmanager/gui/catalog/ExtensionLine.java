package qupath.ext.extensionmanager.gui.catalog;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import org.controlsfx.glyphfont.FontAwesome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.ExtensionCatalogManager;
import qupath.ext.extensionmanager.core.catalog.Extension;
import qupath.ext.extensionmanager.core.savedentities.InstalledExtension;
import qupath.ext.extensionmanager.core.savedentities.SavedCatalog;
import qupath.ext.extensionmanager.gui.ExtensionCatalogModel;
import qupath.ext.extensionmanager.gui.UiUtils;
import qupath.fx.dialogs.Dialogs;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
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
    private final ExtensionCatalogManager extensionCatalogManager;
    private final ExtensionCatalogModel model;
    private final SavedCatalog savedCatalog;
    private final Extension extension;
    private final Runnable onInvalidExtensionDirectory;
    @FXML
    private Label name;
    @FXML
    private Tooltip descriptionTooltip;
    @FXML
    private Label updateAvailable;
    @FXML
    private Region separator;
    @FXML
    private Button add;
    @FXML
    private Button settings;
    @FXML
    private Button delete;
    @FXML
    private Button info;
    @FXML
    private Tooltip infoTooltip;

    /**
     * Create the container.
     *
     * @param extensionCatalogManager the extension catalog manager this window should use
     * @param model the model to use when accessing data
     * @param savedCatalog the catalog owning the extension to display
     * @param extension the extension to display
     * @param onInvalidExtensionDirectory a function that will be called if an operation needs to access the extension
     *                                    directory (see {@link ExtensionCatalogManager#getExtensionDirectoryPath()})
     *                                    but this directory is currently invalid. It lets the possibility to the user to
     *                                    define and create a valid directory before performing the operation (which would
     *                                    fail if the directory is invalid). This function is guaranteed to be called from
     *                                    the JavaFX Application Thread
     * @throws IOException when an error occurs while creating the container
     */
    public ExtensionLine(
            ExtensionCatalogManager extensionCatalogManager,
            ExtensionCatalogModel model,
            SavedCatalog savedCatalog,
            Extension extension,
            Runnable onInvalidExtensionDirectory
    ) throws IOException {
        this.extensionCatalogManager = extensionCatalogManager;
        this.model = model;
        this.savedCatalog = savedCatalog;
        this.extension = extension;
        this.onInvalidExtensionDirectory = onInvalidExtensionDirectory;

        UiUtils.loadFXML(this, ExtensionLine.class.getResource("extension_line.fxml"));

        ReadOnlyObjectProperty<Optional<InstalledExtension>> installedExtension = model.getInstalledExtension(
                savedCatalog,
                extension
        );
        if (installedExtension.get().isPresent()) {
            name.setText(String.format("%s %s", extension.name(), installedExtension.get().get().releaseName()));
        } else {
            name.setText(extension.name());
        }
        installedExtension.addListener((p, o, n) -> {
            if (n.isPresent()) {
                name.setText(String.format("%s %s", extension.name(), n.get().releaseName()));
            } else {
                name.setText(extension.name());
            }
        });

        descriptionTooltip.setText(extension.description());
        Tooltip.install(separator, name.getTooltip());

        updateAvailable.setVisible(true); //TODO: change
        //TODO: add tooltip
        updateAvailable.managedProperty().bind(updateAvailable.visibleProperty());

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

        add.setDisable(extension.releases().stream()
                .noneMatch(release -> release.versionRange().isCompatible(extensionCatalogManager.getVersion()))
        );

        infoTooltip.setText(String.format("%s\n%s", extension.description(), extension.homepage()));
    }

    @FXML
    private void onAddClicked(ActionEvent ignored) {
        try {
            ExtensionModificationWindow extensionModificationWindow = new ExtensionModificationWindow(
                    extensionCatalogManager,
                    savedCatalog,
                    extension,
                    model.getInstalledExtension(savedCatalog, extension).get().orElse(null),
                    onInvalidExtensionDirectory
            );
            extensionModificationWindow.initOwner(getScene().getWindow());
            extensionModificationWindow.show();
        } catch (IOException e) {
            logger.error("Error when creating extension modification window", e);
        }
    }

    @FXML
    private void onSettingsClicked(ActionEvent ignored) {
        try {
            ExtensionModificationWindow extensionModificationWindow = new ExtensionModificationWindow(
                    extensionCatalogManager,
                    savedCatalog,
                    extension,
                    model.getInstalledExtension(savedCatalog, extension).get().orElse(null),
                    onInvalidExtensionDirectory
            );
            extensionModificationWindow.initOwner(getScene().getWindow());
            extensionModificationWindow.show();
        } catch (IOException e) {
            logger.error("Error when creating extension modification window", e);
        }
    }

    @FXML
    private void onDeleteClicked(ActionEvent ignored) {
        Path directoryToDelete;
        try {
            directoryToDelete = extensionCatalogManager.getExtensionDirectory(savedCatalog, extension);
        } catch (IOException | InvalidPathException | SecurityException | NullPointerException e) {
            logger.error("Cannot retrieve directory containing the files of the extension to delete", e);

            Dialogs.showErrorMessage(
                    resources.getString("Catalog.ExtensionLine.error"),
                    MessageFormat.format(
                            resources.getString("Catalog.ExtensionLine.cannotDeleteExtension"),
                            e.getLocalizedMessage()
                    )
            );
            return;
        }

        var confirmation = Dialogs.showConfirmDialog(
                resources.getString("Catalog.ExtensionLine.removeExtension"),
                MessageFormat.format(
                        resources.getString("Catalog.ExtensionLine.remove"),
                        extension.name(),
                        directoryToDelete
                )
        );
        if (!confirmation) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                extensionCatalogManager.removeExtension(savedCatalog, extension);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).handle((v, error) -> {
            if (error == null) {
                Dialogs.showInfoNotification(
                        resources.getString("Catalog.ExtensionLine.extensionManager"),
                        MessageFormat.format(
                                resources.getString("Catalog.ExtensionLine.removed"),
                                extension.name()
                        )
                );
            } else {
                logger.error("Error while deleting extension", error);

                Dialogs.showErrorMessage(
                        resources.getString("Catalog.ExtensionLine.error"),
                        MessageFormat.format(
                                resources.getString("Catalog.ExtensionLine.cannotDeleteExtension"),
                                error.getLocalizedMessage()
                        )
                );
            }
            return null;
        });
    }

    @FXML
    private void onInfoClicked(ActionEvent ignored) {
        try {
            ExtensionDetails extensionDetails = new ExtensionDetails(extension);
            extensionDetails.initOwner(getScene().getWindow());
            extensionDetails.show();
        } catch (IOException e) {
            logger.error("Error when creating extension detail pane", e);
        }
    }
}
