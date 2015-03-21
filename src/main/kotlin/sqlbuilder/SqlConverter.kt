package sqlbuilder

import java.io.InputStream
import java.io.Reader
import java.math.BigDecimal
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Timestamp
import java.sql.Types
import org.slf4j.LoggerFactory

/**
 * Helper class that sets/gets from sql objects according to type.
 */
public class SqlConverter() {
    companion object {
        private val logger = LoggerFactory.getLogger(javaClass)

        throws(javaClass<SQLException>())
        fun setParameter(ps: PreparedStatement, param: Any?, index: Int, `type`: Class<*>? = null, sqlType: Int? = null) {
            var convertedParam = param

            logger.debug("set({},{})", index, convertedParam)

            if (convertedParam == null) {
                if (sqlType != null) {
                    // SqlType was specified directly, so set it
                    ps.setNull(index, sqlType)
                } else {
                    when (`type`) {
                        null -> {
                            try {
                                ps.setNull(index, Types.NULL)
                            } catch (e: SQLException) {
                                throw PersistenceException("null parameter without type at index<" + index + ">", e)
                            }

                        }
                        else -> {
                            when {
                                javaClass<String>() == `type` -> {
                                    ps.setNull(index, Types.VARCHAR)
                                }
                                javaClass<Short>().isAssignableFrom(`type`) -> {
                                    ps.setNull(index, Types.SMALLINT)
                                }
                                javaClass<Int>().isAssignableFrom(`type`) -> {
                                    ps.setNull(index, Types.INTEGER)
                                }
                                javaClass<Long>().isAssignableFrom(`type`) -> {
                                    ps.setNull(index, Types.BIGINT)
                                }
                                javaClass<Boolean>().isAssignableFrom(`type`) -> {
                                    ps.setNull(index, Types.TINYINT)
                                }
                                javaClass<Timestamp>().isAssignableFrom(`type`) -> {
                                    ps.setNull(index, Types.TIMESTAMP)
                                }
                                javaClass<Number>().isAssignableFrom(`type`) -> {
                                    ps.setNull(index, Types.DECIMAL)
                                }
                                javaClass<java.util.Date>().isAssignableFrom(`type`) -> {
                                    ps.setNull(index, Types.DATE)
                                }
                                javaClass<Boolean>().isAssignableFrom(`type`) -> {
                                    ps.setNull(index, Types.BOOLEAN)
                                }
                                javaClass<Enum<*>>().isAssignableFrom(`type`) -> {
                                    ps.setNull(index, Types.INTEGER)
                                }
                                javaClass<Char>().isAssignableFrom(`type`) -> {
                                    ps.setNull(index, Types.CHAR)
                                }
                                else -> {
                                    throw PersistenceException("unknown null param type " + `type`)
                                }
                            }
                        }
                    }
                }
            } else {
                if (convertedParam is LazyValue) {
                    try {
                        convertedParam = (convertedParam as LazyValue).eval()
                    } catch (e: Exception) {
                        throw PersistenceException("unable to eval lazy value: " + convertedParam, e)
                    }
                }
                when (convertedParam) {
                    is BigDecimal -> {
                        ps.setBigDecimal(index, convertedParam as BigDecimal)
                    }
                    is Int -> {
                        ps.setInt(index, (convertedParam as Int))
                    }
                    is Long -> {
                        ps.setLong(index, (convertedParam as Long))
                    }
                    is Short -> {
                        ps.setShort(index, (convertedParam as Short))
                    }
                    is Double -> {
                        ps.setDouble(index, (convertedParam as Double))
                    }
                    is Float -> {
                        ps.setFloat(index, (convertedParam as Float))
                    }
                    is Timestamp -> {
                        ps.setTimestamp(index, convertedParam as Timestamp)
                    }
                    is java.util.Date -> {
                        ps.setDate(index, Date((convertedParam as java.util.Date).getTime()))
                    }
                    is String -> {
                        ps.setString(index, convertedParam as String)
                    }
                    is ByteArray -> {
                        ps.setBytes(index, convertedParam as ByteArray)
                    }
                    is InputStream -> {
                        ps.setBinaryStream(index, convertedParam as InputStream)
                    }
                    is Reader -> {
                        ps.setCharacterStream(index, convertedParam as Reader)
                    }
                    is Boolean -> {
                        ps.setBoolean(index, (convertedParam as Boolean))
                    }
                    is Enum<*> -> {
                        ps.setInt(index, (convertedParam as Enum<*>).ordinal())
                    }
                    is Char -> {
                        ps.setString(index, convertedParam!!.toString())
                    }
                    else -> {
                        throw PersistenceException("unknown param type " + convertedParam?.javaClass)
                    }
                }
            }
        }
    }
}
