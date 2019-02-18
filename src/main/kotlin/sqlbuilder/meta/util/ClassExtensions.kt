package sqlbuilder.meta.util

import sqlbuilder.meta.Transient
import java.lang.reflect.Field
import java.lang.reflect.Modifier

fun Class<*>.findFieldInHierarchy(name: String): Field? {
    try {
        return this.getDeclaredField(name)
    } catch (e: NoSuchFieldException) {
        val superclass = this.superclass
        if (kotlin.Any::class.java != superclass) return superclass.findFieldInHierarchy(name)
    }

    return null
}

val Field.isTransient: Boolean
    get() {
        val modifiers = this.modifiers
        return Modifier.isTransient(modifiers) || this.isAnnotationPresent(Transient::class.java)
    }