package sqlbuilder.pool

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.sql.Connection
import java.lang.reflect.InvocationTargetException
import java.sql.SQLException
import org.slf4j.LoggerFactory

/**
 * @author Laurent Van der Linden
 */
class TransactionalConnection(val target: Connection, val datasource: DataSourceImpl) : InvocationHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    var lastModified = System.currentTimeMillis()

    var callStack: String? = null

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        return synchronized(datasource) {
            lastModified = System.currentTimeMillis()
            val methodName = method.name
            if ("close" == methodName) {
                datasource.freeConnection(this)
            } else {
                try {
                    if (args == null) {
                        method.invoke(target)
                    } else {
                        method.invoke(target, *args)
                    }
                } catch (e: InvocationTargetException) {
                    throw e.cause!!
                }

            }
        }
    }

    public fun close(rollback: Boolean) {
        if (rollback) {
            try {
                target.rollback()
            } catch (e: SQLException) {
                logger.warn("rollback failed, closing", e)
            }
        }
        try {
            target.close()
        } catch (e: SQLException) {
            logger.warn("close failed", e)
        }

    }

  fun setClientUser(name: String) {
    try {
      target.setClientInfo("clientUser", name);
    } catch (e: Exception) {
        logger.warn("unable to set clientUser to $name: ${e.message}");
    } catch (e: Error) {
      logger.warn("unable to set clientUser to $name: ${e.message}");
    }
  }

    fun ping() {
        lastModified = System.currentTimeMillis()
    }
}