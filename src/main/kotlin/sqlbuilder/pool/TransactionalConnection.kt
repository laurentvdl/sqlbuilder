package sqlbuilder.pool

import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.sql.Connection
import java.sql.SQLException

/**
 * @author Laurent Van der Linden
 */
class TransactionalConnection(val target: Connection, val datasource: DataSourceImpl) : InvocationHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    var lastModified = System.currentTimeMillis()

    val excludedMethodsToLog = listOf("setAutoCommit", "setReadOnly", "commit", "rollback")

    var lastMethod: Method? = null
    var lastArguments: Array<out Any>? = null
    var lastCallstack: String? = null

    private var valid = true

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        if (args != null && args.isNotEmpty() && !excludedMethodsToLog.contains(method.name)) {
            lastMethod = method
            lastArguments = args

            if (datasource.recordStacks) {
                val writer = StringWriter()
                Exception("${method.name} stacktrace").printStackTrace(PrintWriter(writer))
                lastCallstack = writer.toString()
            }
        }

        return synchronized(datasource) {
            lastModified = System.currentTimeMillis()
            val methodName = method.name
            if ("close" == methodName) {
                if (valid) {
                    datasource.freeConnection(this)
                } else {
                    null
                }
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

    fun close(rollback: Boolean) {
        valid = false

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

    override fun toString(): String{
        return "TransactionalConnection(target=$target, lastModified=$lastModified)"
    }


}