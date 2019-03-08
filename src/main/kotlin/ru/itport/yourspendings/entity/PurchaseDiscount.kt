package ru.itport.yourspendings.entity

import javax.persistence.*

@Entity
@Table(name="purchases_discounts")
data class PurchaseDiscount(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    val uid:Long?=0,

    @Column(name="amount")
    val amount:Double,

    @ManyToOne
    @JoinColumn(name="purchase_id")
    val purchase: Purchase,

    @ManyToOne
    @JoinColumn(name="discount_id")
    val discount: Discount
)