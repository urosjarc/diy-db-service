package com.urosjarc.dbmessiah

import com.urosjarc.dbmessiah.domain.queries.BatchQuery
import com.urosjarc.dbmessiah.domain.queries.Page
import com.urosjarc.dbmessiah.domain.queries.QueryBuilder
import com.urosjarc.dbmessiah.exceptions.EngineException
import java.sql.Connection
import kotlin.reflect.KClass

/**
 * Every method will recive huge config object where all infos will be written
 */
open class QueryConnection(conn: Connection, val ser: Serializer) {

    val eng = Engine(conn = conn)

    fun <T : Any> drop(table: KClass<T>, cascade: Boolean = false): Int {
        val query = this.ser.dropQuery(kclass = table, cascade = cascade)
        return this.eng.update(query = query)
    }

    fun <T : Any> create(table: KClass<T>): Int {
        val query = this.ser.createQuery(kclass = table)
        return this.eng.update(query = query)
    }

    fun <T : Any> delete(table: KClass<T>, cascade: Boolean = false): Int {
        val query = this.ser.deleteQuery(kclass = table, cascade = cascade)
        return this.eng.update(query = query)
    }

    fun <T : Any> select(table: KClass<T>): List<T> {
        val query = this.ser.query(kclass = table)
        return this.eng.query(query = query) {
            this.ser.mapper.decode(resultSet = it, kclass = table)
        }
    }

    fun <T : Any> select(table: KClass<T>, page: Page<T>): List<T> {
        val query = this.ser.query(kclass = table, page = page)
        return this.eng.query(query = query) {
            this.ser.mapper.decode(resultSet = it, kclass = table)
        }
    }

    fun <T : Any> insert(row: T): Boolean {
        val T = this.ser.mapper.getTableInfo(obj = row)

        //If object has pk then reject it since its allready identified
        if (T.primaryKey.getValue(obj = row) != null) return false //Only objects who doesnt have primary key can be inserted!!!

        //Insert it
        val query = this.ser.insertQuery(obj = row, batch = false)
        val pk = this.eng.insert(query = query, onGeneratedKeysFail = this.ser.selectLastId) { rs, i -> rs.getInt(i) }

        //If pk didn't retrieved insert didn't happend
        if (pk == null) return false

        //Set primary key on object
        T.primaryKey.setValue(obj = row, value = pk)

        //Return success
        return true
    }

    fun <T : Any> update(row: T): Boolean {
        val T = this.ser.mapper.getTableInfo(obj = row)

        //If object has not pk then reject since it must be first created
        if (T.primaryKey.getValue(obj = row) == null) return false //Only objects who doesnt have primary key can be inserted!!!

        //Update object
        val query = this.ser.updateQuery(obj = row)

        //Return number of updates
        val count = this.eng.update(query = query)

        //Success if only 1
        if (count == 1) return true
        else if (count == 0) return false
        else throw EngineException("Number of updated rows must be 1 or 0 but number of updated rows was: $count")
    }

    fun <T : Any> delete(row: T, cascade: Boolean = false): Boolean {
        val T = this.ser.mapper.getTableInfo(obj = row)

        //If object has not pk then reject since it must be first created
        if (T.primaryKey.getValue(obj = row) == null) return false //Only objects who doesnt have primary key can be inserted!!!

        //Delete object if primary key exists
        val query = this.ser.deleteQuery(obj = row, cascade = cascade)

        //Update rows and get change count
        val count = this.eng.update(query = query)

        //Success if only 1
        if (count == 0) return false
        else if (count == 1) {
            T.primaryKey.setValue(obj = row, value = null)
            return true
        } else throw EngineException("Number of deleted rows must be 1 or 0 but number of updated rows was: $count")
    }

    fun <T : Any, K : Any> select(table: KClass<T>, pk: K): T? {
        val query = this.ser.query(kclass = table, pk = pk)
        return this.eng.query(query = query) {
            this.ser.mapper.decode(resultSet = it, kclass = table)
        }.firstOrNull()
    }


    /**
     * Managing rows in batch
     */
    fun <T : Any> insert(vararg rows: T): Int {
        val T = this.ser.mapper.getTableInfo(obj = rows[0])

        //Filter only those whos primary key is null
        val fobjs = rows.filter { T.primaryKey.getValue(it) == null }

        //If no object has free primary key then finish
        if (fobjs.isEmpty()) return 0

        //Insert it to db
        val query = this.ser.insertQuery(obj = fobjs[0], batch = true)

        //Execute query
        val batchQuery = BatchQuery(sql = query.sql, valueMatrix = fobjs.map { T.queryValues(obj = it).toList() })

        //Return count of updated elements
        return this.eng.batch(batchQuery = batchQuery)
    }

    fun <T : Any> update(vararg rows: T): Int {
        val T = this.ser.mapper.getTableInfo(obj = rows[0])

        //Filter only those whos primary key is null
        val fobjs = rows.filter { T.primaryKey.getValue(it) != null }

        //If no object has free primary key then finish
        if (fobjs.isEmpty()) return 0

        //Update objects
        val query = this.ser.updateQuery(obj = fobjs[0])
        val valueMatrix = fobjs.map { listOf(*T.queryValues(obj = it), T.primaryKey.queryValue(obj = it)) }
        val batchQuery = BatchQuery(sql = query.sql, valueMatrix = valueMatrix)

        //Return result
        return this.eng.batch(batchQuery = batchQuery)
    }

    fun <T : Any> delete(vararg rows: T, cascade: Boolean = false): Int {
        val T = this.ser.mapper.getTableInfo(obj = rows[0])

        //Filter only those whos primary key is not null
        val fobjs = rows.filter { T.primaryKey.getValue(it) != null }

        //If no object has free primary key then finish
        if (fobjs.isEmpty()) return 0

        //Delete objects
        val query = this.ser.deleteQuery(obj = fobjs[0], cascade = cascade)
        val valueMatrix = fobjs.map { listOf(T.primaryKey.queryValue(obj = it)) }
        val batchQuery = BatchQuery(sql = query.sql, valueMatrix = valueMatrix)

        //Return result
        return this.eng.batch(batchQuery = batchQuery)
    }


    fun <IN : Any> call(procedure: IN, vararg outputs: KClass<*>): List<List<Any>?> {
        val query = this.ser.callQuery(obj = procedure)
        return this.eng.execute(query = query) { i, rs ->
            this.ser.mapper.decodeMany(resultSet = rs, i = i, outputs = outputs)
        }
    }

    /**
     * Generic queries
     */
    fun query(getSql: () -> String): List<List<Any>?> {
        val query = this.ser.query(getSql = getSql)
        return this.eng.execute(query = query) { i, rs ->
            this.ser.mapper.decodeMany(resultSet = rs, i = i)
        }
    }

    fun query(vararg outputs: KClass<*>, getSql: () -> String): List<List<Any>?> {
        val query = this.ser.query(getSql = getSql)
        return this.eng.execute(query = query) { i, rs ->
            this.ser.mapper.decodeMany(resultSet = rs, i = i, outputs = outputs)
        }
    }

    fun <IN : Any> query(vararg outputs: KClass<*>, input: IN, getSql: (queryBuilder: QueryBuilder<IN>) -> String): List<List<Any>?> {
        val query = this.ser.query(input = input, getSql = getSql)
        return this.eng.execute(query = query) { i, rs ->
            this.ser.mapper.decodeMany(resultSet = rs, i = i, outputs = outputs)
        }
    }

}
