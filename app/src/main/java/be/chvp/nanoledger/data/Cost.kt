package be.chvp.nanoledger.data

data class Cost(
    val amount: Amount,
    val type: CostType,
) {
    fun contains(query: String) = amount.contains(query)
    fun format(currencyBeforeAmount: Boolean, currencyAmountSpacing: Boolean): String {
        return "${type.repr} ${amount.format(currencyBeforeAmount, currencyAmountSpacing)}"
    }
}
