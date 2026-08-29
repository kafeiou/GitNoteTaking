package inmethod.gitnotetaking.test;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

public class BackupZipUnitTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testNativeZipBackup() throws Exception {
        File repoDir = tempFolder.newFolder("note_repo");
        File note1 = new File(repoDir, "my_note.md");
        try (FileWriter writer = new FileWriter(note1)) {
            writer.write("# Hello Note");
        }

        File attachDir = new File(repoDir, "my_note.md_attach");
        attachDir.mkdirs();
        File photo = new File(attachDir, "test.png");
        try (FileWriter writer = new FileWriter(photo)) {
            writer.write("fake photo bytes");
        }

        File zipOutput = tempFolder.newFile("backup.zip");

        // Execute zip
        try (FileOutputStream fos = new FileOutputStream(zipOutput);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos)) {
            zipDirectory(repoDir, repoDir, zos);
            zos.flush();
        }

        assertTrue("Zip 檔案必須成功建立且大於 0 bytes", zipOutput.exists() && zipOutput.length() > 0);

        // Verify zip contents
        try (ZipFile zipFile = new ZipFile(zipOutput)) {
            assertNotNull("應包含 my_note.md", zipFile.getEntry("my_note.md"));
            assertNotNull("應包含 my_note.md_attach/test.png", zipFile.getEntry("my_note.md_attach/test.png"));
        }
    }

    private void zipDirectory(File rootDir, File sourceFile, ZipOutputStream zos) throws IOException {
        if (sourceFile.isDirectory()) {
            File[] files = sourceFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    zipDirectory(rootDir, file, zos);
                }
            }
        } else {
            String relativePath = rootDir.toURI().relativize(sourceFile.toURI()).getPath();
            ZipEntry zipEntry = new ZipEntry(relativePath);
            zipEntry.setTime(sourceFile.lastModified());
            zos.putNextEntry(zipEntry);
            try (FileInputStream fis = new FileInputStream(sourceFile);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = bis.read(buffer)) >= 0) {
                    zos.write(buffer, 0, length);
                }
            }
            zos.closeEntry();
        }
    }
}
