package ru.itport.yourspendings.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import org.springframework.validation.annotation.Validated
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Validated
@Table(name="shops")
data class Shop (

        @Id
        @Column(name="id")
        @NotNull
        var uid:String?,

        @Column(name="name")
        val name:String,

        @Column(name="latitude")
        val latitude:Double,

        @Column(name="longitude")
        val longitude:Double,

        @Column(name="updated_at")
        override val updatedAt: Date,

        @OneToMany(mappedBy="place")
        @JsonBackReference(value="shop-purchase")
        var purchases:List<Purchase>? = null,

        @ManyToOne
        @JoinColumn(name="user_id")
        val user: PurchaseUser

): YModel()
