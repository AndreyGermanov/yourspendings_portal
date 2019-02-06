package ru.itport.yourspendings.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import java.net.InetAddress

@Controller
class HomeController {

    @Autowired
    lateinit var env: Environment

    @GetMapping("/")
    private fun home(theModel: Model):String {
        theModel.addAttribute("port",env.getProperty("local.server.port"))
        theModel.addAttribute("host", InetAddress.getLocalHost().hostName)
        return "index"
    }

}