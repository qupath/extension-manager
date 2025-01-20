package qupath.ext.extensionmanager.gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.ExtensionCatalogManager;
import qupath.ext.extensionmanager.core.catalog.Catalog;
import qupath.ext.extensionmanager.core.catalog.CatalogFetcher;
import qupath.ext.extensionmanager.core.savedentities.SavedCatalog;
import qupath.ext.extensionmanager.core.tools.GitHubRawLinkFinder;
import qupath.fx.dialogs.Dialogs;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * A window to manage catalogs.
 */
class CatalogManager extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(CatalogManager.class);
    private static final ResourceBundle resources = UiUtils.getResources();
    private static final String CATALOG_FILE_NAME = "catalog.json";
    private final ExtensionCatalogManager extensionCatalogManager;
    private final Runnable onInvalidExtensionDirectory;
    @FXML
    private TableView<SavedCatalog> catalogTable;
    @FXML
    private TableColumn<SavedCatalog, String> nameColumn;
    @FXML
    private TableColumn<SavedCatalog, URI> urlColumn;
    @FXML
    private TableColumn<SavedCatalog, String> descriptionColumn;
    @FXML
    private TableColumn<SavedCatalog, Button> removeColumn;
    @FXML
    private TextField catalogUrl;

    /**
     * Create the window.
     *
     * @param extensionCatalogManager the extension catalog manager this window should use
     * @param model the model to use when accessing data
     * @param onInvalidExtensionDirectory a function that will be called if an operation needs to access the extension
     *                                    directory (see {@link ExtensionCatalogManager#getExtensionDirectoryPath()})
     *                                    but this directory is currently invalid. It lets the possibility to the user to
     *                                    define and create a valid directory before performing the operation (which would
     *                                    fail if the directory is invalid). This function is guaranteed to be called from
     *                                    the JavaFX Application Thread
     * @throws IOException when an error occurs while creating the window
     */
    public CatalogManager(
            ExtensionCatalogManager extensionCatalogManager,
            ExtensionCatalogModel model,
            Runnable onInvalidExtensionDirectory
    ) throws IOException {
        this.extensionCatalogManager = extensionCatalogManager;
        this.onInvalidExtensionDirectory = onInvalidExtensionDirectory;

        UiUtils.loadFXML(this, CatalogManager.class.getResource("catalog_manager.fxml"));

        catalogTable.setItems(model.getCatalogs());

        setColumns();
        setDoubleClickHandler();
        setRightClickHandler();
    }

    @FXML
    private void onAddClicked(ActionEvent ignored) {
        UiUtils.promptExtensionDirectory(extensionCatalogManager.getExtensionDirectoryPath(), onInvalidExtensionDirectory);

        String catalogUrl = this.catalogUrl.getText();
        if (catalogUrl == null || catalogUrl.isBlank()) {
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        ProgressWindow progressWindow;
        try {
            progressWindow = new ProgressWindow(
                    MessageFormat.format(
                            resources.getString("CatalogManager.fetching"),
                            catalogUrl
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
                Platform.runLater(() -> progressWindow.setStatus(MessageFormat.format(
                        resources.getString("CatalogManager.attemptingToGetRawLink"),
                        catalogUrl
                )));

                URI uri;
                try {
                    uri = GitHubRawLinkFinder.getRawLinkOfFileInRepository(catalogUrl, CATALOG_FILE_NAME::equals).get();
                } catch (ExecutionException e) {
                    logger.debug("Attempt to get raw link of {} failed. Considering it to be a raw link.", catalogUrl, e);
                    uri = new URI(catalogUrl);
                }

                URI finalUri = uri;
                Platform.runLater(() -> {
                    progressWindow.setProgress(0.5f);
                    progressWindow.setStatus(MessageFormat.format(
                            resources.getString("CatalogManager.fetchingCatalogLocatedAt"),
                            finalUri.toString()
                    ));
                });
                Catalog catalog = CatalogFetcher.getCatalog(uri).get();
                Platform.runLater(() -> progressWindow.setProgress(1));

                if (extensionCatalogManager.getCatalogs().stream().anyMatch(savedCatalog -> savedCatalog.name().equals(catalog.name()))) {
                    Platform.runLater(() -> new Alert(
                            Alert.AlertType.ERROR,
                            MessageFormat.format(
                                    resources.getString("CatalogManager.catalogAlreadyExists"),
                                    catalog.name()
                            )
                    ).show());
                    return;
                }

                try {
                    extensionCatalogManager.addCatalog(List.of(new SavedCatalog(
                            catalog.name(),
                            catalog.description(),
                            new URI(catalogUrl),
                            uri,
                            true
                    )));
                } catch (URISyntaxException | SecurityException | NullPointerException | IOException e) {
                    logger.error("Error when saving {}", catalog.name(), e);

                    Platform.runLater(() -> new Alert(
                            Alert.AlertType.ERROR,
                            MessageFormat.format(
                                    resources.getString("CatalogManager.cannotSaveCatalog"),
                                    e.getLocalizedMessage()
                            )
                    ).show());
                }
            } catch (Exception e) {
                logger.debug("Error when fetching catalog at {}", catalogUrl, e);

                Platform.runLater(() -> new Alert(
                        Alert.AlertType.ERROR,
                        MessageFormat.format(
                                resources.getString("CatalogManager.cannotAddCatalog"),
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
        removeColumn.setCellValueFactory(cellData -> {
            Button button = new Button(resources.getString("CatalogManager.remove"));
            button.setDisable(!cellData.getValue().deletable());
            button.setOnAction(event -> {
                UiUtils.promptExtensionDirectory(extensionCatalogManager.getExtensionDirectoryPath(), onInvalidExtensionDirectory);

                deleteCatalogs(List.of(cellData.getValue()));
            });
            return new SimpleObjectProperty<>(button);
        });

        nameColumn.setCellFactory(getStringCellFactory());
        urlColumn.setCellFactory(column -> {
            TableCell<SavedCatalog, URI> tableCell = new TableCell<>() {
                @Override
                protected void updateItem(URI item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                        setTooltip(null);
                    } else {
                        setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
                        setText(item.toString());
                        setTooltip(new Tooltip(item.toString()));
                    }
                }
            };
            tableCell.setAlignment(Pos.CENTER_LEFT);
            return tableCell;
        });
        descriptionColumn.setCellFactory(getStringCellFactory());
        removeColumn.setCellFactory(column -> {
            TableCell<SavedCatalog, Button> tableCell = new TableCell<>() {
                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        setGraphic(item);
                    }
                }
            };
            tableCell.setAlignment(Pos.CENTER);
            return tableCell;
        });
    }

    private void setDoubleClickHandler() {
        catalogTable.setRowFactory(tv -> {
            TableRow<SavedCatalog> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    String url = row.getItem().uri().toString();

                    UiUtils.openLinkInWebBrowser(url).exceptionally(error -> {
                        logger.error("Error when opening {} in browser", url, error);

                        Dialogs.showErrorMessage(
                                resources.getString("CatalogManager.browserError"),
                                MessageFormat.format(
                                        resources.getString("CatalogManager.cannotOpen"),
                                        url,
                                        error.getLocalizedMessage()
                                )
                        );

                        return null;
                    });
                }
            });
            return row;
        });
    }

    private void setRightClickHandler() {
        catalogTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        ContextMenu menu = new ContextMenu();
        catalogTable.setContextMenu(menu);

        MenuItem copyItem = new MenuItem(resources.getString("CatalogManager.copyUrl"));
        copyItem.setOnAction(ignored -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(catalogTable.getSelectionModel().getSelectedItem().uri().toString());
            Clipboard.getSystemClipboard().setContent(content);
        });
        menu.getItems().add(copyItem);

        MenuItem removeItem = new MenuItem(resources.getString("CatalogManager.remove"));
        removeItem.setOnAction(ignored -> {
            UiUtils.promptExtensionDirectory(extensionCatalogManager.getExtensionDirectoryPath(), onInvalidExtensionDirectory);

            List<String> nonDeletableCatalogs = catalogTable.getSelectionModel().getSelectedItems().stream()
                    .filter(savedCatalog -> !savedCatalog.deletable())
                    .map(SavedCatalog::name)
                    .toList();
            if (!nonDeletableCatalogs.isEmpty()) {
                Dialogs.showErrorMessage(
                        resources.getString("CatalogManager.error"),
                        MessageFormat.format(
                                resources.getString("CatalogManager.cannotBeDeleted"),
                                nonDeletableCatalogs.size() == 1 ? nonDeletableCatalogs.getFirst() : nonDeletableCatalogs.toString()
                        )
                );
                return;
            }

            deleteCatalogs(catalogTable.getSelectionModel().getSelectedItems());
        });
        menu.getItems().add(removeItem);
    }

    private void deleteCatalogs(List<SavedCatalog> catalogs) {
        List<SavedCatalog> catalogsToDelete = catalogs.stream()
                .filter(SavedCatalog::deletable)
                .toList();
        if (catalogsToDelete.isEmpty()) {
            return;
        }

        boolean deleteExtensions = Dialogs.showConfirmDialog(
                resources.getString("CatalogManager.deleteCatalog"),
                MessageFormat.format(
                        resources.getString("CatalogManager.deleteExtensions"),
                        catalogsToDelete.stream()
                                .map(savedCatalog -> {
                                    try {
                                        return String.format("\"%s\"", extensionCatalogManager.getCatalogDirectory(savedCatalog));
                                    } catch (IOException | InvalidPathException | SecurityException | NullPointerException e) {
                                        logger.error("Cannot retrieve path of {}", savedCatalog.name(), e);
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.joining("\n"))
                )
        );

        try {
            extensionCatalogManager.removeCatalogs(
                    catalogsToDelete,
                    deleteExtensions
            );
        } catch (IOException | SecurityException | NullPointerException e) {
            logger.error("Error when removing {}", catalogsToDelete, e);

            Dialogs.showErrorMessage(
                    resources.getString("CatalogManager.error"),
                    MessageFormat.format(
                            resources.getString("CatalogManager.cannotRemoveSelectedCatalogs"),
                            e.getLocalizedMessage()
                    )
            );
        }
    }

    private static Callback<TableColumn<SavedCatalog, String>, TableCell<SavedCatalog, String>> getStringCellFactory() {
        return column -> {
            TableCell<SavedCatalog, String> tableCell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                        setTooltip(null);
                    } else {
                        setText(item);
                        setTooltip(new Tooltip(item));
                    }
                }
            };
            tableCell.setAlignment(Pos.CENTER_LEFT);
            return tableCell;
        };
    }
}
