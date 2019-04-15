package db.adapters

import java.util.ArrayList
import java.util.HashMap

/**
 * Interface which should implement any database adapter
 */
interface IDatabaseAdapter {
    fun configure(config: HashMap<String, Any>)
    fun insert(collectionName: String, data: ArrayList<HashMap<String, Any>>): Int?
    fun update(collectionName: String, data: ArrayList<HashMap<String, Any>>): Int?
    fun select(sql: String, collectionName: String?): ArrayList<HashMap<String, Any>>
}
