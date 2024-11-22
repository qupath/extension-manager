package qupath.ext.extensionmanager.gui.index;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.indexmodel.Extension;
import qupath.ext.extensionmanager.gui.UiUtils;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * A container displaying the description and the clickable homepage of an extension.
 */
class ExtensionDetails extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionDetails.class);
    @FXML
    private Label description;
    @FXML
    private Hyperlink homepage;

    /**
     * Create the container.
     *
     * @param extension the extension whose information should be displayed
     * @throws IOException when an error occurs while creating the container
     */
    public ExtensionDetails(Extension extension) throws IOException {
        UiUtils.loadFXML(this, ExtensionDetails.class.getResource("extension_details.fxml"));

        description.setText(extension.description());
        homepage.setText(extension.homepage().toString());
    }

    @FXML
    private void onHomepageClicked(ActionEvent ignored) {
        try {
            UiUtils.openInBrowser(homepage.getText());
        } catch (URISyntaxException | IOException e) {
            logger.error(String.format("Error when opening %s in browser", homepage.getText()), e);

            new Alert(
                    Alert.AlertType.ERROR,
                    String.format("Cannot open '%s':\n%s", homepage.getText(), e.getLocalizedMessage())
            ).show();
        }
    }
}
