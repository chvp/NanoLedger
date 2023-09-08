package be.chvp.nanoledger.data.parser

import be.chvp.nanoledger.data.Transaction
import kotlin.test.Test
import kotlin.test.assertEquals

class TransactionParserTest {

    @Test
    fun canParseSimpleTransaction() {
        val result = extractTransactions(
            """
            |2023-08-31 * Payee | Note
            |    assets            € -5.00
            |    expenses    € 5.00
            """.trimMargin().lines()
        )

        assertEquals(1, result.size)
        val transaction: Transaction = result[0]

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
        val result = extractTransactions(
            """
            |2023-08-31 * Payee
            |    assets            € -5.00
            |    expenses    € 5.00
            """.trimMargin().lines()
        )

        assertEquals(1, result.size)
        val transaction: Transaction = result[0]

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
        val transactions = extractTransactions(
            """
            |2023-08-31 * Payee | Note
            |    assets            € -5.00
            |    expenses    € 5.00
            |2023-09-01 * Payee 2 | Note 2
            |${'\t'}assets            € -10.00
            |${'\t'}expenses:thing ${'\t'}   € 6.00
            |${'\t'}expenses:thing 2    € 4.00
            """.trimMargin().lines()
        )

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

    @Test
    fun canParseJournalWithDirective() {
        val transactions = extractTransactions(
            """
            |include other.journal
            |2023-08-31 * Payee | Note
            |    assets            € -5.00
            |    expenses    € 5.00
            """.trimMargin().lines()
        )

        assertEquals(1, transactions.size)
    }

    @Test
    fun canParseUnmarkedTransaction() {
        val transactions = extractTransactions(
            """
            |2023-09-05 Shop | Groceries
            |    assets:checking                                         -2.19 EUR
            |    expenses:groceries                                       2.19 EUR
            """.trimMargin().lines()
        )

        assertEquals(1, transactions.size)
        assertEquals(null, transactions[0].status)
    }

    @Test
    fun canParseTransactionSecondaryDate() {
        val transactions = extractTransactions(
            """
            |2023-09-08=2023-09-09 * Shop | Groceries
            |    assets:checking                                         -2.19 EUR
            |    expenses:groceries                                       2.19 EUR
            """.trimMargin().lines()
        )

        assertEquals(1, transactions.size)
        assertEquals("2023-09-08=2023-09-09", transactions[0].date)
    }
}
