package sqlbuilder.rowhandler

import sqlbuilder.IncorrectJoinMapping
import sqlbuilder.PersistenceException
import sqlbuilder.ResultSet
import sqlbuilder.RowHandler
import sqlbuilder.meta.MetaResolver
import sqlbuilder.meta.PropertyReference
import java.lang.reflect.Field
import java.sql.SQLException
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedList
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

abstract class JoiningRowHandler<T : Any> : ListRowHandler<T>, RowHandler, ReflectionHandler, ExpandingRowHandler {
    private val beans = HashMap<MappingKey, Any>()
    private val propertyReferenceCache = HashMap<Class<*>, List<PropertyReference>>()
    private val columnToIndex: MutableMap<String, Int> = HashMap()
    private val tableAliasScopedPropertyToColumn: MutableMap<TableAliasScopedPropertyReference, String> = HashMap()
    private val relationFieldCache = HashMap<BeanProperty, Field>()
    private var expansionTypes = HashMap<String,Class<*>>()
    private val keyCache = HashMap<Class<*>, List<PropertyReference>>()

    override var metaResolver: MetaResolver? = null

    val list: MutableList<T> = ArrayList()

    override var result: MutableList<T> = list

    @Suppress("UNCHECKED_CAST")
    protected fun <S> getById(beanClass: Class<S>, prefix: String?,ids: List<Any?>): S? {
        return beans[MappingKey(beanClass, prefix, ids)] as S
    }

    protected fun <S : Any> putById(instance: S, prefix: String?, ids: List<Any?>): S {
        beans.put(MappingKey(instance.javaClass, prefix, ids), instance)
        return instance
    }

    protected fun putById(type: Class<*>, prefix: String?, ids: List<Any>) {
        beans.put(MappingKey(type, prefix, ids), Object())
    }

    protected open fun addPrimaryBean(instance: T) {
        list.add(instance)
    }

    /**
     * Map all columns from specified table to bean properties that exist.
     * @param set JDBC ResultSet
     * @param tableAlias prefix for fields that are to be mapped, by default this will be the table name if your JDBC drivers supports it,
     * otherwise specify a prefix for all columns either manually: users.id as users_id or using the prefix macro: {Users.* as users}
     * @param instance bean to map to
     * @throws SQLException
     * @return same instance populated
     */
    @Throws(SQLException::class)
    protected open fun <S : Any> mapSetToBean(set: ResultSet, tableAlias: String?, instance: S): S {
        createColumnToIndexCache(set)
        val javaClass = instance.javaClass
        var propertyReferences = propertyReferenceCache[javaClass]
        if (propertyReferences == null) {
            propertyReferences = metaResolver!!.getProperties(javaClass, true)
            propertyReferenceCache.put(javaClass, propertyReferences)
        }
        for (property in propertyReferences) {
            val index = getColumnIndex(tableAlias ?: metaResolver!!.getTableName(javaClass), property)
            if (index != null) {
                try {
                    property.set(instance, set.getObject(property.classType, index))
                } catch(e: SQLException) {
                    throw PersistenceException("failed to retreive ${property.name} from resultset at index $index using type ${property.classType}", e)
                }
            }
        }
        return instance
    }

    @SuppressWarnings("unchecked")
    @Throws(SQLException::class)
    protected fun <S> getColumnFromTable(set: ResultSet, prefix: String?, property: PropertyReference, propertyType: Class<S>): S? {
        createColumnToIndexCache(set)
        val index = getColumnIndex(prefix, property)
        if (index != null) {
            return set.getObject(propertyType, index)
        }
        return null
    }

    protected fun getColumnIndex(tableAlias: String?, property: PropertyReference): Int? {
        return if (tableAlias == null) {
            columnToIndex[property.columnName] ?: throw PersistenceException("no column was found for property $property using column name ${property.columnName}")
        } else {
            val columnName = tableAliasScopedPropertyToColumn[TableAliasScopedPropertyReference(tableAlias.toLowerCase(), property.columnName)]
                    ?: indexFQColumnName(property.columnName, tableAlias)
            columnToIndex[columnName] ?: columnToIndex[property.columnName]
        }
    }

