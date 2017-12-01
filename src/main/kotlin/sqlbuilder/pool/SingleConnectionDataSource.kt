package sqlbuilder.pool

import java.io.PrintWriter
import java.sql.Connection
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * Adapter to use SqlBuilder with an externally provided sql Connection.
 * Will not close the target connection.
 */
class SingleConnectionDataSource(private val target: Connection) : DataSource {
    override fun getConnection(): Connection? {
        return NonClosingConnection(target)
    }

    override fun getConnection(username: String?, password: String?): Connection? {
        return connection
    }

    override fun setLogWriter(out: PrintWriter?) {
        throw UnsupportedOperationException()
    }

    override fun getLogWriter(): PrintWriter? {
        throw UnsupportedOperationException()
    }

    override fun setLoginTimeout(seconds: Int) {
        throw UnsupportedOperationException()
    }

    override fun getParentLogger(): Logger? {
        throw UnsupportedOperationException()
    }

    override fun getLoginTimeout(): Int {
        throw UnsupportedOperationException()
    }

    override fun isWrapperFor(iface: Class<*>?): Boolean {
        throw UnsupportedOperationException()
    }

    override fun <T : Any?> unwrap(iface: Class<T>?): T {
        throw UnsupportedOperationException()
    }
}

private class NonClosingConnection(private val connection: Connection) : Connection by connection {
    override fun close() {}
}