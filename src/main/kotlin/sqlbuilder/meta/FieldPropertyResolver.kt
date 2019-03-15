package sqlbuilder.meta

import sqlbuilder.Configuration
import sqlbuilder.allFields
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.HashSet

class FieldPropertyResolver : PropertyResolver {
    override fun resolvePropertiesForBean(beanClass: Class<*>, mutators: Boolean, configuration: Configuration): List<PropertyReference> {
        val result = ArrayList<PropertyReference>()
        val names = HashSet<String>()

        val fields = beanClass.allFields
        for (field in fields) {
            val modifiers = field.modifiers
            val name = field.name!!
            if (!names.contains(name) && !Modifier.isAbstract(modifiers) && !Modifier.isStatic(modifiers) && isSqlType(field.type!!, configuration) && !isTransient(field)) {
                result.add(JavaFieldPropertyReference(field))
            }
        }

        return result
    }

    private fun isTransient(field: Field) = Modifier.isTransient(field.modifiers) || field.isAnnotationPresent(Transient::class.java)

    private fun isSqlType(clazz: Class<*>, configuration: Configuration): Boolean {
        return configuration.objectMapperForType(clazz) != null
    }
}