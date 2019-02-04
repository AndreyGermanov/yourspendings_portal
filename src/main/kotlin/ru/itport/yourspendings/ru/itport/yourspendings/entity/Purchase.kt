package ru.itport.yourspendings.ru.itport.yourspendings.entity

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
    @ManyToOne
    @JoinColumn(name="place_id")
    var place: Shop? = null,
    @Column(name="user_id")
    val userId: String,
    @OneToMany(mappedBy="purchase")
    var images:List<PurchaseImage>? = null
)