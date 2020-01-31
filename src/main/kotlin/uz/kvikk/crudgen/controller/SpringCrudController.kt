package uz.kvikk.crudgen.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uz.kvikk.crudgen.domain.SpringCrudDTO
import uz.kvikk.crudgen.service.SpringCrudService


@RestController
@RequestMapping("spring-crud")
class SpringCrudController(private val springCrudService: SpringCrudService) {

    @PostMapping("generate")
    fun generate(@RequestBody springCrudDTO: SpringCrudDTO): ResponseEntity<*> = ResponseEntity.ok(springCrudService.generate(springCrudDTO))

}