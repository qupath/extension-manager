package qupath.ext.extensionmanager.core.catalog;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * A class to fetch a catalog.
 */
public class CatalogFetcher {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES) // convert snake case to camel case
            .create();

    private CatalogFetcher() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * Attempt to get a catalog from the provided URL.
     *
     * @param uri the URI pointing to the raw content of the catalog
     * @return a CompletableFuture with the catalog or a failed CompletableFuture if the provided URL doesn't point to
     * a valid catalog
     */
    public static CompletableFuture<Catalog> getCatalog(URI uri) {
        if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    String.format("Unknown scheme %s in %s", uri.getScheme(), uri)
            ));
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        return httpClient.sendAsync(
                        HttpRequest.newBuilder()
                                .uri(uri)
                                .timeout(REQUEST_TIMEOUT)
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString()
                )
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException(String.format(
                                "Request to %s failed with status code %d.", uri, response.statusCode()
                        ));
                    }

                    Catalog catalog = gson.fromJson(response.body(), Catalog.class);
                    if (catalog == null) {
                        throw new RuntimeException(String.format("The response to %s is empty.", uri));
                    }
                    try {
                        catalog.checkValidity();
                    } catch (IllegalStateException e) {
                        throw new RuntimeException(
                                String.format(String.format("The response to %s doesn't contain a valid Catalog", uri)),
                                e
                        );
                    }

                    return catalog;
                })
                .whenComplete((i, e) -> httpClient.close());
    }
}
