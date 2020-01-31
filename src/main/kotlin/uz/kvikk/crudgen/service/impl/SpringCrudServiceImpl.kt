package uz.kvikk.crudgen.service.impl

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.throws
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uz.kvikk.crudgen.domain.ClassDomain
import uz.kvikk.crudgen.domain.SpringCrudDTO
import uz.kvikk.crudgen.domain.enums.FieldType
import uz.kvikk.crudgen.service.SpringCrudService
import java.sql.ResultSet
import java.sql.SQLException
import javax.persistence.Table

@Service
class SpringCrudServiceImpl : SpringCrudService {
    override fun generate(springCrudDTO: SpringCrudDTO): Any {
        if (
                springCrudDTO.pojo == null ||
                springCrudDTO.mapperName == null ||
                springCrudDTO.serviceName == null ||
                springCrudDTO.serviceImplName == null ||
                springCrudDTO.controller == null ||
                springCrudDTO.pojo!!.className == null ||
                springCrudDTO.pojo!!.tableName == null ||
                springCrudDTO.pojo!!.fields == null ||
                springCrudDTO.controller!!.className == null ||
                springCrudDTO.controller!!.mapping == null
        )
            throw RuntimeException("Request data is not compatible!")

        val (pojo,
                mapperName,
                serviceName,
                serviceImplName,
                controller) = springCrudDTO

        val pojoSb = StringBuilder()
        val mapperSb = StringBuilder()
        val serviceSb = StringBuilder()
        val serviceImplSb = StringBuilder()
        val controllerSb = StringBuilder()

        val fieldList = pojo!!.fields
        val pojoClassName = createDTO(pojo.className!!, pojo.tableName!!, fieldList!!, pojoSb)
        val mapper = createMapper(mapperName!!, pojoClassName, fieldList, mapperSb)

        val service = createServiceCrud(serviceName!!, pojoClassName, serviceSb)
        createServiceImplCrud(serviceImplName!!, service, pojoClassName, mapper, serviceImplSb)
        createControllerCrud(controller!!.className!!, controller!!.mapping!!, service, controllerSb)

        return hashMapOf(
                "pojo" to pojoSb.toString(),
                "mapper" to mapperSb.toString(),
                "service" to serviceSb.toString(),
                "serviceImpl" to serviceImplSb.toString(),
                "controller" to controllerSb.toString())
    }

    private fun createDTO(pojoName: String, tableName: String?, list: List<ClassDomain>, stringBuilder: StringBuilder?): ClassName {
        val typeBuilder = TypeSpec.classBuilder(pojoName)
                .addModifiers(KModifier.DATA)

        if (!tableName.isNullOrEmpty())
            typeBuilder.addAnnotation(AnnotationSpec.builder(Table::class)
                    .addMember("name = %S", tableName)
                    .build())

        val paramsBuilder = FunSpec.constructorBuilder()
        for ((fieldName, fieldType, columnName) in list) {
            val value = fieldType!!.value()
            paramsBuilder.addParameter(ParameterSpec.builder(fieldName!!, value.copy(nullable = true))
                    .defaultValue("%L", null)
                    .build())
            typeBuilder.addProperty(PropertySpec.builder(fieldName, value.copy(nullable = true))
                    .mutable()
                    .initializer(fieldName)
                    .build())
        }

        typeBuilder.primaryConstructor(paramsBuilder.build())

        val pojoFile = FileSpec.builder("", pojoName)
                .addType(typeBuilder.build())
                .build()

        pojoFile.writeTo(stringBuilder ?: System.out)

        return ClassName("", pojoName)
    }

