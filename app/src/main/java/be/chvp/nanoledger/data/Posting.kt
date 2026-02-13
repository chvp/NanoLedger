package be.chvp.nanoledger.data

data class Amount(
    val quantity: String,
    val currency: String,
    val original: String,
) {
    fun contains(query: String) = original.contains(query, ignoreCase = true)
}

data class Posting(
    val account: String?,
    val amount: Amount?,
    val note: String?,
) {
    // secondary constructor for empty Posting
    constructor(currency: String) : this(null, Amount("", currency, ""), null)

    fun contains(query: String): Boolean {
        if (account?.contains(query, ignoreCase = true) ?: false) { return true }
        if (note?.contains(query, ignoreCase = true) ?: false) { return true }
        if (amount?.contains(query) ?: false) { return true }

        return false
    }

    fun isNote() = account == null && amount == null && note != ""

    fun isEmpty() = (account ?: "") == "" && (amount == null || amount.quantity == "") && note == null

    fun isVirtual() = account?.let { it.startsWith("(") && it.endsWith(")") } ?: false
}
