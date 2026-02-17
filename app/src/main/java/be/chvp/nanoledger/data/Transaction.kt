package be.chvp.nanoledger.data

data class Transaction(
    val firstLine: Int,
    val lastLine: Int,
    val date: String,
    val status: String?,
    val code: String?,
    val payee: String?,
    val note: String?,
    val postings: List<Posting>,
) {
    fun contains(query: String): Boolean {
        if (payee?.contains(query, ignoreCase = true) ?: false) return true
        if (note?.contains(query, ignoreCase = true) ?: false) return true
        if (postings.any { it.contains(query) }) return true
        return false
    }

    fun header(): String {
        val res = StringBuilder()
        res.append(date)
        if ((status ?: " ") != " ") res.append(" $status")
        if ((code ?: "") != "") res.append(" ($code)")
        res.append(' ')
        res.append(listOf(payee, note).filter { s -> s != null }.joinToString(" | "))
        return res.toString().trim()
    }

    fun format(postingWidth: Int, currencyBeforeAmount: Boolean, currencyAmountSpacing: Boolean, currencyEnabled: Boolean): String {
        val result = StringBuilder()
        result.append("${header()}\n")
        for (posting in postings) {
            result.append("${posting.format(postingWidth, currencyBeforeAmount, currencyAmountSpacing, currencyEnabled)}\n")
        }
        result.append('\n')
        return result.toString()
    }
}
