package com.vdt.blog

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.thymeleaf.spring5.SpringTemplateEngine

@SpringBootApplication
class BlogApplication {
    @Bean
    fun sprintTemplateEngine() {
        val engine = SpringTemplateEngine()
        engine.addDialect(LayoutDialect())
    }
}

fun main(args: Array<String>) {
    runApplication<BlogApplication>(*args)
}
