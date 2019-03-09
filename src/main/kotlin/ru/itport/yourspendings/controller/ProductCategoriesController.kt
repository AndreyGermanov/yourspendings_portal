package ru.itport.yourspendings.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.entity.ProductCategory

@RestController
@RequestMapping("/api/productCategory")
class ProductCategoriesController:EntityController<ProductCategory>("ProductCategory") {

    override fun getItemId(id:Any):Any = id.toString().toLongOrNull() ?: 0

    override fun list(@RequestParam("filter_fields") filterFields:ArrayList<String>?,
                      @RequestParam("filter_value") filterValue:String?,
                      @RequestParam("limit") limit:Int?,
                      @RequestParam("skip") skip:Int?,
                      @RequestParam("order") order:String?): Any {
        val list:ArrayList<ProductCategory> = super.list(filterFields, filterValue, 0, 0, order) as ArrayList<ProductCategory>
        val result = list.map {
            var parentList = getParentList(it).reversed()
            var fullId = if (parentList.size>0) parentList.joinToString("/")+"/"+it.uid else it.uid.toString()
            //parentList.reverse()
            hashMapOf(
                "uid" to it.uid,
                "name" to it.name,
                "parent" to it.parent,
                "level" to parentList.size,
                "fullId" to fullId
            )
        }.sortedBy {
            it["fullId"].toString()
        }.map {
            it.apply {
                remove("parents")
                remove("fullId")
            }
        }
        val skip = skip ?: 0
        val limit = limit ?: result.size
        return result.subList(skip, if (skip+limit>result.size) result.size; else skip+limit)
    }

    private fun getParentList(item:ProductCategory):ArrayList<Long> {
        var parent = item.parent
        var result = ArrayList<Long>()
        if (parent == null) return result;
        while (parent != null) {
            result.add(parent.uid!!)
            parent = parent.parent
        }
        return result
    }

}