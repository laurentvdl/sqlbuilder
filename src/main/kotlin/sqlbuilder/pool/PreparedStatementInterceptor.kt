package sqlbuilder.pool

import java.sql.ResultSet
import java.util.concurrent.Callable

interface PreparedStatementInterceptor {
    fun intercept(query: String, execution: Callable<ResultSet>): ResultSet
}
