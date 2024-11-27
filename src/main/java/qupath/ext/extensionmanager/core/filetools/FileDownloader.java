package qupath.ext.extensionmanager.core.filetools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * A class to download a file and get information on the download progress.
 */
public class FileDownloader {

    private static final Logger logger = LoggerFactory.getLogger(FileDownloader.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String CONTENT_LENGTH_ATTRIBUTE = "content-length";
    private static final int BUFFER_SIZE = 4096;

    private FileDownloader() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * Download a file located at the provided URI and place it on the provided path.
     * This function may take a lot of time depending on the internet connection and the
     * size of the download.
     *
     * @param uri the URI pointing to the file to download. It must contain "http" or "https"
     * @param outputPath the path the downloaded file should have. It will be overridden if it already exists
     * @param onProgress a function that will be called at different steps during the download. Its parameter
     *                   will be a float between 0 and 1 indicating the progress of the download (0: beginning,
     *                   1: finished). This function will be called from the calling thread
     * @throws IOException if an I/O error occurs when sending the request or receiving the file
     * @throws InterruptedException if the download is interrupted
     * @throws IllegalArgumentException if the provided URI does not contain a valid scheme ("http" or "https")
     * @throws java.io.FileNotFoundException if the downloaded file already exists but is a directory rather than a regular
     * file, does not exist but cannot be created, or cannot be opened for any other reason
     * @throws SecurityException if a security manager exists and its checkWrite method denies write access to the file
     */
    public static void downloadFile(URI uri, Path outputPath, Consumer<Float> onProgress) throws IOException, InterruptedException {
        if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) {
            throw (new IllegalArgumentException(
                    String.format("Unknown scheme %s in %s", uri.getScheme(), uri)
            ));
        }

        try (
                HttpClient client = HttpClient
                        .newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .build()
        ) {
            HttpResponse<InputStream> response = client.send(
                    HttpRequest.newBuilder()
                            .uri(uri)
                            .timeout(REQUEST_TIMEOUT)
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            OptionalInt contentLength = getContentLength(response);

            try (
                    InputStream inputStream = response.body();
                    FileOutputStream fileOutputStream = new FileOutputStream(outputPath.toFile())
            ) {
                byte[] buffer = new byte[BUFFER_SIZE];
                long bytesDownloaded = 0L;

                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) > -1) {
                    bytesDownloaded += bytesRead;

                    if (contentLength.isPresent()) {
                        onProgress.accept((float) bytesDownloaded / contentLength.getAsInt());
                    }

                    fileOutputStream.write(buffer, 0, bytesRead);

                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                }
            }
        }
    }

    private static OptionalInt getContentLength(HttpResponse<?> response) {
        if (!response.headers().map().containsKey(CONTENT_LENGTH_ATTRIBUTE)) {
            logger.debug(String.format(
                    "%s not found in %s. Cannot indicate progress of %s download",
                    CONTENT_LENGTH_ATTRIBUTE,
                    response.headers().map(),
                    response.uri()
            ));
            return OptionalInt.empty();
        }
        List<String> contentLengthEntry = response.headers().map().get(CONTENT_LENGTH_ATTRIBUTE);

        if (contentLengthEntry.isEmpty()) {
            logger.debug(String.format(
                    "%s empty in %s. Cannot indicate progress of %s download",
                    CONTENT_LENGTH_ATTRIBUTE,
                    response.headers().map(),
                    response.uri()
            ));
            return OptionalInt.empty();
        }
        String contentLength = contentLengthEntry.get(0);

        try {
            return OptionalInt.of(Integer.parseInt(contentLength));
        } catch (NumberFormatException e) {
            logger.debug(String.format(
                    "The %s header %s cannot be converted to a number. Cannot indicate progress of %s download",
                    CONTENT_LENGTH_ATTRIBUTE,
                    contentLength,
                    response.uri()
            ));
            return OptionalInt.empty();
        }
    }
}
