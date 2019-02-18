package sqlbuilder.meta

import sqlbuilder.Configuration
import sqlbuilder.meta.util.findFieldInHierarchy
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.HashSet
import java.util.LinkedList

open class MethodPropertyResolver : PropertyResolver {
    override fun resolvePropertiesForBean(beanClass: Class<*>, mutators: Boolean, configuration: Configuration): List<PropertyReference> {
        val prefixes = if (mutators) {
            setOf("set")
        } else {
            setOf("get", "is")
        }
        val result = LinkedList<PropertyReference>()
        val names = HashSet<String>()

        val methods = beanClass.methods
        for (method in methods) {
            val name = method.name!!
            val methodModifiers = method.modifiers
            for (prefix in prefixes) {
                if (name.startsWith(prefix) && !Modifier.isTransient(methodModifiers) && !Modifier.isStatic(methodModifiers)) {
                    val propertyName = name.substring(prefix.length, prefix.length + 1).toLowerCase() + name.substring(prefix.length + 1)
                    var accept = true
                    val privateField = beanClass.findFieldInHierarchy(propertyName)
                    accept = if (privateField == null) {
                        false
                    } else {
                        accept && !isTransient(privateField)
                    }
                    if (accept) {
                        if (mutators) {
                            val parameters = method.parameterTypes
                            if (parameters != null && parameters.size == 1 && (isSqlType(parameters[0], configuration) || Enum::class.java.isAssignableFrom(parameters[0]))) {
                                result.add(JavaGetterSetterPropertyReference(propertyName, method, parameters[0]))
                                names.add(propertyName)
                            }
                        } else {
                            val returnType = method.returnType
                            if (returnType != null && isSqlType(returnType, configuration)) {
                                result.add(JavaGetterSetterPropertyReference(propertyName, method, returnType))
                                names.add(propertyName)
                            }
                        }
                    }
                }
            }
        }

        return result
    }

    private fun isSqlType(clazz: Class<*>, configuration: Configuration): Boolean {
        return configuration.objectMapperForType(clazz) != null
    }

    protected fun isTransient(privateField: Field): Boolean {
        val modifiers = privateField.modifiers
        return Modifier.isTransient(modifiers) || privateField.isAnnotationPresent(Transient::class.java)
    }
}