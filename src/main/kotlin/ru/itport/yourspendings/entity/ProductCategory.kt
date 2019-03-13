package ru.itport.yourspendings.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import javax.persistence.*

@Entity
@Table(name="product_categories")
data class ProductCategory (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var uid: Int? = null,

    @Column(name = "name")
    var name: String,

    @ManyToOne
    @JoinColumn(name = "parent_id")
    var parent: ProductCategory? = null,

    @OneToMany(mappedBy = "parent")
    @JsonBackReference(value = "category-subcategory")
    var subCategories: List<ProductCategory>? = null,

    @OneToMany(mappedBy = "category")
    @JsonBackReference(value = "category-product")
    var products: List<PurchaseProduct>? = null

):BaseModel()