package be.chvp.nanoledger.data

data class Amount(
    val quantity: String,
    val currency: String,
    val original: String,
) {
    fun contains(query: String) = original.contains(query, ignoreCase = true)
}

enum class CostType {
    UNIT,
    TOTAL,
}

data class Cost(
    val amount: Amount,
    val type: CostType,
) {
    fun contains(query: String) = amount.contains(query)
}

data class Posting(
    val account: String?,
    val amount: Amount?,
    val cost: Cost?,
    val assertion: Amount?,
    val assertionCost: Cost?,
    val note: String?,
) {
    // secondary constructor for empty Posting
    constructor(currency: String) : this(null, Amount("", currency, ""), null, null, null, null)

    fun contains(query: String): Boolean {
        if (account?.contains(query, ignoreCase = true) ?: false) { return true }
        if (note?.contains(query, ignoreCase = true) ?: false) { return true }
        if (amount?.contains(query) ?: false) { return true }
        if (cost?.contains(query) ?: false) { return true }
        if (assertion?.contains(query) ?: false) { return true }
        if (assertionCost?.contains(query) ?: false) { return true }

        return false
    }

    fun isNote() = account == null && amount == null && note != ""

    fun isEmpty(): Boolean {
        if ((account ?: "") != "") { return false }
        if ((amount?.quantity ?: "") != "") { return false }
        if ((cost?.amount?.quantity ?: "") != "") { return false }
        if ((assertion?.quantity ?: "") != "") { return false }
        if ((assertionCost?.amount?.quantity ?: "") != "") { return false }
        if ((note ?: "") != "") { return false }

        return true
    }

    fun isVirtual() = account?.let { it.startsWith("(") && it.endsWith(")") } ?: false
}
