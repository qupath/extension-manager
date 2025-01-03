package qupath.ext.extensionmanager.core.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.ExecutionException;

/**
 * This class needs a public GitHub repository for testing. Currently,
 * this <a href="https://github.com/qupath/qupath-macOS-extension/tree/master">repository</a> is used because
 * it was archived and unlikely to be changed / removed. If that were to happen, the static final fields of this class
 * would need to be changed to a new repository.
 * <p>
 * Note that functions tested in this class use the GitHub API, which has restrictions on the number of requests per
 * hour. Therefore, some tests may start to fail if they are run several times in a row.
 */
public class TestGitHubRawLinkFinder {

    private static final String REPO_URI = "https://github.com/qupath/qupath-macOS-extension";
    private static final String FILE_IN_ROOT = ".gitignore";
    private static final URI FILE_IN_ROOT_RAW_LINK = URI.create("https://raw.githubusercontent.com/qupath/qupath-macOS-extension/master/.gitignore");
    private static final String FILE_IN_ROOT_LINK = "https://github.com/qupath/qupath-macOS-extension/blob/master/.gitignore";
    private static final String REPO_SUB_FOLDER = "https://github.com/qupath/qupath-macOS-extension/tree/master/src/main/resources/META-INF/services";
    private static final String FILE_IN_SUB_FOLDER = "qupath.lib.gui.extensions.QuPathExtension";
    private static final URI FILE_IN_SUB_FOLDER_RAW_LINK = URI.create("https://raw.githubusercontent.com/qupath/qupath-macOS-extension/master/src/main/resources/META-INF/services/qupath.lib.gui.extensions.QuPathExtension");
    private static final String FILE_IN_SUB_FOLDER_LINK = "https://github.com/qupath/qupath-macOS-extension/blob/master/src/main/resources/META-INF/services/qupath.lib.gui.extensions.QuPathExtension";

    @Test
    void Check_Null_Url() {
        Assertions.assertThrows(
                ExecutionException.class,
                () -> GitHubRawLinkFinder.getRawLinkOfFileInRepository(
                        null,
                        file -> true
                ).get()
        );
    }

    @Test
    void Check_Null_Predicate() {
        Assertions.assertThrows(
                ExecutionException.class,
                () -> GitHubRawLinkFinder.getRawLinkOfFileInRepository(
                        REPO_URI,
                        null
                ).get()
        );
    }

    @Test
    void Check_File_Not_Present_In_Repository() {
        Assertions.assertThrows(
                ExecutionException.class,
                () -> GitHubRawLinkFinder.getRawLinkOfFileInRepository(
                        REPO_URI,
                        "some_file_not_present_in_repo"::equals
                ).get()
        );
    }

    @Test
    void Check_File_In_Root_With_Parent_Folder_Link() throws ExecutionException, InterruptedException {
        URI fileUri = GitHubRawLinkFinder.getRawLinkOfFileInRepository(
                REPO_URI,
                FILE_IN_ROOT::equals
        ).get();

        Assertions.assertEquals(FILE_IN_ROOT_RAW_LINK, fileUri);
    }

    @Test
    void Check_File_In_Root_With_File_Link() throws ExecutionException, InterruptedException {
        URI fileUri = GitHubRawLinkFinder.getRawLinkOfFileInRepository(
                FILE_IN_ROOT_LINK,
                FILE_IN_ROOT::equals
        ).get();

        Assertions.assertEquals(FILE_IN_ROOT_RAW_LINK, fileUri);
    }

    @Test
    void Check_File_In_Sub_Folder_With_Parent_Folder_Link() throws ExecutionException, InterruptedException {
        URI fileUri = GitHubRawLinkFinder.getRawLinkOfFileInRepository(
                REPO_SUB_FOLDER,
                FILE_IN_SUB_FOLDER::equals
        ).get();

        Assertions.assertEquals(FILE_IN_SUB_FOLDER_RAW_LINK, fileUri);
    }

    @Test
    void Check_File_In_Sub_Folder_With_File_Link() throws ExecutionException, InterruptedException {
        URI fileUri = GitHubRawLinkFinder.getRawLinkOfFileInRepository(
                FILE_IN_SUB_FOLDER_LINK,
                FILE_IN_SUB_FOLDER::equals
        ).get();

        Assertions.assertEquals(FILE_IN_SUB_FOLDER_RAW_LINK, fileUri);
    }
}
