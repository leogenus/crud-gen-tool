package uz.kvikk.crudgen

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CrudGenApplication

fun main(args: Array<String>) {
    runApplication<CrudGenApplication>(*args)
}
