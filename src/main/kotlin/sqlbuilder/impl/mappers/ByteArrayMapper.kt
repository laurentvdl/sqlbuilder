package sqlbuilder.impl.mappers

import sqlbuilder.mapping.*
import java.sql.Types

/**
 * Fallback Mapper to be used as a last resort.
 *
 * @author Laurent Van der Linden.
 */
class ByteArrayMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): ByteArray? {
        return params.resultSet.getBytes(params.index)
    }

    override fun toSQL(params: ToSQLMappingParameters) {
        if (params.value != null) {
            params.preparedStatement.setBytes(params.index, params.value as ByteArray)
        } else {
            params.preparedStatement.setNull(params.index, Types.BINARY)
        }
    }

    override fun handles(targetType: Class<*>): Boolean {
        return ByteArray::class.java == targetType
    }
}