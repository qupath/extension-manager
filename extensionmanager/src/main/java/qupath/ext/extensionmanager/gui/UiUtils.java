package qupath.ext.extensionmanager.gui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Utility methods related to the user interface.
 */
public class UiUtils {

    private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ext.extensionmanager.strings");
    private static final GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");

    /**
     * A CSS class.
     */
    public enum CssClass {
        /**
         * A class to apply to an odd element of a vertical list
         */
        ODD_ROW("odd-row"),
        /**
         * A class to make an element yellow with a shadow
         */
        STAR("star"),
        /**
         * A class to make an element invisible
         */
        INVISIBLE("invisible");

        private final String className;

        CssClass(String className) {
            this.className = className;
        }
    }

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
     * @return the resources of this project
     */
    public static ResourceBundle getResources() {
        return resources;
    }

    /**
     * Get the name of a CSS class.
     *
     * @param cssClass the CSS class containing the name to retrieve
     * @return the name of the provided CSS class
     */
    public static String getClassName(CssClass cssClass) {
        return cssClass.className;
    }

    /**
     * Open the provided link in a web browser. This won't do anything if browsing is not supported by the computer.
     *
     * @param url the link to open
     * @return a CompletableFuture that will complete exceptionally if an error occurs while browsing the provided link
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
     * Open the provided directory with the platform's file explorer. This won't do anything if browsing files is not
     * supported by the computer.
     *
     * @param directory the path to the directory to browse
     * @return a CompletableFuture that will complete exceptionally if an error occurs while browsing the provided directory
     */
    public static CompletableFuture<Void> openFolderInFileExplorer(Path directory) {
        return CompletableFuture.runAsync(() -> {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;

            if (desktop != null && desktop.isSupported(Desktop.Action.OPEN)) {
                try {
                    desktop.open(directory.toFile());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Run the provided Runnable if the provided extension directory property doesn't point to a valid directory. The
     * provided Runnable can be for example used to prompt the user for a new extension directory path.
     *
     * @param extensionDirectoryProperty the property to check
     * @param onInvalidExtensionDirectory the Runnable to call if the provided path is not a valid directory
     */
    public static void promptExtensionDirectory(
            ObservableValue<Path> extensionDirectoryProperty,
            Runnable onInvalidExtensionDirectory
    ) {
        Path extensionDirectory = extensionDirectoryProperty.getValue();

        if (extensionDirectory == null || !Files.isDirectory(extensionDirectory)) {
            onInvalidExtensionDirectory.run();
        }
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
     * Propagates changes made to an observable list to another observable list. The listening list is updated in the
     * JavaFX Application Thread.
     *
     * @param listToUpdate the list to update
     * @param listToListen the list to listen
     * @return the listener that was added to the list to listen, so that it can be removed when needed
     * @param <T> the type of the elements of the lists
     */
    public static <T> ListChangeListener<? super T> bindListInUIThread(List<T> listToUpdate, ObservableList<T> listToListen) {
        listToUpdate.addAll(listToListen);

        ListChangeListener<? super T> listener = change -> Platform.runLater(() -> {
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
        });
        listToListen.addListener(listener);

        return listener;
    }

    /**
     * Propagates changes made to a property to another property.
     * <p>
     * The listening property is updated in the UI thread.
     *
     * @param propertyToUpdate the property to update
     * @param propertyToListen the property to listen
     * @return the listener that was added to the property to listen, so that it can be removed when needed
     * @param <T> the type of the property
     */
    public static <T> ChangeListener<? super T> bindPropertyInUIThread(WritableValue<T> propertyToUpdate, ObservableValue<T> propertyToListen) {
        propertyToUpdate.setValue(propertyToListen.getValue());

        ChangeListener<? super T> listener = (p, o, n) -> {
            if (Platform.isFxApplicationThread()) {
                propertyToUpdate.setValue(n);
            } else {
                Platform.runLater(() -> propertyToUpdate.setValue(n));
            }
        };
        propertyToListen.addListener(listener);

        return listener;
    }
}
