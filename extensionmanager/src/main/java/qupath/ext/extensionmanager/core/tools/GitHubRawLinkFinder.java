package qupath.ext.extensionmanager.core.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class to find the link pointing to the raw content of a file hosted on a GitHub repository.
 */
public class GitHubRawLinkFinder {

    private static final Logger logger = LoggerFactory.getLogger(GitHubRawLinkFinder.class);
    private static final String GITHUB_HOST = "github.com";
    private static final Pattern REPOSITORY_PATTERN = Pattern.compile("^/([^/]+)/([^/]+)(?:/[^/]+/[^/]+/(.*))?");
    private static final String GET_REPOSITORY_CONTENT_URL = "https://api.github.com/repos/%s/%s/contents/%s";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Gson gson = new GsonBuilder().create();
    private record GitHubRepository(String username, String repository, String path) {}
    private record FileEntry(String name, URI download_url) {}

    private GitHubRawLinkFinder() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * Get the link pointing to the raw content of the provided file within the provided GitHub
     * repository.
     *
     * @param url the URL pointing to the GitHub repository containing the file to find. It can be a link to any directory
     *            or file within the repository. If it's a link to a directory, all direct children of this directory will
     *            be searched. If it's a link to a file, only this file will be searched. It must contain "http" or "https"
     * @param filePredicate a predicate on the name of the file to find
     * @return a CompletableFuture with the link pointing to the raw content of the desired file, or a failed CompletableFuture
     * if it couldn't be retrieved
     */
    public static CompletableFuture<URI> getRawLinkOfFileInRepository(String url, Predicate<String> filePredicate) {
        if (url == null) {
            return CompletableFuture.failedFuture(new NullPointerException(
                    "The provided URL is null"
            ));
        }

        GitHubRepository gitHubRepository;
        try {
            gitHubRepository = getGitHubRepository(url);
        } catch (IllegalArgumentException e) {
            return CompletableFuture.failedFuture(e);
        }
        logger.debug("{} found in {}", gitHubRepository, url);

        URI uri;
        try {
            uri = new URI(String.format(
                    GET_REPOSITORY_CONTENT_URL,
                    gitHubRepository.username(),
                    gitHubRepository.repository(),
                    gitHubRepository.path()
            ));
        } catch (URISyntaxException e) {
            return CompletableFuture.failedFuture(e);
        }

        if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    String.format("Unknown scheme %s in %s", uri.getScheme(), uri)
            ));
        }

        HttpClient client = HttpClient
                .newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        logger.debug("Sending request to {}", uri);
        return client.sendAsync(
                HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(REQUEST_TIMEOUT)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        ).thenApply(response -> {
            if (response.statusCode() != 200) {
                throw new RuntimeException(String.format(
                        "Request to %s failed with status code %d.", uri, response.statusCode()
                ));
            }
            logger.debug("Got response from {} with status 200:\n{}", uri, response.body());

            return getFileEntries(uri, response.body()).stream()
                    .filter(fileEntry -> {
                        if (fileEntry.name != null && fileEntry.download_url != null) {
                            logger.debug("File entry {} retained because it has a name and a download URL", fileEntry);
                            return true;
                        } else {
                            logger.debug("File entry {} skipped because it lacks a name or a download URL", fileEntry);
                            return false;
                        }
                    })
                    .filter(fileEntry -> {
                        if (filePredicate.test(fileEntry.name)) {
                            logger.debug("File entry {} retained because it matches the predicate", fileEntry);
                            return true;
                        } else {
                            logger.debug("File entry {} skipped because it doesn't match the predicate", fileEntry);
                            return false;
                        }
                    })
                    .findAny()
                    .map(FileEntry::download_url)
                    .orElseThrow(() -> new NoSuchElementException(String.format(
                            "The response to %s doesn't contain any file name matching the required name",
                            uri
                    )));
        }).whenComplete((rawLink, error) -> client.shutdown());
    }

    private static GitHubRepository getGitHubRepository(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

        if (!Objects.equals(uri.getHost(), GITHUB_HOST)) {
            throw new IllegalArgumentException(String.format("The provided URI %s is not coming from %s", uri, GITHUB_HOST));
        }

        String path = uri.getPath();
        if (path == null) {
            throw new IllegalArgumentException(String.format("The provided URI %s has no path", uri));
        }

        Matcher matcher = REPOSITORY_PATTERN.matcher(path);

        if (matcher.find() && matcher.groupCount() > 2) {
            return new GitHubRepository(
                    matcher.group(1),
                    matcher.group(2),
                    Objects.requireNonNullElse(matcher.group(3), "")
            );
        } else {
            throw new IllegalArgumentException(String.format("Username or repository not found in %s", uri));
        }
    }

    private static List<FileEntry> getFileEntries(URI uri, String body) {
        try {
            return Objects.requireNonNull(
                    gson.fromJson(body, new TypeToken<List<FileEntry>>(){}.getType()),
                    String.format("The response to %s is empty.", uri)
            );
        } catch (JsonSyntaxException e) {
            logger.debug("{} is not a list of file entries. Trying to see if it's a file entry", body, e);

            FileEntry fileEntry = Objects.requireNonNull(
                    gson.fromJson(body, FileEntry.class),
                    String.format("The response to %s is empty.", uri)
            );
            logger.debug("{} is a file entry", body);

            return List.of(fileEntry);
        }
    }
}
