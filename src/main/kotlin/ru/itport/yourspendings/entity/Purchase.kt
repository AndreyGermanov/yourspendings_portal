package ru.itport.yourspendings.entity

import org.springframework.data.rest.core.config.Projection
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
    var images:List<PurchaseImage>? = null,

    @ManyToOne
    @JoinColumn(name="user_id")
    val user:PurchaseUser,

    @OneToMany(mappedBy="purchase")
    var purchaseDiscounts:List<PurchaseDiscount>? = null,

    @OneToMany(mappedBy="purchase")
    var products:List<PurchaseProduct>? = null

): YModel(updatedAt)

@Projection(name="fullPurchase",types=[Purchase::class])
public interface fullPurchase {
    val id:String
    val date: Date
    val updatedAt:Date
    val place: Shop
    val images: List<PurchaseImage>
    val purchaseDiscounts: List<PurchaseDiscount>
    val user:PurchaseUser

}