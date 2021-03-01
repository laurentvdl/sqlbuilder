package sqlbuilder.kotlin

import sqlbuilder.Backend
import sqlbuilder.Select
import sqlbuilder.SqlBuilder
import sqlbuilder.TransactionIsolation

fun <R> SqlBuilder.withTransaction(
        transactionIsolation: TransactionIsolation = TransactionIsolation.TRANSACTION_DEFAULT,
        readonly: Boolean = false,
        reuseExisting: Boolean = true,
        block: (sqlBuilder: SqlBuilder) -> R): R {
    if (reuseExisting) {
        if (this is Backend && this.isInTransaction()) {
            return block(this)
        }
    }

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