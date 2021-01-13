package sqlbuilder.rowhandler

import org.slf4j.LoggerFactory
import sqlbuilder.Backend
import sqlbuilder.PersistenceException
import sqlbuilder.ResultSet
import sqlbuilder.meta.MetaResolver
import sqlbuilder.meta.PropertyReference
import java.util.ArrayList
import java.util.HashMap
import java.util.regex.Pattern

/**
 * Used for mapping custom query results to a bean.
 * Scans metadata and looks for a corresponding mutable property.
 *
 * @author Laurent Van der Linden
 */
class DynamicBeanRowHandler<T : Any>(private val beanClass: Class<T>, private val backend: Backend) : ListRowHandler<T>, ReflectionHandler, PropertiesHandler, BeanListRowHandler<T>() {
    private val trace = LoggerFactory.getLogger("sqlbuildertrace")

    var mappings: Map<Any, String>? = null

    @Suppress("SetterBackingFieldAssignment")
    override var properties: List<PropertyReference>? = null
        set(setters) {
            refCache.clear()
            for (ref in setters!!) {
                refCache[ref.columnName.toLowerCase()] = ref
            }
        }

    override var metaResolver: MetaResolver? = null

    private val refCache = HashMap<String, PropertyReference>()
    private val propertyCache: MutableList<PropertyReference?> = ArrayList()

    private val simplifier1 = Pattern.compile("(\\w+\\.)|\\.|-")
    private val simplifier2 = Pattern.compile("(\\w+\\.)|\\.|-|_")

    override fun mapSetToListItem(set: ResultSet): T {
        try {
            val meta = set.getJdbcResultSet().metaData
            val columns = meta.columnCount
            val bean = backend.beanFactory.instantiate(beanClass)
            for (index in 1..columns) {
                if (propertyCache.size < index) {
                    val cName = meta.getColumnLabel(index)!!.toLowerCase()
                    val mappedColumn = mappings?.get(cName) ?: mappings?.get(index) ?: cName
                    val prop = refCache[mappedColumn.toLowerCase()]
                            ?: refCache[simplifier1.matcher(cName).replaceAll("").toLowerCase()]
                            ?: refCache[simplifier2.matcher(cName).replaceAll("").toLowerCase()]

                    if (prop != null) {
                        propertyCache.add(prop)
                    } else {
                        trace.debug("cannot set '$cName' on <${beanClass.name}> $refCache")
                        propertyCache.add(null)
                    }
                }

                val prop = propertyCache[index - 1]
                prop?.set(bean, set.getObject(prop.classType, index))
            }

            return bean
        } catch (e: PersistenceException) {
            throw e
        } catch (e: Exception) {
            throw PersistenceException(e.message, e)
        }

    }

    override var result: MutableList<T> = list
}