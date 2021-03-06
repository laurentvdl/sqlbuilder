package sqlbuilder.impl.mappers

import sqlbuilder.mapping.BiMapper
import sqlbuilder.mapping.ToObjectMappingParameters
import sqlbuilder.mapping.ToSQLMappingParameters
import java.sql.Types

/**
 * @author Laurent Van der Linden.
 */
class ShortMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Short? {
        val value = params.resultSet.getShort(params.index)
        return if (params.resultSet.wasNull()) {
            null
        } else {
            value
        }
    }

    override fun toSQL(params: ToSQLMappingParameters) {
        if (params.value != null) {
            params.preparedStatement.setShort(params.index, params.value as Short)
        } else {
            params.preparedStatement.setNull(params.index, Types.SMALLINT)
        }
    }

    override fun handles(targetType: Class<*>): Boolean {
        return Short::class.java == targetType || java.lang.Short::class.java == targetType
    }
}