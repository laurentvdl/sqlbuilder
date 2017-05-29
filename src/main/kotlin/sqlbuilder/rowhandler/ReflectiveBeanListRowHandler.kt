package sqlbuilder.rowhandler

import sqlbuilder.PersistenceException
import sqlbuilder.ResultSet
import sqlbuilder.meta.MetaResolver
import sqlbuilder.meta.PropertyReference

/**
 * @author Laurent Van der Linden
 */
open class ReflectiveBeanListRowHandler<T : Any>(protected var beanClass: Class<T>) : BeanListRowHandler<T>(),
        ReflectionHandler, PropertiesHandler {
    override var properties: List<PropertyReference>? = null
    override var metaResolver: MetaResolver? = null

    override fun mapSetToListItem(set: ResultSet): T {
        val bean = beanClass.newInstance()

        properties?.withIndex()?.forEach { pair ->
            try {
                pair.value.set(bean, set.getObject(pair.value.classType, pair.index + 1))
            } catch(e: Exception) {
                throw PersistenceException("failed to retreive value from resultset for property ${pair.value}", e)
            }
        }

        return bean
    }
}