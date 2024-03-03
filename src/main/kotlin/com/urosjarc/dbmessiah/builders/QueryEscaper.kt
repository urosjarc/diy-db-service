package com.urosjarc.dbmessiah.builders

import com.urosjarc.dbmessiah.Mapper
import com.urosjarc.dbmessiah.Serializer
import com.urosjarc.dbmessiah.exceptions.MappingException
import com.urosjarc.dbmessiah.extend.ext_owner
import kotlin.reflect.KProperty1


/**
 * The [QueryEscaper] class is responsible for building custom SQL [Query].
 *
 * @param IN The type of [input].
 * @property input The class where input values will be located.
 * @property mapper The [Mapper] object to help in serialization process.
 *
 * @throws MappingException if the [input] class is not registered in global inputs.
 */
public open class QueryEscaper(
    public val ser: Serializer
) {
    public fun <T : Any> name(column: KProperty1<T, *>): String {
        return ser.escaped(name = column.name)
    }

    public fun <T : Any> column(kprop: KProperty1<T, *>): String {
        val owner = kprop.ext_owner
        val T = this.ser.mapper.getTableInfo(kclass = owner)
        val C = T.getColumn(kprop = kprop) ?: throw MappingException("Table $T does not have registered column property: $kprop")
        return this.ser.escaped(column = C)
    }

    public inline fun <reified T : Any> INSERT(vararg kprops: KProperty1<T, *>): String {
        val columns = kprops.map { this.name(column = it) }.joinToString(", ")
        return "INSERT INTO ${this.table<T>()} ($columns)"
    }

    public inline fun <reified T : Any> SELECT(): String {
        return "SELECT * FROM ${this.table<T>()}"
    }

    public inline fun <reified T : Any> DELETE(): String {
        return "DELETE FROM ${this.table<T>()}"
    }

    public inline fun <reified T : Any> UPDATE(): String {
        return "UPDATE ${this.table<T>()}"
    }

    public inline fun <reified T : Any> table(): String {
        val T = this.ser.mapper.getTableInfo(kclass = T::class)
        return this.ser.escaped(tableInfo = T)
    }

    public inline fun <reified T : Any> procedure(): String {
        val P = this.ser.mapper.getProcedure(kclass = T::class)
        return this.ser.escaped(procedure = P)
    }

}
