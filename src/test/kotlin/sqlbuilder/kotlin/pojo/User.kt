package sqlbuilder.kotlin.pojo

import sqlbuilder.meta.Table

@Table("users")
data class User (
    var id: Long?,
    var username: String?,
    var birthYear: Short?,
    var files: MutableList<File>?
)