package sqlbuilder

import sqlbuilder.meta.PropertyReference
import sqlbuilder.meta.MetaResolver

/**
 * @author Laurent Van der Linden
 */
public open class ReflectiveBeanListRowHandler<T>(protected var beanClass: Class<T>) : BeanListRowHandler<T>(), ReflectionHandler, PropertiesHandler {
    override var properties: List<PropertyReference>? = null
    override var metaResolver: MetaResolver? = null

    override fun mapSetToListItem(resultSet: ResultSet): T {
        try {
            val bean = beanClass.newInstance()

            properties?.withIndex()?.forEach { pair ->
                pair.value.set(bean, resultSet.getObject(pair.value.classType, pair.index + 1))
            }

            return bean
        } catch (e: Throwable) {
            throw PersistenceException(e.getMessage(), e)
        }
    }
}