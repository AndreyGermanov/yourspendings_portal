package ru.itport.yourspendings.entity

import javax.persistence.*

@Entity
@Table(name="report_queries")
class ReportQuery (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var uid: Int? = null,

    @Column(name = "name")
    var name: String,

    @Column(name = "sort_order")
    var order:Int,

    @Column(name = "enabled")
    var enabled:Boolean,

    @Column(name = "query")
    var query:String,

    @Column(name = "params")
    var params:String,

    @Column(name = "output_format")
    var outputFormat:String,

    @ManyToOne
    @JoinColumn(name = "report_id")
    var report:Report? = null

):BaseModel()