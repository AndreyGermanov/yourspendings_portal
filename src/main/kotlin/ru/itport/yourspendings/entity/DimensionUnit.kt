package ru.itport.yourspendings.entity

import javax.persistence.*

@Entity
@Table(name="dimension_units")
class DimensionUnit (

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    var id:Int? = null,

    @Column(name="name")
    val name:String
)
