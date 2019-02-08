package ru.itport.yourspendings.clouddb

interface CloudDBService {
    fun syncData()
    fun startDataSync()
    fun stopDataSync()
}