    @Throws(SQLException::class)
    private fun createColumnToIndexCache(set: ResultSet) {
        if (columnToIndex.isEmpty()) {
            val metaData = set.getJdbcResultSet().metaData
            val columnCount = metaData.columnCount
            for (x in 1..columnCount) {
                val tableName = metaData.getTableName(x)
                val columnLabel = metaData.getColumnLabel(x)
                columnToIndex.put(columnLabel.toLowerCase(), x)
                if (tableName?.isNotEmpty() == true) {
                    val key = indexFQColumnName(columnLabel, tableName)
                    if (columnToIndex.containsKey(key)) {
                        throw IncorrectJoinMapping(set.query, x, columnToIndex[key]!!, tableName, columnLabel)
                    }
                    columnToIndex.put(key, x)
                }
            }
        }
    }

    private fun indexFQColumnName(column: String, table: String) = table.toLowerCase().replace('.', '_') + "_" + column.toLowerCase()

    /**
     * Map primary bean and add to resultlist in unique fashion
     * @param set active ResultSet
     * @param primaryType type of primary bean
     * @param prefix prefix for fields that are to be mapped, by default this will be the table name if your JDBC drivers supports it,
     * otherwise specify a prefix for all columns either manually: users.id as users_id or using the prefix macro: {Users.* as users}
     * @return newly mapped object or cached value if the primary result is not unique
     * @throws SQLException
     */
    fun mapPrimaryBean(set: ResultSet, primaryType: Class<T>, prefix: String?): T {
        val keyValues = getKeyValues(set, getKeys(primaryType), prefix, false)
        var instance = getById(primaryType, prefix, keyValues)
        if (instance == null) {
            instance = mapSetToBean(set, prefix?.toLowerCase(), primaryType.newInstance())
            addPrimaryBean(instance)
            putById(instance, prefix, keyValues)
        }

        return instance!!
    }

    protected fun getKeys(type: Class<*>): List<PropertyReference> {
        return keyCache.getOrPut(type) {
            val keys = metaResolver?.getKeys(type) ?: throw IllegalStateException("MetaResolver is not set")
            if (keys.isEmpty()) {
                throw IllegalArgumentException("No primary key is defined for type <$type>, annotate a key using @sqlbuilder.meta.Id")
            }
            @Suppress("UNCHECKED_CAST")
            return keys
        }
    }

    protected fun getKeyValues(set: ResultSet, keys: List<PropertyReference>, prefix: String?, allowNull: Boolean): List<Any?> {
        return keys.mapTo(LinkedList()) { key ->
            val value = getColumnFromTable(set, prefix, key, key.classType)
            if (value == null && !allowNull) {
                throw PersistenceException("no value was found for key $key in this resultset using column alias" +
                        " '${prefix}_${key.columnName}', make sure to include all key columns in the query: ${set.query}")
            }
            value
        }
    }

    private fun <R, W : Any> joinInstance(set: ResultSet, owner: R, targetType: Class<W>, prefix: String): W? {
        if (owner != null) {
            val keyValues = getKeyValues(set, getKeys(targetType), prefix, true)

            val inResultSet = keyValues.all { it != null }
            if (inResultSet) {
                // look in cache first
                @Suppress("UNCHECKED_CAST")
                var instance = getById(targetType, prefix, keyValues as List<Any>)
                if (instance == null) {
                    // create new instance
                    instance = mapSetToBean(set, prefix, targetType.newInstance())
                    putById(instance, prefix, keyValues)
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
                    embeddedSet = HashSet()
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
                    embeddedSet = ArrayList()
                    property.setter.call(owner, embeddedSet)
                }
                embeddedSet
            }!!

            relationList.add(instance)
        }

        return instance
    }

    inline fun <reified P : Any> join(set: ResultSet, owner: Any?, property: KProperty<Collection<P>?>, prefix: String?) =
            join(set, owner, property.name, P::class.java, prefix)

