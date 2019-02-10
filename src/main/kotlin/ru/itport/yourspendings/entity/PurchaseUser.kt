package ru.itport.yourspendings.entity

import java.util.*
import javax.persistence.*

@Entity
@Table(name="purchase_users")
data class PurchaseUser(

        @Id
        @Column(name="id")
        val id:String,

        @Column(name="email")
        val email:String,

        @Column(name="name")
        val name:String,

        @Column(name="phone")
        val phone:String,

        @Column(name="disabled")
        val isDisabled:Boolean,

        @Column(name="udated_at")
        override val updatedAt: Date,

        @OneToMany(mappedBy="user")
        val shops: List<Shop>? = null,

        @OneToMany(mappedBy="user")
        val purchases: List<Purchase>? = null

): YModel(updatedAt) {
}