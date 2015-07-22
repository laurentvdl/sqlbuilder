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

        throws(SQLException::class)
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
                        convertedParam = convertedParam.eval()
                    } catch (e: Exception) {
                        throw PersistenceException("unable to eval lazy value: " + convertedParam, e)
                    }
                }
                when (convertedParam) {
                    is BigDecimal -> {
                        ps.setBigDecimal(index, convertedParam)
                    }
                    is Int -> {
                        ps.setInt(index, convertedParam)
                    }
                    is Long -> {
                        ps.setLong(index, convertedParam)
                    }
                    is Short -> {
                        ps.setShort(index, convertedParam)
                    }
                    is Double -> {
                        ps.setDouble(index, convertedParam)
                    }
                    is Float -> {
                        ps.setFloat(index, convertedParam)
                    }
                    is Timestamp -> {
                        ps.setTimestamp(index, convertedParam)
                    }
                    is java.util.Date -> {
                        ps.setDate(index, Date(convertedParam.getTime()))
                    }
                    is String -> {
                        ps.setString(index, convertedParam)
                    }
                    is ByteArray -> {
                        ps.setBytes(index, convertedParam)
                    }
                    is InputStream -> {
                        ps.setBinaryStream(index, convertedParam)
                    }
                    is Reader -> {
                        ps.setCharacterStream(index, convertedParam)
                    }
                    is Boolean -> {
                        ps.setBoolean(index, convertedParam)
                    }
                    is Enum<*> -> {
                        ps.setInt(index, convertedParam.ordinal())
                    }
                    is Char -> {
                        ps.setString(index, convertedParam.toString())
                    }
                    else -> {
                        throw PersistenceException("unknown param type " + convertedParam.javaClass)
                    }
                }
            }
        }
    }
}
