package com.urosjarc.dbmessiah.impl

import com.urosjarc.dbmessiah.Test_Serializer
import com.urosjarc.dbmessiah.data.Query
import com.urosjarc.dbmessiah.data.QueryValue
import com.urosjarc.dbmessiah.domain.C
import com.urosjarc.dbmessiah.domain.Table
import com.urosjarc.dbmessiah.impl.postgresql.PgSchema
import com.urosjarc.dbmessiah.impl.postgresql.PgSerializer
import com.urosjarc.dbmessiah.serializers.BasicTS
import com.urosjarc.dbmessiah.serializers.StringTS
import com.urosjarc.dbmessiah.serializers.UUIDTS
import org.junit.jupiter.api.BeforeEach
import java.sql.JDBCType
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class Test_PgSerializer : Test_Serializer() {
    override var testProcedures: Boolean = false
    override fun wrap(name: String): String = "\"$name\""

    @BeforeEach
    override fun init() {
        super.init()
        this.otherTables = listOf(
            Table(
                Other::pk, foreignKeys = listOf(
                    Other::cascades to Other::class
                ), constraints = listOf(
                    Other::unique to listOf(C.UNIQUE),
                    Other::cascades to listOf(C.CASCADE_DELETE, C.CASCADE_UPDATE)
                )
            )
        )
        this.schema = PgSchema(name = "main", tables = this.tables + this.otherTables + this.tablesNonAutoUUID)
        this.ser = PgSerializer(
            schemas = listOf(this.schema as PgSchema),
            globalSerializers = BasicTS.sqlite,
        )
    }

    @Test
    override fun `test plantUML`() {
        assertEquals(
            actual = this.ser.plantUML(),
            expected = listOf(
                "@startuml",
                "skinparam backgroundColor darkgray",
                "skinparam ClassBackgroundColor lightgray",
                "",
                "package main <<Folder>> {",
                "\t class main.Parent {",
                "\t\t pk: Int",
                "\t }",
                "\t class main.Child {",
                "\t\t pk: Int",
                "\t\t fk: Parent",
                "\t }",
                "\t class main.Other {",
                "\t\t pk: Int",
                "\t\t cascades: Other",
                "\t }",
                "\t class main.UUIDChild {",
                "\t\t pk: UUID",
                "\t }",
                "}",
                "",
                "main.Child -down-> main.Parent: fk",
                "main.Other -down-> main.Other: cascades",
                "",
                "@enduml"
            ).joinToString("\n")
        )
    }
    @Test
    override fun `test insertRow auto int incremental`() {
        listOf(false, true).forEach {
            assertEquals(
                actual = this.ser.insertRow(row = Parent(col = "col123"), batch = it),
                expected = Query(
                    sql = "INSERT INTO ${wrap("main")}.${wrap("Parent")} (${wrap("col")}) VALUES (?)" + if(!it) " RETURNING \"pk\"" else "",
                    QueryValue(name = "col", value = "col123", jdbcType = JDBCType.VARCHAR, encoder = StringTS.string(100).encoder)
                )
            )
        }
    }
    @Test
    override fun `test escaped name`() {
        assertEquals(expected = "\"name\"", actual = ser.escaped("name"))
    }

    @Test
    override fun `test createTable`() {
        assertEquals(
            expected = this.ser.createTable(Parent::class),
            actual = Query(sql = """CREATE TABLE IF NOT EXISTS "main"."Parent" ("pk" SERIAL PRIMARY KEY, "col" VARCHAR(100) NOT NULL)"""),
        )
        assertEquals(
            expected = this.ser.createTable(Child::class),
            actual = Query(sql = """CREATE TABLE IF NOT EXISTS "main"."Child" ("pk" INTEGER PRIMARY KEY, "fk" INTEGER NOT NULL, "col" VARCHAR(100) NOT NULL, FOREIGN KEY ("fk") REFERENCES "main"."Parent" ("pk"))"""),
        )
        assertEquals(
            expected = this.ser.createTable(Other::class),
            actual = Query(sql = """CREATE TABLE IF NOT EXISTS "main"."Other" ("pk" INTEGER PRIMARY KEY, "cascades" INTEGER NOT NULL, "canBeNull" INTEGER, "notNull" INTEGER NOT NULL, "unique" INTEGER NOT NULL UNIQUE, FOREIGN KEY ("cascades") REFERENCES "main"."Other" ("pk") ON UPDATE CASCADE ON DELETE CASCADE)"""),
        )
    }

    @Test
    override fun `test dropSchema`() {
        assertEquals(
            actual = this.ser.dropSchema(schema = this.schema),
            expected = Query(sql = "DROP SCHEMA IF EXISTS \"main\"")
        )
        assertEquals(
            actual = this.ser.dropSchema(schema = this.schema, cascade = true),
            expected = Query(sql = "DROP SCHEMA IF EXISTS \"main\" CASCADE")
        )
    }

    @Test
    override fun `test createSchema`() {
        assertEquals(
            actual = this.ser.createSchema(schema = this.schema),
            expected = Query(sql = "CREATE SCHEMA IF NOT EXISTS \"main\"")
        )
    }

    @Test
    override fun `test insertRow auto UUID incremental`() = Unit

    @Test
    override fun `test insertRow non auto uuid incremental`() {
        listOf(false, true).forEach {
            val row = UUIDChild(pk = UUID.randomUUID(), fk = UUID.randomUUID(), col = "col123")
            assertEquals(
                actual = this.ser.insertRow(row, batch = it),
                expected = Query(
                    sql = "INSERT INTO ${wrap("main")}.${wrap("UUIDChild")} (${wrap("pk")}, ${wrap("col")}, ${wrap("fk")}) VALUES (?, ?, ?)",
                    QueryValue(name = "pk", value = row.pk, jdbcType = JDBCType.CHAR, encoder = UUIDTS.sqlite.encoder),
                    QueryValue(name = "col", value = row.col, jdbcType = JDBCType.VARCHAR, encoder = StringTS.string(100).encoder),
                    QueryValue(name = "fk", value = row.fk, jdbcType = JDBCType.CHAR, encoder = UUIDTS.sqlite.encoder),
                )
            )
        }
    }

    override fun `test createProcedure`() {
        TODO("Not yet implemented")
    }

    override fun `test callProcedure`() {
        TODO("Not yet implemented")
    }

    override fun `test dropProcedure`() {
        TODO("Not yet implemented")
    }

}
