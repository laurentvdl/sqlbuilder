package sqlbuilder.rowhandler

import sqlbuilder.PersistenceException
import sqlbuilder.ResultSet
import sqlbuilder.RowHandler
import java.io.IOException
import java.io.OutputStream
import java.sql.SQLException

/**
 * Used to pump a single BLOB field to an OutputStream.
 *
 * @author Laurent Van der Linden
 */
public class FieldStreamHandler(private val os: OutputStream) : RowHandler {
    @Throws(SQLException::class)
    override fun handle(set: ResultSet, row: Int): Boolean {
        val iStream = set.getJdbcResultSet().getBinaryStream(1)
        try {
            iStream?.copyTo(os, 16384)
            return false
        } catch (e: IOException) {
            throw PersistenceException("error copying blob to stream", e)
        }

    }
}
