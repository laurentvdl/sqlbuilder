package sqlbuilder.meta

import sqlbuilder.Configuration
import sqlbuilder.allFields
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedList

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
        if (mutators) {
            return mutatorPropertiesCache.getOrPut(beanClass) {
                scanProperties(beanClass, mutators)
            }
        } else {
            return accessorPropertiesCache.getOrPut(beanClass) {
                scanProperties(beanClass, mutators)
            }
        }
    }

    fun scanProperties(beanClass: Class<*>, mutators: Boolean): List<PropertyReference> {
        val prefixes: Set<String>
        if (mutators) {
            prefixes = setOf("set")
        } else {
            prefixes = setOf("get", "is")
        }
        val result = LinkedList<PropertyReference>()
        val names = HashSet<String>()
        val methods = beanClass.methods
        for (method in methods) {
            val name = method.name!!
            val methodModifiers = method.modifiers
            if (prefixes.any { name.startsWith(it) } && !Modifier.isTransient(methodModifiers) && !Modifier.isStatic(methodModifiers)) {
                val propertyName = name.substring(3, 4).toLowerCase() + name.substring(4)
                var accept = true
                val privateField = findField(propertyName, beanClass)
                if (privateField == null) {
                    accept = false
                } else {
                    accept = accept && !isTransient(privateField)
                }
                if (accept) {
                    if (mutators) {
                        val parameters = method.parameterTypes
                        if (parameters != null && parameters.size == 1 && (isSqlType(parameters[0]) || Enum::class.java.isAssignableFrom(parameters[0]))) {
                            result.add(JavaGetterSetterPropertyReference(propertyName, method, parameters[0]))
                            names.add(propertyName)
                        }
                    } else {
                        val returnType = method.returnType
                        if (returnType != null && isSqlType(returnType)) {
                            result.add(JavaGetterSetterPropertyReference(propertyName, method, returnType))
                            names.add(propertyName)
                        }
                    }
                }
            }
        }

        if (result.isEmpty()) {
            val fields = beanClass.allFields
            for (field in fields) {
                val modifiers = field.modifiers
                val name = field.name!!
                if (!names.contains(name) && Modifier.isPublic(modifiers) && !Modifier.isFinal(modifiers) && !Modifier.isAbstract(modifiers) && !Modifier.isStatic(modifiers) && isSqlType(field.type!!) && !Modifier.isTransient(modifiers)) {
                    result.add(JavaFieldPropertyReference(field))
                }
            }
        }

        return result.sortedBy { it.columnName }
    }

    protected fun isTransient(privateField: Field): Boolean {
        val modifiers = privateField.modifiers
        return Modifier.isTransient(modifiers) || privateField.isAnnotationPresent(Transient::class.java)
    }

    private fun isSqlType(clazz: Class<*>): Boolean {
        return configuration.objectMapperForType(clazz) != null
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