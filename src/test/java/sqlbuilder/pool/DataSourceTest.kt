package sqlbuilder.pool

import org.h2.jdbc.JdbcSQLException
import org.junit.Test
import java.sql.SQLException

/**
 * @author Laurent Van der Linden.
 */
public class DataSourceTest {
    @Test(expected = JdbcSQLException::class)
    @Throws(SQLException::class, InterruptedException::class)
    public fun idleCleanup() {
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
}