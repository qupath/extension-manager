package qupath.ext.extensionmanager.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.tools.FileTools;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A class loader that loads extension classes.
 * <p>
 * This class is thread-safe.
 */
class ExtensionClassLoader extends URLClassLoader {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionClassLoader.class);
    private final Set<String> addedJars = new HashSet<>();
    private final List<Runnable> runnables = new ArrayList<>();

    /**
     * Create the extension class loader.
     *
     * @param parent the class loader that should be the parent of this class loader
     */
    public ExtensionClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    /**
     * Load a JAR file located on the provided path.
     *
     * @param jarPath the path of the JAR file to load
     * @throws java.io.IOError if an I/O error occurs while obtaining the absolute path of the provided path
     * @throws MalformedURLException if an error occurred while converting the provided path to a URL
     * @throws NullPointerException if the provided path is null
     */
    public void addJar(Path jarPath) throws MalformedURLException {
        synchronized (this) {
            addURL(jarPath.toUri().toURL());
            logger.debug("File {} loaded by extension class loader", jarPath);

            String jarName = jarPath.getFileName().toString();
            String nameWithoutVersion = FileTools.stripVersionFromFileName(jarName);
            if (addedJars.contains(nameWithoutVersion)) {
                logger.warn(
                        "A JAR file with the same name ({}) was already added to this class loader. {} will probably not be loaded",
                        nameWithoutVersion,
                        jarName
                );
            }
            addedJars.add(nameWithoutVersion);
        }

        List<Runnable> runnables;
        synchronized (this) {
            runnables = new ArrayList<>(this.runnables);
        }

        for (Runnable runnable: runnables) {
            try {
                runnable.run();
            } catch (Exception e) {
                logger.error("Error when calling runnable of class loader", e);
            }
        }
    }

    /**
     * Set a runnable to be called each time a JAR file is loaded by this class loader. The call may happen from any thread.
     *
     * @param runnable the runnable to run when a JAR file is loaded
     * @throws NullPointerException if the provided path is null
     */
    public synchronized void addOnJarLoadedRunnable(Runnable runnable) {
        runnables.add(Objects.requireNonNull(runnable));
    }
}
