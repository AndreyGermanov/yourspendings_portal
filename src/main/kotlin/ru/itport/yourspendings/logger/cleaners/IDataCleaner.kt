package cleaners

import java.util.HashMap

interface IDataCleaner {
    fun clean()
    fun configure(config: HashMap<String, Any>?)

}
