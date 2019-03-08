package ru.itport.yourspendings.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import javax.persistence.*

@Entity
@Table(name="product_categories")
class ProductCategory (

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name="id")
    var uid:Long? = null,

    @Column(name="name")
    val name:String,

    @ManyToOne
    @JoinColumn(name="parent_id")
    var parent:ProductCategory?=null,

    @OneToMany(mappedBy="parent")
    @JsonBackReference
    var subCategories: List<ProductCategory>? = null,

    @OneToMany(mappedBy="category")
    var products: List<PurchaseProduct>? = null

)