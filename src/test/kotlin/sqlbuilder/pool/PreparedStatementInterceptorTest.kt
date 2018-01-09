package sqlbuilder.pool

import org.junit.Test
import sqlbuilder.impl.SqlBuilderImpl
import java.sql.ResultSet
import java.util.concurrent.Callable
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PreparedStatementInterceptorTest {
    @Test
    fun interceptsPreparedQueries() {
        val dummyInterceptor = DummyInterceptor()

        val sqlBuilder = SqlBuilderImpl(DataSourceImpl(
                DefaultConfig(
                        null, null, "jdbc:h2:mem:test", Drivers.H2
                )
        ).apply { preparedStatementInterceptor =  dummyInterceptor })

        val query = "select * from information_schema.tables"
        sqlBuilder.select().sql(query).selectMaps()

        assertEquals(query, dummyInterceptor.query, "query should have been intercepted")
        assertTrue(dummyInterceptor.timeToRun > 0, "query time was recorded")
    }
}

private class DummyInterceptor : PreparedStatementInterceptor {
    var query: String? = null
    var timeToRun: Long = 0L

    override fun intercept(query: String, execution: Callable<ResultSet>): ResultSet {
        this.query = query
        val start = System.nanoTime()
        try {
            return execution.call()
        } finally {
            this.timeToRun = System.nanoTime() - start
        }
    }
}