package qupath.ext.extensionmanager.gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
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
import qupath.ext.extensionmanager.core.index.IndexFetcher;
import qupath.ext.extensionmanager.core.index.Index;
import qupath.ext.extensionmanager.core.savedentities.SavedIndex;
import qupath.ext.extensionmanager.core.tools.GitHubRawLinkFinder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A window that allows managing indexes.
 */
public class IndexManager extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(IndexManager.class);
    private static final ResourceBundle resources = UiUtils.getResources();
    private static final String INDEX_FILE_NAME = "index.json";
    private final ExtensionIndexManager extensionIndexManager;
    private final Runnable onInvalidExtensionDirectory;
    @FXML
    private TableView<SavedIndex> indexTable;
    @FXML
    private TableColumn<SavedIndex, String> nameColumn;
    @FXML
    private TableColumn<SavedIndex, URI> urlColumn;
    @FXML
    private TableColumn<SavedIndex, String> descriptionColumn;
    @FXML
    private TextField indexUrl;

    /**
     * Create the window.
     *
     * @param extensionIndexManager the extension index manager this window should use
     * @param model the model to use when accessing data
     * @param onInvalidExtensionDirectory a function that will be called if an operation needs to access the extension
     *                                    directory (see {@link ExtensionIndexManager#getExtensionDirectoryPath()})
     *                                    but this directory is currently invalid. It lets the possibility to the user to
     *                                    define and create a valid directory before performing the operation (which would
     *                                    fail if the directory is invalid). This function is guaranteed to be called from
     *                                    the JavaFX Application Thread
     * @throws IOException when an error occurs while creating the window
     */
    public IndexManager(
            ExtensionIndexManager extensionIndexManager,
            ExtensionIndexModel model,
            Runnable onInvalidExtensionDirectory
    ) throws IOException {
        this.extensionIndexManager = extensionIndexManager;
        this.onInvalidExtensionDirectory = onInvalidExtensionDirectory;

        UiUtils.loadFXML(this, IndexManager.class.getResource("index_manager.fxml"));

        indexTable.setItems(model.getIndexes());

        setColumns();
        setDoubleClickHandler();
        setRightClickHandler();
    }

    @FXML
    private void onAddClicked(ActionEvent ignored) {
        UiUtils.promptExtensionDirectory(extensionIndexManager.getExtensionDirectoryPath(), onInvalidExtensionDirectory);

        String indexUrl = this.indexUrl.getText();
        if (indexUrl == null || indexUrl.isBlank()) {
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        ProgressWindow progressWindow;
        try {
            progressWindow = new ProgressWindow(
                    MessageFormat.format(
                            resources.getString("IndexManager.fetching"),
                            indexUrl
                    ),
                    executor::shutdownNow
            );
        } catch (IOException e) {
            logger.error("Error while creating progress window", e);
            new Alert(
                    Alert.AlertType.ERROR,
                    resources.getString("IndexManager.cannotCreateProgressWindow")
            ).show();

            executor.shutdown();
            return;
        }
        progressWindow.show();

        executor.execute(() -> {
            try {
                Platform.runLater(() -> progressWindow.setStatus(MessageFormat.format(
                        resources.getString("IndexManager.attemptingToGetRawLink"),
                        indexUrl
                )));

                URI uri;
                try {
                    uri = GitHubRawLinkFinder.getRawLinkOfFileInRepository(indexUrl, INDEX_FILE_NAME::equals).get();
                } catch (ExecutionException e) {
                    logger.debug("Attempt to get raw link of {} failed. Considering it to be a raw link.", indexUrl, e);
                    uri = new URI(indexUrl);
                }

                URI finalUri = uri;
                Platform.runLater(() -> {
                    progressWindow.setProgress(0.5f);
                    progressWindow.setStatus(MessageFormat.format(
                            resources.getString("IndexManager.fetchingIndexLocatedAt"),
                            finalUri.toString()
                    ));
                });
                Index index = IndexFetcher.getIndex(uri).get();
                Platform.runLater(() -> progressWindow.setProgress(1));

                if (extensionIndexManager.getIndexes().stream().anyMatch(savedIndex -> savedIndex.name().equals(index.name()))) {
                    Platform.runLater(() -> new Alert(
                            Alert.AlertType.ERROR,
                            MessageFormat.format(
                                    resources.getString("IndexManager.indexAlreadyExists"),
                                    index.name()
                            )
                    ).show());
                    return;
                }

                try {
                    extensionIndexManager.addIndex(List.of(new SavedIndex(
                            index.name(),
                            index.description(),
                            new URI(indexUrl),
                            uri
                    )));
                } catch (URISyntaxException | SecurityException | NullPointerException | IOException e) {
                    logger.error("Error when saving index {}", index.name(), e);

                    Platform.runLater(() -> new Alert(
                            Alert.AlertType.ERROR,
                            MessageFormat.format(
                                    resources.getString("IndexManager.cannotSaveIndex"),
                                    e.getLocalizedMessage()
                            )
                    ).show());
                }
            } catch (Exception e) {
                logger.debug("Error when fetching index at {}", indexUrl, e);

                Platform.runLater(() -> new Alert(
                        Alert.AlertType.ERROR,
                        MessageFormat.format(
                                resources.getString("IndexManager.cannotAddIndex"),
                                e.getLocalizedMessage()
                        )
                ).show());
            } finally {
                Platform.runLater(progressWindow::close);
            }
        });
        executor.shutdown();
    }

    private void setColumns() {
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().name()));
        urlColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().uri()));
        descriptionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().description()));

        urlColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(URI item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
                    setText(item.toString());
                }
            }
        });
    }

    private void setDoubleClickHandler() {
        indexTable.setRowFactory(tv -> {
            TableRow<SavedIndex> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    String url = row.getItem().uri().toString();

                    UiUtils.openLinkInWebBrowser(url).exceptionally(error -> {
                        logger.error("Error when opening {} in browser", url, error);

                        Platform.runLater(() -> new Alert(
                                Alert.AlertType.ERROR,
                                MessageFormat.format(
                                        resources.getString("IndexManager.cannotOpen"),
                                        url,
                                        error.getLocalizedMessage()
                                )
                        ).show());

                        return null;
                    });
                }
            });
            return row;
        });
    }

    private void setRightClickHandler() {
        indexTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        ContextMenu menu = new ContextMenu();
        indexTable.setContextMenu(menu);

        MenuItem copyItem = new MenuItem(resources.getString("IndexManager.copyUrl"));
        copyItem.setOnAction(ignored -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(indexTable.getSelectionModel().getSelectedItem().uri().toString());
            Clipboard.getSystemClipboard().setContent(content);
        });
        menu.getItems().add(copyItem);

        MenuItem removeItem = new MenuItem(resources.getString("IndexManager.remove"));
        removeItem.setOnAction(ignored -> {
            try {
                extensionIndexManager.removeIndexes(indexTable.getSelectionModel().getSelectedItems());
            } catch (IOException | SecurityException | NullPointerException e) {
                logger.error("Error when removing {}", indexTable.getSelectionModel().getSelectedItems(), e);

                new Alert(
                        Alert.AlertType.ERROR,
                        MessageFormat.format(
                                resources.getString("IndexManager.cannotRemoveSelectedIndexes"),
                                e.getLocalizedMessage()
                        )
                ).show();
            }
        });
        menu.getItems().add(removeItem);
    }
}
