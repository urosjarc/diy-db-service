package com.urosjarc.dbmessiah.sqlite

import com.urosjarc.dbmessiah.Engine
import com.urosjarc.dbmessiah.Serializer
import com.urosjarc.dbmessiah.impl.DbMessiahService

class SqliteService(
    eng: Engine,
    ser: Serializer,
) : DbMessiahService(eng = eng, ser = ser) {

    data class Ids(val id: Int)

    override fun <T : Any> insertTable(obj: T): Boolean {
        val T = this.ser.mapper.getTableInfo(obj = obj)
        val query = this.ser.insertQuery(obj = obj)
        val pQuery = this.eng.prepareQuery(query = query, autoGeneratedKey = T.primaryKey.kprop)

        val id = this.eng.executeInsert(pQuery = pQuery, onGeneratedKeysFail = "select last_insert_rowid();") { rs, i ->
            rs.getInt(i)
        }

        if (id != null) {
            T.primaryKey.set(obj = obj, value = id)
            return true
        }

        return false
    }
}
