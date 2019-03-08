package ru.itport.yourspendings.entity

import javax.persistence.*

@Entity
@Table(name="purchase_products")
class PurchaseProduct (

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    var uid:Long? = null,

    @Column(name="name")
    val name:String,

    @Column(name="price")
    val price:Double,

    @Column(name="discount")
    var discount:Double = 0.0,

    @Column(name="count")
    val count:Double,

    @ManyToOne
    @JoinColumn(name="unit_id")
    var unit: DimensionUnit,

    @ManyToOne
    @JoinColumn(name="purchase_id")
    var purchase: Purchase,

    @ManyToOne
    @JoinColumn(name="product_category_id")
    var category: ProductCategory

)