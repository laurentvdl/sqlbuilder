package sqlbuilder.pool

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.concurrent.Callable

class PreparedStatementWrapper(private val target: PreparedStatement, private val query: String, private val interceptor: PreparedStatementInterceptor) : PreparedStatement by target {
    override fun executeQuery(): ResultSet {
        return interceptor.intercept(query, Callable { target.executeQuery() })
    }
}