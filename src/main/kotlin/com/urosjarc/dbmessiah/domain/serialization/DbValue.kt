package com.urosjarc.dbmessiah.domain.serialization

import com.urosjarc.dbmessiah.domain.queries.QueryValue
import com.urosjarc.dbmessiah.exceptions.ColumnException
import java.sql.JDBCType
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

abstract class DbValue(
    val kprop: KProperty1<Any, Any?>,
    val dbType: String,
    val jdbcType: JDBCType,
    val encoder: Encoder<*>,
    val decoder: Decoder<*>,
) {
    abstract val inited: Boolean
    abstract val path: String
    abstract val name: String

    val kclass: KClass<*> = kprop.returnType.classifier as KClass<*>

    val hash = (this.kclass.simpleName + this.kprop.name).hashCode()
    override fun equals(other: Any?): Boolean = this.hashCode() == other.hashCode()
    override fun hashCode(): Int = this.hash
    override fun toString(): String = "DbValue(name=${this.name}, dbType='${this.dbType}', jdbcType='${this.jdbcType.name}')"
    fun queryValue(obj: Any): QueryValue =
        QueryValue(name = this.name, value = this.getValue(obj = obj), jdbcType = this.jdbcType, encoder = this.encoder)

    fun setValue(obj: Any, value: Any?) {
        try {
            val kp = this.kprop as KMutableProperty1<Any, Any?>
            try {
                kp.set(receiver = obj, value = value)
            } catch (e: ClassCastException) {
                throw ColumnException(
                    msg = "Trying to set column $path value to '$value' but failed! Probably because incompatible types or receiving object is missing matching property: $obj",
                    cause = e
                )
            }
        } catch (e: ClassCastException) {
            throw ColumnException("Trying to set column $path value to '$value' but the column is immutable!", e)
        }

    }

    fun getValue(obj: Any): Any? {
        try {
            return this.kprop.get(receiver = obj)
        } catch (e: Throwable) {
            throw ColumnException(
                msg = "Trying to get object value $path but failed, probably because property '${this.kprop.name}' does not exists inside '${obj::class.simpleName}' object: $obj",
                cause = e
            )
        }

    }
}
