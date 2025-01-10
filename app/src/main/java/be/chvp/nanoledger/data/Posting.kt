package be.chvp.nanoledger.data

data class Amount(val quantity: String, val currency: String, val original: String)

data class Posting(
    val account: String?,
    val amount: Amount?,
    val note: String?,
) {
    // secondary constructor for empty Posting
    constructor() : this(null, null, null) {}

    fun contains(query: String) = account?.contains(query, ignoreCase = true) ?: false

    fun isNote() = account == null && amount == null && note != ""

    fun isEmpty() = account == null && amount == null && note == null
}
