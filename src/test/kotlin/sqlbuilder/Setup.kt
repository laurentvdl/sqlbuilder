package sqlbuilder

import kotlin.platform.platformStatic

object Setup {
    platformStatic fun createTables(sqlBuilder: SqlBuilder) {
        sqlBuilder.update().updateStatement("drop table users if exists")
        sqlBuilder.update().updateStatement("""
        create table if not exists users (
            id bigint auto_increment primary key,
            username varchar(255) not null,
            birthyear int,
            gender char(1)
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
}