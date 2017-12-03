package sqlbuilder.kotlin.beans

import sqlbuilder.meta.Table

@Table("files")
data class File(
    var id: Long?,
    var userid: Long?,
    var name: String?,
    var attributes: MutableSet<Attribute>?
)