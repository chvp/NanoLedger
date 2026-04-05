package be.chvp.nanoledger.data

import be.chvp.nanoledger.data.Posting
import kotlin.test.Test
import kotlin.test.assertEquals

class PostingTest {
    @Test
    fun postingWithoutAmountShouldNotFormatWithTrailingWhitespace() {
        val formatted = Posting("some account", null, null, null, null, null).format(80, false, false, false)

        assertEquals("    some account", formatted)
    }
}
