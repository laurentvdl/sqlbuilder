package sqlbuilder.kotlin.select

import sqlbuilder.Relation
import sqlbuilder.ResultSet
import sqlbuilder.Select
import sqlbuilder.WhereGroup
import sqlbuilder.WhereGroupVisitor
import sqlbuilder.rowhandler.JoiningPagedRowHandler
import sqlbuilder.rowhandler.JoiningRowHandler
import kotlin.reflect.KProperty

inline fun <reified T : Any> Select.selectBeans(): List<T> = this.selectBeans(T::class.java)

inline fun <reified T : Any> Select.selectBean(): T? = this.selectBean(T::class.java)

inline fun <reified T : Any> Select.selectField(): T? = this.selectField(T::class.java)

inline fun <reified T : Any> Select.selectAllField(): List<T> = this.selectAllField(T::class.java)

fun <P : KProperty<Any?>> Select.excludeProperties(vararg excludes: P): Select = this.excludeFields(*(excludes.map { it.name }.toTypedArray()))

fun <P : KProperty<Any?>> Select.includeProperties(vararg includes: P): Select = this.includeFields(*(includes.map { it.name }.toTypedArray()))

inline fun Select.where(block: WhereGroup.() -> Unit) {
    val whereGroup = this.where()
    block(whereGroup)
    whereGroup.endWhere()
}

inline fun WhereGroup.group(relation: Relation = Relation.AND, block: WhereGroup.() -> Unit) {
    val group = this.group(relation)
    block(group)
    group.endGroup()
}

inline fun <T> WhereGroup.or(iterable: Iterable<T>, crossinline visitor: (subGroup: WhereGroup, item: T) -> Unit): WhereGroup {
    return this.or(iterable, object: WhereGroupVisitor<T> {
        override fun forItem(subGroup: WhereGroup, item: T) {
            visitor(subGroup, item)
        }
    })
}

inline fun <T> WhereGroup.and(iterable: Iterable<T>, crossinline visitor: (subGroup: WhereGroup, item: T) -> Unit): WhereGroup {
    return this.and(iterable, object: WhereGroupVisitor<T> {
        override fun forItem(subGroup: WhereGroup, item: T) {
            visitor(subGroup, item)
        }
    })
}

inline fun <reified T : Any> Select.selectJoinedEntities(joinedEntities: Set<Class<*>> = emptySet(), crossinline rowHandler: JoiningRowHandler<T>.(set: ResultSet, row: Int) -> Unit): List<T> {
    return this.select(object : JoiningRowHandler<T>() {
        override fun handle(set: ResultSet, row: Int): Boolean {
            rowHandler(this, set, row)
            return true
        }
    }.entities(joinedEntities.plus(T::class.java)))
}

inline fun <reified T : Any> Select.selectJoinedEntitiesPaged(offset: Int, rows: Int, prefix: String, joinedEntities: Set<Class<*>> = emptySet(), crossinline rowHandler: JoiningPagedRowHandler<T>.(set: ResultSet, row: Int) -> Unit): List<T> {
    return this.select(object : JoiningPagedRowHandler<T>(offset, rows, prefix) {
        override fun handleInPage(set: ResultSet, row: Int) {
            rowHandler(this, set, row)
        }
    }.entities(joinedEntities.plus(T::class.java)))
}