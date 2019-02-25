package ru.itport.yourspendings.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
@Table(name="users")
data class User (

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    val uid:Int,

    @Column(name="name")
    val name:String,

    @Column(name="password")
    @JsonIgnore
    val password:String,


    @Column(name="enabled")
    val enabled:Boolean,

    @ManyToMany
    @JoinTable(name="users_roles", joinColumns=[JoinColumn(name="user_id")], inverseJoinColumns = [JoinColumn(name="role_id")])
    var roles:List<Role>? = null

)