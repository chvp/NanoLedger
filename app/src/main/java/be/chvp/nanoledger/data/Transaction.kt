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
    fun contains(query: String) =
        payee.contains(query, ignoreCase = true) || (note?.contains(query, ignoreCase = true) ?: false) ||
            postings.any {
                it.contains(query)
            }
}
