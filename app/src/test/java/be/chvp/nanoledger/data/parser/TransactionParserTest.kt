package be.chvp.nanoledger.data.parser

import be.chvp.nanoledger.data.CostType
import be.chvp.nanoledger.data.Transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransactionParserTest {
    @Test
    fun canParseSimpleTransaction() {
        val result =
            extractTransactions(
                """
                |2023-08-31 * Payee | Note
                |    assets            € -5.00
                |    expenses    € 5.00
                """.trimMargin().lines(),
            )

        assertEquals(1, result.size)
        val transaction: Transaction = result[0]

        assertEquals(0, transaction.firstLine)
        assertEquals(2, transaction.lastLine)
        assertEquals("2023-08-31", transaction.date)
        assertEquals("*", transaction.status)
        assertEquals("Payee", transaction.payee)
        assertEquals("Note", transaction.note)
        assertEquals(2, transaction.postings.size)
        assertEquals("assets", transaction.postings[0].account)
        assertEquals("€ -5.00", transaction.postings[0].amount?.original)
        assertEquals("expenses", transaction.postings[1].account)
        assertEquals("€ 5.00", transaction.postings[1].amount?.original)
    }

    @Test
    fun canParseSimpleTransactionNoNote() {
        val result =
            extractTransactions(
                """
                |2023-08-31 * Payee
                |    assets            € -5.00
                |    expenses    € 5.00
                """.trimMargin().lines(),
            )

        assertEquals(1, result.size)
        val transaction: Transaction = result[0]

        assertEquals(0, transaction.firstLine)
        assertEquals(2, transaction.lastLine)
        assertEquals("2023-08-31", transaction.date)
        assertEquals("*", transaction.status)
        assertEquals("Payee", transaction.payee)
        assertEquals(null, transaction.note)
        assertEquals(2, transaction.postings.size)
        assertEquals("assets", transaction.postings[0].account)
        assertEquals("€ -5.00", transaction.postings[0].amount?.original)
        assertEquals("expenses", transaction.postings[1].account)
        assertEquals("€ 5.00", transaction.postings[1].amount?.original)
    }

    @Test
    fun canParseSimpleTransactionWithCode() {
        val result =
            extractTransactions(
                """
                |2023-08-31 * (123) Payee | Note
                |    assets            € -5.00
                |    expenses    € 5.00
                """.trimMargin().lines(),
            )

        assertEquals(1, result.size)
        val transaction: Transaction = result[0]

        assertEquals(0, transaction.firstLine)
        assertEquals(2, transaction.lastLine)
        assertEquals("2023-08-31", transaction.date)
        assertEquals("*", transaction.status)
        assertEquals("123", transaction.code)
        assertEquals("Payee", transaction.payee)
        assertEquals("Note", transaction.note)
        assertEquals(2, transaction.postings.size)
        assertEquals("assets", transaction.postings[0].account)
        assertEquals("€ -5.00", transaction.postings[0].amount?.original)
        assertEquals("expenses", transaction.postings[1].account)
        assertEquals("€ 5.00", transaction.postings[1].amount?.original)
    }

    @Test
    fun canParseSimpleJournal() {
        val transactions =
            extractTransactions(
                """
                |2023-08-31 * Payee | Note
                |    assets            € -5.00
                |    expenses    € 5.00
                |2023-09-01 * Payee 2 | Note 2
                |${'\t'}assets            € -10.00
                |${'\t'}expenses:thing ${'\t'}   € 6.00
                |${'\t'}expenses:thing 2    € 4.00
                """.trimMargin().lines(),
            )

        assertEquals(2, transactions.size)
        assertEquals(0, transactions[0].firstLine)
        assertEquals(2, transactions[0].lastLine)
        assertEquals("2023-08-31", transactions[0].date)
        assertEquals("*", transactions[0].status)
        assertEquals("Payee", transactions[0].payee)
        assertEquals("Note", transactions[0].note)
        assertEquals(2, transactions[0].postings.size)
        assertEquals("assets", transactions[0].postings[0].account)
        assertEquals("€ -5.00", transactions[0].postings[0].amount?.original)
        assertEquals("expenses", transactions[0].postings[1].account)
        assertEquals("€ 5.00", transactions[0].postings[1].amount?.original)

        assertEquals(3, transactions[1].firstLine)
        assertEquals(6, transactions[1].lastLine)
        assertEquals("2023-09-01", transactions[1].date)
        assertEquals("*", transactions[1].status)
        assertEquals("Payee 2", transactions[1].payee)
        assertEquals("Note 2", transactions[1].note)
        assertEquals(3, transactions[1].postings.size)
        assertEquals("assets", transactions[1].postings[0].account)
        assertEquals("€ -10.00", transactions[1].postings[0].amount?.original)
        assertEquals("expenses:thing", transactions[1].postings[1].account)
        assertEquals("€ 6.00", transactions[1].postings[1].amount?.original)
        assertEquals("expenses:thing 2", transactions[1].postings[2].account)
        assertEquals("€ 4.00", transactions[1].postings[2].amount?.original)
    }

    @Test
    fun canParseJournalWithDirective() {
        val transactions =
            extractTransactions(
                """
                |include other.journal
                |2023-08-31 * Payee | Note
                |    assets            € -5.00
                |    expenses    € 5.00
                """.trimMargin().lines(),
            )

        assertEquals(1, transactions.size)
    }

    @Test
    fun canParseUnmarkedTransaction() {
        val transactions =
            extractTransactions(
                """
                |2023-09-05 Shop | Groceries
                |    assets:checking                                         -2.19 EUR
                |    expenses:groceries                                       2.19 EUR
                """.trimMargin().lines(),
            )

        assertEquals(1, transactions.size)
        assertEquals(null, transactions[0].status)
    }

    @Test
    fun canParseTransactionSecondaryDate() {
        val transactions =
            extractTransactions(
                """
                |2023-09-08=2023-09-09 * Shop | Groceries
                |    assets:checking                                         -2.19 EUR
                |    expenses:groceries                                       2.19 EUR
                """.trimMargin().lines(),
            )

        assertEquals(1, transactions.size)
        assertEquals("2023-09-08=2023-09-09", transactions[0].date)
    }

    @Test
    fun canParsePostingNote() {
        val transactions =
            extractTransactions(
                """
                |2023-09-08 * Shop | Groceries
                |    ; Note for the transaction
                |    assets:checking                                         -2.19 EUR; Payee:Test
                |    assets:cash                                             -1.00 EUR
                |    ; another note
                |    expenses:groceries                                       3.19 EUR      ; Payee: Another
                """.trimMargin().lines(),
            )

        assertEquals("Note for the transaction", transactions[0].postings[0].comment)
        assertEquals("Payee:Test", transactions[0].postings[1].comment)
        assertEquals(null, transactions[0].postings[2].comment)
        assertEquals("another note", transactions[0].postings[3].comment)
        assertEquals("Payee: Another", transactions[0].postings[4].comment)
    }

    @Test
    fun testPostingWithOnlyNoteIsNotEmpty() {
        val transactions =
            extractTransactions(
                """
                |2023-09-08 * Shop | Groceries
                |    ; Note for the transaction
                |    assets:checking                                         -2.19 EUR
                |    expenses:groceries                                       2.19 EUR
                """.trimMargin().lines(),
            )

        assertEquals("Note for the transaction", transactions[0].postings[0].comment)
        assertTrue(transactions[0].postings[0].isComment())
        assertFalse(transactions[0].postings[0].isEmpty())
    }

    @Test
    fun canParsePostingWithNoAmountAndNote() {
        val transactions =
            extractTransactions(
                """
                |2023-09-08 * Shop | Groceries
                |    ; Note for the transaction
                |    assets:checking                                         -2.19 EUR
                |    expenses:groceries                                                ; Casual note
                """.trimMargin().lines(),
            )

        assertEquals("Casual note", transactions[0].postings[2].comment)
        assertEquals(null, transactions[0].postings[2].amount)
        assertEquals("expenses:groceries", transactions[0].postings[2].account)
    }



    @Test
    fun canParsePostingWithNoCurrency() {
        val postingString = "some account  1,000.00"

        val posting = extractPosting(postingString)
        val amount = posting.amount
        assertEquals("1,000.00", amount?.original)
        assertEquals("1,000.00", amount?.quantity)
        assertEquals("", amount?.currency)
        assertEquals("some account", posting.account)
        assertEquals(null, posting.cost)
        assertEquals(null, posting.assertion)
        assertEquals(null, posting.assertionCost)
        assertEquals(null, posting.comment)
    }

    @Test
    fun canParseAmountWithNoCurrency() {
        val amountString = "1,000.00"

        val amount = extractAmount(amountString)
        assertEquals("1,000.00", amount.original)
        assertEquals("1,000.00", amount.quantity)
        assertEquals("", amount.currency)
    }

    @Test
    fun canParseNegativeAmountWithNoCurrency() {
        val amountString = "-1,000.00"

        val amount = extractAmount(amountString)
        assertEquals("-1,000.00", amount.original)
        assertEquals("-1,000.00", amount.quantity)
        assertEquals("", amount.currency)
    }

    @Test
    fun canParseAmountWithSimpleCurrencyBefore1() {
        val amountString = "€ 1,000.00"

        val amount = extractAmount(amountString)
        assertEquals("€ 1,000.00", amount.original)
        assertEquals("1,000.00", amount.quantity)
        assertEquals("€", amount.currency)
    }

    @Test
    fun canParseAmountWithSimpleCurrencyBefore2() {
        val amountString = "€- 1,000.00"

        val amount = extractAmount(amountString)
        assertEquals("€- 1,000.00", amount.original)
        assertEquals("- 1,000.00", amount.quantity)
        assertEquals("€", amount.currency)
    }

    @Test
    fun canParseAmountWithSimpleCurrencyBefore3() {
        val amountString = "EUR -1,000.00"

        val amount = extractAmount(amountString)
        assertEquals("EUR -1,000.00", amount.original)
        assertEquals("-1,000.00", amount.quantity)
        assertEquals("EUR", amount.currency)
    }

    @Test
    fun canParseAmountWithSimpleCurrencyBefore4() {
        val amountString = "EUR1,000.00"

        val amount = extractAmount(amountString)
        assertEquals("EUR1,000.00", amount.original)
        assertEquals("1,000.00", amount.quantity)
        assertEquals("EUR", amount.currency)
    }

    @Test
    fun canParseAmountWithSimpleCurrencyAfter1() {
        val amountString = "5,0.0 €"

        val amount = extractAmount(amountString)
        assertEquals("5,0.0 €", amount.original)
        assertEquals("5,0.0", amount.quantity)
        assertEquals("€", amount.currency)
    }

    @Test
    fun canParseAmountWithSimpleCurrencyAfter2() {
        val amountString = "5,0.0€"

        val amount = extractAmount(amountString)
        assertEquals("5,0.0€", amount.original)
        assertEquals("5,0.0", amount.quantity)
        assertEquals("€", amount.currency)
    }

    @Test
    fun canParseAmountWithSimpleCurrencyAfter3() {
        val amountString = "5,0.0 EUR"

        val amount = extractAmount(amountString)
        assertEquals("5,0.0 EUR", amount.original)
        assertEquals("5,0.0", amount.quantity)
        assertEquals("EUR", amount.currency)
    }

    @Test
    fun canParseAmountWithSimpleCurrencyAfter4() {
        val amountString = "5,0.0EUR"

        val amount = extractAmount(amountString)
        assertEquals("5,0.0EUR", amount.original)
        assertEquals("5,0.0", amount.quantity)
        assertEquals("EUR", amount.currency)
    }

    @Test
    fun canParseAmountWithComplexCurrencyBefore1() {
        val amountString = "\"5,0\" 5,0.0"

        val amount = extractAmount(amountString)
        assertEquals("\"5,0\" 5,0.0", amount.original)
        assertEquals("5,0.0", amount.quantity)
        assertEquals("\"5,0\"", amount.currency)
    }

    @Test
    fun canParseAmountWithComplexCurrencyBefore2() {
        val amountString = "\"5,0\"- 5,0.0"

        val amount = extractAmount(amountString)
        assertEquals("\"5,0\"- 5,0.0", amount.original)
        assertEquals("- 5,0.0", amount.quantity)
        assertEquals("\"5,0\"", amount.currency)
    }

    @Test
    fun canParseAmountWithComplexCurrencyAfter1() {
        val amountString = "100005,0.0\"a commodity with spaces, what will they think of next?\""

        val amount = extractAmount(amountString)
        assertEquals("100005,0.0\"a commodity with spaces, what will they think of next?\"", amount.original)
        assertEquals("100005,0.0", amount.quantity)
        assertEquals("\"a commodity with spaces, what will they think of next?\"", amount.currency)
    }

    @Test
    fun canParseAmountWithComplexCurrencyAfter2() {
        val amountString = "-    100005,0.0 \"*&+\""

        val amount = extractAmount(amountString)
        assertEquals("-    100005,0.0 \"*&+\"", amount.original)
        assertEquals("-    100005,0.0", amount.quantity)
        assertEquals("\"*&+\"", amount.currency)
    }

    @Test
    fun canParsePostingWithComplexCurrencyAfterAndAssertion() {
        val postingString = "some account name   -    100005,0.0 \"*&+\"=abc"

        val posting = extractPosting(postingString)
        assertEquals("some account name", posting.account)

        val amount = posting.amount!!
        assertEquals("-    100005,0.0 \"*&+\"", amount.original)
        assertEquals("-    100005,0.0", amount.quantity)
        assertEquals("\"*&+\"", amount.currency)

        val assertion = posting.assertion!!
        assertEquals("abc", assertion.original)
        assertEquals("", assertion.quantity)
        assertEquals("", assertion.currency)

        assertEquals(null, posting.cost)
        assertEquals(null, posting.assertionCost)
        assertEquals(null, posting.comment)
    }

    @Test
    fun canParsePostingWithComplexCurrencyAfterAndCost() {
        val postingString = "another account name  -    100005,0.0 \"*&+\"@ 15 EUR"

        val posting = extractPosting(postingString)
        assertEquals("another account name", posting.account)

        val amount = posting.amount!!
        assertEquals("-    100005,0.0 \"*&+\"", amount.original)
        assertEquals("-    100005,0.0", amount.quantity)
        assertEquals("\"*&+\"", amount.currency)

        val cost = posting.cost!!
        assertEquals("15", cost.amount.quantity)
        assertEquals("EUR", cost.amount.currency)
        assertEquals(CostType.UNIT, cost.type)

        assertEquals(null, posting.assertion)
        assertEquals(null, posting.assertionCost)
        assertEquals(null, posting.comment)
    }

    @Test
    fun canParsePostingWithSimpleCurrencyBeforeAndAssertion() {
        val postingString = "account  € 8.00 = € -2.00"

        val posting = extractPosting(postingString)
        assertEquals("account", posting.account)

        val amount = posting.amount!!
        assertEquals("€ 8.00", amount.original)
        assertEquals("8.00", amount.quantity)
        assertEquals("€", amount.currency)

        val assertion = posting.assertion!!
        assertEquals("€ -2.00", assertion.original)
        assertEquals("-2.00", assertion.quantity)
        assertEquals("€", assertion.currency)

        assertEquals(null, posting.cost)
        assertEquals(null, posting.assertionCost)
        assertEquals(null, posting.comment)
    }

    @Test
    fun canParsePostingWithOnlyAssertion() {
        val postingString = "account  = 10"

        val posting = extractPosting(postingString)
        assertEquals("account", posting.account)

        val assertion = posting.assertion!!
        assertEquals("10", assertion.original)
        assertEquals("10", assertion.quantity)
        assertEquals("", assertion.currency)

        assertEquals(null, posting.amount)
        assertEquals(null, posting.cost)
        assertEquals(null, posting.assertionCost)
        assertEquals(null, posting.comment)
    }

    @Test
    fun canParsePostingWithEverything() {
        val postingString = "pizza    € 10 @ 5 USD = € 20 @@ 10 USD ; and even a comment"

        val posting = extractPosting(postingString)
        assertEquals("pizza", posting.account)

        val amount = posting.amount!!
        assertEquals("€ 10", amount.original)
        assertEquals("10", amount.quantity)
        assertEquals("€", amount.currency)

        val cost = posting.cost!!
        assertEquals(CostType.UNIT, cost.type)
        assertEquals("5 USD", cost.amount.original)
        assertEquals("5", cost.amount.quantity)
        assertEquals("USD", cost.amount.currency)

        val assertion = posting.assertion!!
        assertEquals("€ 20", assertion.original)
        assertEquals("20", assertion.quantity)
        assertEquals("€", assertion.currency)

        val assertionCost = posting.assertionCost!!
        assertEquals(CostType.TOTAL, assertionCost.type)
        assertEquals("10 USD", assertionCost.amount.original)
        assertEquals("10", assertionCost.amount.quantity)
        assertEquals("USD", assertionCost.amount.currency)

        assertEquals("and even a comment", posting.comment)
    }

    @Test
    fun doesNotCrashOnExpressionAmount1() {
        val amountString = "(1 * € 2)"

        val amount = extractAmount(amountString)
        assertEquals("(1 * € 2)", amount.original)
        assertEquals("", amount.quantity)
        assertEquals("", amount.currency)
    }

    @Test
    fun doesNotCrashOnExpressionAmount2() {
        val amountString = "(3 * 10.5 €)"

        val amount = extractAmount(amountString)
        assertEquals("(3 * 10.5 €)", amount.original)
        assertEquals("", amount.quantity)
        assertEquals("", amount.currency)
    }

    @Test
    fun doesNotCrashOnInvalidAmount() {
        val amountString = "abc"

        val amount = extractAmount(amountString)
        assertEquals("abc", amount.original)
        assertEquals("", amount.quantity)
        assertEquals("", amount.currency)
    }
}
