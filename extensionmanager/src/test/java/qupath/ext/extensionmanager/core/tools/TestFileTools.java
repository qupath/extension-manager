package qupath.ext.extensionmanager.core.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestFileTools {

    @Test
    void Check_Directory_Not_Empty_When_Null() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> FileTools.isDirectoryNotEmpty(null)
        );
    }

    @Test
    void Check_Directory_Not_Empty_When_Exists_And_Empty() throws IOException {
        Path directory = Files.createTempDirectory(null);

        boolean isDirectoryNotEmpty = FileTools.isDirectoryNotEmpty(directory);

        Assertions.assertFalse(isDirectoryNotEmpty);
    }

    @Test
    void Check_Directory_Not_Empty_When_Exists_And_Not_Empty() throws IOException {
        Path directory = Files.createTempDirectory(null);
        Files.createFile(directory.resolve("file"));

        boolean isDirectoryNotEmpty = FileTools.isDirectoryNotEmpty(directory);

        Assertions.assertTrue(isDirectoryNotEmpty);
    }

    @Test
    void Check_Directory_Not_Empty_When_Does_Not_Exist() throws IOException {
        Path directory = Files.createTempDirectory(null);
        Files.delete(directory);

        boolean isDirectoryNotEmpty = FileTools.isDirectoryNotEmpty(directory);

        Assertions.assertFalse(isDirectoryNotEmpty);
    }

    @Test
    void Check_Directory_Not_Empty_When_File() throws IOException {
        Path file = Files.createTempFile(null, null);

        boolean isDirectoryNotEmpty = FileTools.isDirectoryNotEmpty(file);

        Assertions.assertFalse(isDirectoryNotEmpty);
    }

    @Test
    void Check_File_Deletion_When_Null() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> FileTools.moveDirectoryToTrashOrDeleteRecursively(null)
        );
    }

    @Test
    void Check_File_Deleted() throws IOException {
        Path file = Files.createTempFile(null, null);

        FileTools.moveDirectoryToTrashOrDeleteRecursively(file.toFile());

        Assertions.assertFalse(Files.exists(file));
    }

    @Test
    void Check_Empty_Directory_Deleted() throws IOException {
        Path directory = Files.createTempDirectory(null);

        FileTools.moveDirectoryToTrashOrDeleteRecursively(directory.toFile());

        Assertions.assertFalse(Files.exists(directory));
    }

    @Test
    void Check_Non_Empty_Directory_Deleted() throws IOException {
        Path directory = Files.createTempDirectory(null);
        Files.createFile(directory.resolve("file"));

        FileTools.moveDirectoryToTrashOrDeleteRecursively(directory.toFile());

        Assertions.assertFalse(Files.exists(directory));
    }

    @Test
    void Check_File_Name_From_URI_When_Null() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> FileTools.getFileNameFromURI(null)
        );
    }

    @Test
    void Check_File_Name_From_URI_When_Not_Present() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> FileTools.getFileNameFromURI(URI.create("http://test/"))
        );
    }

    @Test
    void Check_File_Name_From_URI_When_Present() {
        String expectedFileName = "file.test";
        URI uri = URI.create(String.format("http://test/%s", expectedFileName));

        String fileName = FileTools.getFileNameFromURI(uri);

        Assertions.assertEquals(expectedFileName, fileName);
    }

    @Test
    void Check_File_Name_From_URI_When_Present_With_Complex_Path() {
        String expectedFileName = "file.test";
        URI uri = URI.create(String.format("http://test/folder/%s", expectedFileName));

        String fileName = FileTools.getFileNameFromURI(uri);

        Assertions.assertEquals(expectedFileName, fileName);
    }

    @Test
    void Check_File_Parent_Of_Another_File_When_Child_Null() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> FileTools.isFileParentOfAnotherFile(null, null)
        );
    }

    @Test
    void Check_File_Not_Parent_Of_Another_File() {
        File file = new File("some_file");
        File otherFile = new File("some_other_file");

        boolean isParent = FileTools.isFileParentOfAnotherFile(file, otherFile);

        Assertions.assertFalse(isParent);
    }

    @Test
    void Check_File_Parent_Of_Another_File_When_Child() {
        File file = new File("some_file");
        File childFile = new File("some_file/some_other_file");

        boolean isParent = FileTools.isFileParentOfAnotherFile(file, childFile);

        Assertions.assertTrue(isParent);
    }

    @Test
    void Check_File_Parent_Of_Another_File_When_Descendant() {
        File file = new File("some_file");
        File childFile = new File("some_file/some_folder/some_other_file");

        boolean isParent = FileTools.isFileParentOfAnotherFile(file, childFile);

        Assertions.assertTrue(isParent);
    }
}
