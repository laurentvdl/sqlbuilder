# SQLBuilder for SQL lovers

[![Build Status](https://travis-ci.org/laurentvdl/sqlbuilder.svg)](https://travis-ci.org/laurentvdl/sqlbuilder)

## What is it ?

- SQL ResultSet to POJO mapper
- query builder, allowing fluent construction of criteria arguments
- comparable to Springs `JdbcTemplate`

## Example:

### Java

```java
List<User> filteredUsers = sqlBuilder.select()
    .where()
        .group()
            .and("username like ?", "java%")
            .and("username like ?", "%user%")
            .and(usernameFilter != null, "username like ?", usernameFilter)
        .endGroup()
        .or("id > 10")
    .endWhere()
    .selectBeans(User.class);

List<User> allUsersAndFiles = sqlBuilder.select()
    .sql("select * from users left join files on users.id = files.userid left join attributes on files.id = attributes.fileid")
    .select(new JoiningRowHandler<User>() {
        @Override
        public boolean handle(@NotNull ResultSet set, int row) throws SQLException {
            final User user = mapPrimaryBean(set, User.class, User.TABLE);
            final File file = join(set, user, "files", File.class, File.TABLE);
            join(set, file, "attributes", Attribute.class, Attribute.TABLE);
            return true;
        }
    });
```

See more examples at <a href="https://github.com/laurentvdl/sqlbuilder/blob/master/src/test/java/sqlbuilder/JavaUsage.java">Java usage</a>.

### Kotlin

```kotlin
val usersWithFiles = sqlBuilder.select()
    .sql("select * from users left join files on users.id = files.userid left join attributes on files.id = attributes.fileid")
    .selectJoinedEntities<User>() { set, _ ->
        val user = mapPrimaryBean(set, User::class.java, "users")
        val file = join(set, user, User::files, "files")
        join(set, file, File::attributes, "attributes")
    }
```

See more examples at <a href="https://github.com/laurentvdl/sqlbuilder/blob/master/src/test/java/sqlbuilder/KotlinUsage.java">Kotlin usage</a>.

## How to use

### Gradle

```groovy
dependencies {
    compile 'com.github.sqlbuilder:sqlbuilder:1.7.0'
}
```

### Maven

```xml
<dependency>
    <groupId>com.github.sqlbuilder</groupId>
    <artifactId>sqlbuilder</artifactId>
    <version>1.7.0</version>
</dependency>
```

## Features:

- non intrusive mapping, no annotations or configuration required to map a query result to
  - a list of POJOs
  - a single POJO
  - a primitive
- supports mapping of joined tables with cursor based pagination (not in memory like Hibernate)
- all mappings are based on `RowHandler` interface
- @Table and @Column annotations are optional, but help repetitive configuration

## It does not:

- generate SQL but for the simplest of cases (selectBeans)
- try to enable type-safety on top of SQL
- create/update table structures
- abstract database dialects

In short, if you love SQL and want to map the result to some POJOs, sqlbuilder might be the tool for you.