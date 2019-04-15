package aggregators;

import cronjobs.CronjobTask
import cronjobs.CronjobTaskStatus
import main.ISyslog

import java.time.Instant

/**
 * Base class for all data aggregators
 */
abstract class DataAggregator : CronjobTask(), IDataAggregator {

    /**
     * Returns a type of collection of tasks, to which current task belongs (loggers, aggregators, archivers etc)
     * @return Collection name as string
     */
    override val collectionType: String
        get() = "aggregators"

    /**
     * Method, which Timer used to run this object as a Cronjob ("TimerTask" implementation)
     */
    override fun run() {
        super.run()
        aggregate()
        taskStatus = CronjobTaskStatus.IDLE
        lastExecTime = Instant.now().epochSecond
        if (syslog != null)
            syslog!!.log(ISyslog.LogLevel.DEBUG, "Task finished", this.javaClass.name, "run")
    }
}
