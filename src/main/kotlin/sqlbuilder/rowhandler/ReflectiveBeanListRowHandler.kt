package sqlbuilder.rowhandler

import sqlbuilder.PersistenceException
import sqlbuilder.ResultSet
import sqlbuilder.meta.PropertyReference
import sqlbuilder.meta.MetaResolver

/**
 * @author Laurent Van der Linden
 */
public open class ReflectiveBeanListRowHandler<T : Any>(protected var beanClass: Class<T>) : BeanListRowHandler<T>(),
        ReflectionHandler, PropertiesHandler {
    override var properties: List<PropertyReference>? = null
    override var metaResolver: MetaResolver? = null

    override fun mapSetToListItem(set: ResultSet): T {
        try {
            val bean = beanClass.newInstance()

            properties?.withIndex()?.forEach { pair ->
                pair.value.set(bean, set.getObject(pair.value.classType, pair.index + 1))
            }

            return bean
        } catch (e: Throwable) {
            throw PersistenceException(e.message, e)
        }
    }
}