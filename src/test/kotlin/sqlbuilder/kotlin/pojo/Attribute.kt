package sqlbuilder.kotlin.pojo

import sqlbuilder.meta.Table

@Table("attributes")
class Attribute {
    var id: Long? = null
    var fileid: Long? = null
    var name: String? = null
    var value: String? = null
}