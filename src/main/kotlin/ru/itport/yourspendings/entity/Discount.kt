package ru.itport.yourspendings.entity

import javax.persistence.*

@Entity
@Table(name="discounts")
data class Discount (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    val id:Int?=0,

    @Column(name="name")
    val name:String,

    @OneToMany(mappedBy="discount")
    var purchaseDiscounts:List<PurchaseDiscount>? = null
)