package sqlbuilder.rowhandler

import sqlbuilder.ResultSet
import java.sql.SQLException
import java.util.*

public abstract class BeanListRowHandler<T> : ListRowHandler<T> {
    var list = ArrayList<T>()

    override val result: MutableList<T> = list

    override fun handle(set: ResultSet, row: Int): Boolean {
        val listItem = mapSetToListItem(set)

        if (listItem != null) {
            list.add(listItem)
        }

        return true
    }


    @Throws(SQLException::class)
    abstract fun mapSetToListItem(set: ResultSet): T

}
