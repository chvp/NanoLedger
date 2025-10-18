package be.chvp.nanoledger.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import be.chvp.nanoledger.R
import be.chvp.nanoledger.ui.main.MainActivity
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.TimeUnit

class FileManagerRule(
    private val composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>,
    private val context: Context = ApplicationProvider.getApplicationContext(),
) : TestRule {
    private val createdUris = mutableListOf<Uri>()

    private val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    fun configureAppWithFile(filename: String) {
        val testContext = InstrumentationRegistry.getInstrumentation().context

        insertIntoDownloads(
            context = context,
            displayName = filename,
            mimeType = "application/octet-stream",
            data = testContext.assets.open(filename).use { it.readBytes() },
        )

        composeRule.onNodeWithContentDescription(context.getString(R.string.settings)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Select file…").assertIsDisplayed().performClick()

        waitForPicker()

        device.findObject(By.descContains("Show roots")).click()
        device.wait(Until.hasObject(By.text("Downloads")), 3_000)
        device.findObject(By.text("Downloads")).click()
        device.wait(Until.hasObject(By.text(filename)), 5_000)
        device.findObject(By.text(filename)).click()

        composeRule.onNodeWithText(context.getString(R.string.settings)).assertIsDisplayed()

        val prefs = context.getSharedPreferences("be.chvp.nanoledger.preferences", Context.MODE_PRIVATE)
        val uriString = prefs.getString("file_uri", null)
        require(!uriString.isNullOrBlank()) { "file_uri preference was not set by the app" }

        composeRule.onNodeWithText(uriString).assertIsDisplayed()

        device.pressBack()
    }

    private fun insertIntoDownloads(
        context: Context,
        displayName: String,
        mimeType: String,
        data: ByteArray,
    ) {
        val resolver = context.contentResolver

        val externalUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI

        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            }

        val uri = resolver.insert(externalUri, values) ?: error("Failed to insert into MediaStore: $displayName")

        resolver.openOutputStream(uri)?.use { os ->
            os.write(data)
            os.flush()
        } ?: error("Failed to open output stream for $uri")

        createdUris += uri
    }

    private fun waitForPicker() {
        device.waitForIdle(TimeUnit.SECONDS.toMillis(2))
        device.wait(Until.findObject(By.descContains("Show roots")), 5_000)
    }

    private fun cleanup() {
        val resolver = context.contentResolver
        createdUris.forEach { runCatching { resolver.delete(it, null, null) } }
        createdUris.clear()
    }

    override fun apply(
        base: Statement,
        description: Description,
    ): Statement =
        object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } finally {
                    cleanup()
                }
            }
        }
}
