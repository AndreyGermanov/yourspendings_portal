package ru.itport.yourspendings.entity

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
        var id:String?,

        @Column(name="name")
        val name:String,

        @Column(name="latitude")
        val latitude:Double,

        @Column(name="longitude")
        val longitude:Double,

        @Column(name="updated_at")
        override val updatedAt: Date,

        @OneToMany(mappedBy="place")
        var purchases:List<Purchase>? = null,

        @ManyToOne
        @JoinColumn(name="user_id")
        val user: PurchaseUser

): YModel(updatedAt)
