package qupath.ext.extensionmanager.core.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * A class to extract a ZIP file and get information on the extraction progress.
 */
public class ZipExtractor {

    private static final Logger logger = LoggerFactory.getLogger(ZipExtractor.class);
    private static final int BUFFER_SIZE = 1024;

    private ZipExtractor() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * Extract files from the provided ZIP file path and place them in the provided output folder. This function may take
     * a lot of time depending on the ZIP file size, but is cancellable.
     *
     * @param inputZipPath the path of the ZIP file to extract
     * @param outputFolderPath the path of a folder that should contain the extracted files
     * @param onProgress a function that will be called at different steps during the extraction. Its parameter will be a
     *                   float between 0 and 1 indicating the progress of the extraction (0: beginning, 1: finished). This
     *                   function will be called from the calling thread
     * @throws IOException if an I/O error has occurred while opening the ZIP file or extracting the files, or if the
     * output directory cannot be created
     * @throws InterruptedException if the running thread is interrupted
     * @throws java.util.zip.ZipException if a ZIP format error has occurred when opening the ZIP file
     * @throws NullPointerException if one of the provided parameter is null
     */
    public static void extractZipToFolder(Path inputZipPath, Path outputFolderPath, Consumer<Float> onProgress) throws IOException, InterruptedException {
        File outputFolder = outputFolderPath.toFile();

        byte[] buffer = new byte[BUFFER_SIZE];
        int numberOfZipFiles = getNumberOfFilesInZip(inputZipPath);

        logger.debug("Starting extracting {} files from {} to {}", numberOfZipFiles, inputZipPath, outputFolderPath);
        try (
                InputStream inputStream = Files.newInputStream(inputZipPath);
                ZipInputStream zipInputStream = new ZipInputStream(inputStream)
        ) {
            ZipEntry zipEntry;
            int i = 0;

            while ((zipEntry= zipInputStream.getNextEntry()) != null) {
                File newFile = createFile(outputFolder, zipEntry);

                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // This is necessary for archives created on Windows, where the root directories
                    // don’t have a corresponding entry in the zip file
                    File parent = newFile.getParentFile();
                    if (parent == null || !parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    try (OutputStream outputStream = Files.newOutputStream(newFile.toPath())) {
                        int len;
                        while ((len = zipInputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, len);

                            if (Thread.interrupted()) {
                                throw new InterruptedException(String.format("Extraction of %s interrupted", inputZipPath));
                            }
                        }
                    }
                    logger.debug("File {} extracted to {}", zipEntry, newFile);
                }

                onProgress.accept((float) i / numberOfZipFiles);
                i++;
            }
        }
    }

    private static int getNumberOfFilesInZip(Path zipPath) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            return zipFile.size();
        }
    }

    /**
     * Create a file object by adding the name of the provided ZIP entry to the provided destination directory.
     * <p>
     * This method guards against writing files to the file system outside the target folder
     * (<a href="https://snyk.io/research/zip-slip-vulnerability">Zip Slip</a> vulnerability).
     *
     * @param destinationDirectory the parent directory of the file to create
     * @param zipEntry the zip entry containing the name of the file to create
     * @return the created file
     * @throws IOException if an I/O error has occurred creating the file or when the created file was outside the target
     * folder
     */
    private static File createFile(File destinationDirectory, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDirectory, zipEntry.getName());

        String destinationDirPath = destinationDirectory.getCanonicalPath();
        String destinationFilePath = destFile.getCanonicalPath();

        if (!destinationFilePath.startsWith(destinationDirPath + File.separator)) {
            throw new IOException(String.format(
                    "The ZIP entry %s is outside of the target directory %s",
                    zipEntry.getName(),
                    destinationDirectory
            ));
        }

        return destFile;
    }
}
