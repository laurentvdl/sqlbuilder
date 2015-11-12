package sqlbuilder.rowhandler

import sqlbuilder.PersistenceException
import sqlbuilder.ResultSet
import sqlbuilder.RowHandler
import java.io.IOException
import java.io.Writer
import java.sql.SQLException

/**
 * Used to pump a single CLOB field to a Writer.
 *
 * @author Laurent Van der Linden
 */
public class FieldWriterHandler(private val writer: Writer) : RowHandler {
    @Throws(SQLException::class)
    override fun handle(set: ResultSet, row: Int): Boolean {
        try {
            set.getJdbcResultSet().getCharacterStream(1)?.copyTo(writer, 16384)
            return false
        } catch (e: IOException) {
            throw PersistenceException("error copying clob to stream", e)
        }
    }
}