    private fun createMapper(mapperName: String, pojoClassName: ClassName, list: List<ClassDomain>, stringBuilder: StringBuilder?): ClassName {

        val mapperClassName = ClassName("", mapperName)
        val funcSpec = FunSpec.builder("mapRow")
                .returns(pojoClassName)
                .throws(SQLException::class)
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("rs", ResultSet::class)
                .addParameter("rowNum", Int::class)
                .addStatement("val row = %T()", pojoClassName)

        for ((fieldName, fieldType, columnName) in list) {
            if (StringUtils.isEmpty(columnName)) continue
            val mFieldName: String = fieldName!!
            val mColumnName: String = columnName!!
            when (fieldType) {
                FieldType.STRING -> funcSpec.addStatement("row.%1L = rs.getString(%2S)", mFieldName, mColumnName)
                FieldType.INT -> funcSpec.addStatement("row.%1L = rs.getInt(%2S)", mFieldName, mColumnName)
                FieldType.BOOLEAN -> funcSpec.addStatement("row.%1L = rs.getBoolean(%2S)", mFieldName, mColumnName)
                FieldType.BYTE_ARRAY -> funcSpec.addStatement("row.%1L = rs.getBytes(%2S)", mFieldName, mColumnName)
                FieldType.BYTE -> funcSpec.addStatement("row.%1L = rs.getByte(%2S)", mFieldName, mColumnName)
                FieldType.LONG -> funcSpec.addStatement("row.%1L = rs.getLong(%2S)", mFieldName, mColumnName)
                FieldType.FLOAT -> funcSpec.addStatement("row.%1L = rs.getFloat(%2S)", mFieldName, mColumnName)
                FieldType.DOUBLE -> funcSpec.addStatement("row.%1L = rs.getDouble(%2S)", mFieldName, mColumnName)
                FieldType.BIG_DECIMAL -> funcSpec.addStatement("row.%1L = rs.getBigDecimal(%2S)", mFieldName, mColumnName)
                FieldType.TIME -> funcSpec.addStatement("row.%1L = rs.getBigDecimal(%2S)", mFieldName, mColumnName)
                FieldType.DATE_SQL -> funcSpec.addStatement("row.%1L = rs.getDate(%2S)", mFieldName, mColumnName)
                FieldType.DATE_JAVA -> funcSpec.addStatement("row.%1L = Date(rs.getDate(%2S).time)", mFieldName, mColumnName)
                FieldType.TIMESTAMP_SQL -> funcSpec.addStatement("row.%1L = rs.getTimestamp(%2S)", mFieldName, mColumnName)
            }
        }

        val pojoFile = FileSpec.builder("", mapperName)
                .addType(TypeSpec.classBuilder(mapperName)
                        .addSuperinterface(RowMapper::class.asClassName().parameterizedBy(pojoClassName))
                        .addAnnotation(Component::class)
                        .addFunction(funcSpec.addStatement("return row").build())
                        .build())
                .build()
        pojoFile.writeTo(stringBuilder ?: System.out)
        return mapperClassName
    }

    private fun createServiceCrud(service: String, pojoClassName: ClassName?, stringBuilder: StringBuilder?): ClassName {
        val serviceName = service.capitalize()
        val anyTypeName = TypeVariableName("*")
        val pageClassName = ClassName("org.springframework.data.domain", "Page")
                .parameterizedBy(pojoClassName ?: anyTypeName)

        val serviceFile = FileSpec.builder("", serviceName)
                .addType(TypeSpec.interfaceBuilder(serviceName)
                        .addFunction(FunSpec.builder("page")
                                .returns(pageClassName)
                                .addParameter("pageable", Pageable::class)
                                .addModifiers(KModifier.ABSTRACT)
                                .build())
/*                    .addFunction(FunSpec.builder("one")
//                            .returns(responseEntityClassName.parameterizedBy(anyTypeName))
                            .build())
                    .addFunction(FunSpec.builder("create")
//                            .returns(responseEntityClassName.parameterizedBy(anyTypeName))
                            .build())
                    .addFunction(FunSpec.builder("update")
//                            .returns(responseEntityClassName.parameterizedBy(anyTypeName))
                            .build())
                    .addFunction(FunSpec.builder("delete")
//                            .returns(responseEntityClassName.parameterizedBy(anyTypeName))
//                            .addStatement("documentService.delete(id, principal)")
//                            .addStatement("return ResponseEntity.ok().build()")
                            .build())*/
                        .build())
                .build()
        serviceFile.writeTo(stringBuilder ?: System.out)
        serviceFile.toString()
        return ClassName("", serviceName)
    }

