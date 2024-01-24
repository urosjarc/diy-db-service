package com.urosjarc.dbjesus.domain

import java.sql.JDBCType

class ObjProperties(
    val primaryKey: ObjProperty,
    val list: MutableList<ObjProperty> = mutableListOf(),
) {
    fun find(name: String): ObjProperty? =
        this.list.firstOrNull { it.name == name }

    fun sqlInsertColumns(escaper: String = "'", separator: String = ", "): String =
        this.list.joinToString(separator = separator) { "$escaper${it.name}$escaper" }

    fun sqlInsertValues(separator: String = ", "): String =
        this.list.joinToString(separator = separator) { "?" }

    fun sqlUpdate(escaper: String = "'", separator: String = ", ", zipper: String = " = "): String =
        this.list.joinToString(separator = separator) { "$escaper${it.name}$escaper$zipper?" }

    val values: MutableList<Any?> get() = this.list.map { it.value }.toMutableList()
    val jdbcTypes: MutableList<JDBCType> get() = this.list.map { it.jdbcType }.toMutableList()

    val encoders: MutableList<Encoder<Any>> get() = list.map { it.encoder }.toMutableList()
}
