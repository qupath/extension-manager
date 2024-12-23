package qupath.ext.extensionmanager;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A web server that serves static files.
 * It must be {@link #close() closed} once no longer used.
 */
public class SimpleServer implements AutoCloseable {

    private static final String HOSTNAME = "localhost";
    private static final AtomicInteger portCounter = new AtomicInteger(8080);
    private final int port;
    private final HttpServer server;
    /**
     * Represents a file to serve from this server.
     *
     * @param name the name of the file
     * @param content a stream pointing to the content of the file
     */
    public record FileToServe(String name, InputStream content) {}

    /**
     * Create the server.
     *
     * @param files the files to serve
     * @throws IOException if the files cannot be copied
     * @throws SecurityException if the user doesn't have enough rights to create a temporary directory
     * @throws java.nio.file.InvalidPathException if a path to a file cannot be created
     * @throws java.io.UncheckedIOException if an I/O error occurs
     */
    public SimpleServer(List<FileToServe> files) throws IOException {
        this.port = portCounter.getAndIncrement();

        Path serverFilesPath = Files.createTempDirectory(null);
        for (FileToServe file: files) {
            Files.copy(file.content(), serverFilesPath.resolve(file.name()));
        }

        server = SimpleFileServer.createFileServer(
                new InetSocketAddress(HOSTNAME, this.port),
                serverFilesPath,
                SimpleFileServer.OutputLevel.INFO
        );
        server.start();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    /**
     * Get the link from where this server serves the file with the provided name.
     * The link is not guaranteed to work if the provided file is not served by this
     * server.
     *
     * @param fileName the name of the file that should be served from the given URI
     * @return the URI pointing to the provided file
     * @throws IllegalArgumentException if the URI cannot be constructed
     */
    public URI getURI(String fileName) {
        return URI.create(String.format("http://%s:%d/%s", HOSTNAME, port, fileName));
    }
}
