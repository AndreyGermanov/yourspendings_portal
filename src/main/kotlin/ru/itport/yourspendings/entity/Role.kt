package ru.itport.yourspendings.entity

import javax.persistence.*

@Entity
@Table(name="authorities")
class Role (

    @Id
    @Column(name="username")
    val userName: String,

    @Column(name="authority")
    val role:String,

    @ManyToOne
    @JoinColumn(name="username")
    val user:User

)