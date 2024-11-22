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
import qupath.ext.extensionmanager.core.ExtensionInstallationInformation;
import qupath.ext.extensionmanager.core.QuPath;
import qupath.ext.extensionmanager.core.indexmodel.Extension;
import qupath.ext.extensionmanager.core.indexmodel.Release;
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
    private final Extension extension;
    private final ExtensionInstallationInformation installedExtension;
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
     * @param extension the extension to modify
     * @param installationInformation information on the already installed extension if this window should allow modifying it,
     *                                or null if the extension is to be installed by this window
     * @throws IOException when an error occurs while creating the window
     */
    public ExtensionModificationWindow(Extension extension, ExtensionInstallationInformation installationInformation) throws IOException {
        this.extension = extension;
        this.installedExtension = installationInformation;

        UiUtils.loadFXML(this, ExtensionModificationWindow.class.getResource("extension_modification_window.fxml"));

        initModality(Modality.APPLICATION_MODAL);

        setTitle(installationInformation == null ?
                String.format("Install %s", extension.name()) :
                String.format("Edit %s", extension.name())
        );

        name.setText(extension.name());

        version.getItems().addAll(
                extension.versions().stream()
                        .filter(release -> release.qupathVersions().isCompatible(QuPath.getQuPathVersion()))
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
        version.getSelectionModel().select(installationInformation == null ?
                version.getItems().isEmpty() ? null : version.getItems().getFirst() :
                version.getItems().stream()
                        .filter(release -> release.name().equals(installationInformation.version()))
                        .findAny()
                        .orElse(version.getItems().isEmpty() ? null : version.getItems().getFirst())
        );

        optionalDependencies.visibleProperty().bind(version.getSelectionModel()
                .selectedItemProperty()
                .map(release -> !release.optionalDependencyUrls().isEmpty())
        );
        optionalDependencies.managedProperty().bind(optionalDependencies.visibleProperty());
        optionalDependencies.setSelected(installationInformation != null && installationInformation.optionalDependenciesInstalled());

        submit.setText(installationInformation == null ? "Install" : "Update");
    }

    @FXML
    private void onSubmitClicked(ActionEvent ignored) {
        if (!version.getSelectionModel().isEmpty()) {
            try {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                ProgressWindow progressWindow = new ProgressWindow(
                        installedExtension == null ?
                                String.format("Installing %s %s...", extension.name(), version.getSelectionModel().getSelectedItem().name()) :
                                String.format("Updating %s to %s...", extension.name(), version.getSelectionModel().getSelectedItem().name()),
                        executor::shutdownNow
                );
                progressWindow.show();

                executor.execute(() -> ExtensionIndexManager.installOrUpdateExtension(
                        extension,
                        new ExtensionInstallationInformation(
                                version.getSelectionModel().getSelectedItem().name(),
                                optionalDependencies.isSelected()
                        ),
                        progress -> Platform.runLater(() -> progressWindow.setProgress(progress)),
                        () -> {
                            Platform.runLater(() -> {
                                progressWindow.close();

                                new Alert(
                                        Alert.AlertType.INFORMATION,
                                        String.format(
                                                "%s %s installed.",
                                                extension.name(),
                                                version.getSelectionModel().getSelectedItem().name()
                                        )
                                ).show();

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
}
