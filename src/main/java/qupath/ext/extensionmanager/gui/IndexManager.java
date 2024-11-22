package qupath.ext.extensionmanager.gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.ExtensionIndexManager;
import qupath.ext.extensionmanager.core.IndexFetcher;
import qupath.ext.extensionmanager.core.IndexMetadata;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A window that allows managing indexes.
 */
public class IndexManager extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(IndexManager.class);
    @FXML
    private TableView<IndexMetadata> indexTable;
    @FXML
    private TableColumn<IndexMetadata, String> nameColumn;
    @FXML
    private TableColumn<IndexMetadata, URI> urlColumn;
    @FXML
    private TableColumn<IndexMetadata, String> descriptionColumn;
    @FXML
    private TextField indexURL;

    /**
     * Create the window.
     *
     * @param model the model to use when accessing data
     * @throws IOException when an error occurs while creating the window
     */
    public IndexManager(ExtensionIndexModel model) throws IOException {
        UiUtils.loadFXML(this, IndexManager.class.getResource("index_manager.fxml"));

        indexTable.setItems(model.getIndexes());

        setColumns();
        setDoubleClickHandler();
        setRightClickHandler();
    }

    @FXML
    private void onAddClicked(ActionEvent ignored) {
        IndexFetcher.getIndex(indexURL.getText()).handle((index, error) -> {
            if (error == null) {
                Platform.runLater(() -> ExtensionIndexManager.addIndex(new IndexMetadata(
                        index.name(),
                        index.description(),
                        URI.create(indexURL.getText())
                )));
            } else {
                logger.debug(String.format("Error when fetching index at %s", indexURL.getText()), error);

                Platform.runLater(() -> new Alert(
                        Alert.AlertType.ERROR,
                        String.format("Cannot add index:\n%s", error.getLocalizedMessage())
                ).show());
            }
            return null;
        });
    }

    private void setColumns() {
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().name()));
        urlColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().url()));
        descriptionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().description()));
    }

    private void setDoubleClickHandler() {
        indexTable.setRowFactory(tv -> {
            TableRow<IndexMetadata> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    try {
                        UiUtils.openInBrowser(row.getItem().url().toString());
                    } catch (URISyntaxException | IOException e) {
                        logger.error(String.format("Error when opening %s in browser", row.getItem().url()), e);

                        new Alert(
                                Alert.AlertType.ERROR,
                                String.format("Cannot open '%s':\n%s", row.getItem().url(), e.getLocalizedMessage())
                        ).show();
                    }
                }
            });
            return row;
        });
    }

    private void setRightClickHandler() {
        indexTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        ContextMenu menu = new ContextMenu();
        indexTable.setContextMenu(menu);

        MenuItem copyItem = new MenuItem("Copy URL");
        copyItem.setOnAction(ignored -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(indexTable.getSelectionModel().getSelectedItem().url().toString());
            Clipboard.getSystemClipboard().setContent(content);
        });
        menu.getItems().add(copyItem);

        MenuItem removeItem = new MenuItem("Remove");
        removeItem.setOnAction(ignored ->
                ExtensionIndexManager.removeIndexes(indexTable.getSelectionModel().getSelectedItems())
        );
        menu.getItems().add(removeItem);
    }
}
