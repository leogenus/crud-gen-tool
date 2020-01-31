package uz.kvikk.crudgen.domain

import uz.kvikk.crudgen.domain.enums.FieldType

data class ClassDomain(
        var fieldName: String? = null,
        var fieldType: FieldType? = null,
        var columnName: String? = null
)