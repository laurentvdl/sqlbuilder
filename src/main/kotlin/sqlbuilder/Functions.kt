package sqlbuilder

import sqlbuilder.meta.PropertyReference
import java.lang.reflect.Field

fun List<PropertyReference>.exclude(excludeFields: Set<String>?): List<PropertyReference> {
    return if (excludeFields != null) crossReference(excludeFields, false) else this
}

fun List<PropertyReference>.include(includeFields: Set<String>?): List<PropertyReference> {
    return if (includeFields != null) crossReference(includeFields, true) else this
}

private fun List<PropertyReference>.crossReference(crossReference: Set<String>, include: Boolean): List<PropertyReference> {
    return this.filter { property -> if (include) crossReference.contains(property.name) else crossReference.contains(property.name).not() }
}

val RowHandler.result: Any?
    get() = when {
        this is ReturningRowHandler<*> -> this.result
        this is OptionalReturningRowHandler<*> -> this.result
        else -> null
    }

val Class<*>.allFields: Array<Field>
    get() {
        val superclass = this.superclass
        if (Any::class.java != superclass) {
            return this.declaredFields.plus(superclass.allFields)
        }
        return this.declaredFields
    }