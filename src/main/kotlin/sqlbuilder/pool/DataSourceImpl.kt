package sqlbuilder.pool

import org.slf4j.LoggerFactory
import sqlbuilder.PersistenceException
import java.io.PrintWriter
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Properties
import java.util.Timer
import java.util.TimerTask
import java.util.logging.Logger
import javax.annotation.PreDestroy
import javax.sql.DataSource

class DataSourceImpl(private val configProvider: ConnectionConfigProvider) : DataSource {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val _idleConnections = ArrayList<TransactionalConnection>()
    private val _activeConnections = ArrayList<TransactionalConnection>()

    private var timer: Timer? = null
    private var active = true

    private var _logWriter: PrintWriter? = null

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

    fun cleanIdles() {
        val currentTs = System.currentTimeMillis()
        synchronized(lock) {
            val iterator = _idleConnections.iterator()
            while (iterator.hasNext()) {
                val connection = iterator.next()
                if (currentTs - connection.lastModified > idleTimeout) {
                    iterator.remove()
                    connection.close(false)
                }
            }
        }
        if (_idleConnections.isEmpty() && _activeConnections.isEmpty()) {
            // not a single connection in use, we can free our Timer thread
            timer?.cancel()
            timer = null
        }
    }

    private fun findZombies() {
        val currentTs = System.currentTimeMillis()
        synchronized(lock) {
            var x = 0
            val iterator = _activeConnections.iterator()
            while (iterator.hasNext()) {
                val connection = iterator.next()
                val diff = currentTs - connection.lastModified
                if (diff > zombieTimeout) {
                    connection.close(true)
                    iterator.remove()
                    logger.error("removed zombie (after $diff milliseconds) that was invoking ${connection.lastMethod?.declaringClass?.name}.${connection.lastMethod?.name}" +
                            " with arguments ${connection.lastArguments.let { Arrays.toString(it) } ?: "[]"}" +
                            (if (connection.lastCallstack == null) "" else " at ${connection.lastCallstack}"))
                    // once it hits the fan, enable debugging
                    recordStacks = true
                }
                x++
            }
        }
    }

    @Throws(SQLException::class)
    override fun getConnection(): Connection {
        loadDriver()
        return Proxy.newProxyInstance(javaClass.classLoader, arrayOf<Class<*>>(Connection::class.java), obtainConnection()) as Connection
    }

    private fun loadDriver() {
        val driver = configProvider.driverClassName
        try {
            Class.forName(driver)
        } catch (e: ClassNotFoundException) {
            throw PersistenceException("Driver <$driver> not in classpath", e)
        }

    }

    @Throws(SQLException::class)
    private fun newFysicalConnection(): Connection {
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

    @Suppress("UNCHECKED_CAST")
    @Throws(SQLException::class)
    override fun <T> unwrap(iface: Class<T>): T {
        if (isWrapperFor(iface)) {
            return this as T
        } else {
            throw SQLException(javaClass.name + " does not implement " + iface.name)
        }
    }

    @Throws(SQLException::class )
    override fun isWrapperFor(iface: Class<*>): Boolean {
        return iface == javaClass
    }

    private fun obtainConnection(): TransactionalConnection {
        val connection = synchronized(lock) {
            if (!active) throw IllegalStateException("datasource is inactive")

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

            val connection: TransactionalConnection
            if (_idleConnections.size > 0) {
                connection = _idleConnections.removeAt(0)
                connection.ping()
            } else {
                try {
                    val jdbcConnection = newFysicalConnection()
                    connection = TransactionalConnection(jdbcConnection, this)
                } catch (e: SQLException) {
                    throw PersistenceException(e.message, e)
                }

            }
            _activeConnections.add(connection)
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

    fun freeConnection(connection: TransactionalConnection) {
        synchronized(lock) {
            _activeConnections.remove(connection)
            if (active) {
                _idleConnections.add(connection)
            } else {
                try {
                    connection.target.close()
                } catch (ignore: Exception) {
                }

            }
        }
    }

    override fun getConnection(username: String?, password: String?): Connection? {
        return connection
    }

    @PreDestroy fun stop() {
        synchronized(lock) {
            active = false
            if (timer != null) {
                timer!!.cancel()
                timer = null
            }
            for (transactionalConnection in _idleConnections) {
                try {
                    transactionalConnection.target.close()
                } catch (ignore: SQLException) {}

            }
            _idleConnections.clear()
        }
        logger.info("stopped datasource ${configProvider.url}")
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

    override fun toString(): String{
        return "DataSourceImpl(idleConnections=$_idleConnections, activeConnections=$_activeConnections, active=$active, zombieTimeout=$zombieTimeout, idleTimeout=$idleTimeout, cleanupDelay=$cleanupDelay, recordStacks=$recordStacks)"
    }

    val idleConnections: List<TransactionalConnection>
       get() = Collections.unmodifiableList(_idleConnections)

    val activeConnections: List<TransactionalConnection>
       get() = Collections.unmodifiableList(_activeConnections)
}
