package uz.kvikk.crudgen.service

import uz.kvikk.crudgen.domain.SpringCrudDTO

interface SpringCrudService {

    fun generate(springCrudDTO: SpringCrudDTO): Any

}