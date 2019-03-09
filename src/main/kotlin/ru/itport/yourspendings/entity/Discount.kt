package ru.itport.yourspendings.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import javax.persistence.*

@Entity
@Table(name="discounts")
data class Discount (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    val uid:Int?=0,

    @Column(name="name")
    val name:String,

    @OneToMany(mappedBy="discount")
    @JsonBackReference(value="purchase-discount")
    var purchaseDiscounts:List<PurchaseDiscount>? = null
)