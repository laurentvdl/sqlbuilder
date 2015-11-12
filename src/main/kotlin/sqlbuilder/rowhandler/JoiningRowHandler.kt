package sqlbuilder.rowhandler

import sqlbuilder.ResultSet
import sqlbuilder.RowHandler
import sqlbuilder.meta.MetaResolver
import sqlbuilder.meta.PropertyReference
import java.lang.reflect.Field
import java.sql.SQLException
import java.util.*
import kotlin.reflect.KMutableProperty

public abstract class JoiningRowHandler<T : Any> : ListRowHandler<T>, RowHandler, ReflectionHandler {
    private val beans = HashMap<MappingKey, Any>()
    private val propertyReferenceCache = HashMap<Class<*>, List<PropertyReference>>()
    private val columnToIndex: MutableMap<String, Int> = HashMap()
    private val relationFieldCache = HashMap<BeanProperty, Field>()

    override var metaResolver: MetaResolver? = null

    val list: MutableList<T> = ArrayList()

    override var result: MutableList<T> = list

    @Suppress("UNCHECKED_CAST")
    protected fun <S> getById(beanClass: Class<S>, ids: List<Any?>): S? {
        return beans[MappingKey(beanClass, ids)] as S
    }

    protected fun <S : Any> putById(instance: S, ids: List<Any?>): S {
        beans.put(MappingKey(instance.javaClass, ids), instance)
        return instance
    }

    protected fun putById(type: Class<*>, ids: List<Any>) {
        beans.put(MappingKey(type, ids), Object())
    }

    protected open fun addPrimaryBean(instance: T) {
        list.add(instance)
    }

    /**
     * Map all columns from specified table to bean properties that exist.
     * @param set JDBC ResultSet
     * @param table name of table without schema/catalog
     * @param instance bean to map to
     * @param mappings map bean property name -> column name
     * @throws SQLException
     * @return same instance populated
     */
    @Throws(SQLException::class)
    protected open fun <S : Any> mapSetToBean(set: ResultSet, table: String, instance: S, mappings: Map<String, String>? = null): S {
        createColumnToIndexCache(set)
        val javaClass = instance.javaClass
        var propertyReferences = propertyReferenceCache[javaClass]
        if (propertyReferences == null) {
            propertyReferences = metaResolver!!.getProperties(javaClass, true)
            propertyReferenceCache.put(javaClass, propertyReferences)
        }
        for (pr in propertyReferences) {
            val property = mappings?.get(pr.name) ?: pr.name
            val index = getColumnIndex(table, property)
            if (index != null) {
                pr.set(instance, set.getObject(pr.classType, index))
            }
        }
        return instance
    }

    @SuppressWarnings("unchecked")
    @Throws(SQLException::class)
    protected fun <S> getColumnFromTable(set: ResultSet, table: String, column: String, propertyType: Class<S>): S? {
        createColumnToIndexCache(set)
        val index = getColumnIndex(table, column)
        if (index == null) return null
        return set.getObject(propertyType, index)
    }

    protected fun getColumnIndex(table: String?, column: String): Int? {
        if (table == null) {
            return columnToIndex.get(column)
        } else {
            return columnToIndex.get(table + column) ?: columnToIndex.get(column)
        }
    }

    @Throws(SQLException::class)
    private fun createColumnToIndexCache(set: ResultSet) {
        val metaData = set.getJdbcResultSet().metaData
        val columnCount = metaData.columnCount
        for (x in 1..columnCount) {
            val tableName = metaData.getTableName(x)
            if (tableName?.isEmpty() ?: true) {
                columnToIndex.put(metaData.getColumnLabel(x)!!.toLowerCase(), x)
            } else {
                columnToIndex.put(tableName!!.toLowerCase() + metaData.getColumnLabel(x)?.toLowerCase(), x)
            }
        }
    }

    /**
     * Map primary bean and add to resultlist in unique fashion
     * @param set active ResultSet
     * @param primaryType type of primary bean
     * @param table table containing primary bean columns
     * @return newly mapped object or cached value if the primary result is not unique
     * @throws SQLException
     */
    protected fun mapPrimaryBean(set: ResultSet, primaryType: Class<T>, table: String): T {
        val keyValues = getKeyValues(set, primaryType, table);
        var instance = getById(primaryType, keyValues);
        if (instance == null) {
            instance = mapSetToBean(set, table, primaryType.newInstance());
            addPrimaryBean(instance)
            putById(instance, keyValues)
        }

        return instance!!
    }

