package ru.itport.yourspendings.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import java.util.*
import javax.persistence.*

@Entity
@Table(name="purchases")
data class Purchase(

    @Id
    @Column(name="id")
    val uid:String,

    @Column(name="date")
    val date: Date,

    @Column(name="updated_at")
    override val updatedAt:Date,

    @ManyToOne
    @JoinColumn(name="place_id")
    var place: Shop? = null,

    @OneToMany(mappedBy="purchase")
    @JsonBackReference(value="purchase-image")
    var images:List<PurchaseImage>? = null,

    @ManyToOne
    @JoinColumn(name="user_id")
    val user:PurchaseUser,

    @OneToMany(mappedBy="purchase")
    @JsonBackReference(value="purchase-discount")
    var purchaseDiscounts:List<PurchaseDiscount>? = null,

    @OneToMany(mappedBy="purchase")
    @JsonBackReference(value="purchase-product")
    var products:List<PurchaseProduct>? = null

): YModel(updatedAt)