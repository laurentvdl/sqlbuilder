package sqlbuilder.impl.mappers

import sqlbuilder.mapping.BiMapper
import sqlbuilder.mapping.ToObjectMappingParameters
import sqlbuilder.mapping.ToSQLMappingParameters
import java.sql.Types

class EnumNameMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Any? {
        val name = params.resultSet.getString(params.index)
        if (params.resultSet.wasNull()) {
            return null
        }
        val enumConstants = params.targetType.enumConstants
        return enumConstants.find { (it as Enum<*>).name == name }
    }

    override fun toSQL(params: ToSQLMappingParameters) {
        if (params.value != null) {
            params.preparedStatement.setString(params.index, (params.value as Enum<*>).name)
        } else {
            params.preparedStatement.setNull(params.index, Types.VARCHAR)
        }
    }

    override fun handles(targetType: Class<*>): Boolean {
        return Enum::class.java.isAssignableFrom(targetType)
    }
}