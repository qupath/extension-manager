package qupath.ext.extensionmanager.gui.catalog;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.ExtensionCatalogManager;
import qupath.ext.extensionmanager.core.Version;
import qupath.ext.extensionmanager.core.catalog.Extension;
import qupath.ext.extensionmanager.core.catalog.Release;
import qupath.ext.extensionmanager.core.savedentities.InstalledExtension;
import qupath.ext.extensionmanager.core.savedentities.SavedCatalog;
import qupath.ext.extensionmanager.core.tools.FileTools;
import qupath.ext.extensionmanager.gui.ProgressWindow;
import qupath.ext.extensionmanager.gui.UiUtils;
import qupath.fx.dialogs.Dialogs;

import java.io.IOException;
import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A window that provide choices to install or modify the installation of an extension.
 * It is modal to its owning window.
 */
class ExtensionModificationWindow extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionModificationWindow.class);
    private static final ResourceBundle resources = UiUtils.getResources();
    private final ExtensionCatalogManager extensionCatalogManager;
    private final SavedCatalog savedCatalog;
    private final Extension extension;
    private final InstalledExtension installedExtension;
    private final Runnable onInvalidExtensionDirectory;
    @FXML
    private Label name;
    @FXML
    private ChoiceBox<Release> release;
    @FXML
    private CheckBox optionalDependencies;
    @FXML
    private TextArea filesToDownload;
    @FXML
    private Label deleteDirectory;
    @FXML
    private Button submit;

    /**
     * Create the window.
     *
     * @param extensionCatalogManager the extension catalog manager this window should use
     * @param savedCatalog the catalog owning the extension to modify
     * @param extension the extension to modify
     * @param installedExtension information on the already installed extension if this window should allow modifying it,
     *                           or null if the extension is to be installed by this window
     * @param onInvalidExtensionDirectory a function that will be called if an operation needs to access the extension
     *                                    directory (see {@link ExtensionCatalogManager#getExtensionDirectoryPath()})
     *                                    but this directory is currently invalid. It lets the possibility to the user to
     *                                    define and create a valid directory before performing the operation (which would
     *                                    fail if the directory is invalid). This function is guaranteed to be called from
     *                                    the JavaFX Application Thread
     * @throws IOException when an error occurs while creating the window
     */
    public ExtensionModificationWindow(
            ExtensionCatalogManager extensionCatalogManager,
            SavedCatalog savedCatalog,
            Extension extension,
            InstalledExtension installedExtension,
            Runnable onInvalidExtensionDirectory
    ) throws IOException {
        this.extensionCatalogManager = extensionCatalogManager;
        this.savedCatalog = savedCatalog;
        this.extension = extension;
        this.installedExtension = installedExtension;
        this.onInvalidExtensionDirectory = onInvalidExtensionDirectory;

        UiUtils.loadFXML(this, ExtensionModificationWindow.class.getResource("extension_modification_window.fxml"));

        UiUtils.promptExtensionDirectory(extensionCatalogManager.getExtensionDirectoryPath(), onInvalidExtensionDirectory);

        initModality(Modality.WINDOW_MODAL);

        setTitle(resources.getString(installedExtension == null ?
                "Catalog.ExtensionModificationWindow.installExtension" :
                "Catalog.ExtensionModificationWindow.editExtension"
        ));

        name.setText(extension.name());

        release.getItems().addAll(extension.releases().stream()
                .filter(release -> release.versionRange().isCompatible(extensionCatalogManager.getVersion()))
                .toList()
        );
        release.setConverter(new StringConverter<>() {
            @Override
            public String toString(Release object) {
                return object == null ? null : object.name();
            }

            @Override
            public Release fromString(String string) {
                return null;
            }
        });
        release.getSelectionModel().select(installedExtension == null ?
                release.getItems().isEmpty() ? null : release.getItems().getFirst() :
                release.getItems().stream()
                        .filter(release -> release.name().equals(installedExtension.releaseName()))
                        .findAny()
                        .orElse(release.getItems().isEmpty() ? null : release.getItems().getFirst())
        );

        optionalDependencies.visibleProperty().bind(release.getSelectionModel()
                .selectedItemProperty()
                .map(release -> !release.optionalDependencyUrls().isEmpty())
        );
        optionalDependencies.managedProperty().bind(optionalDependencies.visibleProperty());
        optionalDependencies.setSelected(installedExtension != null && installedExtension.optionalDependenciesInstalled());

        filesToDownload.textProperty().bind(Bindings.createStringBinding(
                () -> {
                    try {
                        return extensionCatalogManager.getDownloadLinks(
                                        savedCatalog,
                                        extension,
                                        new InstalledExtension(
                                                release.getSelectionModel().getSelectedItem().name(),
                                                optionalDependencies.isSelected()
                                        )
                                )
                                .stream()
                                .map(URI::toString)
                                .collect(Collectors.joining("\n"));
                    } catch (NullPointerException | SecurityException | IllegalArgumentException e) {
                        logger.error("Error while retrieving download links", e);
                        return resources.getString("Catalog.ExtensionModificationWindow.cannotRetrieveLinks");
                    }
                },
                release.getSelectionModel().selectedItemProperty(),
                optionalDependencies.selectedProperty()
        ));

        try {
            Path extensionDirectory = extensionCatalogManager.getExtensionDirectory(
                    savedCatalog,
                    extension
            );

            if (FileTools.isDirectoryNotEmpty(extensionDirectory)) {
                deleteDirectory.setText(MessageFormat.format(
                        resources.getString("Catalog.ExtensionModificationWindow.replaceDirectory"),
                        extensionDirectory.toString()
                ));
            } else {
                deleteDirectory.setVisible(false);
                deleteDirectory.setManaged(false);
            }
        } catch (IOException | InvalidPathException | SecurityException | NullPointerException e) {
            logger.error("Cannot see if extension directory is not empty", e);

            deleteDirectory.setText(resources.getString("Catalog.ExtensionModificationWindow.extensionDirectoryNotRetrieved"));
        }

        submit.textProperty().bind(Bindings.createStringBinding(
                () -> resources.getString(getSubmitText()),
                release.getSelectionModel().selectedItemProperty()
        ));
    }

    @FXML
    private void onSubmitClicked(ActionEvent ignored) {
        if (release.getSelectionModel().isEmpty()) {
            return;
        }
        Release selectedRelease = release.getSelectionModel().getSelectedItem();

        UiUtils.promptExtensionDirectory(extensionCatalogManager.getExtensionDirectoryPath(), onInvalidExtensionDirectory);

        try {
            if (installedExtension == null && isJarAlreadyDownloaded(selectedRelease)) {
                var confirmation = new Dialogs.Builder()
                        .alertType(Alert.AlertType.CONFIRMATION)
                        .buttons(
                                new ButtonType(resources.getString("Catalog.ExtensionModificationWindow.continueAnyway"), ButtonBar.ButtonData.OK_DONE),
                                ButtonType.CANCEL
                        )
                        .title(resources.getString("Catalog.ExtensionModificationWindow.extensionAlreadyInstalled"))
                        .content(new Label(resources.getString("Catalog.ExtensionModificationWindow.extensionAlreadyInstalledDetails")))
                        .resizable()
                        .showAndWait()
                        .orElse(ButtonType.CANCEL).getButtonData() == ButtonBar.ButtonData.OK_DONE;

                if (!confirmation) {
                    return;
                }
            }
        } catch (NullPointerException | InvalidPathException e) {
            logger.debug(
                    "Cannot get file name from {}. Assuming {} with release {} is not already installed",
                    selectedRelease.mainUrl(),
                    extension.name(),
                    selectedRelease.name()
            );
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        ProgressWindow progressWindow;
        try {
            progressWindow = new ProgressWindow(
                    MessageFormat.format(
                            resources.getString(installedExtension == null ?
                                    "Catalog.ExtensionModificationWindow.installing" :
                                    "Catalog.ExtensionModificationWindow.updating"
                            ),
                            extension.name(),
                            selectedRelease.name()
                    ),
                    executor::shutdownNow
            );
        } catch (IOException e) {
            logger.error("Error while creating progress window", e);
            executor.shutdown();
            return;
        }
        progressWindow.initOwner(this);
        progressWindow.show();

        executor.execute(() -> {
            try {
                extensionCatalogManager.installOrUpdateExtension(
                        savedCatalog,
                        extension,
                        new InstalledExtension(
                                selectedRelease.name(),
                                optionalDependencies.isSelected()
                        ),
                        progress -> Platform.runLater(() -> progressWindow.setProgress(progress)),
                        (step, resource) -> Platform.runLater(() -> progressWindow.setStatus(MessageFormat.format(
                                resources.getString(switch (step) {
                                    case DOWNLOADING -> "Catalog.ExtensionModificationWindow.downloading";
                                    case EXTRACTING_ZIP -> "Catalog.ExtensionModificationWindow.extracting";
                                }),
                                resource
                        )))
                );

                Dialogs.showInfoNotification(
                        resources.getString("Catalog.ExtensionModificationWindow.extensionManager"),
                        MessageFormat.format(
                                resources.getString("Catalog.ExtensionModificationWindow.installed"),
                                extension.name(),
                                release.getSelectionModel().getSelectedItem().name()
                        )
                );
            } catch (Exception e) {
                logger.error("Error while installing extension", e);

                Platform.runLater(() -> new Alert(
                        Alert.AlertType.ERROR,
                        MessageFormat.format(
                                resources.getString("Catalog.ExtensionModificationWindow.notInstalled"),
                                extension.name(),
                                release.getSelectionModel().getSelectedItem().name(),
                                e.getLocalizedMessage()
                        )
                ).show());
            }

            Platform.runLater(() -> {
                progressWindow.close();
                close();
            });
        });
        executor.shutdown();
    }

    private String getSubmitText() {
        if (installedExtension == null) {
            return "Catalog.ExtensionModificationWindow.install";
        } else if (release.getSelectionModel().getSelectedItem() == null) {
            return "Catalog.ExtensionModificationWindow.update";
        } else {
            try {
                Version selectedVersion = new Version(release.getSelectionModel().getSelectedItem().name());
                Version installedVersion = new Version(installedExtension.releaseName());

                if (selectedVersion.compareTo(installedVersion) < 0) {
                    return "Catalog.ExtensionModificationWindow.downgrade";
                } else if (selectedVersion.compareTo(installedVersion) > 0) {
                    return "Catalog.ExtensionModificationWindow.update";
                } else {
                    return "Catalog.ExtensionModificationWindow.reinstall";
                }
            } catch (IllegalArgumentException e) {
                logger.debug("Cannot create version from selected item or installed release", e);
                return "Catalog.ExtensionModificationWindow.update";
            }
        }
    }

    private boolean isJarAlreadyDownloaded(Release release) {
        return Stream.concat(
                extensionCatalogManager.getManuallyInstalledJars().stream(),
                extensionCatalogManager.getCatalogManagedInstalledJars().stream()
        )
                .map(Path::getFileName)
                .filter(Objects::nonNull)
                .map(Path::toString)
                .anyMatch(path -> path.equalsIgnoreCase(FileTools.getFileNameFromURI(release.mainUrl())));
    }
}
