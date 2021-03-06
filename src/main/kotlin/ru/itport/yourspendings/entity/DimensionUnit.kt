package ru.itport.yourspendings.entity

import javax.persistence.*

@Entity
@Table(name="dimension_units")
data class DimensionUnit (

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    var uid:Int? = null,

    @Column(name="name")
    val name:String
):BaseModel()
