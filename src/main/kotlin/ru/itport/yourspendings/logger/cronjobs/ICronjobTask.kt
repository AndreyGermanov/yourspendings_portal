package cronjobs

import java.util.HashMap

/**
 * Interface which should be implemented by any object to be able to execute as a task by cronjob (base class CronjobTask
 * shows sample of implementation)
 */
interface ICronjobTask {
    val name: String
    var isEnabled: Boolean
    var taskStatus: CronjobTaskStatus
    var lastExecTime: Long?
    var lastStartTime: Long?
    val taskInfo: HashMap<String, Any>
    val lastRecord: Any?
    val lastRecordTimestamp: Long
    val collectionType: String
    fun run()
}
