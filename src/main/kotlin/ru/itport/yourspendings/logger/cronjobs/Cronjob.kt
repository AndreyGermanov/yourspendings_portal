package cronjobs

import java.util.TimerTask

/**
 * Class which implements cronjob for "Timer" instance
 */
class Cronjob
/**
 * Class constructor
 * @param task Task which cronjob should run (either logger, or aggregator or any other)
 * @param pollPeriod Run frequency in seconds
 */
(// Object, which used as a task
        /**
         * Returns task object which this cronjob runs
         * @return
         */
        val task: ICronjobTask, pollPeriod: Int) : TimerTask() {
    // Cronjob start frequency in seconds
    /**
     * Returns cronjob run frequency
     * @return
     */
    var pollPeriod = 0

    init {
        this.pollPeriod = pollPeriod
    }

    /**
     * Method which Timer thread calls every time to run cronjob
     */
    override fun run() {
        if (task.isEnabled) {
            task.run()
        }
    }
}