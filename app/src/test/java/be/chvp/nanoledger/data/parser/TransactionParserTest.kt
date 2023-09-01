package be.chvp.nanoledger.data.parser

import be.chvp.nanoledger.data.Posting
import be.chvp.nanoledger.data.Transaction
import cc.ekblad.konbini.ParserResult
import cc.ekblad.konbini.parseToEnd
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TransactionParserTest {

    @Test
    fun canParseDate() {
        val result = dateParser.parseToEnd("2023-09-01")
        assertIs<ParserResult.Ok<String>>(result)
        assertEquals("2023-09-01", result.result)
    }

    @Test
    fun canParseSimplePosting() {
        val result = postingParser.parseToEnd("    assets        € -5.00")

        assertIs<ParserResult.Ok<Posting>>(result)
        val posting: Posting = result.result

        assertEquals("assets", posting.account)
        assertEquals("€ -5.00", posting.amount)
    }

    @Test
    fun canParseSimplePostingTab() {
        val result = postingParser.parseToEnd("\tassets        € -5.00")

        assertIs<ParserResult.Ok<Posting>>(result)
        val posting: Posting = result.result

        assertEquals("assets", posting.account)
        assertEquals("€ -5.00", posting.amount)
    }

    @Test
    fun canParseSimplePostingNoAmount() {
        val result = postingParser.parseToEnd("\tassets     ")

        assertIs<ParserResult.Ok<Posting>>(result)
        val posting: Posting = result.result

        assertEquals("assets", posting.account)
        assertEquals(null, posting.amount)
    }

    @Test
    fun canParseSimpleTransaction() {
        val result = transactionParser.parseToEnd(
            """
            |2023-08-31 * Payee | Note
            |    assets            € -5.00
            |    expenses    € 5.00
            """.trimMargin()
        )

        assertIs<ParserResult.Ok<Transaction>>(result)
        val transaction: Transaction = result.result

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

    @Test
    fun canParseSimpleTransactionNoNote() {
        val result = transactionParser.parseToEnd(
            """
            |2023-08-31 * Payee
            |    assets            € -5.00
            |    expenses    € 5.00
            """.trimMargin()
        )

        assertIs<ParserResult.Ok<Transaction>>(result)
        val transaction: Transaction = result.result

        assertEquals("2023-08-31", transaction.date)
        assertEquals("*", transaction.status)
        assertEquals("Payee", transaction.payee)
        assertEquals(null, transaction.note)
        assertEquals(2, transaction.postings.size)
        assertEquals("assets", transaction.postings[0].account)
        assertEquals("€ -5.00", transaction.postings[0].amount)
        assertEquals("expenses", transaction.postings[1].account)
        assertEquals("€ 5.00", transaction.postings[1].amount)
    }

    @Test
    fun canParseSimpleJournal() {
        val result = journalParser.parseToEnd(
            """
            |2023-08-31 * Payee | Note
            |    assets            € -5.00
            |    expenses    € 5.00
            |2023-09-01 * Payee 2 | Note 2
            |${'\t'}assets            € -10.00
            |${'\t'}expenses:thing ${'\t'}   € 6.00
            |${'\t'}expenses:thing 2    € 4.00
            """.trimMargin()
        )

        assertIs<ParserResult.Ok<List<Transaction>>>(result)
        val transactions: List<Transaction> = result.result

        assertEquals(2, transactions.size)
        assertEquals("2023-08-31", transactions[0].date)
        assertEquals("*", transactions[0].status)
        assertEquals("Payee", transactions[0].payee)
        assertEquals("Note", transactions[0].note)
        assertEquals(2, transactions[0].postings.size)
        assertEquals("assets", transactions[0].postings[0].account)
        assertEquals("€ -5.00", transactions[0].postings[0].amount)
        assertEquals("expenses", transactions[0].postings[1].account)
        assertEquals("€ 5.00", transactions[0].postings[1].amount)

        assertEquals("2023-09-01", transactions[1].date)
        assertEquals("*", transactions[1].status)
        assertEquals("Payee 2", transactions[1].payee)
        assertEquals("Note 2", transactions[1].note)
        assertEquals(3, transactions[1].postings.size)
        assertEquals("assets", transactions[1].postings[0].account)
        assertEquals("€ -10.00", transactions[1].postings[0].amount)
        assertEquals("expenses:thing", transactions[1].postings[1].account)
        assertEquals("€ 6.00", transactions[1].postings[1].amount)
        assertEquals("expenses:thing 2", transactions[1].postings[2].account)
        assertEquals("€ 4.00", transactions[1].postings[2].amount)
    }
}
