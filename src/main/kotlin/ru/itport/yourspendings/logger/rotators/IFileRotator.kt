package rotators

import java.util.HashMap

interface IFileRotator {
    fun rotate()
    fun configure(config: HashMap<String, Any>?)
}
