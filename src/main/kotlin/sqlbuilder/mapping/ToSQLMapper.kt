package sqlbuilder.mapping

import java.sql.PreparedStatement
import java.sql.SQLException

/**
 * Maps a value to a JDBC PreparedStatement parameter at the specified index
 *
 * @author Laurent Van der Linden.
 */
public interface ToSQLMapper {
    @Throws(SQLException::class)
    fun toSQL(params: ToSQLMappingParameters)
    fun handles(targetType: Class<*>): Boolean
}

public class ToSQLMappingParameters (
    val index: Int, val preparedStatement: PreparedStatement, val value: Any?, val sourceType: Class<*>
)