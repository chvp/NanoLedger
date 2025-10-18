package be.chvp.nanoledger.ui.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    @Before
    fun setupFile() {
        fileManagerRule.configureAppWithFile("test.journal")
    }

    @Test
    fun showsTransactionFromMockFile() {
        composeRule.onNodeWithText("2023-09-04 * Friend | Reconciliation").assertIsDisplayed()
    }
}
