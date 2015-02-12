package sqlbuilder.meta

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Date
import java.util.HashSet
import java.util.LinkedList
import java.lang
import org.slf4j.LoggerFactory

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
public class StaticJavaResolver() : MetaResolver {
    override fun <T> getTableName(beanClass: Class<T>): String {
        try {
            val field = findField("TABLE", beanClass)
            if (field != null && field.getType() == javaClass<String>()) {
                return field.get(null) as String
            }
        } catch (ignore: IllegalAccessException) {}

        return beanClass.getSimpleName().toLowerCase()
    }

    override fun <T> getProperties(beanClass: Class<T>, mutators: Boolean): MutableList<PropertyReference> {
        val prefix: String
        if (mutators) {
            prefix = "set"
        } else {
            prefix = "get"
        }
        val result = LinkedList<PropertyReference>()
        val names = HashSet<String>()
        val methods = beanClass.getMethods()
        for (method in methods) {
            val name = method.getName()!!
            val methodModifiers = method.getModifiers()
            if (name.startsWith(prefix) && !Modifier.isTransient(methodModifiers) && !Modifier.isStatic(methodModifiers)) {
                val propertyName = name.substring(3, 4).toLowerCase() + name.substring(4)
                val flatName = propertyName.toLowerCase().toLowerCase()
                var accept = true
                val privateField = findField(propertyName, beanClass)
                if (privateField == null) {
                    accept = false
                } else {
                    val modifiers = privateField.getModifiers()
                    if (Modifier.isTransient(modifiers)) accept = false
                }
                if (accept) {
                    if (mutators) {
                        val parameters = method.getParameterTypes()
                        if (parameters != null && parameters.size == 1 && (isSqlType(parameters[0]) || javaClass<Enum<*>>().isAssignableFrom(parameters[0]))) {
                            result.add(JavaGetterSetterPropertyReference(flatName, method, parameters[0]))
                            names.add(flatName)
                        }
                    } else {
                        val returnType = method.getReturnType()
                        if (returnType != null && isSqlType(returnType)) {
                            result.add(JavaGetterSetterPropertyReference(flatName, method, returnType))
                            names.add(flatName)
                        }
                    }
                }
            }
        }
        val fields = beanClass.getFields()
        for (field in fields) {
            val modifiers = field.getModifiers()
            val name = field.getName()!!
            if (!names.contains(name) && Modifier.isPublic(modifiers) && !Modifier.isFinal(modifiers) && !Modifier.isAbstract(modifiers) && !Modifier.isStatic(modifiers) && isSqlType(field.getType()!!) && !Modifier.isTransient(modifiers)) {
                result.add(JavaFieldPropertyReference(name, field, field.getType()!!))
            }
        }
        return result
    }

    override fun <T> findField(name: String, fieldType: Class<in T>): Field? {
        try {
            return fieldType.getDeclaredField(name)
        } catch (e: NoSuchFieldException) {
            val superclass = fieldType.getSuperclass()!!
            if (javaClass<Any>() != superclass) return findField(name, superclass)
        }

        return null
    }

    override fun <T> getKeys(beanClass: Class<T>): Array<String> {
        try {
            val field: Field?
            field = findField("KEYS", beanClass)
            if (field != null) {
                if (field.getType() == javaClass<Array<String>>()) {
                    return field.get(null) as Array<String>
                }
            }
        } catch (ignore: IllegalAccessException) {
        }

        return array<String>("id")
    }

    class object {
        private val logger = LoggerFactory.getLogger(javaClass())!!

        public fun isSqlType(fieldType: Class<*>): Boolean {
            val isSqlType = javaClass<String>() == fieldType ||
                    javaClass<Int>() == fieldType ||
                    javaClass<lang.Integer>() == fieldType ||
                    javaClass<Short>() == fieldType ||
                    javaClass<lang.Short>() == fieldType ||
                    javaClass<Double>() == fieldType ||
                    javaClass<Double>() == fieldType ||
                    javaClass<Long>() == fieldType ||
                    javaClass<lang.Long>() == fieldType ||
                    javaClass<Float>() == fieldType ||
                    javaClass<lang.Float>() == fieldType ||
                    javaClass<Char>() == fieldType ||
                    javaClass<Date>() == fieldType ||
                    javaClass<java.sql.Date>() == fieldType ||
                    javaClass<Timestamp>() == fieldType ||
                    javaClass<BigDecimal>() == fieldType ||
                    javaClass<ByteArray>() == fieldType ||
                    javaClass<Boolean>() == fieldType ||
                    javaClass<lang.Boolean>() == fieldType ||
                    javaClass<Enum<*>>().isAssignableFrom(fieldType)
            if (!isSqlType) {
                logger.debug("$fieldType is not a Sql type")
            }
            return isSqlType
        }
    }
}
