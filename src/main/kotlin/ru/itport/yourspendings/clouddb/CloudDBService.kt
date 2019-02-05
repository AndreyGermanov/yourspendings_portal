package ru.itport.yourspendings.clouddb

interface CloudDBService {

    fun init()
    fun syncData(callback:()->Unit)
    fun startDataSync()
    fun stopDataSync()
}