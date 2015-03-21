package sqlbuilder.pool

import sqlbuilder.PersistenceException

import javax.annotation.PreDestroy
import javax.sql.DataSource
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.ArrayList
import java.util.Properties
import java.util.Timer
import java.util.TimerTask
import org.slf4j.LoggerFactory
import java.util.logging.Logger

public class DataSourceImpl(private val configProvider: ConnectionConfigProvider) : DataSource {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val idleConnections = ArrayList<TransactionalConnection>()
    private val activeConnections = ArrayList<TransactionalConnection>()
    private var timer: Timer? = null
    private var active = true

    private var _logWriter: PrintWriter? = null
    private var _loginTimeout: Long? = 0L

    private val lock = Object()

    /**
     * Set the time a connection can be held without any invocations. After this time, the connection will be rolled back.
     * <br/>120000 by default
     * @param zombieTimeout in milliseconds
     * @return this for chaining
     */
    var zombieTimeout = 300000

    /**
     * Set the time a connection is held in a pool for reuse.
     * <br/>60000 by default
     * @param idleTimeout in milliseconds
     * @return this for chaining
     */
    var idleTimeout = 60000

    /**
     * Set the garbage collection interval of the background thread.
     * <br/> 10000 by default
     * <br/> used for testing
     * @param cleanupDelay in milliseconds
     * @return this for chaining
     */
    var cleanupDelay = 10000

    /**
     * Active recording of getConnection. Only use this when debugging as the cost is heavy.
     */
    var recordStacks = false

    public fun cleanIdles() {
        val currentTs = System.currentTimeMillis()
        synchronized(lock) {
            var x = 0
            while (x < idleConnections.size()) {
                val connection = idleConnections.get(x)
                if (currentTs - connection.lastModified > idleTimeout) {
                    connection.close(false)
                    idleConnections.remove(x--)
                }
                x++
            }
        }
        if (idleConnections.isEmpty() && activeConnections.isEmpty()) {
            // not a single connection in use, we can free our Timer thread
            timer!!.cancel()
            timer = null
        }
    }

    private fun findZombies() {
        val currentTs = System.currentTimeMillis()
        synchronized(lock) {
            var x = 0
            while (x < activeConnections.size()) {
                val connection = activeConnections.get(x)
                val diff = currentTs - connection.lastModified
                if (diff > zombieTimeout) {
                    connection.close(true)
                    activeConnections.remove(x--)
                    logger.error("removed zombie (after $diff millis) that was obtained from: ${connection.callStack}")
                    // once it hits the fan, enable debugging
                    recordStacks = true
                }
                x++
            }
        }
    }

    throws(javaClass<SQLException>())
    override fun getConnection(): Connection {
        loadDriver()
        return Proxy.newProxyInstance(javaClass.getClassLoader(), array<Class<*>>(javaClass<Connection>()), obtainConnection()) as Connection
    }

    private fun loadDriver() {
        val driver = configProvider.driverClassName
        try {
            Class.forName(driver)
        } catch (e: ClassNotFoundException) {
            throw PersistenceException("Driver <" + driver + "> not in classpath", e)
        }

    }

    throws(javaClass<SQLException>())
    protected fun newFysicalConnection(): Connection {
        val properties = configProvider.properties ?: Properties()

        // ask DB2 to explain it's error codes
        properties.setProperty("retrieveMessagesFromServerOnGetMessage", "true")

        if (configProvider.username != null) {
            properties.setProperty("user", configProvider.username)
        }
        if (configProvider.password != null) {
            properties.setProperty("password", configProvider.password)
        }

        return DriverManager.getConnection(configProvider.url, properties)
    }

    [suppress("UNCHECKED_CAST")]
    throws(javaClass<SQLException>())
    override fun <T> unwrap(iface: Class<T>): T {
        if (isWrapperFor(iface)) {
            return this as T
        } else {
            throw SQLException(javaClass.getName() + " does not implement " + iface.getName())
        }
    }

    throws(javaClass<SQLException>())
    override fun isWrapperFor(iface: Class<*>): Boolean {
        return iface == javaClass
    }

    protected fun obtainConnection(): TransactionalConnection {
        val connection = synchronized(lock) {
            if (!active) throw IllegalStateException("datasource is inactive")
            val connection: TransactionalConnection
            if (idleConnections.size() > 0) {
                connection = idleConnections.remove(0)
                connection.ping()
            } else {
                try {
                    val jdbcConnection = newFysicalConnection()
                    connection = TransactionalConnection(jdbcConnection, this)
                    if (recordStacks) {
                        val writer = StringWriter()
                        Exception("getConnection stacktrace").printStackTrace(PrintWriter(writer))
                        connection.callStack = writer.toString()
                    }
                } catch (e: SQLException) {
                    throw PersistenceException(e.getMessage(), e)
                }

            }
            activeConnections.add(connection)
            connection
        }

        val identityPlugin = configProvider.identityPlugin
        if (identityPlugin != null) {
            // set username of actual user for tracing
            val traceUser = identityPlugin.traceUsername
            if (traceUser != null) connection.setClientUser(traceUser)
        }
        return connection
    }

    public fun freeConnection(connection: TransactionalConnection) {
        synchronized(lock) {
            activeConnections.remove(connection)
            if (active) {
                idleConnections.add(connection)
                if (timer == null) {
                    val timer = Timer(configProvider.url + " cleaner", true)
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            cleanIdles()
                            findZombies()
                        }
                    }, cleanupDelay.toLong(), cleanupDelay.toLong())

                    this.timer = timer
                }
            } else {
                try {
                    connection.target.close()
                } catch (ignore: Exception) {
                }

            }
        }
    }

    public fun killConnection(connection: TransactionalConnection) {
        synchronized(lock) {
            activeConnections.remove(connection)
            try {
                connection.target.close()
            } catch (ignore: Exception) {
            }
        }
    }


    override fun getConnection(username: String?, password: String?): Connection? {
        return getConnection()
    }

    PreDestroy
    public fun stop() {
        synchronized(lock) {
            active = false
            if (timer != null) {
                timer!!.cancel()
                timer = null
            }
            for (transactionalConnection in idleConnections) {
                try {
                    transactionalConnection.target.close()
                } catch (ignore: SQLException) {}

            }
            idleConnections.clear()
        }
        logger.info("stopped datasource ${configProvider.url}")
    }

    private fun getDefaultPlugin(): IdentityPlugin? {
        return configProvider.identityPlugin
    }

    override fun getParentLogger(): Logger? {
        throw UnsupportedOperationException()
    }

    override fun getLogWriter(): PrintWriter? {
        return _logWriter
    }

    override fun setLogWriter(out: PrintWriter?) {
        _logWriter = out
    }

    override fun setLoginTimeout(seconds: Int) {
        throw UnsupportedOperationException()
    }
    override fun getLoginTimeout(): Int {
        throw UnsupportedOperationException()
    }
}
