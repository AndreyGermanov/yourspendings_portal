package ru.itport.yourspendings.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name="users")
data class User (
    @Id
    @Column(name="username")
    val username:String,
    @Column(name="password")
    val password:String,
    @Column(name="enabled")
    val enabled:Boolean
)