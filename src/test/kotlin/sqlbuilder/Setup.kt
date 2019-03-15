package sqlbuilder

import sqlbuilder.impl.SqlBuilderImpl
import sqlbuilder.pool.DataSourceImpl
import sqlbuilder.pool.DefaultConfig
import sqlbuilder.pool.Drivers
import kotlin.test.assertEquals

object Setup {
    @JvmStatic fun createTables() {
        sqlBuilder.update().updateStatement("drop table users if exists")
        sqlBuilder.update().updateStatement("""
        create table if not exists users (
            id bigint auto_increment primary key,
            username varchar(255) not null,
            birthyear int,
            sex char(1),
            parent_id bigint,
            superuser smallint,
            active smallint
        )
        """)

        sqlBuilder.update().updateStatement("drop table files if exists")
        sqlBuilder.update().updateStatement("""
        create table if not exists files (
            id bigint auto_increment primary key,
            userid bigint not null,
            name varchar(255) not null,
             foreign key(userid) references users(id) on delete cascade
        )
        """)

        sqlBuilder.update().updateStatement("drop table attributes if exists")
        sqlBuilder.update().updateStatement("""
        create table if not exists attributes (
            id bigint auto_increment primary key,
            fileid bigint not null,
            name varchar(255) not null,
            value varchar(255) not null,
            foreign key(fileid) references files(id) on delete cascade
        )
        """)
    }

    val dataSource = DataSourceImpl(
            DefaultConfig(
                    null, null, "jdbc:h2:mem:test", Drivers.H2
            )
    )
    private val sqlBuilder = SqlBuilderImpl(dataSource)

    fun insertOneUser(username: String = "test a"): Long {
        val key = sqlBuilder.insert().getKeys(true).insertBean(CachingTest.SerializableUser(
                username = username,
                id = null
        ))
        assertEquals(1L, key, "generated key incorrect")
        return key
    }
}