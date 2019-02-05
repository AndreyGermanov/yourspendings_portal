package ru.itport.yourspendings.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ErrorController {

    @GetMapping("/error")
    fun error():String {
        return ""
    }
}