package sqlbuilder

import sqlbuilder.meta.PropertyReference

import java.util.ArrayList
import sqlbuilder.meta.MetaResolver
import java.sql.SQLException

public abstract class BeanListRowHandler<T> : ListRowHandler<T> {
    override var list = ArrayList<T>()

    override val result: MutableList<T> = list

    override fun handle(set: ResultSet, row: Int): Boolean {
        val listItem = mapSetToListItem(set)

        if (listItem != null) {
            list.add(listItem)
        }

        return true
    }


    throws(SQLException::class)
    abstract fun mapSetToListItem(set: ResultSet): T

}
