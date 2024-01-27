package com.urosjarc.dbmessiah

import com.urosjarc.dbmessiah.domain.columns.C
import com.urosjarc.dbmessiah.domain.schema.Schema
import com.urosjarc.dbmessiah.domain.table.Table
import com.urosjarc.dbmessiah.impl.DbMessiahEngine
import com.urosjarc.dbmessiah.impl.DbMessiahService
import com.urosjarc.dbmessiah.serializers.SqliteSerializer
import com.urosjarc.dbmessiah.impl.basicDbTypeSerializers
import com.zaxxer.hikari.HikariConfig

data class Entity2(
    val id_entity2: Int,
    val name: String,
    val username: String,
    val age: Int,
    val money: Float
)

data class Entity(
    val id_entity: Int?,
    val name: String,
    val username: String,
    val age: Int,
    val money: Float
)

fun main() {

    val config = HikariConfig().also {
        it.jdbcUrl = "jdbc:sqlite:/home/urosjarc/vcs/db-jesus/src/test/resources/chinook.sqlite"
        it.username = null
        it.password = null
    }

    val serializer = SqliteSerializer(
        escaper = "'",
        globalSerializers = basicDbTypeSerializers,
        schemas = listOf(
            Schema(
                name = "main", serializers = listOf(),
                tables = listOf(
                    Table(primaryKey = Entity::id_entity),
                    Table(
                        primaryKey = Entity2::id_entity2,
                        foreignKeys = listOf(
                            Entity2::age to String::class
                        ),
                        constraints = listOf(
                            Entity2::age to listOf(C.UNIQUE, C.AUTO_INC),
                            Entity2::name to listOf(C.UNIQUE, C.AUTO_INC)
                        )
                    )
                )
            )
        )
    )

    val service = DbMessiahService(
        eng = DbMessiahEngine(config = config),
        ser = serializer
    )
    val e = Entity(id_entity = null, name="Uros", username = "urosjarc", age=31, money = 0f)
    service.createTable(kclass = Entity::class)
    println(service.insertTable(e))
}
