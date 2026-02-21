NanoLedger is a [Plain Text Accounting](https://plaintextaccounting.org/) data entry app for Android.

<a href="https://f-droid.org/packages/be.chvp.nanoledger/"><img alt="Get it on F-Droid" src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="80px"/></a>

Only ledger/hledger-like syntax is supported.
View your transactions and quickly add new ones, with autocomplete on fields where it makes sense.

NanoLedger also supports deleting and editing transactions.
For editing, note that NanoLedger does not support the full (h)ledger syntax.
Transactions added with NanoLedger should be editable, but if you use more esoteric amount syntax, NanoLedger might not parse those correctly.
Dates are also required to be in ISO syntax (the same way NanoLedger writes them out).
If your date is not in this format, the current date will be picked.
