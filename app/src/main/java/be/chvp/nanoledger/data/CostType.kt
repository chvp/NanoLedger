package be.chvp.nanoledger.data

/**
 * Represents the type of cost in ledger entries.
 * UNIT denotes unit cost, TOTAL denotes total cost.
 */
enum class CostType(
    val repr: String,
) {
    UNIT("@"),
    TOTAL("@@"),
}
