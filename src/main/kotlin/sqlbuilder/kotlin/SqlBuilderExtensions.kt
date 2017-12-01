package sqlbuilder.kotlin

import sqlbuilder.Select
import sqlbuilder.SqlBuilder
import sqlbuilder.TransactionIsolation

inline fun <R> SqlBuilder.withTransaction(
        transactionIsolation: TransactionIsolation = TransactionIsolation.TRANSACTION_DEFAULT,
        readonly: Boolean = false,
        block: (sqlBuilder: SqlBuilder) -> R): R {
    this.startTransaction(transactionIsolation, readonly)
    try {
        val result = block(this)
        this.commitTransaction()
        return result
    } finally {
        this.endTransaction()
    }
}

inline fun <R> SqlBuilder.select(block: Select.() -> R): R = block(this.select())