package sqlbuilder.mapping

import java.sql.ResultSet
import java.sql.SQLException

/**
 * Maps a SQL column to a Object (which can be null)
 *
 * @author Laurent Van der Linden.
 */
public interface ToObjectMapper {
    @Throws(SQLException::class)
    fun toObject(params: ToObjectMappingParameters): Any?
    fun handles(targetType: Class<*>): Boolean
}

public class ToObjectMappingParameters (val index: Int, val resultSet: ResultSet, val targetType: Class<*>)