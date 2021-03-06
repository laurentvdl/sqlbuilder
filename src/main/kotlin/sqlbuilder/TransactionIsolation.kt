package sqlbuilder

import java.sql.Connection

enum class TransactionIsolation(val isolation: Int) {
    TRANSACTION_NONE(Connection.TRANSACTION_NONE),
    TRANSACTION_READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
    TRANSACTION_READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    TRANSACTION_REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
    TRANSACTION_SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE),
    TRANSACTION_DEFAULT(-1),
}