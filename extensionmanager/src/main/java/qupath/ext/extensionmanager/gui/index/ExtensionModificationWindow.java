package qupath.ext.extensionmanager.gui.index;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.ExtensionIndexManager;
import qupath.ext.extensionmanager.core.index.model.Extension;
import qupath.ext.extensionmanager.core.index.model.Release;
import qupath.ext.extensionmanager.core.savedentities.InstalledExtension;
import qupath.ext.extensionmanager.core.savedentities.SavedIndex;
import qupath.ext.extensionmanager.gui.ProgressWindow;
import qupath.ext.extensionmanager.gui.UiUtils;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A window that provide choices to install or modify the installation of an extension.
 */
class ExtensionModificationWindow extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionModificationWindow.class);
    private final ExtensionIndexManager extensionIndexManager;
    private final SavedIndex savedIndex;
    private final Extension extension;
    private final InstalledExtension installedExtension;
    @FXML
    private Label name;
    @FXML
    private ChoiceBox<Release> version;
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

        setTitle(installedExtension == null ?
                String.format("Install %s", extension.name()) :
                String.format("Edit %s", extension.name())
        );

        name.setText(extension.name());

        version.getItems().addAll(
                extension.releases().stream()
                        .filter(release -> release.versionRange().isCompatible(extensionIndexManager.getVersion()))
                        .toList()
        );
        version.setConverter(new StringConverter<>() {
            @Override
            public String toString(Release object) {
                return object == null ? null : object.name();
            }

            @Override
            public Release fromString(String string) {
                return null;
            }
        });
        version.getSelectionModel().select(installedExtension == null ?
                version.getItems().isEmpty() ? null : version.getItems().getFirst() :
                version.getItems().stream()
                        .filter(release -> release.name().equals(installedExtension.releaseName()))
                        .findAny()
                        .orElse(version.getItems().isEmpty() ? null : version.getItems().getFirst())
        );

        optionalDependencies.visibleProperty().bind(version.getSelectionModel()
                .selectedItemProperty()
                .map(release -> !release.optionalDependencyUrls().isEmpty())
        );
        optionalDependencies.managedProperty().bind(optionalDependencies.visibleProperty());
        optionalDependencies.setSelected(installedExtension != null && installedExtension.optionalDependenciesInstalled());

        submit.setText(installedExtension == null ? "Install" : "Update");
    }

    @FXML
    private void onSubmitClicked(ActionEvent ignored) {
        if (version.getSelectionModel().isEmpty()) {
            return;
        }

        try {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            ProgressWindow progressWindow = new ProgressWindow(
                    installedExtension == null ?
                            String.format("Installing %s %s...", extension.name(), version.getSelectionModel().getSelectedItem().name()) :
                            String.format("Updating %s to %s...", extension.name(), version.getSelectionModel().getSelectedItem().name()),
                    executor::shutdownNow
            );
            progressWindow.show();

            executor.execute(() -> extensionIndexManager.installOrUpdateExtension(
                    savedIndex,
                    extension,
                    new InstalledExtension(
                            version.getSelectionModel().getSelectedItem().name(),
                            optionalDependencies.isSelected()
                    ),
                    progress -> Platform.runLater(() -> progressWindow.setProgress(progress)),
                    status -> Platform.runLater(() -> progressWindow.setStatus(status)),
                    error -> {
                        if (error != null) {
                            logger.error("Error while installing extension", error);
                        }

                        Platform.runLater(() -> {
                            progressWindow.close();

                            if (error == null) {
                                new Alert(
                                        Alert.AlertType.INFORMATION,
                                        String.format(
                                                "%s %s installed.",
                                                extension.name(),
                                                version.getSelectionModel().getSelectedItem().name()
                                        )
                                ).show();
                            } else {
                                new Alert(
                                        Alert.AlertType.ERROR,
                                        String.format(
                                                "%s %s not installed: %s.",
                                                extension.name(),
                                                version.getSelectionModel().getSelectedItem().name(),
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
}
