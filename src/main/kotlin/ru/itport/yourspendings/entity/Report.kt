package ru.itport.yourspendings.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import javax.persistence.*

@Entity
@Table(name="reports")
class Report (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var uid: Int? = null,

    @Column(name = "name")
    var name: String,

    @Column(name="post_script")
    var postScript:String,

    @OneToMany(mappedBy = "report", cascade = [CascadeType.ALL])
    @JsonBackReference(value = "report-query")
    var queries: List<ReportQuery>? = null


):BaseModel()