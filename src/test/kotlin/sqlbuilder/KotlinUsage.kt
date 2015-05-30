package sqlbuilder

import org.junit.Test
import sqlbuilder.pool.DataSourceImpl
import sqlbuilder.pool.DefaultConfig
import sqlbuilder.pool.Drivers
import org.junit.Before
import kotlin.test.*
import sqlbuilder.impl.SqlBuilderImpl

class KotlinUsage {
    private val sqlBuilder = SqlBuilderImpl(DataSourceImpl(
            DefaultConfig(
                    null, null, "jdbc:h2:mem:test", Drivers.H2
            )
    ))

    Before fun setup() {
        Setup.createTables(sqlBuilder)

        assertEquals(1L, sqlBuilder.insert().getKeys(true).into("users").insertBean(with(User()) {
            username = "test a"
            this
        }), "generated key incorrect")

        assertEquals(2L, sqlBuilder.insert().getKeys(true).into("users").insertBean(with(User()) {
            username = "test b"
            this
        }), "generated key incorrect")

        sqlBuilder.insert().into("files").insertBean(with(File()) {
            userid = 1
            name = "home"
            this
        })

        sqlBuilder.insert().into("files").insertBean(with(File()) {
            userid = 1
            name = "guest"
            this
        })

        sqlBuilder.insert().into("attributes").insertBean(with(Attribute()) {
            fileid = 1
            name = "size"
            value = "2kb"
            this
        })
    }

    Test fun selectBeans() {
        val users = sqlBuilder.select().from("users").selectBeans(javaClass<User>())
        assertEquals(2, users.size(), "row count")
        assertEquals(1L, users[0].id)
        assertEquals(users[0].username, "test a")
    }

    Test fun selectMap() {
        val usersAsMaps = sqlBuilder.select().from("users").selectMaps("*")
        assertEquals(1L, usersAsMaps[0].get("id"))
        assertEquals(1L, usersAsMaps[0].get(0))
    }

    Test fun joinHandler() {
        val usersWithFiles = sqlBuilder.select()
                .sql("select * from users left join files on users.id = files.userid left join attributes on files.id = attributes.fileid")
                .select(object : JoiningRowHandler<User>() {
                    override fun handle(set: ResultSet, row: Int): Boolean {
                        val user = mapPrimaryBean(set, javaClass<User>(), "users")
                        val file = joinList(set, user, User::files, javaClass<File>(), "files")
                        joinSet(set, file, File::attributes, javaClass<Attribute>(), "attributes")
                        return true
                    }
                })
        assertEquals(2, usersWithFiles[0].files?.size(), "first user should have 2 files")
    }

    Test fun criteria() {
        val filteredCount = sqlBuilder.select()
                .from("users")
                .where()
                .and("username like ?", "%a")
                .and("username like ?", "test%")
                .endWhere()
                .selectField("count(*)", javaClass<Long>())

        assertEquals(1L, filteredCount, "expecting x users starting matching wildcard")
    }

    class User() {
        var id: Long? = null
        var username: String? = null
        var birthYear: Short? = null
        var files: MutableList<File>? = null
    }

    class File() {
        var id: Long? = null
        var userid: Long? = null
        var name: String? = null
        var attributes: MutableSet<Attribute>? = null
    }

    class Attribute() {
        var id: Long? = null
        var fileid: Long? = null
        var name: String? = null
        var value: String? = null
    }
}