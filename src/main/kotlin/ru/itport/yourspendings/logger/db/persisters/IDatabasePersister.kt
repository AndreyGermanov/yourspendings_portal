package db.persisters

import cronjobs.ICronjobTask

import java.util.HashMap

internal interface IDatabasePersister : ICronjobTask {
    fun persist(): Int?
}
