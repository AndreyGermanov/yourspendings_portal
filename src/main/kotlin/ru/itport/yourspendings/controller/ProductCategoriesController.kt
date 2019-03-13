package ru.itport.yourspendings.controller

import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.entity.ProductCategory

@RestController
@RequestMapping("/api/productCategory")
class ProductCategoriesController:EntityController<ProductCategory>("ProductCategory") {

    override fun getItemId(id:Any):Any = id.toString().toLongOrNull() ?: 0

    override fun list(@RequestBody body:Any?): Any {
        val result = (super.list(parseListRequest(body).apply { skip=0;limit=0;}) as ArrayList<ProductCategory>).map {
            var parentList = getParentList(it).reversed()
            var fullId = if (parentList.isNotEmpty()) parentList.joinToString("/")+"/"+it.uid else it.uid.toString()
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
        val req = parseListRequest(body).apply { if (limit!! <=0 ) limit = result.size}
        return result.subList(req.skip!!, if (req.skip!!+req.limit!!>result.size) result.size; else req.skip!!+req.limit!!)
    }

    private fun getParentList(item:ProductCategory):ArrayList<Int> {
        var parent = item.parent
        var result = ArrayList<Int>()
        if (parent == null) return result;
        while (parent != null) {
            result.add(parent.uid!!)
            parent = parent.parent
        }
        return result
    }

}