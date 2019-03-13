package ru.itport.yourspendings.entity

import javax.persistence.*

@Entity
@Table(name="purchase_products")
data class PurchaseProduct(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var uid: Long? = null,

    @Column(name = "name")
    var name: String,

    @Column(name = "price")
    var price: Double = 0.0,

    @Column(name = "discount")
    var discount: Double = 0.0,

    @Column(name = "count")
     var count: Double = 0.0,

    @ManyToOne
    @JoinColumn(name = "unit_id")
    var unit: DimensionUnit? = null,

    @ManyToOne
    @JoinColumn(name = "purchase_id")
    var purchase: Purchase? = null,

    @ManyToOne
    @JoinColumn(name = "product_category_id")
    var category: ProductCategory? = null
):BaseModel()