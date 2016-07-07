package sqlbuilder.meta

import sqlbuilder.Configuration
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*

/**
 * Scans for public static fields containing info about the tablename and primary key(s):
 * <code>
 *   public final static String TABLE = "orders";
 *   public final static String[] KEYS = {"id"};
 * </code>
 * <br/>
 * Properties are searched by looking at regular getters/setters with a backing field and also
 * by looking at public fields.
 *
 * @author Laurent Van der Linden
 */
public class StaticJavaResolver(val configuration: Configuration) : MetaResolver {
    override fun getTableName(beanClass: Class<*>): String {
        try {
            val field = findField("TABLE", beanClass)
            if (field != null && field.type == String::class.java) {
                return field.get(null) as String
            }
        } catch (ignore: IllegalAccessException) {}

        return beanClass.simpleName.toLowerCase()
    }

    override fun getProperties(beanClass: Class<*>, mutators: Boolean): List<PropertyReference> {
        val prefix: String
        if (mutators) {
            prefix = "set"
        } else {
            prefix = "get"
        }
        val result = LinkedList<PropertyReference>()
        val names = HashSet<String>()
        val methods = beanClass.methods
        for (method in methods) {
            val name = method.name!!
            val methodModifiers = method.modifiers
            if (name.startsWith(prefix) && !Modifier.isTransient(methodModifiers) && !Modifier.isStatic(methodModifiers)) {
                val propertyName = name.substring(3, 4).toLowerCase() + name.substring(4)
                val flatName = propertyName.toLowerCase().toLowerCase()
                var accept = true
                val privateField = findField(propertyName, beanClass)
                if (privateField == null) {
                    accept = false
                } else {
                    val modifiers = privateField.modifiers
                    if (Modifier.isTransient(modifiers)) accept = false
                }
                if (accept) {
                    if (mutators) {
                        val parameters = method.parameterTypes
                        if (parameters != null && parameters.size == 1 && (isSqlType(parameters[0]) || Enum::class.java.isAssignableFrom(parameters[0]))) {
                            result.add(JavaGetterSetterPropertyReference(flatName, method, parameters[0]))
                            names.add(flatName)
                        }
                    } else {
                        val returnType = method.returnType
                        if (returnType != null && isSqlType(returnType)) {
                            result.add(JavaGetterSetterPropertyReference(flatName, method, returnType))
                            names.add(flatName)
                        }
                    }
                }
            }
        }
        val fields = beanClass.fields
        for (field in fields) {
            val modifiers = field.modifiers
            val name = field.name!!
            if (!names.contains(name) && Modifier.isPublic(modifiers) && !Modifier.isFinal(modifiers) && !Modifier.isAbstract(modifiers) && !Modifier.isStatic(modifiers) && isSqlType(field.type!!) && !Modifier.isTransient(modifiers)) {
                result.add(JavaFieldPropertyReference(name, field, field.type!!))
            }
        }
        return result.sortedBy { it.name }
    }

    private fun isSqlType(clazz: Class<*>): Boolean {
        return configuration.objectMapperForType(clazz) != null
    }

    override fun findField(name: String, fieldType: Class<*>): Field? {
        try {
            return fieldType.getDeclaredField(name)
        } catch (e: NoSuchFieldException) {
            val superclass = fieldType.superclass
            if (Any::class.java != superclass) return findField(name, superclass)
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    override fun getKeys(beanClass: Class<*>): Array<String> {
        try {
            val field: Field?
            field = findField("KEYS", beanClass)
            if (field != null) {
                if (field.type == Array<String>::class.java) {
                    return field.get(null) as Array<String>
                }
            }
        } catch (ignore: IllegalAccessException) {
        }

        return arrayOf("id")
    }
}
