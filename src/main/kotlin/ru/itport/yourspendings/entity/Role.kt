package ru.itport.yourspendings.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import javax.persistence.*

@Entity
@Table(name="roles")
data class Role (

    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    var uid: Int,

    @Column(name="role_id")
    var roleId:String,

    @Column(name="name")
    var name:String,

    @ManyToMany
    @JoinTable(name="users_roles", joinColumns = [JoinColumn(name="role_id")], inverseJoinColumns = [JoinColumn(name="user_id")])
    @JsonBackReference
    var users:List<User>?=null
):BaseModel()