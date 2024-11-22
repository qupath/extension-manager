package qupath.ext.extensionmanager.gui;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Utility methods related to the user interface.
 */
public class UiUtils {

    private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ext.extensionmanager.strings");

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
     * @throws URISyntaxException when the provided link is not a valid URI
     * @throws IOException if the user default browser is not found, or it fails
     * to be launched, or the default handler application failed to be launched
     */
    public static void openInBrowser(String url) throws URISyntaxException, IOException {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;

        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            desktop.browse(new URI(url));
        }
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
