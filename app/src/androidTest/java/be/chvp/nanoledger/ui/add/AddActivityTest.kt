package be.chvp.nanoledger.ui.add

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import be.chvp.nanoledger.R
import be.chvp.nanoledger.ui.main.MainActivity
import be.chvp.nanoledger.utils.FileManagerRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val fileManagerRule = FileManagerRule(composeRule)

    val context: Context by lazy { ApplicationProvider.getApplicationContext() }

    @Test
    fun canDoSimpleAdd() {
        fileManagerRule.configureAppWithFile("test.journal")
        composeRule.onNodeWithContentDescription(context.getString(R.string.add)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText(context.getString(R.string.payee)).assertIsDisplayed().performTextReplacement("Payee")
        composeRule.onNodeWithText(context.getString(R.string.note)).assertIsDisplayed().performTextReplacement("Note")
        composeRule.onNodeWithText(context.getString(R.string.account)).assertIsDisplayed().performTextReplacement("Account 1")
        composeRule.onAllNodesWithText(context.getString(R.string.amount)).onFirst().performScrollTo().assertIsDisplayed().performTextReplacement("10")
        composeRule.onAllNodesWithText(context.getString(R.string.account)).onLast().performScrollTo().assertIsDisplayed().performTextReplacement("Account 2")
        composeRule.onAllNodesWithText(context.getString(R.string.amount)).onLast().performScrollTo().assertIsDisplayed().performTextReplacement("-10")
        composeRule.onNodeWithContentDescription(context.getString(R.string.save)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Payee | Note", substring = true).assertIsDisplayed()
    }

    @Test
    fun usesCombinedNotesAndPayeesForAutocompleteWhenOnlyOneIsEnabled() {
        fileManagerRule.configureAppWithFile("only-payees.journal")
        composeRule.onNodeWithContentDescription(context.getString(R.string.add)).assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription(context.getString(R.string.change_transaction_fields)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText(context.getString(R.string.remove_payee)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText(context.getString(R.string.note)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Employer").assertIsDisplayed()
    }
}
