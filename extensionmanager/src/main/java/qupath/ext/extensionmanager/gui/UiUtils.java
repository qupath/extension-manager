package qupath.ext.extensionmanager.gui;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Utility methods related to the user interface.
 */
public class UiUtils {

    private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ext.extensionmanager.strings");
    private static final GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");

    private UiUtils() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * Loads the FXML file located at the provided URL and set its controller.
     *
     * @param controller the controller of the FXML file to load
     * @param url the path of the FXML file to load
     * @throws IOException if an error occurs while loading the FXML file
     */
    public static void loadFXML(Object controller, URL url) throws IOException {
        FXMLLoader loader = new FXMLLoader(url, resources);
        loader.setRoot(controller);
        loader.setController(controller);
        loader.load();
    }

    /**
     * Open the provided link in a web browser. This won't do anything if
     * browsing is not supported by the computer.
     *
     * @param url the link to open
     * @return a CompletableFuture that will complete exceptionally if an error occurs while
     * browsing the provided link
     */
    public static CompletableFuture<Void> openLinkInWebBrowser(String url) {
        return CompletableFuture.runAsync(() -> {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;

            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(new URI(url));
                } catch (IOException | URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Open the provided folder with the platform's file explorer. This won't do anything if
     * browsing files is not supported by the computer.
     *
     * @param folder the path to the folder to browse
     * @return a CompletableFuture that will complete exceptionally if an error occurs while
     * browsing the provided folder
     */
    public static CompletableFuture<Void> openFolderInFileExplorer(String folder) {
        return CompletableFuture.runAsync(() -> {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;

            if (desktop != null && desktop.isSupported(Desktop.Action.OPEN)) {
                try {
                    desktop.open(new File(folder));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Return a JavaFX Node that displays the provided Glyph.
     *
     * @param glyph the glyph to display
     * @return a JavaFX Node that displays the provided Glyph
     */
    public static Glyph getFontAwesomeIcon(FontAwesome.Glyph glyph) {
        return fontAwesome.create(glyph);
    }

    /**
     * Propagates changes made to an observable list to another observable list.
     * The listening list is updated in the JavaFX Application Thread.
     *
     * @param listToUpdate the list to update
     * @param listToListen the list to listen
     * @param <T> the type of the elements of the lists
     */
    public static <T> void bindListInUIThread(ObservableList<T> listToUpdate, ObservableList<T> listToListen) {
        listToUpdate.addAll(listToListen);

        listToListen.addListener((ListChangeListener<? super T>) change -> Platform.runLater(() -> {
            if (Platform.isFxApplicationThread()) {
                while (change.next()) {
                    if (change.wasAdded()) {
                        listToUpdate.addAll(change.getAddedSubList());
                    } else {
                        listToUpdate.removeAll(change.getRemoved());
                    }
                }
                // Change needs to be reset, otherwise calling this function several times
                // with the same listToListen parameter will work for only one of them
                change.reset();
            } else {
                Platform.runLater(() -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            listToUpdate.addAll(change.getAddedSubList());
                        } else {
                            listToUpdate.removeAll(change.getRemoved());
                        }
                    }
                    change.reset();
                });
            }
        }));
    }

    /**
     * Propagates changes made to a property to another property.
     * The listening property is updated in the JavaFX Application Thread.
     *
     * @param propertyToUpdate the property to update
     * @param propertyToListen the property to listen
     * @param <T> the type of the property
     */
    public static <T> void bindPropertyInUIThread(WritableValue<T> propertyToUpdate, ObservableValue<T> propertyToListen) {
        propertyToUpdate.setValue(propertyToListen.getValue());

        propertyToListen.addListener((p, o, n) -> {
            if (Platform.isFxApplicationThread()) {
                propertyToUpdate.setValue(n);
            } else {
                Platform.runLater(() -> propertyToUpdate.setValue(n));
            }
        });
    }
}
