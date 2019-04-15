package db.adapters

import java.util.ArrayList
import java.util.HashMap

class FirebaseDatabaseAdapter(): DatabaseAdapter() {

    override fun processUpdateQuery(collectionName: String, data: ArrayList<HashMap<String, Any>>, isNew: Boolean): Int {
        return 0
    }

}