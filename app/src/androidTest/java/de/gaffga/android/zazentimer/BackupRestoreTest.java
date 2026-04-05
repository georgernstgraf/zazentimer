package de.gaffga.android.zazentimer;

import android.content.Context;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertTrue;

/**
 * Instrumented tests for backup/restore logic.
 * Tests the core backup/restore file operations directly without SAF UI.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class BackupRestoreTest {

    @Rule
    public ActivityScenarioRule<ZazenTimerActivity> activityRule =
            new ActivityScenarioRule<>(ZazenTimerActivity.class);

    @Test
    public void testBackupCreatesValidZip() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        File tempBackup = new File(context.getCacheDir(), "test_backup.zip");
        if (tempBackup.exists()) tempBackup.delete();

        // Create a backup zip mimicking the SettingsFragment.doRealBackup() logic
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempBackup));

        // Add database file
        File dbFile = context.getDatabasePath("zentimer");
        if (dbFile.exists()) {
            zos.putNextEntry(new ZipEntry("zentimer"));
            writeFileToZip(dbFile, zos);
            zos.closeEntry();
        }

        // Add app files
        File filesDir = context.getFilesDir();
        File[] files = filesDir.listFiles(f -> !f.getName().equals("InstantRun"));
        if (files != null) {
            for (File file : files) {
                zos.putNextEntry(new ZipEntry(file.getName()));
                writeFileToZip(file, zos);
                zos.closeEntry();
            }
        }
        zos.close();

        assertTrue("Backup file should exist", tempBackup.exists());
        assertTrue("Backup file should not be empty", tempBackup.length() > 0);

        ZipFile zipFile = new ZipFile(tempBackup);
        assertTrue("Zip should contain entries", zipFile.size() > 0);
        zipFile.close();

        tempBackup.delete();
    }

    @Test
    public void testRestoreFromZip() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Create a test zip with a fake database entry
        File tempBackup = new File(context.getCacheDir(), "test_restore.zip");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempBackup));
        zos.putNextEntry(new ZipEntry("zentimer"));
        zos.write("test-db-data".getBytes());
        zos.closeEntry();
        zos.putNextEntry(new ZipEntry("test_file.txt"));
        zos.write("test-file-content".getBytes());
        zos.closeEntry();
        zos.close();

        // Verify zip is readable and contains expected entries
        ZipFile zipFile = new ZipFile(tempBackup);
        assertTrue("Zip should have 2 entries", zipFile.size() == 2);

        ZipEntry dbEntry = zipFile.getEntry("zentimer");
        assertTrue("Should find zentimer entry", dbEntry != null);

        // Simulate restore: extract to temp dir
        File restoreDir = new File(context.getCacheDir(), "test_restore");
        restoreDir.mkdirs();
        java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File outFile = new File(restoreDir, entry.getName());
            FileOutputStream fos = new FileOutputStream(outFile);
            java.io.InputStream is = zipFile.getInputStream(entry);
            byte[] buf = new byte[32768];
            int read;
            while ((read = is.read(buf)) > 0) {
                fos.write(buf, 0, read);
            }
            fos.close();
            is.close();
            assertTrue("Extracted file should exist", outFile.exists());
            assertTrue("Extracted file should not be empty", outFile.length() > 0);
        }
        zipFile.close();

        // Cleanup
        new File(restoreDir, "zentimer").delete();
        new File(restoreDir, "test_file.txt").delete();
        restoreDir.delete();
        tempBackup.delete();
    }

    private void writeFileToZip(File file, ZipOutputStream zos) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        byte[] buf = new byte[32768];
        int read;
        while ((read = fis.read(buf)) > 0) {
            zos.write(buf, 0, read);
        }
        fis.close();
    }
}
