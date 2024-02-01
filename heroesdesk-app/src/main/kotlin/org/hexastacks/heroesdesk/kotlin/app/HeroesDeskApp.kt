package org.hexastacks.heroesdesk.kotlin.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
open class HeroesDeskApp

fun main(args: Array<String>) {
    runApplication<HeroesDeskApp>(*args)
}
