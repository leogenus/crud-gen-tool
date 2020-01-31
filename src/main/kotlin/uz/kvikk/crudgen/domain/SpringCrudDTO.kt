package uz.kvikk.crudgen.domain


data class PojoDTO(
        var className: String? = null,
        var tableName: String? = null,
        var fields: List<ClassDomain>? = null
)

data class ControllerDTO(
        var className: String? = null,
        var mapping: String? = null
)

data class SpringCrudDTO(
        var pojo: PojoDTO? = null,
        var mapperName: String? = null,
        var serviceName: String? = null,
        var serviceImplName: String? = null,
        var controller: ControllerDTO? = null
)