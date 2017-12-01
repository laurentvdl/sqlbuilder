package sqlbuilder.rowhandler

import sqlbuilder.PersistenceException
import sqlbuilder.ResultSet
import sqlbuilder.ReturningRowHandler
import sqlbuilder.RowMap
import java.sql.SQLException
import java.util.ArrayList

class MapRowHandler : ReturningRowHandler<List<RowMap>> {
    private var columnCount: Int = 0
    private var columnNames: List<String?>? = null

    val list = ArrayList<RowMap>()

    override var result: List<RowMap> = list

    override fun handle(set: ResultSet, row: Int): Boolean {
        try {
            if (columnNames == null) {
                // cache meta data
                val meta = set.getJdbcResultSet().metaData
                columnCount = meta.columnCount
                columnNames = (1..columnCount).map { meta.getColumnName(it) }
            }
            val rowMap = RowMap()
            for (i in 0 until columnCount) {
                handleColumn(set, rowMap, columnNames!![i], i)
            }
            handleMap(rowMap)
            return true
        } catch (e: Throwable) {
            throw PersistenceException(e.message, e)
        }

    }

    @Throws(SQLException::class)
    private fun handleColumn(set: ResultSet, rowMap: RowMap, columnName: String?, column: Int) {
        if (columnName == null) {
            throw NullPointerException("no column name found for index $column")
        }
        val value = set.getJdbcResultSet().getObject(column + 1)
        rowMap.put(column, value)
        rowMap.put(columnName.trim(), value)
    }

    private fun handleMap(rowMap: RowMap) {
        list.add(rowMap)
    }
}