    protected fun <T> getKeyValues(set: ResultSet, aType: Class<T>, table: String): List<Any?> {
        return metaResolver!!.getKeys(aType).mapTo(LinkedList<Any?>()) { key ->
            getColumnFromTable(set, table, key, Any::class.java)
        }
    }

    private fun <R, W : Any> joinInstance(set: ResultSet, owner: R, targetType: Class<W>, table: String): W? {
        if (owner != null) {
            val keyValues = getKeyValues(set, targetType, table)

            var inResultSet = keyValues.all { it != null }
            if (inResultSet) {
                // look in cache first
                @Suppress("UNCHECKED_CAST")
                var instance = getById(targetType, keyValues as List<Any>)
                if (instance == null) {
                    // create new instance
                    instance = mapSetToBean(set, table, targetType.newInstance())
                    putById(instance, keyValues)
                }
                return instance
            }
        }

        return null
    }

    protected fun <R, W : Any> joinSet(set: ResultSet, owner: R, property: KMutableProperty<MutableSet<W>?>,
                                       targetType: Class<W>, table: String): W? {
        val instance = joinInstance(set, owner, targetType, table)
        if (instance != null) {
            val relationSet = run {
                var embeddedSet = property.getter.call(owner)
                if (embeddedSet == null) {
                    embeddedSet = HashSet<W>()
                    property.setter.call(owner, embeddedSet)
                }
                embeddedSet
            }!!

            relationSet.add(instance)
        }

        return instance
    }

    protected fun <R, W : Any> joinList(set: ResultSet, owner: R, property: KMutableProperty<MutableList<W>?>,
                                        targetType: Class<W>, table: String): W? {
        val instance = joinInstance(set, owner, targetType, table)
        if (instance != null) {
            val relationList = run {
                var embeddedSet = property.getter.call(owner)
                if (embeddedSet == null) {
                    embeddedSet = ArrayList<W>()
                    property.setter.call(owner, embeddedSet)
                }
                embeddedSet
            }!!

            relationList.add(instance)
        }

        return instance
    }

    /**
     * Automated joiner, using reflection to detect joined tables, relation cardinality and List initialization.
     * @param set active ResultSet
     * @param owner Object from which we join, which holds the specified property (can be null)
     * @param property attribute in owner that receives the joined result
     * @param targetType type of the joined result
     * @param table table of the joined result
     * @param <W> joined result type
     * @return the joined object that was attached to the owner
     * @throws SQLException
     */
    protected fun <W : Any> join(set: ResultSet, owner: Any?, property: String,
                                 targetType: Class<W>, table: String): W? {
        if (owner != null) {
            val keyValues = getKeyValues(set, targetType, table)

            val cacheKey = BeanProperty(owner.javaClass, property)
            val relationField = relationFieldCache.get(cacheKey)
                    ?: run<Field> {
                val relationField = metaResolver!!.findField(property, owner.javaClass)
                if (relationField == null) throw IllegalArgumentException("${owner.javaClass} has no property named <$property>")
                relationField.isAccessible = true
                relationFieldCache.put(cacheKey, relationField)
                relationField
            }

            val isList = List::class.java.isAssignableFrom(relationField.type!!)
            val isSet = Set::class.java.isAssignableFrom(relationField.type!!)

            // look in cache first
            @Suppress("UNCHECKED_CAST")
            var instance = getById(targetType, keyValues)
            if (instance == null) {
                // create new instance
                instance = mapSetToBean(set, table, targetType.newInstance())
                putById(instance, keyValues)
            }
            if (isList) {
                @Suppress("UNCHECKED_CAST")
                var relationList = relationField.get(owner) as MutableList<W>?
                if (relationList == null) {
                    relationList = ArrayList<W>()
                    relationField.set(owner, relationList)
                }
                if (!relationList.containsRaw(instance)) relationList.add(instance!!)
            } else
                if (isSet) {
                    @Suppress("UNCHECKED_CAST")
                    val relationSet = relationField.get(owner) as MutableSet<W>? ?:
                            run<MutableSet<W>> {
                                val setValue = HashSet<W>()
                                relationField.set(owner, setValue)
                                setValue
                            }
                    if (!relationSet.containsRaw(instance)) relationSet.add(instance!!)
                } else {
                    relationField.set(owner, instance)
                }
            return instance!!
        }
        return null
    }

    data class MappingKey(val aType: Class<*>, val keyValues: List<*>)

    data class BeanProperty(val aType: Class<*>, val property: String)
}