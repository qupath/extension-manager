package qupath.ext.extensionmanager.gui.catalog;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.catalog.Extension;
import qupath.ext.extensionmanager.gui.UiUtils;
import qupath.fx.dialogs.Dialogs;
import qupath.fx.utils.FXUtils;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * A window displaying the description and the clickable homepage of an extension.
 * It is modal to its owning window and can be easily closed with shortcuts.
 */
class ExtensionDetails extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionDetails.class);
    private static final ResourceBundle resources = UiUtils.getResources();
    @FXML
    private Label description;
    @FXML
    private Hyperlink homepage;
    @FXML
    private Label notCompatible;

    /**
     * Create the window.
     *
     * @param extension the extension whose information should be displayed
     * @param noAvailableRelease whether no release of this extension can be installed
     * @throws IOException when an error occurs while creating the container
     */
    public ExtensionDetails(Extension extension, boolean noAvailableRelease) throws IOException {
        UiUtils.loadFXML(this, ExtensionDetails.class.getResource("extension_details.fxml"));

        FXUtils.addCloseWindowShortcuts(this);
        initModality(Modality.WINDOW_MODAL);

        setTitle(extension.name());

        description.setText(extension.description());

        homepage.setText(extension.homepage().toString());

        notCompatible.setVisible(noAvailableRelease);
        notCompatible.setManaged(notCompatible.isVisible());
    }

    @FXML
    private void onHomepageClicked(ActionEvent ignored) {
        String link = homepage.getText();

        UiUtils.openLinkInWebBrowser(link).exceptionally(error -> {
            logger.error("Error when opening {} in browser", link, error);

            Dialogs.showErrorMessage(
                    resources.getString("Catalog.ExtensionDetails.browserError"),
                    MessageFormat.format(
                            resources.getString("Catalog.ExtensionDetails.cannotOpen"),
                            link,
                            error.getLocalizedMessage()
                    )
            );

            return null;
        });
    }

    @FXML
    private void onCloseClicked(ActionEvent ignored) {
        close();
    }
}
