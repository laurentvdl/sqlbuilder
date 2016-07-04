package sqlbuilder

import org.slf4j.LoggerFactory
import sqlbuilder.mapping.ToSQLMappingParameters
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Types

/**
 * Helper class that sets/gets from sql objects according to type.
 */
public class SqlConverter(private val configuration: Configuration) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Throws(SQLException::class)
    fun setParameter(ps: PreparedStatement, param: Any?, index: Int, valueType: Class<*>? = null, sqlType: Int? = null) {
        var convertedParam = param
        var convertedValueType = valueType

        logger.debug("set({},{})", index, convertedParam)

        if (convertedParam == null) {
            if (sqlType != null) {
                ps.setNull(index, sqlType)
            } else {
                when (valueType) {
                    null -> {
                        try {
                            ps.setNull(index, Types.NULL)
                        } catch (e: SQLException) {
                            throw PersistenceException("null parameter without type at index<$index>", e)
                        }
                    }
                    else -> {
                        configuration.sqlMapperForType(valueType)?.toSQL(ToSQLMappingParameters(index, ps, null, valueType)) ?:
                            throw PersistenceException("no mapper registered for type $valueType")
                    }
                }
            }
        } else {
            if (convertedParam is LazyValue) {
                try {
                    convertedParam = convertedParam.eval()
                    if (convertedParam != null) {
                        convertedValueType = convertedParam.javaClass
                    }
                } catch (e: Exception) {
                    throw PersistenceException("unable to eval lazy value: " + convertedParam, e)
                }
            }

            if (convertedValueType != null) {
                val parameters = ToSQLMappingParameters(index, ps, convertedParam, convertedValueType)
                configuration.sqlMapperForType(convertedValueType)?.toSQL(parameters)
            } else {
                ps.setObject(index, convertedParam)
            }
        }
    }
}
