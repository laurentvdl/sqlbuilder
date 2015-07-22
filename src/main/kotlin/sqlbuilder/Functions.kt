package sqlbuilder

import java.util.Arrays
import sqlbuilder.meta.PropertyReference

/**
 * Duplication of Kotlin extension f for Closable
 */
public inline fun <T : AutoCloseable, R> T.usea(block: (T) -> R): R {
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

public fun List<PropertyReference>.exclude(excludeFields: Array<out String>?): List<PropertyReference> {
    return if (excludeFields != null) crossReference(excludeFields, true) else this
}

public fun List<PropertyReference>.include(includeFields: Array<out String>?): List<PropertyReference> {
    return if (includeFields != null) crossReference(includeFields, true) else this
}

private fun List<PropertyReference>.crossReference(crossReference: Array<out String>, include: Boolean): List<PropertyReference> {
    Arrays.sort(crossReference)

    return this.filter { property ->
        val index = Arrays.binarySearch(crossReference, property.name)
        if (include) index >= 0 else index < 0
    }
}
