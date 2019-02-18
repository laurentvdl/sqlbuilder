package sqlbuilder.meta

import sqlbuilder.Configuration
import sqlbuilder.allFields
import java.lang.reflect.Field
import java.util.HashMap

/**
 * Scans for public static fields containing info about the tablename and primary key(s):
 * <code>
 *   public final static String TABLE = "orders";
 *   public final static String[] KEYS = {"id"};
 * </code>
 * <br/>
 * Properties are searched by looking at regular getters/setters with a backing field and also
 * by looking at public fields.
 */
open class ReflectionResolver(val configuration: Configuration) : MetaResolver {
    private val tableNameCache = HashMap<Class<*>,String>()
    private val mutatorPropertiesCache = HashMap<Class<*>,List<PropertyReference>>()
    private val accessorPropertiesCache = HashMap<Class<*>,List<PropertyReference>>()
    private val fieldForTypeCache = HashMap<FieldForTypeQuery,Field>()
    private val keysCache = HashMap<Class<*>,List<PropertyReference>>()

    override fun getTableName(beanClass: Class<*>): String {
        return tableNameCache.getOrPut(beanClass) {
            val annotatedTableName = beanClass.getAnnotation(Table::class.java)?.name
            if (annotatedTableName != null) {
                return annotatedTableName
            }

            try {
                val field = findField("TABLE", beanClass)
                if (field != null && field.type == String::class.java) {
                    return field.get(null) as String
                }
            } catch (ignore: IllegalAccessException) {}

            return beanClass.simpleName.toLowerCase()
        }
    }

    override fun getProperties(beanClass: Class<*>, mutators: Boolean): List<PropertyReference> {
        return if (mutators) {
            mutatorPropertiesCache.getOrPut(beanClass) {
                scanProperties(beanClass, mutators)
            }
        } else {
            accessorPropertiesCache.getOrPut(beanClass) {
                scanProperties(beanClass, mutators)
            }
        }
    }

    fun scanProperties(beanClass: Class<*>, mutators: Boolean): List<PropertyReference> {
        configuration.propertyResolutionStrategies.forEach { propertyResolver ->
            val propertiesForBean = propertyResolver.resolvePropertiesForBean(beanClass, mutators, configuration)
            if (propertiesForBean.isNotEmpty()) {
                return propertiesForBean.sortedBy { it.columnName }
            }
        }

        return emptyList()
    }

    override fun findField(name: String, fieldType: Class<*>): Field? {
        return fieldForTypeCache.getOrPut(FieldForTypeQuery(name, fieldType)) {
            try {
                return fieldType.getDeclaredField(name)
            } catch (e: NoSuchFieldException) {
                val superclass = fieldType.superclass
                if (Any::class.java != superclass) return findField(name, superclass)
            }

            return null
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun getKeys(beanClass: Class<*>): List<PropertyReference> {
        return keysCache.getOrPut(beanClass) {
            val keyFields = beanClass.allFields.filter { field -> field.isAnnotationPresent(Id::class.java) }
            if (keyFields.isNotEmpty()) {
                return keyFields.map(::JavaFieldPropertyReference)
            }

            try {
                val field = findField("KEYS", beanClass)
                if (field != null) {
                    if (field.type == Array<String>::class.java) {
                        val values = listOf(*field.get(null) as Array<String>)
                        return beanClass.allFields.filter { values.contains(it.name) }.map(::JavaFieldPropertyReference)
                    }
                }
            } catch (ignore: IllegalAccessException) {}

            val idField = findField("id", beanClass)
            if (idField != null) {
                return listOf(idField).map(::JavaFieldPropertyReference)
            }

            return emptyList()
        }
    }
}

private data class FieldForTypeQuery(val field: String, val type: Class<*>)