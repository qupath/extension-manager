package qupath.ext.extensionmanager.gui.index;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.ExtensionIndexManager;
import qupath.ext.extensionmanager.core.index.Extension;
import qupath.ext.extensionmanager.core.index.Release;
import qupath.ext.extensionmanager.core.savedentities.InstalledExtension;
import qupath.ext.extensionmanager.core.savedentities.SavedIndex;
import qupath.ext.extensionmanager.core.tools.FileTools;
import qupath.ext.extensionmanager.gui.ProgressWindow;
import qupath.ext.extensionmanager.gui.UiUtils;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * A window that provide choices to install or modify the installation of an extension.
 */
class ExtensionModificationWindow extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionModificationWindow.class);
    private static final ResourceBundle resources = UiUtils.getResources();
    private final ExtensionIndexManager extensionIndexManager;
    private final SavedIndex savedIndex;
    private final Extension extension;
    private final InstalledExtension installedExtension;
    @FXML
    private Label name;
    @FXML
    private ChoiceBox<Release> release;
    @FXML
    private CheckBox optionalDependencies;
    @FXML
    private Button submit;

    /**
     * Create the window.
     *
     * @param extensionIndexManager the extension index manager this window should use
     * @param savedIndex the index owning the extension to modify
     * @param extension the extension to modify
     * @param installedExtension information on the already installed extension if this window should allow modifying it,
     *                           or null if the extension is to be installed by this window
     * @throws IOException when an error occurs while creating the window
     */
    public ExtensionModificationWindow(
            ExtensionIndexManager extensionIndexManager,
            SavedIndex savedIndex,
            Extension extension,
            InstalledExtension installedExtension
    ) throws IOException {
        this.extensionIndexManager = extensionIndexManager;
        this.savedIndex = savedIndex;
        this.extension = extension;
        this.installedExtension = installedExtension;

        UiUtils.loadFXML(this, ExtensionModificationWindow.class.getResource("extension_modification_window.fxml"));

        initModality(Modality.APPLICATION_MODAL);

        setTitle(MessageFormat.format(
                resources.getString(installedExtension == null ?
                        "Index.ExtensionModificationWindow.installX" :
                        "Index.ExtensionModificationWindow.editX"
                ),
                extension.name()
        ));

        name.setText(extension.name());

        release.getItems().addAll(extension.releases().stream()
                .filter(release -> release.versionRange().isCompatible(extensionIndexManager.getVersion()))
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

        submit.setText(resources.getString(installedExtension == null ?
                "Index.ExtensionModificationWindow.install" :
                "Index.ExtensionModificationWindow.update"
        ));
    }

    @FXML
    private void onSubmitClicked(ActionEvent ignored) {
        if (release.getSelectionModel().isEmpty()) {
            return;
        }
        Release selectedRelease = release.getSelectionModel().getSelectedItem();

        try {
            if (isJarAlreadyDownloaded(selectedRelease)) {
                var confirmation = new Alert(
                        Alert.AlertType.CONFIRMATION,
                        resources.getString("Index.ExtensionModificationWindow.extensionAlreadyInstalled")
                ).showAndWait();

                if (confirmation.isEmpty() || !confirmation.get().equals(ButtonType.OK)) {
                    return;
                }
            }
        } catch (NullPointerException | InvalidPathException e) {
            logger.debug(
                    "Cannot get file name from {}. Assuming {} with release {} is not already installed",
                    selectedRelease.mainUrl(),
                    extension,
                    selectedRelease
            );
        }

        try {
            ExecutorService executor = Executors.newSingleThreadExecutor();

            ProgressWindow progressWindow = new ProgressWindow(
                    MessageFormat.format(
                            resources.getString(installedExtension == null ?
                                    "Index.ExtensionModificationWindow.installing" :
                                    "Index.ExtensionModificationWindow.updating"
                            ),
                            extension.name(),
                            selectedRelease.name()
                    ),
                    executor::shutdownNow
            );
            progressWindow.show();

            executor.execute(() -> extensionIndexManager.installOrUpdateExtension(
                    savedIndex,
                    extension,
                    new InstalledExtension(
                            selectedRelease.name(),
                            optionalDependencies.isSelected()
                    ),
                    progress -> Platform.runLater(() -> progressWindow.setProgress(progress)),
                    (step, resource) -> Platform.runLater(() -> progressWindow.setStatus(MessageFormat.format(
                            resources.getString(switch (step) {
                                case DOWNLOADING -> "Index.ExtensionModificationWindow.downloading";
                                case EXTRACTING_ZIP -> "Index.ExtensionModificationWindow.extracting";
                            }),
                            resource
                    ))),
                    error -> {
                        if (error != null) {
                            logger.error("Error while installing extension", error);
                        }

                        Platform.runLater(() -> {
                            progressWindow.close();

                            if (error == null) {
                                new Alert(
                                        Alert.AlertType.INFORMATION,
                                        MessageFormat.format(
                                                resources.getString("Index.ExtensionModificationWindow.installed"),
                                                extension.name(),
                                                release.getSelectionModel().getSelectedItem().name()
                                        )
                                ).show();
                            } else {
                                new Alert(
                                        Alert.AlertType.ERROR,
                                        MessageFormat.format(
                                                resources.getString("Index.ExtensionModificationWindow.notInstalled"),
                                                extension.name(),
                                                release.getSelectionModel().getSelectedItem().name(),
                                                error.getLocalizedMessage()
                                        )
                                ).show();
                            }

                            close();
                        });

                        executor.shutdown();
                    }
            ));
        } catch (IOException e) {
            logger.error("Error while creating progress window", e);
        }
    }

    private boolean isJarAlreadyDownloaded(Release release) {
        return Stream.concat(
                extensionIndexManager.getManuallyInstalledJars().stream(),
                extensionIndexManager.getIndexedManagedInstalledJars().stream()
        )
                .map(Path::getFileName)
                .filter(Objects::nonNull)
                .map(Path::toString)
                .anyMatch(path -> path.equalsIgnoreCase(FileTools.getFileNameFromURI(release.mainUrl())));
    }
}
