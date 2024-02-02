package com.urosjarc.dbmessiah

import com.urosjarc.dbmessiah.domain.schema.Schema
import com.urosjarc.dbmessiah.domain.table.Table
import kotlin.random.Random

data class TestProcedure(
    val child_pk: Int,
    val parent_pk: Int,
)

data class Input(
    val child_pk: Int,
    val parent_pk: Int,
)

data class Child(
    var pk: Int? = null,
    val fk: Int,
    val col: String
) {
    companion object {
        fun get(pk: Int? = null, fk: Int, seed: Int): Child {
            val random = Random(seed = seed)
            return Child(
                pk = pk,
                fk = fk,
                col = random.nextInt().toString()
            )
        }
    }
}

data class Parent(
    var pk: Int? = null,
    var col: String
) {
    companion object {
        fun get(pk: Int? = null, seed: Int): Parent {
            val random = Random(seed = seed)
            return Parent(pk = pk, col = random.nextInt().toString())
        }
    }
}


val testSchema = Schema(
    name = "main", tables = listOf(
        Table(primaryKey = Parent::pk),
        Table(
            primaryKey = Child::pk, foreignKeys = listOf(
                Child::fk to Parent::class
            )
        )
    )
)
