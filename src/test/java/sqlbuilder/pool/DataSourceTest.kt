package sqlbuilder.pool

import org.h2.jdbc.JdbcSQLException
import org.junit.Test
import java.lang.reflect.Proxy
import java.sql.SQLException
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

/**
 * @author Laurent Van der Linden.
 */
class DataSourceTest {
    @Test(expected = JdbcSQLException::class)
    @Throws(SQLException::class, InterruptedException::class) fun idleCleanup() {
        val dataSource = DataSourceImpl(DefaultConfig(null, null, "jdbc:h2:mem:test", Drivers.H2))
        dataSource.idleTimeout = 30
        dataSource.zombieTimeout = 40
        dataSource.cleanupDelay = 10

        val connection1 = dataSource.connection
        // this will stop the timer
        connection1.close()

        Thread.sleep(40)

        dataSource.connection

        Thread.sleep(50)

        // should be zombie killed

        connection1.createStatement().executeQuery("select 1")
    }

    @Test
    fun closedConnectionHandling() {
        val dataSource = DataSourceImpl(DefaultConfig(null, null, "jdbc:h2:mem:test", Drivers.H2))

        val transactionalConnection = Proxy.getInvocationHandler(dataSource.connection) as TransactionalConnection
        transactionalConnection.target.close()
        dataSource.freeConnection(transactionalConnection)

        val transactionalConnectionTwo = Proxy.getInvocationHandler(dataSource.connection) as TransactionalConnection

        assertFalse(transactionalConnectionTwo.target.isClosed)
        assertNotEquals(transactionalConnection, transactionalConnectionTwo)
    }
}