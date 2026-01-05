package qupath.ext.extensionmanager.gui.catalog;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.ExtensionCatalogManager;
import qupath.ext.extensionmanager.core.Version;
import qupath.ext.extensionmanager.core.catalog.Catalog;
import qupath.ext.extensionmanager.core.catalog.Extension;
import qupath.ext.extensionmanager.core.catalog.Release;
import qupath.ext.extensionmanager.gui.UiUtils;
import qupath.fx.dialogs.Dialogs;

import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * A container that displays information and controls of an extension.
 * <p>
 * An instance of this class must be {@link #close() closed} once no longer used.
 */
class ExtensionLine extends HBox implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionLine.class);
    private static final ResourceBundle resources = UiUtils.getResources();
    private final ExtensionCatalogManager extensionCatalogManager;
    private final Catalog catalog;
    private final Extension extension;
    private final Runnable onInvalidExtensionDirectory;
    private final ExtensionModel extensionModel;
    @FXML
    private Label name;
    @FXML
    private Tooltip descriptionTooltip;
    @FXML
    private Hyperlink updateAvailable;
    @FXML
    private Tooltip updateAvailableTooltip;
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
     * @param catalog the catalog owning the extension to display
     * @param extension the extension to display
     * @param onInvalidExtensionDirectory a function that will be called if an operation needs to access the extension
     *                                    directory (see {@link ExtensionCatalogManager#getExtensionsDirectory()}) but this
     *                                    directory is currently invalid. It lets the possibility to the user to define
     *                                    and create a valid directory before performing the operation (which would fail
     *                                    if the directory is invalid). This function is guaranteed to be called from the
     *                                    JavaFX Application Thread
     * @throws IOException when an error occurs while creating the container
     */
    public ExtensionLine(
            ExtensionCatalogManager extensionCatalogManager,
            Catalog catalog,
            Extension extension,
            Runnable onInvalidExtensionDirectory
    ) throws IOException {
        this.extensionCatalogManager = extensionCatalogManager;
        this.catalog = catalog;
        this.extension = extension;
        this.onInvalidExtensionDirectory = onInvalidExtensionDirectory;
        this.extensionModel = new ExtensionModel(extension);

        UiUtils.loadFXML(this, ExtensionLine.class.getResource("extension_line.fxml"));

        ObservableValue<Optional<Release>> installedRelease = extensionModel.getInstalledRelease();
        if (installedRelease.getValue().isPresent()) {
            name.setText(String.format("%s %s", extension.getName(), installedRelease.getValue().get().getVersion().toString()));
        } else {
            name.setText(extension.getName());
        }
        installedRelease.addListener((p, o, n) -> {
            if (n.isPresent()) {
                name.setText(String.format("%s %s", extension.getName(), n.get().getVersion().toString()));
            } else {
                name.setText(extension.getName());
            }
        });

        Glyph star = UiUtils.getFontAwesomeIcon(FontAwesome.Glyph.STAR);
        star.getStyleClass().add(extension.isStarred() ?
                UiUtils.getClassName(UiUtils.CssClass.STAR) :
                UiUtils.getClassName(UiUtils.CssClass.INVISIBLE)
        );
        name.setContentDisplay(ContentDisplay.LEFT);
        name.setGraphic(star);

        StringBuilder descriptionText = new StringBuilder(extension.getDescription());
        if (extension.isStarred()) {
            descriptionText.append("\n");
            descriptionText.append(resources.getString("Catalog.ExtensionLine.starredExtension"));
        }
        if (noAvailableRelease()) {
            descriptionText.append("\n");
            descriptionText.append(resources.getString("Catalog.ExtensionLine.extensionNotCompatible"));
        }
        descriptionTooltip.setText(descriptionText.toString());
        Tooltip.install(separator, descriptionTooltip);

        updateAvailable.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> {
                    if (installedRelease.getValue().isEmpty()) {
                        return false;
                    }
                    Version installedVersion = installedRelease.getValue().get().getVersion();

                    Optional<String> availableVersion = extension.getReleases().stream()
                            .filter(release -> release.isCompatible(extensionCatalogManager.getVersion()))
                            .filter(release -> release.getVersion().compareTo(installedVersion) > 0)
                            .map(release -> release.getVersion().toString())
                            .findAny();

                    availableVersion.ifPresent(version -> updateAvailableTooltip.setText(MessageFormat.format(
                            resources.getString("Catalog.ExtensionLine.updateAvailableDetails"),
                            version
                    )));

                    return availableVersion.isPresent();
                },
                installedRelease
        ));
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
                () -> installedRelease.getValue().isEmpty(),
                installedRelease
        ));
        settings.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> installedRelease.getValue().isPresent(),
                installedRelease
        ));
        delete.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> installedRelease.getValue().isPresent(),
                installedRelease
        ));

        add.managedProperty().bind(add.visibleProperty());
        settings.managedProperty().bind(settings.visibleProperty());
        delete.managedProperty().bind(delete.visibleProperty());

        add.setDisable(noAvailableRelease());

        infoTooltip.setText(String.format("%s\n%s", extension.getDescription(), extension.getHomepage()));
    }

    @Override
    public void close() {
        extensionModel.close();
    }

    @FXML
    private void onUpdateAvailableClicked(ActionEvent ignored) {
        onSettingsClicked(ignored);
    }

    @FXML
    private void onAddClicked(ActionEvent ignored) {
        try {
            ExtensionModificationWindow extensionModificationWindow = new ExtensionModificationWindow(
                    extensionCatalogManager,
                    catalog,
                    extension,
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
                    catalog,
                    extension,
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
            directoryToDelete = extensionCatalogManager.getExtensionDirectory(catalog.getName(), extension.getName());
        } catch (Exception e) {
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

        boolean confirmation = Dialogs.showConfirmDialog(
                resources.getString("Catalog.ExtensionLine.removeExtension"),
                MessageFormat.format(
                        resources.getString("Catalog.ExtensionLine.remove"),
                        extension.getName(),
                        directoryToDelete
                )
        );
        if (!confirmation) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                extensionCatalogManager.removeExtension(catalog, extension);
            } catch (IOException | ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).handle((v, error) -> {
            if (error == null) {
                Dialogs.showInfoNotification(
                        resources.getString("Catalog.ExtensionLine.extensionManager"),
                        MessageFormat.format(
                                resources.getString("Catalog.ExtensionLine.removed"),
                                extension.getName()
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
            ExtensionDetails extensionDetails = new ExtensionDetails(extension, noAvailableRelease());
            extensionDetails.initOwner(getScene().getWindow());
            extensionDetails.show();
        } catch (IOException e) {
            logger.error("Error when creating extension detail pane", e);
        }
    }

    private boolean noAvailableRelease() {
        return extension.getReleases().stream().noneMatch(release -> release.isCompatible(extensionCatalogManager.getVersion()));
    }
}
