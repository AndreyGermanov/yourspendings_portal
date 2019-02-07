package ru.itport.yourspendings.entity

import java.util.*
import javax.persistence.Column
import javax.persistence.MappedSuperclass

@MappedSuperclass
open class YModel (
    @Column(name="updated_at")
    open val updatedAt: Date
)