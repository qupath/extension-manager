package qupath.ext.extensionmanager.gui.index;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.index.Extension;
import qupath.ext.extensionmanager.gui.UiUtils;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * A window displaying the description and the clickable homepage of an extension.
 */
class ExtensionDetails extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionDetails.class);
    private static final ResourceBundle resources = UiUtils.getResources();
    @FXML
    private Label description;
    @FXML
    private Hyperlink homepage;

    /**
     * Create the window.
     *
     * @param extension the extension whose information should be displayed
     * @throws IOException when an error occurs while creating the container
     */
    public ExtensionDetails(Extension extension) throws IOException {
        UiUtils.loadFXML(this, ExtensionDetails.class.getResource("extension_details.fxml"));

        initModality(Modality.APPLICATION_MODAL);

        setTitle(extension.name());
        description.setText(extension.description());
        homepage.setText(extension.homepage().toString());
    }

    @FXML
    private void onHomepageClicked(ActionEvent ignored) {
        UiUtils.openLinkInWebBrowser(homepage.getText()).exceptionally(error -> {
            logger.error("Error when opening {} in browser", homepage.getText(), error);

            Platform.runLater(() -> new Alert(
                    Alert.AlertType.ERROR,
                    MessageFormat.format(
                            resources.getString("Index.ExtensionDetails.cannotOpen"),
                            homepage.getText(),
                            error.getLocalizedMessage()
                    )
            ).show());

            return null;
        });
    }
}
