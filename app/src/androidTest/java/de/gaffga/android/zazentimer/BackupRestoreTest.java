package de.gaffga.android.zazentimer;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.gaffga.android.zazentimer.screens.MainPage;
import de.gaffga.android.zazentimer.screens.SettingsPage;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class BackupRestoreTest {

    @Rule
    public ActivityScenarioRule<ZazenTimerActivity> activityRule =
            new ActivityScenarioRule<>(ZazenTimerActivity.class);

    @Before
    public void setup() {
        Intents.init();
    }

    @After
    public void teardown() {
        Intents.release();
    }

    /**
     * Test that backup writes a valid zip via SAF.
     * Mocks the SAF picker by providing a temp file URI as the "user's chosen location".
     */
    @Test
    public void testBackupCreatesValidZip() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Create a temp file to receive the backup
        File tempBackup = new File(context.getCacheDir(), "test_backup.zip");
        if (tempBackup.exists()) tempBackup.delete();
        Uri backupUri = Uri.fromFile(tempBackup);

        Intents.intending(
                org.hamcrest.Matchers.allOf(
                        androidx.test.espresso.intent.matcher.IntentMatchers.hasAction(Intent.ACTION_CREATE_DOCUMENT)
                )
        ).respondWith(
                new Instrumentation.ActivityResult(Activity.RESULT_OK,
                        new Intent().setData(backupUri))
        );

        // Navigate to settings
        MainPage mainPage = new MainPage();
        mainPage.verifyMainScreenIsDisplayed();
        androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu(context);
        androidx.test.espresso.Espresso.onView(
                androidx.test.espresso.matcher.ViewMatchers.withText(R.string.menu_settings)
        ).perform(androidx.test.espresso.action.ViewActions.click());

        // Trigger backup
        SettingsPage settingsPage = new SettingsPage();
        settingsPage.clickBackup();

        // Wait for async backup
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}

        // Verify backup file exists and is a valid zip
        assertTrue("Backup file should exist", tempBackup.exists());
        assertTrue("Backup file should not be empty", tempBackup.length() > 0);

        // Verify it's a valid zip by reading entries
        try {
            java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(tempBackup);
            assertTrue("Zip should contain at least one entry", zipFile.size() > 0);
            zipFile.close();
        } catch (Exception e) {
            throw new AssertionError("Backup file should be a valid zip: " + e.getMessage());
        }

        tempBackup.delete();
    }

    /**
     * Test that restore reads a zip via SAF.
     * Creates a minimal backup zip, then feeds it through the SAF restore flow.
     */
    @Test
    public void testRestoreReadsFromZip() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Create a minimal valid backup zip
        File tempBackup = new File(context.getCacheDir(), "test_restore.zip");
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempBackup));
            zos.putNextEntry(new ZipEntry("zentimer"));
            zos.write("test-db-data".getBytes());
            zos.closeEntry();
            zos.close();
        } catch (Exception e) {
            throw new AssertionError("Failed to create test zip: " + e.getMessage());
        }

        Uri backupUri = Uri.fromFile(tempBackup);

        Intents.intending(
                org.hamcrest.Matchers.allOf(
                        androidx.test.espresso.intent.matcher.IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT)
                )
        ).respondWith(
                new Instrumentation.ActivityResult(Activity.RESULT_OK,
                        new Intent().setData(backupUri))
        );

        // Navigate to settings
        MainPage mainPage = new MainPage();
        mainPage.verifyMainScreenIsDisplayed();
        androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu(context);
        androidx.test.espresso.Espresso.onView(
                androidx.test.espresso.matcher.ViewMatchers.withText(R.string.menu_settings)
        ).perform(androidx.test.espresso.action.ViewActions.click());

        // Trigger restore (this clicks restore + confirms the dialog)
        SettingsPage settingsPage = new SettingsPage();
        settingsPage.clickRestoreAndConfirm();

        // Wait for async restore
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}

        tempBackup.delete();
    }
}
