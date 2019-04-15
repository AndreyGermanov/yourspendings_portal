package db.persisters

import cronjobs.CronjobTask
import cronjobs.CronjobTaskStatus

import java.time.Instant

abstract class DatabasePersister : CronjobTask(), IDatabasePersister {

    /**
     * Returns a type of collection of tasks, to which current task belongs (loggers, aggregators, archivers etc)
     * @return Collection name as string
     */
    override val collectionType: String
        get() = "persisters"

    /**
     * Method, which Timer used to run this object as a Cronjob ("TimerTask" implementation)
     */
    override fun run() {
        super.run()
        persist()
        taskStatus = CronjobTaskStatus.IDLE
        lastExecTime = Instant.now().epochSecond
    }
}
