package qupath.ext.extensionmanager.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * A class loader that loads extension classes.
 * <p>
 * This class is thread-safe.
 */
class ExtensionClassLoader extends URLClassLoader {

    /**
     * Create the extension class loader.
     *
     * @param parent the class loader that should be the parent of this
     *               class loader
     * @throws SecurityException if the user doesn't have enough rights to create the class loader
     */
    public ExtensionClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    /**
     * Load a JAR file located on the provided path.
     *
     * @param jarPath the path to the JAR file to load
     * @throws java.io.IOError if an I/O error occurs while obtaining the absolute path of the
     * provided path
     * @throws SecurityException if the user doesn't have read rights on the provided path
     * @throws MalformedURLException if an error occurred while converting the provided path to a URL
     */
    public synchronized void addJar(Path jarPath) throws MalformedURLException {
        addURL(jarPath.toUri().toURL());
    }
}
