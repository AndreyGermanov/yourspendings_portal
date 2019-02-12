package ru.itport.yourspendings.entity

import javax.persistence.*

@Entity
@Table(name="users")
data class User (

    @Id
    @Column(name="username")
    val username:String,

    @Column(name="password")
    val password:String,

    @Column(name="enabled")
    val enabled:Boolean,

    @OneToMany(mappedBy="user")
    val roles:List<Role>

)