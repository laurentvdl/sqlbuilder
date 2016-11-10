package sqlbuilder

import java.util.Arrays
import sqlbuilder.meta.PropertyReference
import java.lang.reflect.Field

/**
 * Duplication of Kotlin extension f for Closable
 */
inline fun <T : AutoCloseable, R> T.usea(block: (T) -> R): R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            this.close()
        } catch (ignore: Exception) {}
        throw e
    } finally {
        if (!closed) {
            this.close()
        }
    }
}

fun List<PropertyReference>.exclude(excludeFields: Array<out String>?): List<PropertyReference> {
    return if (excludeFields != null) crossReference(excludeFields, false) else this
}

fun List<PropertyReference>.include(includeFields: Array<out String>?): List<PropertyReference> {
    return if (includeFields != null) crossReference(includeFields, true) else this
}

private fun List<PropertyReference>.crossReference(crossReference: Array<out String>, include: Boolean): List<PropertyReference> {
    Arrays.sort(crossReference)

    return this.filter { property ->
        val index = Arrays.binarySearch(crossReference, property.name)
        if (include) index >= 0 else index < 0
    }
}

val RowHandler.result: Any?
    get() {
        if (this is ReturningRowHandler<*>) {
            return this.result
        } else if (this is OptionalReturningRowHandler<*>){
            return this.result
        } else {
            return null
        }
    }

val Class<*>.allFields: Array<Field>
    get() {
        val superclass = this.superclass
        if (Any::class.java != superclass) {
            return this.declaredFields.plus(superclass.allFields)
        }
        return this.declaredFields
    }