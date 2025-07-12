package be.chvp.nanoledger.data.parser

import be.chvp.nanoledger.data.Amount
import be.chvp.nanoledger.data.Posting
import be.chvp.nanoledger.data.Transaction

val datePart = "((\\d{4}[-/.])?\\d{1,2}[-/.]\\d{1,2}(=(\\d{4}[-/.])?\\d{1,2}[-/.]\\d{1,2})?)"
val headerRegex = Regex("^$datePart[ \t]*(\\*|!)?([^|]*)(\\|(.*))?$")
val postingRegex = Regex("^[ \t]+\\S.*$")

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
                val posting = extractPosting(lines[i])
                if (posting != null) {
                    lastLine = i
                    postings.add(posting)
                }
                i += 1
            }
            result.add(Transaction(firstLine, lastLine, date, status, payee, note, postings))
        }
    }
    return result
}

val commentRegex = Regex("[ \\t]*;.*$")
val postingSplitRegex = Regex("[ \\t]{2,}")

fun extractPosting(line: String): Posting? {
    // the three components of a posting
    var account: String? = null
    var amount: Amount? = null
    var note: String? = null

    // check if we have a note in the posting
    val commentMatch = commentRegex.find(line)
    if (commentMatch != null) {
        note = commentMatch.value
    }

    val stripped = line.replace(commentRegex, "").trim()
    // if we have more content than just a note, continue parsing
    if (stripped.isNotEmpty()) {
        val components = stripped.split(postingSplitRegex, limit = 2)
        account = components[0]

        // if we have more than the account, is the amount of the posting, parse it
        if (components.size > 1) {
            amount = extractAmount(components[1].trim())
        }
    }

    return Posting(account, amount, note)
}

val assertionRegex = Regex("=.*$")
val costRegex = Regex("@.*$")
val quantityAtStartRegex = Regex("^(-? *[0-9][0-9,.]*)(.*)")
val quantityAtEndRegex = Regex("(-? *[0-9][0-9,.]*)$")

fun extractAmount(string: String): Amount {
    val stripped =
        string
            .trim()
            .replace(assertionRegex, "")
            .trim()
            .replace(costRegex, "")
            .trim()

    if (stripped.length == 0) {
        return Amount("", "", string)
    }

    val matchForStart = quantityAtStartRegex.find(stripped)
    if (matchForStart != null) {
        val groups = matchForStart.groups
        val quantity = groups[1]!!.value.trim()
        val currency = groups[2]!!.value.trim()
        return Amount(quantity, currency, string)
    }
    val matchForEnd = quantityAtEndRegex.find(stripped)
    if (matchForEnd != null) {
        val quantity = matchForEnd.value.trim()
        val currency = stripped.replace(quantityAtEndRegex, "").trim()
        return Amount(quantity, currency, string)
    }

    return Amount("", "", string)
}
