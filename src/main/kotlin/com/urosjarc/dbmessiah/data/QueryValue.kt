package com.urosjarc.dbmessiah.data

import java.sql.JDBCType

/**
 * Represents a SQL [QueryValue] to be used in a JDBC prepared statement.
 * This [QueryValue] will replace question marks in the final SQL statement.
 *
 * @property name The name of the value.
 * @property value The actual value.
 * @property jdbcType The JDBC data type of the value.
 * @property encoder The [Encoder] used to serialize the value.
 */
public data class QueryValue(
    public val name: String,
    public val value: Any?,
    public val jdbcType: JDBCType,
    public val encoder: Encoder<*>
) {


    /**
     * Represents [value] that will be escaped with quotations.
     */
    public val escapped: String
        get() = when (this.value) {
            is String -> "'$value'"
            is Char -> "'$value'"
            else -> this.value.toString()
        }

    /** @suppress */

    private val hash = this.name.hashCode()

    /** @suppress */
    override fun hashCode(): Int = this.hash

    /** @suppress */
    override fun toString(): String {
        return "$name: ($jdbcType,$escapped)"
    }

    /** @suppress */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as QueryValue
        return name == other.name
    }


}
