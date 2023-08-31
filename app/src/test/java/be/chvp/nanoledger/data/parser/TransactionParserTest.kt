package be.chvp.nanoledger.data.parser

import com.copperleaf.kudzu.parser.ParserContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionParserTest {

    @Test
    fun canParseSimplePosting() {
        val (parsed, remaining) = PostingParser().parse(
            ParserContext.fromString("    assets        € -5.00\n")
        )
        assertTrue(remaining.isEmpty())
        val posting = parsed.value
        assertEquals("assets", posting.account)
        assertEquals("€ -5.00", posting.amount)
    }

    @Test
    fun canParseSimpleTransaction() {
        val (parsed, remaining) = TransactionParser().parse(
            ParserContext.fromString(
                """
                |2023-08-31 * Payee | Note
                |    assets            € -5.00
                |    expenses    € 5.00
                |
                """.trimMargin()
            )
        )
        assertTrue(remaining.isEmpty())
        val transaction = parsed.value

        assertEquals("2023-08-31", transaction.date)
        assertEquals("*", transaction.status)
        assertEquals("Payee", transaction.payee)
        assertEquals("Note", transaction.note)
        assertEquals(2, transaction.postings.size)
        assertEquals("assets", transaction.postings[0].account)
        assertEquals("€ -5.00", transaction.postings[0].amount)
        assertEquals("expenses", transaction.postings[1].account)
        assertEquals("€ 5.00", transaction.postings[1].amount)
    }
}
