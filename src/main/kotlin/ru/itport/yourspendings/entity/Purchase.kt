package ru.itport.yourspendings.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import java.util.*
import javax.persistence.*

@Entity
@Table(name="purchases")
data class Purchase (

    @Id
    @Column(name = "id")
    var uid: String,

    @Column(name = "date")
    var date: Date?,

    @Column(name = "updated_at")
    override val updatedAt: Date? = null,

    @ManyToOne
    @JoinColumn(name = "place_id")
    var place: Shop? = null,

    @OneToMany(mappedBy = "purchase")
    @JsonBackReference(value = "purchase-image")
    var images: List<PurchaseImage>? = null,

    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: PurchaseUser? = null,

    @OneToMany(mappedBy = "purchase")
    @JsonBackReference(value = "purchase-discount")
    var purchaseDiscounts: List<PurchaseDiscount>? = null,

    @OneToMany(mappedBy = "purchase")
    @JsonBackReference(value = "purchase-product")
    var products: List<PurchaseProduct>? = null

):YModel(updatedAt)

