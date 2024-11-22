package qupath.ext.extensionmanager.core.indexmodel;

import java.net.URL;

/**
 * Utility functions used for model validation.
 */
class Utils {

    private static final String GITHUB_HOST = "github.com";
    private Utils() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * Check that a field is not null.
     *
     * @param field the field to check
     * @param fieldName the name of the field (used for logging)
     * @param objectName the name of the object containing the provided field (used for logging)
     * @throws IllegalStateException when the provided field is null
     */
    public static void checkField(Object field, String fieldName, String objectName) {
        if (field == null) {
            throw new IllegalStateException(String.format("'%s' field not found in %s", fieldName, objectName));
        }
    }

    /**
     * Check that a URL comes from github.com.
     *
     * @param url the URL to check
     * @throws IllegalStateException when the provided URL is not coming from github.com
     */
    public static void checkGithubURL(URL url) {
        if (!GITHUB_HOST.equals(url.getHost())) {
            throw new IllegalStateException(String.format(
                    "The %s URL is not a %s URL", url, GITHUB_HOST
            ));
        }
    }
}
