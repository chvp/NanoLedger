package be.chvp.nanoledger.data

data class Amount(
    val quantity: String,
    val currency: String,
    val original: String,
) {
    fun contains(query: String) = original.contains(query, ignoreCase = true)
    fun format(currencyBeforeAmount: Boolean, currencyAmountSpacing: Boolean, currencyEnabled: Boolean): String {
        if (!currencyEnabled) { return quantity.trim() }
        val spacer = if (currencyAmountSpacing) " " else ""
        val result = if (currencyBeforeAmount) "$currency$spacer$quantity" else "$quantity$spacer$currency"
        return result.trim()
    }
}
