package sqlbuilder.impl.mappers

import sqlbuilder.mapping.BiMapper
import sqlbuilder.mapping.ToObjectMappingParameters
import sqlbuilder.mapping.ToSQLMappingParameters
import java.sql.Types

class EnumIndexMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Any? {
        val index = params.resultSet.getInt(params.index)
        if (params.resultSet.wasNull()) {
            return null
        }
        val enumConstants = params.targetType.enumConstants
        if (enumConstants.size <= index) {
            throw IllegalStateException("the enum ${params.targetType} does not have a value for ordinal $index")
        }
        return enumConstants[index]
    }

    override fun toSQL(params: ToSQLMappingParameters) {
        if (params.value != null) {
            params.preparedStatement.setInt(params.index, (params.value as Enum<*>).ordinal)
        } else {
            params.preparedStatement.setNull(params.index, Types.INTEGER)
        }
    }

    override fun handles(targetType: Class<*>): Boolean {
        return Enum::class.java.isAssignableFrom(targetType)
    }
}