    fun createServiceImplCrud(service: String, interfaceName: ClassName, pojoClassName: ClassName?, mapperClassName: ClassName, stringBuilder: StringBuilder?): ClassName {
        val serviceName = service.capitalize()
        val anyTypeName = TypeVariableName("*")
        val pageClassName = ClassName("org.springframework.data.domain", "Page")
                .parameterizedBy(pojoClassName ?: anyTypeName)
        val serviceImplFile = FileSpec.builder("", serviceName)
                .addType(TypeSpec.classBuilder(serviceName)
                        .addSuperinterface(interfaceName)
                        .addAnnotation(Service::class)
                        .primaryConstructor(FunSpec.constructorBuilder()
                                .addParameter("jdbcTemplate", NamedParameterJdbcTemplate::class)
                                .addParameter("mapper", mapperClassName)
                                .build())
                        .addFunction(FunSpec.builder("page")
                                .returns(pageClassName)
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter("pageable", Pageable::class)
                                .addStatement("""
                                    return jdbcTemplate.query(%1S, hashMapOf<String, Any>(
                                        "offset" to pageable.offset, 
                                        "pageSize" to pageable.pageSize), %2L)
                                """.trimIndent(), "select * from k_table t order by t.ID desc OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY", "mapper")
                                .build())
                        .addProperty(PropertySpec.builder("jdbcTemplate", NamedParameterJdbcTemplate::class, KModifier.PRIVATE)
                                .initializer("jdbcTemplate")
                                .build())
                        .addProperty(PropertySpec.builder("mapper", mapperClassName, KModifier.PRIVATE)
                                .initializer("mapper")
                                .build())
/*                    .addFunction(FunSpec.builder("one")
//                            .returns(responseEntityClassName.parameterizedBy(anyTypeName))
                            .build())
                    .addFunction(FunSpec.builder("create")
//                            .returns(responseEntityClassName.parameterizedBy(anyTypeName))
                            .build())
                    .addFunction(FunSpec.builder("update")
//                            .returns(responseEntityClassName.parameterizedBy(anyTypeName))
                            .build())
                    .addFunction(FunSpec.builder("delete")
//                            .returns(responseEntityClassName.parameterizedBy(anyTypeName))
//                            .addStatement("documentService.delete(id, principal)")
//                            .addStatement("return ResponseEntity.ok().build()")
                            .build())*/
                        .build())
                .build()
        serviceImplFile.writeTo(stringBuilder ?: System.out)
        return ClassName("", serviceName)
    }

    fun createControllerCrud(controller: String, urlRootPath: String, serviceClassName: ClassName, stringBuilder: StringBuilder?) {
        val controllerName = controller.capitalize()
        val anyTypeName = TypeVariableName("*")
        val responseEntityClassName = ClassName("org.springframework.http", "ResponseEntity")
        val responseEntityClass = ClassName("org.springframework.http", "ResponseEntity")
        val pageClassName = ClassName("org.springframework.data.domain", "Page").parameterizedBy(anyTypeName)
        val argName = serviceClassName.simpleName.decapitalize()
        val file = FileSpec.builder("", controllerName)
                .addType(TypeSpec.classBuilder(controllerName)
                        .addAnnotation(RestController::class)
                        .addAnnotation(AnnotationSpec.builder(RequestMapping::class)
                                .addMember("%S", urlRootPath)
                                .build())
                        .primaryConstructor(FunSpec.constructorBuilder()
                                .addParameter(argName, serviceClassName)
                                .build())
                        .addFunction(FunSpec.builder("page")
                                .returns(responseEntityClassName.parameterizedBy(pageClassName))
                                .addAnnotation(AnnotationSpec.builder(GetMapping::class)
                                        .addMember("%S", "page")
                                        .build())
                                .addParameter("pageable", Pageable::class)
                                .addStatement("return ResponseEntity.ok($argName.page(pageable))")
                                .build())
                        /* .addFunction(FunSpec.builder("one")
                                 .returns(responseEntityClassName.parameterizedBy(anyTypeName))
                                 .build())
                         .addFunction(FunSpec.builder("create")
                                 .returns(responseEntityClassName.parameterizedBy(anyTypeName))
                                 .build())
                         .addFunction(FunSpec.builder("update")
                                 .returns(responseEntityClassName.parameterizedBy(anyTypeName))
                                 .build())
                         .addFunction(FunSpec.builder("delete")
                                 .returns(responseEntityClassName.parameterizedBy(anyTypeName))
                                 .addStatement("documentService.delete(id, principal)")
                                 .addStatement("return ResponseEntity.ok().build()")
                                 .build())*/
                        .addProperty(PropertySpec.builder(argName, serviceClassName, KModifier.PRIVATE)
                                .initializer(argName)
                                .build())
                        .build())
                .build()

        file.writeTo(stringBuilder ?: System.out)
    }


}