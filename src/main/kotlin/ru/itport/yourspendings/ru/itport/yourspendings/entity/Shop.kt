package ru.itport.yourspendings.ru.itport.yourspendings.entity

import javax.persistence.*

@Entity
@Table(name="shops")
data class Shop(
        @Id
        @Column(name="id")
        var id:String,
        @Column(name="name")
        val name:String,
        @Column(name="latitude")
        val latitude:Double,
        @Column(name="longitude")
        val longitude:Double,
        @Column(name="user_id")
        val userId:String,
        @OneToMany(mappedBy="place")
        var purchases:List<Purchase>? = null
)
