package ru.itport.yourspendings.controller

import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.entity.ProductCategory

@RestController
@RequestMapping("/api/productCategory")
class ProductCategoriesController:EntityController<ProductCategory>("ProductCategory") {

    override fun getItemId(id:Any):Any = id.toString().toIntOrNull() ?: 0

    override fun list(@RequestBody body:Any?): Any {
        val result = (super.list(parseListRequest(body).apply { skip=0;limit=0;}) as ArrayList<ProductCategory>).map {
            val parentList = getParentList(it).reversed()
            val parentPath = if (parentList.isNotEmpty()) parentList.joinToString("/"); else ""
            val fullId =if (parentPath.isNotEmpty()) parentPath+"/"+it.name else it.name
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
               remove("fullId")
            }
        }
        val req = parseListRequest(body).apply { if (limit!! <=0 ) limit = result.size}
        return result.subList(req.skip!!, if (req.skip!!+req.limit!!>result.size) result.size; else req.skip!!+req.limit!!)
    }

    private fun getParentList(item:ProductCategory):ArrayList<String> {
        var parent = item.parent
        var result = ArrayList<String>()
        if (parent == null) return result;
        while (parent != null) {
            result.add(parent.name!!)
            parent = parent.parent
        }
        return result
    }

}