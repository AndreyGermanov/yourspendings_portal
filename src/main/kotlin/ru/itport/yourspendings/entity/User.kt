package ru.itport.yourspendings.entity

import javax.persistence.*

@Entity
@Table(name="users")
data class User (

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    var uid:Int,

    @Column(name="name")
    var name:String,

    @Column(name="password")
    var password:String,


    @Column(name="enabled")
    var enabled:Boolean,

    @ManyToMany
    @JoinTable(name="users_roles", joinColumns=[JoinColumn(name="user_id")], inverseJoinColumns = [JoinColumn(name="role_id")])
    var roles:List<Role>? = null

)