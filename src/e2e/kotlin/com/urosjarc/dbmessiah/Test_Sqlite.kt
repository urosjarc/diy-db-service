package com.urosjarc.dbmessiah

import com.urosjarc.dbmessiah.domain.table.Table
import com.urosjarc.dbmessiah.exceptions.TesterException
import com.urosjarc.dbmessiah.impl.sqlite.SqliteSerializer
import com.urosjarc.dbmessiah.impl.sqlite.SqliteService
import com.urosjarc.dbmessiah.types.AllTS
import com.zaxxer.hikari.HikariConfig
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

open class Test_Sqlite {
    open var children = mutableListOf<Child>()
    open var parents = mutableListOf<Parent>()

    companion object {
        private lateinit var service: SqliteService

        @JvmStatic
        @BeforeAll
        fun init() {
            val conf = HikariConfig().apply {
                this.jdbcUrl = "jdbc:sqlite::memory:"
                this.username = null
                this.password = null
            }
            val ser = SqliteSerializer(
                tables = listOf(
                    Table(Parent::pk),
                    Table(Child::pk, listOf(Child::fk to Parent::class)),
                ),
                globalSerializers = AllTS.basic,
                globalOutputs = listOf(Output::class),
                globalInputs = listOf(Input::class),
            )
            service = SqliteService(conf = conf, ser)
        }
    }

    @BeforeEach
    fun seed() {
        service.query {
            it.drop(Child::class)
            it.drop(Parent::class)
            it.create(Parent::class)
            it.create(Child::class)
        }

        val numParents = 5
        val numChildren = 5
        children = mutableListOf()
        parents = mutableListOf()

        //Inserting tables
        service.query {
            repeat(times = numParents) { p ->
                val parent = Parent.get(seed = p)
                parents.add(parent)
                val parentInserted = it.insert(row = parent)
                if (parent.pk == null || !parentInserted) throw TesterException("Parent was not inserted: $parent")
                repeat(numChildren) { c ->
                    val child = Child.get(fk = parent.pk!!, seed = p * numChildren + c)
                    children.add(child)
                    val childInserted = it.insert(row = child)
                    if (child.pk == null || !childInserted) throw TesterException("Children was not inserted: $child")
                }
            }

            //Testing current state
            val insertedParents = it.select(table = Parent::class)
            val insertedChildren = it.select(table = Child::class)

            if (insertedChildren != children || insertedParents != parents)
                throw TesterException("Test state does not match with expected state")

        }
    }

    @Test
    fun insert() {
    }

}
