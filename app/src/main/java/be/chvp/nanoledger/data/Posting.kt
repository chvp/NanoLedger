package be.chvp.nanoledger.data

data class Posting(
    val account: String?,
    val amount: Amount?,
    val cost: Cost?,
    val assertion: Amount?,
    val assertionCost: Cost?,
    val comment: String?,
) {
    // secondary constructor for empty Posting
    constructor(currency: String) : this(null, Amount("", currency, ""), null, null, null, null)

    fun contains(query: String): Boolean {
        if (account?.contains(query, ignoreCase = true) ?: false) { return true }
        if (comment?.contains(query, ignoreCase = true) ?: false) { return true }
        if (amount?.contains(query) ?: false) { return true }
        if (cost?.contains(query) ?: false) { return true }
        if (assertion?.contains(query) ?: false) { return true }
        if (assertionCost?.contains(query) ?: false) { return true }

        return false
    }

    fun isComment(): Boolean {
        if (account != null) { return false }
        if (amount != null) { return false }
        if (cost != null) { return false }
        if (assertion != null) { return false }
        if (assertionCost != null) { return false }

        return comment != null
    }

    fun fullAmountDisplayString(): String {
        var result = ""
        result += amount?.original ?: ""
        if (cost != null) {
            result += " ${cost.type.repr} ${cost.amount.original}"
        }
        if (assertion != null) {
            result += " = ${assertion.original}"
        }
        if (assertionCost != null) {
            result += " ${assertionCost.type.repr} ${assertionCost.amount.original}"
        }
        if (comment != null) {
            result += "  ; $comment"
        }
        return result.trim()
    }

    fun format(width: Int, currencyBeforeAmount: Boolean, currencyAmountSpacing: Boolean, currencyEnabled: Boolean): String {
        var fullAmountString = ""
        if ((amount?.quantity ?: "") != "") {
            fullAmountString += amount!!.format(currencyBeforeAmount, currencyAmountSpacing, currencyEnabled)
        }
        if ((cost?.amount?.quantity ?: "") != "") {
            fullAmountString += ' ' + cost!!.format(currencyBeforeAmount, currencyAmountSpacing, currencyEnabled)
        }
        if ((assertion?.quantity ?: "") != "") {
            fullAmountString += " = " + assertion!!.format(currencyBeforeAmount, currencyAmountSpacing, currencyEnabled)
        }
        if ((assertionCost?.amount?.quantity ?: "") != "") {
            fullAmountString += ' ' + assertionCost!!.format(currencyBeforeAmount, currencyAmountSpacing, currencyEnabled)
        }
        fullAmountString = fullAmountString.trim()
        val fillWidth = (width - fullAmountString.length - (account ?: "").length - 4).coerceAtLeast(2)
        val spaces = " ".repeat(fillWidth)
        if (isComment()) {
            return "    ; $comment"
        }
        var result = "    ${account ?: ""}$spaces$fullAmountString"
        if ((comment ?: "") != "") {
            result += "  ; $comment"
        }
        return result

    }

    fun isEmpty(): Boolean {
        if ((account ?: "") != "") { return false }
        if ((amount?.quantity ?: "") != "") { return false }
        if ((cost?.amount?.quantity ?: "") != "") { return false }
        if ((assertion?.quantity ?: "") != "") { return false }
        if ((assertionCost?.amount?.quantity ?: "") != "") { return false }
        if ((comment ?: "") != "") { return false }

        return true
    }

    fun isVirtual() = account?.let { it.startsWith("(") && it.endsWith(")") } ?: false

    fun withAccount(account: String?) = Posting(account, amount, cost, assertion, assertionCost, comment)
    fun withAmount(amount: Amount?) = Posting(account, amount, cost, assertion, assertionCost, comment)
    fun withCost(cost: Cost?) = Posting(account, amount, cost, assertion, assertionCost, comment)
    fun withAssertion(assertion: Amount?) = Posting(account, amount, cost, assertion, assertionCost, comment)
    fun withAssertionCost(assertionCost: Cost?) = Posting(account, amount, cost, assertion, assertionCost, comment)
    fun withComment(comment: String?) = Posting(account, amount, cost, assertion, assertionCost, comment)
}
