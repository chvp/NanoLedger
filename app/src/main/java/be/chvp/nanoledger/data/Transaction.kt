package be.chvp.nanoledger.data

data class Transaction(
    val firstLine: Int,
    val lastLine: Int,
    val date: String,
    val status: String?,
    val payee: String,
    val note: String?,
    val postings: List<Posting>,
) {
    fun contains(query: String): Boolean {
        if (payee.contains(query, ignoreCase = true)) return true
        if (note?.contains(query, ignoreCase = true) ?: false) return true
        if (postings.any { it.contains(query) }) return true
        return false
    }
}