    /**
     * Automated joiner, using reflection to detect joined tables, relation cardinality and List initialization.
     * @param set active ResultSet
     * @param owner Object from which we join, which holds the specified property (can be null)
     * @param property attribute in owner that receives the joined result
     * @param targetType type of the joined result
     * @param prefix prefix for fields that are to be mapped, by default this will be the table name if your JDBC drivers supports it,
     * otherwise specify a prefix for all columns either manually: users.id as users_id or using the prefix macro: {Users.* as users}
     * @param <W> joined result type
     * @return the joined object that was attached to the owner
     * @throws SQLException
     */
    fun <W : Any> join(set: ResultSet, owner: Any?, property: String,
                                 targetType: Class<W>, prefix: String?): W? {
        if (owner != null) {
            val keyValues = getKeyValues(set, getKeys(targetType), prefix ?: metaResolver!!.getTableName(targetType), true)

            val inResultSet = keyValues.all { it != null }

            val cacheKey = BeanProperty(owner.javaClass, property)
            val relationField = relationFieldCache[cacheKey]
                    ?: run<Field> {
                        val relationField = metaResolver!!.findField(property, owner.javaClass)
                                ?: throw IllegalArgumentException("${owner.javaClass} has no property named <$property>")
                        relationField.isAccessible = true
                        relationFieldCache[cacheKey] = relationField
                        relationField
                    }

            val isList = List::class.java.isAssignableFrom(relationField.type!!)
            val isSet = Set::class.java.isAssignableFrom(relationField.type!!)

            initializeCollections<W>(isList, relationField, owner, isSet)

            if (inResultSet) {
                // look in cache first
                @Suppress("UNCHECKED_CAST")
                var instance = getById(targetType, prefix, keyValues)
                if (instance == null) {
                    // create new instance
                    instance = mapSetToBean(set, prefix, targetType.newInstance())
                    putById(instance, prefix, keyValues)
                }
                if (isList) {
                    @Suppress("UNCHECKED_CAST")
                    var relationList = relationField.get(owner) as MutableList<W>
                    if (!relationList.contains(instance)) relationList.add(instance!!)
                } else if (isSet) {
                    @Suppress("UNCHECKED_CAST")
                    val relationSet = relationField.get(owner) as MutableSet<W>
                    if (!relationSet.contains(instance)) relationSet.add(instance!!)
                } else {
                    relationField.set(owner, instance)
                }
                return instance!!
            }
        }
        return null
    }

    private fun <W : Any> initializeCollections(isList: Boolean, relationField: Field, owner: Any?, isSet: Boolean) {
        if (isList) {
            var relationList = relationField.get(owner) as MutableList<W>?
            if (relationList == null) {
                relationList = ArrayList()
                relationField.set(owner, relationList)
            }
        } else if (isSet) {
            var relationSet = relationField.get(owner) as HashSet<W>?
            if (relationSet == null) {
                relationSet = HashSet()
                relationField.set(owner, relationSet)
            }
        }
    }

    override fun expand(sql: String?): String? {
        if (sql != null) {
            return """\{(\w+)\.\*(\s+as\s+(\w+))?}""".toRegex(setOf(RegexOption.IGNORE_CASE)).replace(sql) { match ->
                val typeName = match.groupValues[1]
                val expansionAlias = match.groupValues[3]
                val type = expansionTypes[typeName]
                if (type != null) {
                    var columnIndex = 0
                    val table = metaResolver!!.getTableName(type).replace("""^(\w+\.)?(\w+)$""".toRegex(), "$2")
                    val alias = if (expansionAlias.isEmpty()) table.replace('.', '_') else expansionAlias
                    val columnPrefix = if (expansionAlias.isEmpty()) table else expansionAlias
                    val properties = metaResolver!!.getProperties(type, true)
                    properties.joinToString(",", transform = { prop ->
                        val columnAlias = "${alias}_${columnIndex++}"
                        tableAliasScopedPropertyToColumn[TableAliasScopedPropertyReference(alias.toLowerCase(), prop.columnName)] = columnAlias
                        "$columnPrefix.${prop.columnName} as $columnAlias"
                    })
                } else {
                    throw PersistenceException("type $typeName is not registered via JoiningRowHandler.entities(Class type) call")
                }

            }
        }

        return sql
    }

    fun entities(vararg types: Class<*>): JoiningRowHandler<T> = entities(types.toSet())

    fun entities(types: Set<Class<*>>): JoiningRowHandler<T> {
        for (type in types) {
            this.expansionTypes.put(type.simpleName, type)
        }
        return this
    }

    data class MappingKey(val aType: Class<*>, val prefix: String?, val keyValues: List<*>)

    data class BeanProperty(val aType: Class<*>, val property: String)

    data class TableAliasScopedPropertyReference(val tableAlias: String, val columnAlias: String)
}