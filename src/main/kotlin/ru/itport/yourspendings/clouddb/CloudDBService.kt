package ru.itport.yourspendings.clouddb

interface CloudDBService {
    fun getLastData(collection:String,timestamp:Long):List<MutableMap<String,Any>>
}