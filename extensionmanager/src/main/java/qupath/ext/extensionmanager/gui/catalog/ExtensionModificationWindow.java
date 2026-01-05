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
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.ExtensionCatalogManager;
import qupath.ext.extensionmanager.core.Version;
import qupath.ext.extensionmanager.core.catalog.Catalog;
import qupath.ext.extensionmanager.core.catalog.Extension;
import qupath.ext.extensionmanager.core.catalog.Release;
import qupath.ext.extensionmanager.core.tools.FileTools;
import qupath.ext.extensionmanager.gui.ProgressWindow;
import qupath.ext.extensionmanager.gui.UiUtils;
import qupath.fx.dialogs.Dialogs;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A window that provide choices to install or modify the installation of an extension.
 * <p>
 * It is modal to its owning window.
 */
class ExtensionModificationWindow extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionModificationWindow.class);
    private static final ResourceBundle resources = UiUtils.getResources();
    private final ExtensionCatalogManager extensionCatalogManager;
    private final Catalog catalog;
    private final Extension extension;
    private final Runnable onInvalidExtensionDirectory;
    @FXML
    private Label name;
    @FXML
    private Label currentVersion;
    @FXML
    private ChoiceBox<Release> release;
    @FXML
    private CheckBox optionalDependencies;
    @FXML
    private TextArea filesToDownload;
    @FXML
    private VBox replaceDirectory;
    @FXML
    private Label replaceDirectoryLabel;
    @FXML
    private Hyperlink replaceDirectoryLink;
    @FXML
    private Button submit;

    /**
     * Create the window.
     *
     * @param extensionCatalogManager the extension catalog manager this window should use
     * @param catalog the catalog owning the extension to modify
     * @param extension the extension to modify
     * @param onInvalidExtensionDirectory a function that will be called if an operation needs to access the extension
     *                                    directory (see {@link ExtensionCatalogManager#getExtensionsDirectory()}) but this
     *                                    directory is currently invalid. It lets the possibility to the user to define
     *                                    and create a valid directory before performing the operation (which would fail
     *                                    if the directory is invalid). This function is guaranteed to be called from the
     *                                    JavaFX Application Thread
     * @throws IOException when an error occurs while creating the window
     */
    public ExtensionModificationWindow(
            ExtensionCatalogManager extensionCatalogManager,
            Catalog catalog,
            Extension extension,
            Runnable onInvalidExtensionDirectory
    ) throws IOException {
        this.extensionCatalogManager = extensionCatalogManager;
        this.catalog = catalog;
        this.extension = extension;
        this.onInvalidExtensionDirectory = onInvalidExtensionDirectory;

        UiUtils.loadFXML(this, ExtensionModificationWindow.class.getResource("extension_modification_window.fxml"));

        UiUtils.promptExtensionDirectory(extensionCatalogManager.getExtensionsDirectory(), onInvalidExtensionDirectory);

        initModality(Modality.WINDOW_MODAL);

        Optional<Release> installedRelease = extension.getInstalledRelease().getValue();

        setTitle(resources.getString(installedRelease.isPresent() ?
                "Catalog.ExtensionModificationWindow.editExtension" :
                "Catalog.ExtensionModificationWindow.installExtension"
        ));

        name.setText(extension.getName());

        if (installedRelease.isPresent()) {
            currentVersion.setText(MessageFormat.format(
                    resources.getString("Catalog.ExtensionModificationWindow.currentVersion"),
                    installedRelease.get().getVersion().toString()
            ));
        } else {
            currentVersion.setVisible(false);
            currentVersion.setManaged(false);
        }

        release.getItems().addAll(extension.getReleases().stream()
                .filter(release -> release.isCompatible(extensionCatalogManager.getVersion()))
                .toList()
        );
        release.setConverter(new StringConverter<>() {
            @Override
            public String toString(Release object) {
                return object == null ? null : object.getVersion().toString();
            }

            @Override
            public Release fromString(String string) {
                return null;
            }
        });
        release.getSelectionModel().select(release.getItems().isEmpty() ? null : release.getItems().getFirst());

        optionalDependencies.visibleProperty().bind(release.getSelectionModel()
                .selectedItemProperty()
                .map(release -> !release.getOptionalDependencyUrls().isEmpty())
        );
        optionalDependencies.managedProperty().bind(optionalDependencies.visibleProperty());
        optionalDependencies.setSelected(extension.areOptionalDependenciesInstalled().get());

        filesToDownload.textProperty().bind(Bindings.createStringBinding(
                () -> {
                    try {
                        return extensionCatalogManager.getDownloadLinks(
                                        release.getSelectionModel().getSelectedItem(),
                                        optionalDependencies.isSelected()
                                )
                                .stream()
                                .map(URI::toString)
                                .collect(Collectors.joining("\n"));
                    } catch (Exception e) {
                        logger.error("Error while retrieving download links", e);
                        return resources.getString("Catalog.ExtensionModificationWindow.cannotRetrieveLinks");
                    }
                },
                release.getSelectionModel().selectedItemProperty(),
                optionalDependencies.selectedProperty()
        ));

        try {
            Path extensionDirectory = extensionCatalogManager.getExtensionDirectory(catalog.getName(), extension.getName());

            if (FileTools.isDirectoryNotEmpty(extensionDirectory)) {
                replaceDirectoryLabel.setText(resources.getString("Catalog.ExtensionModificationWindow.replaceDirectory"));
                replaceDirectoryLink.setText(extensionDirectory.toString());
            } else {
                replaceDirectory.setVisible(false);
                replaceDirectory.setManaged(false);
            }
        } catch (Exception e) {
            logger.error("Cannot see if extension directory is not empty", e);

            replaceDirectoryLabel.setText(resources.getString("Catalog.ExtensionModificationWindow.extensionDirectoryNotRetrieved"));
            replaceDirectoryLink.setVisible(false);
            replaceDirectoryLink.setManaged(false);
        }

        submit.textProperty().bind(Bindings.createStringBinding(
                () -> resources.getString(getSubmitText()),
                release.getSelectionModel().selectedItemProperty()
        ));
    }

    @FXML
    private void onReplacedDirectoryClicked(ActionEvent ignored) {
        UiUtils.openFolderInFileExplorer(Paths.get(replaceDirectoryLink.getText()));
    }

    @FXML
    private void onSubmitClicked(ActionEvent ignored) {
        if (release.getSelectionModel().isEmpty()) {
            return;
        }
        Release selectedRelease = release.getSelectionModel().getSelectedItem();

        UiUtils.promptExtensionDirectory(extensionCatalogManager.getExtensionsDirectory(), onInvalidExtensionDirectory);

        try {
            if (extension.getInstalledRelease().getValue().isEmpty() && isJarAlreadyDownloaded(selectedRelease)) {
                boolean confirmation = new Dialogs.Builder()
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
        } catch (Exception e) {
            logger.debug(
                    "Cannot get file name from {}. Assuming {} with release {} is not already installed",
                    selectedRelease.getMainUrl(),
                    extension,
                    selectedRelease
            );
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        ProgressWindow progressWindow;
        try {
            progressWindow = new ProgressWindow(
                    MessageFormat.format(
                            resources.getString(extension.getInstalledRelease().getValue().isPresent() ?
                                    "Catalog.ExtensionModificationWindow.updating" :
                                    "Catalog.ExtensionModificationWindow.installing"
                            ),
                            extension.getName(),
                            selectedRelease.getVersion().toString()
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
                        catalog,
                        extension,
                        selectedRelease,
                        optionalDependencies.isSelected(),
                        progress -> Platform.runLater(() -> progressWindow.setProgress(progress)),
                        (step, resource) -> Platform.runLater(() -> progressWindow.setStatus(MessageFormat.format(
                                resources.getString(switch (step) {
                                    case DOWNLOADING -> "Catalog.ExtensionModificationWindow.downloading";
                                    case EXTRACTING_ZIP -> "Catalog.ExtensionModificationWindow.extracting";
                                }),
                                resource
                        )))
                );

                Platform.runLater(() -> {
                    progressWindow.close();
                    Dialogs.showInfoNotification(
                            resources.getString("Catalog.ExtensionModificationWindow.extensionManager"),
                            MessageFormat.format(
                                    resources.getString("Catalog.ExtensionModificationWindow.installed"),
                                    extension.getName(),
                                    release.getSelectionModel().getSelectedItem().getVersion().toString()
                            )
                    );
                    close();
                });
            } catch (Exception e) {
                Platform.runLater(progressWindow::close);

                if (e instanceof InterruptedException || e.getCause() instanceof InterruptedException) {
                    logger.debug("Installation of {} interrupted", extension, e);
                } else {
                    logger.error("Error while installing {}", extension, e);

                    Platform.runLater(() -> Dialogs.showErrorMessage(
                            resources.getString("Catalog.ExtensionModificationWindow.installationError"),
                            MessageFormat.format(
                                    resources.getString("Catalog.ExtensionModificationWindow.notInstalled"),
                                    extension.getName(),
                                    release.getSelectionModel().getSelectedItem().getVersion().toString(),
                                    e.getLocalizedMessage()
                            )
                    ));
                }
            }
        });
        executor.shutdown();
    }

    private String getSubmitText() {
        if (extension.getInstalledRelease().getValue().isEmpty()) {
            return "Catalog.ExtensionModificationWindow.install";
        } else if (release.getSelectionModel().getSelectedItem() == null) {
            return "Catalog.ExtensionModificationWindow.update";
        } else {
            Version selectedVersion = release.getSelectionModel().getSelectedItem().getVersion();
            Version installedVersion = extension.getInstalledRelease().getValue().get().getVersion();

            if (selectedVersion.compareTo(installedVersion) < 0) {
                return "Catalog.ExtensionModificationWindow.downgrade";
            } else if (selectedVersion.compareTo(installedVersion) > 0) {
                return "Catalog.ExtensionModificationWindow.update";
            } else {
                return "Catalog.ExtensionModificationWindow.reinstall";
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
                .anyMatch(path -> path.equalsIgnoreCase(FileTools.getFileNameFromURI(release.getMainUrl())));
    }
}
