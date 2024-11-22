package qupath.ext.extensionmanager.core;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * A simple class to simulate communication with QuPath.
 */
public class QuPath {

    /**
     * @return the directory containing the installed extensions
     */
    public static StringProperty getExtensionDirectory() {
        return new SimpleStringProperty("/home/leo/test");
    }

    /**
     * @return a text describing the QuPath version with form "v[MAJOR].[MINOR].[PATCH]"
     * or "v[MAJOR].[MINOR].[PATCH]-rc[RELEASE_CANDIDATE]"
     */
    public static String getQuPathVersion() {
        return "v0.6.0-rc3";
    }
}
