package sqlbuilder.pool

import sqlbuilder.PersistenceException
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

class ConfigurationConnectionProvider(private val configProvider: ConnectionConfigProvider) : ConnectionProvider {
    init {
        loadDriver()
    }

    private fun loadDriver() {
        try {
            Class.forName(configProvider.driverClassName)
        } catch (e: ClassNotFoundException) {
            throw PersistenceException("Driver <${configProvider.driverClassName}> not in classpath", e)
        }
    }

    override val connection: Connection
        get() {
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

    override val identifier: String
        get() = configProvider.url
}
