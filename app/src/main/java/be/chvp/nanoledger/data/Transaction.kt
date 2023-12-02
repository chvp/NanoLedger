package be.chvp.nanoledger.data

data class Posting(
    val account: String,
    val amount: String?,
)

data class Transaction(
    val date: String,
    val status: String?,
    val payee: String,
    val note: String?,
    val postings: List<Posting>,
)
