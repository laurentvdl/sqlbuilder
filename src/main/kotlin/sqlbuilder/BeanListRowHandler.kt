package sqlbuilder

import sqlbuilder.meta.PropertyReference

import java.util.ArrayList
import sqlbuilder.meta.MetaResolver

public class BeanListRowHandler<T>(protected var beanClass: Class<T>) : ListRowHandler<T>, ReflectionHandler, PropertiesHandler, BeanRowHandler<T> {
    override var list = ArrayList<T>()

    override val result: MutableList<T> = list

    override var properties: List<PropertyReference>? = null
    override var metaResolver: MetaResolver? = null

    override fun handle(set: ResultSet, row: Int): Boolean {
        try {
            val bean = beanClass.newInstance()

            properties?.withIndices()?.forEach { pair ->
                pair.second.set(bean, set.getObject(pair.second.classType, pair.first + 1))
            }

            handle(bean, set)
            return true
        } catch (e: Throwable) {
            throw PersistenceException(e.getMessage(), e)
        }

    }

    override fun handle(bean: T, set: ResultSet) {
        list.add(bean)
    }
}
