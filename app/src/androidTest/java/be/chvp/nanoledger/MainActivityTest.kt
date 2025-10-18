package be.chvp.nanoledger

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import be.chvp.nanoledger.ui.main.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val mediaStoreSeederRule = MediaStoreSeederRule()

    private val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    private lateinit var appContext: Context

    @Before
    fun setupFile() {
        appContext = ApplicationProvider.getApplicationContext()

        // Load bytes from androidTest asset
        val testContext = InstrumentationRegistry.getInstrumentation().context

        mediaStoreSeederRule.insertIntoDownloads(
            context = appContext,
            displayName = "test.journal",
            mimeType = "application/octet-stream",
            data = testContext.assets.open("test.journal").use { it.readBytes() },
        )

        composeRule.onNodeWithText("Go to the settings to configure one.").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Select file…").assertIsDisplayed().performClick()

        waitForPicker()

        device.findObject(By.descContains("Show roots")).click()
        device.wait(Until.hasObject(By.text("Downloads")), 3_000)
        device.findObject(By.text("Downloads")).click()
        device.wait(Until.hasObject(By.text("test.journal")), 5_000)
        device.findObject(By.text("test.journal")).click()

        composeRule.onNodeWithText("Settings").assertIsDisplayed()

        val prefs = appContext.getSharedPreferences("be.chvp.nanoledger.preferences", Context.MODE_PRIVATE)
        val uriString = prefs.getString("file_uri", null)
        require(!uriString.isNullOrBlank()) { "file_uri preference was not set by the app" }

        composeRule.onNodeWithText(uriString).assertIsDisplayed()

        device.pressBack()
    }

    @Test
    fun showsAccountFromMockFile() {
        composeRule.onNodeWithText("2023-09-04 * Friend | Reconciliation").assertIsDisplayed()
    }

    private fun waitForPicker() {
        device.waitForIdle(TimeUnit.SECONDS.toMillis(2))
        device.wait(Until.findObject(By.descContains("Show roots")), 5_000)
    }
}
