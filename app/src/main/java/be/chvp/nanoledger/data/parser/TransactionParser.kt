package be.chvp.nanoledger.data.parser

import be.chvp.nanoledger.data.Posting
import be.chvp.nanoledger.data.Transaction

val datePart = "((\\d{4}[-/.])?\\d{1,2}[-/.]\\d{1,2}(=(\\d{4}[-/.])?\\d{1,2}[-/.]\\d{1,2})?)"
val headerRegex = Regex("^$datePart[ \t]*(\\*|!)?([^|]*)(\\|(.*))?$")
val postingRegex = Regex("^[ \t]+\\S.*$")
val postingSplitRegex = Regex("[ \\t]{2,}")
val commentRegex = Regex(";.*$")

fun extractTransactions(lines: List<String>): List<Transaction> {
    val result = ArrayList<Transaction>()
    var i = 0
    while (i < lines.size) {
        val match = headerRegex.find(lines[i])
        i += 1
        if (match != null) {
            val firstLine = i - 1
            var lastLine = i - 1
            val groups = match.groups
            val date = groups[1]!!.value
            val status = groups[5]?.value
            val payee = groups[6]!!.value.trim()
            val note = groups[8]?.value?.trim()

            val postings = ArrayList<Posting>()
            while (i < lines.size && postingRegex.find(lines[i]) != null) {
                val stripped = lines[i].trim().replace(commentRegex, "")
                i += 1
                if (stripped.length > 0) {
                    lastLine = i - 1
                    val components = stripped.split(postingSplitRegex, limit = 2)
                    if (components.size > 1) {
                        postings.add(Posting(components[0], components[1]))
                    } else {
                        postings.add(Posting(components[0], null))
                    }
                }
            }
            result.add(Transaction(firstLine, lastLine, date, status, payee, note, postings))
        }
    }
    return result
}
