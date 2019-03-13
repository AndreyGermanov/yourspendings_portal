package ru.itport.yourspendings.entity

import javax.persistence.*

@Entity
@Table(name="purchase_images")
data class PurchaseImage (

    @Id
    @Column(name="id")
    val uid:String,

    @Column(name="timestamp")
    val timestamp:Int,

    @ManyToOne
    @JoinColumn(name="purchase_id")
    val purchase:Purchase

):BaseModel()