package ru.itport.yourspendings.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.entity.DimensionUnit

@RestController
@RequestMapping("/api/dimensionUnit")
class DimensionUnitsController:EntityController<DimensionUnit>("DimensionUnit") {
    override fun getItemId(id: Any): Any = id.toString().toIntOrNull() ?: 0
}