package ru.itport.yourspendings.entity

import java.util.*
import javax.persistence.*

@Entity
@Table(name="shops")
data class Shop (
        @Id
        @Column(name="id")
        var id:String?,
        @Column(name="name")
        val name:String,
        @Column(name="latitude")
        val latitude:Double,
        @Column(name="longitude")
        val longitude:Double,
        @Column(name="user_id")
        val userId:String,
        @Column(name="updated_at")
        override val updatedAt: Date,
        @OneToMany(mappedBy="place")
        var purchases:List<Purchase>? = null
): YModel(updatedAt)
