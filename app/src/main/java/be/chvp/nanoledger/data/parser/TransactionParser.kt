package be.chvp.nanoledger.data.parser

import be.chvp.nanoledger.data.Posting
import be.chvp.nanoledger.data.Transaction
import cc.ekblad.konbini.Parser
import cc.ekblad.konbini.chain1
import cc.ekblad.konbini.char
import cc.ekblad.konbini.many1
import cc.ekblad.konbini.parser
import cc.ekblad.konbini.regex
import cc.ekblad.konbini.tryParse
import cc.ekblad.konbini.whitespace

val dateParser: Parser<String> = regex("\\d{4}-\\d{1,2}-\\d{1,2}")
val statusParser: Parser<String> = regex("\\*|!")
val postingParser: Parser<Posting> = parser {
    regex("[ \\t]")
    whitespace()
    val line = regex("[^\n]*")
    val components = line.trim().split(Regex("[ \\t]{2,}"), limit = 2)
    Posting(components[0], if (components.size == 2) components[1] else null)
}

val transactionParser: Parser<Transaction> = parser {
    val date = dateParser()
    whitespace()
    val status = tryParse(statusParser)
    whitespace()
    val description = regex("[^\n]*")
    val descComponents = description.split(Regex("[ \\t]*\\|[ \\t]*"), limit = 2)
    char('\n')
    val postings = chain1(postingParser, parser { char('\n') })
    Transaction(
        date,
        status,
        descComponents[0],
        if (descComponents.size == 2) descComponents[1] else null,
        postings.terms
    )
}

val journalParser: Parser<List<Transaction>> = parser {
    chain1(
        transactionParser,
        parser {
            many1(
                parser {
                    char('\n')
                }
            )
        }
    ).terms
}

// fun JournalParser(): Parser<ValueNode<List<Transaction>>> = MappedParser(
//     SeparatedByParser(
//         TransactionParser(),
//         SequenceParser(OptionalWhitespaceParser(), NewlineCharParser())
//     )
// ) { l ->
//     l.nodeList.map { node -> node.value }
// }
