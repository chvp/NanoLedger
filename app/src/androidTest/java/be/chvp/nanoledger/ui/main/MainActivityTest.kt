package be.chvp.nanoledger.ui.main

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import be.chvp.nanoledger.R
import be.chvp.nanoledger.utils.FileManagerRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
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
    fun showsTransaction() {
        composeRule.onNodeWithText("2023-09-04 * Friend | Reconciliation").assertIsDisplayed()
    }

    @Test
    fun allowsSearchByPayee() {
        composeRule.onNodeWithContentDescription(context.getString(R.string.search)).assertIsDisplayed().performClick()
        // Should still show the transactions if nothing is entered
        composeRule.onNodeWithText("2023-09-04 * Friend | Reconciliation").assertIsDisplayed()
        composeRule.onNodeWithTag("search-field").assertIsDisplayed().performTextInput("Employer")
        composeRule.onNodeWithText("2023-09-01 * Employer | Payment").assertIsDisplayed()
        composeRule.onNodeWithText("2023-09-04 * Friend | Reconciliation").assertDoesNotExist()

        composeRule.onNodeWithContentDescription(context.getString(R.string.stop_searching)).assertIsDisplayed().performClick()
        // Stopping the search should remove the filter
        composeRule.onNodeWithText("2023-09-04 * Friend | Reconciliation").assertIsDisplayed()
    }

    @Test
    fun allowsSearchByAmount() {
        composeRule.onNodeWithContentDescription(context.getString(R.string.search)).assertIsDisplayed().performClick()
        // Should still show the transactions if nothing is entered
        composeRule.onNodeWithText("2023-09-04 * Friend | Reconciliation").assertIsDisplayed()
        composeRule.onNodeWithTag("search-field").assertIsDisplayed().performTextInput("100")
        composeRule.onNodeWithText("2023-09-01 * Employer | Payment").assertIsDisplayed()
        composeRule.onNodeWithText("2023-09-04 * Friend | Reconciliation").assertDoesNotExist()

        composeRule.onNodeWithContentDescription(context.getString(R.string.stop_searching)).assertIsDisplayed().performClick()
        // Stopping the search should remove the filter
        composeRule.onNodeWithText("2023-09-04 * Friend | Reconciliation").assertIsDisplayed()
    }
}
