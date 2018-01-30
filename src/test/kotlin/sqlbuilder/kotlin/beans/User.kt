package sqlbuilder.kotlin.beans

import sqlbuilder.meta.Table

@Table("users")
data class User (
    var id: Long?,
    var username: String?,
    var birthYear: Short?,
    var files: List<File>?
)