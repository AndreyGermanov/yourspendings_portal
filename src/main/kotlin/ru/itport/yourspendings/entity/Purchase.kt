package ru.itport.yourspendings.entity

import java.util.*
import javax.persistence.*

@Entity
@Table(name="purchases")
data class Purchase(

    @Id
    @Column(name="id")
    val id:String,

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
    val user:PurchaseUser

): YModel(updatedAt)