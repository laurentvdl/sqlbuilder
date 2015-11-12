package sqlbuilder.impl.mappers

import sqlbuilder.mapping.*
import java.sql.Types

/**
 * Fallback Mapper to be used as a last resort.
 *
 * @author Laurent Van der Linden.
 */
class AnyMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Any? {
        return params.resultSet.getObject(params.index)
    }

    override fun toSQL(params: ToSQLMappingParameters) {
        if (params.value != null) {
            params.preparedStatement.setObject(params.index, params.value)
        } else {
            params.preparedStatement.setNull(params.index, Types.JAVA_OBJECT)
        }
    }

    override fun handles(targetType: Class<*>): Boolean {
        return true
    }
}