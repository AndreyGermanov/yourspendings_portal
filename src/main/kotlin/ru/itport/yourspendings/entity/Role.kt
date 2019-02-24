package ru.itport.yourspendings.entity

import javax.persistence.*

@Entity
@Table(name="authorities")
class Role (

    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    val userName: Int,

    @Column(name="authority")
    val role:String,

    @ManyToOne
    @JoinColumn(name="user_id")
    val user:User

)