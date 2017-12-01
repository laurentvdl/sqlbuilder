package sqlbuilder.kotlin.update

import sqlbuilder.Update
import kotlin.reflect.KProperty

fun <P : KProperty<Any?>> Update.excludeProperties(vararg excludes: P): Update = this.excludeFields(*(excludes.map { it.name }.toTypedArray()))

fun <P : KProperty<Any?>> Update.includeProperties(vararg includes: P): Update = this.includeFields(*(includes.map { it.name }.toTypedArray()))
