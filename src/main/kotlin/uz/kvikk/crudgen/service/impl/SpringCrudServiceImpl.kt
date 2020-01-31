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
import org.springframework.web.bind.annotation.*
import uz.kvikk.crudgen.domain.ClassDomain
import uz.kvikk.crudgen.domain.SpringCrudDTO
import uz.kvikk.crudgen.domain.enums.FieldType
import uz.kvikk.crudgen.service.SpringCrudService
import java.sql.ResultSet
import java.sql.SQLException
import javax.persistence.Column
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
        val idField = fieldList!!.find { d -> d.fieldType == FieldType.ID } ?: throw RuntimeException("ID field not found!")

        val pojoClassName = createDTO(pojo.className!!, pojo.tableName!!.toUpperCase(), fieldList, pojoSb)
        val mapper = createMapper(mapperName!!, pojoClassName, fieldList, mapperSb)

        val service = createServiceCrud(serviceName!!, pojoClassName, serviceSb, idField.fieldName!!)
        createServiceImplCrud(serviceImplName!!, service, pojoClassName, pojo.tableName!!.toUpperCase(), mapper, serviceImplSb, fieldList, idField)
        createControllerCrud(controller!!.className!!, controller.mapping!!, service, controllerSb, idField.fieldName!!, pojoClassName)

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
                    .addMember("name = %S", tableName.toUpperCase())
                    .build())

        val paramsBuilder = FunSpec.constructorBuilder()
        for ((fieldName, fieldType, columnName) in list) {
            val value = fieldType!!.value()
            paramsBuilder.addParameter(ParameterSpec.builder(fieldName!!, value.copy(nullable = true))
                    .addAnnotation(AnnotationSpec.builder(Column::class)
                            .addMember("name = %S", columnName!!.toUpperCase())
                            .build())
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
            val mColumnName: String = columnName!!.toUpperCase()
            when (fieldType) {
                FieldType.ID -> funcSpec.addStatement("row.%1L = rs.getString(%2S)", mFieldName, mColumnName)
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

    private fun createServiceCrud(service: String, pojoClassName: ClassName, stringBuilder: StringBuilder?, idFieldName: String): ClassName {
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
                        .addFunction(FunSpec.builder("one")
                                .returns(pojoClassName)
                                .addParameter(idFieldName, String::class)
                                .addModifiers(KModifier.ABSTRACT)
                                .build())
                        .addFunction(FunSpec.builder("create")
                                .returns(pojoClassName)
                                .addParameter(pojoClassName.simpleName.decapitalize(), pojoClassName)
                                .addModifiers(KModifier.ABSTRACT)
                                .build())
                        .addFunction(FunSpec.builder("update")
                                .returns(pojoClassName)
                                .addParameter(idFieldName, String::class)
                                .addParameter(pojoClassName.simpleName.decapitalize(), pojoClassName)
                                .addModifiers(KModifier.ABSTRACT)
                                .build())
                        .addFunction(FunSpec.builder("delete")
                                .addParameter(idFieldName, String::class)
                                .addModifiers(KModifier.ABSTRACT)
                                .build())
                        .build())
                .build()
        serviceFile.writeTo(stringBuilder ?: System.out)
        serviceFile.toString()
        return ClassName("", serviceName)
    }

    fun createServiceImplCrud(service: String, interfaceName: ClassName, pojoClassName: ClassName, tableName: String, mapperClassName: ClassName, stringBuilder: StringBuilder?, list: List<ClassDomain>, idField: ClassDomain): ClassName {
        val serviceName = service.capitalize()
        val anyTypeName = TypeVariableName("*")
        val pageClassName = ClassName("org.springframework.data.domain", "Page")
                .parameterizedBy(pojoClassName ?: anyTypeName)


        val insertColumnBuilder = StringBuilder()
        val insertValueBuilder = StringBuilder()
        val insertFieldBuilder = StringBuilder()
        val updateColumnBuilder = StringBuilder()
        for ((fieldName, fieldType, columnName) in list) {
            if (StringUtils.isEmpty(columnName) || fieldType == FieldType.ID) continue
            if (insertColumnBuilder.isNotEmpty())
                insertColumnBuilder.append(", ")
            insertColumnBuilder.append(columnName)

            if (insertValueBuilder.isNotEmpty())
                insertValueBuilder.append(", ")
            insertValueBuilder.append("?")

            if (insertFieldBuilder.isNotEmpty())
                insertFieldBuilder.append(", ")
            insertFieldBuilder.append("${pojoClassName.simpleName.decapitalize()}.$fieldName")

            if (updateColumnBuilder.isNotEmpty())
                updateColumnBuilder.append(", ")
            updateColumnBuilder.append("$columnName=?")
        }
        insertColumnBuilder.append(", ").append(idField.columnName!!.toUpperCase())
        insertValueBuilder.append(", ?")
        insertFieldBuilder.append(", ").append(idField.fieldName)

        val serviceImplFile = FileSpec.builder("", serviceName)
                .addImport("org.springframework.data.domain", "PageImpl")
                .addImport("java.util", "UUID")
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
                                |val list = jdbcTemplate.query(%1S, hashMapOf<String, Any>(
                                |                    "offset" to pageable.offset, 
                                |                    "pageSize" to pageable.pageSize), %3L)
                                |val total = jdbcTemplate.queryForObject(%2S, mapOf<String, Any?>(), Long::class.java)
                                |return PageImpl(list, pageable, total!!)
                                """.trimMargin(),
                                        "select * from $tableName t order by t.${idField.columnName!!.toUpperCase()} desc OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY",
                                        "select count(*) from $tableName t",
                                        "mapper")
                                .build())
                        .addProperty(PropertySpec.builder("jdbcTemplate", NamedParameterJdbcTemplate::class, KModifier.PRIVATE)
                                .initializer("jdbcTemplate")
                                .build())
                        .addProperty(PropertySpec.builder("mapper", mapperClassName, KModifier.PRIVATE)
                                .initializer("mapper")
                                .build())
                        .addFunction(FunSpec.builder("one")
                                .returns(pojoClassName)
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(idField.fieldName!!, String::class)
                                .addStatement("return jdbcTemplate.queryForObject(%1S, hashMapOf(%2S to %2L), mapper)!!",
                                        "select * from $tableName t where t.${idField.columnName!!.toUpperCase()}=:${idField.fieldName!!}",
                                        idField.fieldName!!)
                                .build())
                        .addFunction(FunSpec.builder("create")
                                .returns(pojoClassName)
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(pojoClassName.simpleName.decapitalize(), pojoClassName)
                                .addStatement("val ${idField.fieldName!!} = UUID.randomUUID().toString()")
                                .addStatement("jdbcTemplate.jdbcTemplate.update(%1S, %2L)",
                                        CodeBlock.builder()
                                                .add("insert into $tableName (%1L) values (%2L)",
                                                        insertColumnBuilder.toString(),
                                                        insertValueBuilder.toString()).build(),
                                        insertFieldBuilder.toString())
                                .addStatement("return this.one(%L)", idField.fieldName!!)
                                .build())
                        .addFunction(FunSpec.builder("update")
                                .returns(pojoClassName)
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(idField.fieldName!!, String::class)
                                .addParameter(pojoClassName.simpleName.decapitalize(), pojoClassName)
                                .addStatement("jdbcTemplate.jdbcTemplate.update(%1S, %2L)",
                                        CodeBlock.builder()
                                                .add("update $tableName set %1L where ${idField.columnName!!.toUpperCase()}=?",
                                                        updateColumnBuilder.toString()).build(),
                                        insertFieldBuilder.toString())
                                .addStatement("return this.one(%L)", idField.fieldName!!)
                                .build())
                        .addFunction(FunSpec.builder("delete")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(idField.fieldName!!, String::class)
                                .addStatement("jdbcTemplate.jdbcTemplate.update(%1S, %2L)", "delete from employee where ${idField.columnName!!.toUpperCase()} = ?", idField.fieldName!!)
                                .build())
                        .build())
                .build()
        serviceImplFile.writeTo(stringBuilder ?: System.out)
        return ClassName("", serviceName)
    }

    fun createControllerCrud(controller: String, urlRootPath: String, serviceClassName: ClassName, stringBuilder: StringBuilder?, idFieldName: String, pojoClassName: ClassName) {
        val controllerName = controller.capitalize()
        val anyTypeName = TypeVariableName("*")
        val responseEntityClassName = ClassName("org.springframework.http", "ResponseEntity")
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
                        .addFunction(FunSpec.builder("one")
                                .returns(responseEntityClassName.parameterizedBy(anyTypeName))
                                .addAnnotation(AnnotationSpec.builder(GetMapping::class)
                                        .addMember("%S", "one")
                                        .build())
                                .addParameter(idFieldName, String::class)
                                .addStatement("return ResponseEntity.ok($argName.one(%L))", idFieldName)
                                .build())
                        .addFunction(FunSpec.builder("create")
                                .returns(responseEntityClassName.parameterizedBy(anyTypeName))
                                .addAnnotation(AnnotationSpec.builder(PostMapping::class)
                                        .addMember("%S", "create")
                                        .build())
                                .addParameter(ParameterSpec.builder(pojoClassName.simpleName.decapitalize(), pojoClassName)
                                        .addAnnotation(RequestBody::class)
                                        .build())
                                .addStatement("return ResponseEntity.ok($argName.create(%L))", pojoClassName.simpleName.decapitalize())
                                .build())
                        .addFunction(FunSpec.builder("update")
                                .returns(responseEntityClassName.parameterizedBy(anyTypeName))
                                .addAnnotation(AnnotationSpec.builder(PutMapping::class)
                                        .addMember("%S", "update")
                                        .build())
                                .addParameter(idFieldName, String::class)
                                .addParameter(ParameterSpec.builder(pojoClassName.simpleName.decapitalize(), pojoClassName)
                                        .addAnnotation(RequestBody::class)
                                        .build())
                                .addStatement("return ResponseEntity.ok($argName.update(%1L, %2L))", idFieldName, pojoClassName.simpleName.decapitalize())
                                .build())
                        .addFunction(FunSpec.builder("delete")
                                .returns(responseEntityClassName.parameterizedBy(anyTypeName))
                                .addAnnotation(AnnotationSpec.builder(DeleteMapping::class)
                                        .addMember("%S", "delete")
                                        .build())
                                .addParameter(idFieldName, String::class)
                                .addStatement("return ResponseEntity.ok($argName.delete(%1L))", idFieldName)
                                .build())
                        /* .addFunction(FunSpec.builder("")
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