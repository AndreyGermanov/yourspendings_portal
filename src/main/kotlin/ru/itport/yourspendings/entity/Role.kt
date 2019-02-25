package ru.itport.yourspendings.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import javax.persistence.*

@Entity
@Table(name="roles")
class Role (

    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    val uid: Int,

    @Column(name="role_id")
    val roleId:String,

    @Column(name="name")
    val name:String,

    @ManyToMany
    @JoinTable(name="users_roles", joinColumns = [JoinColumn(name="role_id")], inverseJoinColumns = [JoinColumn(name="user_id")])
    @JsonBackReference
    var users:List<User>?=null
)