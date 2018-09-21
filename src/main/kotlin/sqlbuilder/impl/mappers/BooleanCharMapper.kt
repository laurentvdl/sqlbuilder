package sqlbuilder.impl.mappers

import sqlbuilder.mapping.BiMapper
import sqlbuilder.mapping.ToObjectMappingParameters
import sqlbuilder.mapping.ToSQLMappingParameters
import java.sql.Types

/**
 * Maps boolean values to a configured database (VAR)CHAR
 */
class BooleanCharMapper(val trueChar: String = "X", val falseChar: String = "_") : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Any? {
        val textRepresentation = params.resultSet.getString(params.index)
                ?: return null
        return trueChar.equals(textRepresentation, ignoreCase = true)
    }

    override fun handles(targetType: Class<*>): Boolean {
        return Boolean::class.java == targetType || Boolean::class.javaPrimitiveType == targetType || java.lang.Boolean::class.java == targetType
    }

    override fun toSQL(params: ToSQLMappingParameters) {
        val booleanValue = params.value as Boolean?
        if (booleanValue != null) {
            params.preparedStatement.setString(params.index, if (booleanValue) trueChar else falseChar)
        } else {
            params.preparedStatement.setNull(params.index, Types.VARCHAR)
        }
    }
}
