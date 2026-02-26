package be.chvp.nanoledger.ui.edit

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import be.chvp.nanoledger.R
import be.chvp.nanoledger.ui.main.MainActivity
import be.chvp.nanoledger.utils.FileManagerRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val fileManagerRule = FileManagerRule(composeRule)

    val context: Context by lazy { ApplicationProvider.getApplicationContext() }

    @Before
    fun setupFile() {
        fileManagerRule.configureAppWithFile("test.journal")
    }

    @Test
    fun canDoSimpleEdit() {
        composeRule.onNodeWithText("2023-09-04 * Friend | Reconciliation").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription(context.getString(R.string.edit)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Reconciliation").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Reconciliation").assertIsDisplayed().performTextReplacement("Changed description")
        composeRule.onNodeWithContentDescription(context.getString(R.string.save)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("2023-09-04 * Friend | Changed description").assertIsDisplayed().performClick()
    }

    @Test
    fun codeInAccountSurvivesEdit() {
        composeRule.onNodeWithText("2023-09-02 * (123) Restaurant | Dinner with friend").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription(context.getString(R.string.edit)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Dinner with friend").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Dinner with friend").assertIsDisplayed().performTextReplacement("Changed description")
        composeRule.onNodeWithContentDescription(context.getString(R.string.save)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("2023-09-02 * (123) Restaurant | Changed description").assertIsDisplayed().performClick()
    }

    @Test
    fun canDoEditWithOnlyNote() {
        composeRule.onNodeWithText("2026-02-26 Found").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription(context.getString(R.string.edit)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Found").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Found").assertIsDisplayed().performTextReplacement("Stolen")
        composeRule.onNodeWithContentDescription(context.getString(R.string.save)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("2026-02-26 Stolen").assertIsDisplayed().performClick()
    }
}
