package be.chvp.nanoledger.data.parser

import be.chvp.nanoledger.data.Posting
import be.chvp.nanoledger.data.Transaction
import com.copperleaf.kudzu.node.mapped.ValueNode
import com.copperleaf.kudzu.parser.Parser
import com.copperleaf.kudzu.parser.chars.CharInParser
import com.copperleaf.kudzu.parser.chars.DigitParser
import com.copperleaf.kudzu.parser.chars.NewlineCharParser
import com.copperleaf.kudzu.parser.many.AtLeastParser
import com.copperleaf.kudzu.parser.many.BetweenTimesParser
import com.copperleaf.kudzu.parser.many.TimesParser
import com.copperleaf.kudzu.parser.mapped.MappedParser
import com.copperleaf.kudzu.parser.maybe.MaybeParser
import com.copperleaf.kudzu.parser.sequence.SequenceParser
import com.copperleaf.kudzu.parser.text.LiteralTokenParser
import com.copperleaf.kudzu.parser.text.OptionalWhitespaceParser
import com.copperleaf.kudzu.parser.text.RequiredWhitespaceParser
import com.copperleaf.kudzu.parser.text.ScanParser

fun PostingParser(): Parser<ValueNode<Posting>> = MappedParser(
    SequenceParser(
        RequiredWhitespaceParser(),
        ScanParser(AtLeastParser(LiteralTokenParser(" "), 2)),
        AtLeastParser(LiteralTokenParser(" "), 2),
        ScanParser(NewlineCharParser()),
        NewlineCharParser()
    )
) { (_, _, account, _, amount, _) ->
    Posting(account.text, amount.text)
}

fun DateParser(): Parser<ValueNode<String>> = MappedParser(
    SequenceParser(
        TimesParser(DigitParser(), 4),
        LiteralTokenParser("-"),
        BetweenTimesParser(DigitParser(), 1, 2),
        LiteralTokenParser("-"),
        BetweenTimesParser(DigitParser(), 1, 2)
    )
) { (_, y, _, m, _, d) -> "${y.text}-${m.text}-${d.text}" }

fun NoteParser(): Parser<ValueNode<String>> = MappedParser(
    SequenceParser(
        LiteralTokenParser("|"),
        OptionalWhitespaceParser(),
        ScanParser(NewlineCharParser())
    )
) { (_, _, _, n) -> n.text }

fun TransactionParser(): Parser<ValueNode<Transaction>> = MappedParser(
    SequenceParser(
        DateParser(),
        RequiredWhitespaceParser(),
        MaybeParser(CharInParser(listOf('*', '!'))),
        OptionalWhitespaceParser(),
        ScanParser(CharInParser(listOf('|', '\n'))),
        MaybeParser(NoteParser()),
        NewlineCharParser(),
        AtLeastParser(PostingParser(), 1)
    )
) { (_, d, _, s, _, p, n, _, ps) ->
    Transaction(
        d.value,
        s.node?.text,
        p.text.trim(),
        n.node?.value,
        ps.nodeList.map { node -> node.value }
    )
}
