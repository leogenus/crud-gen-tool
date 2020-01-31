package uz.kvikk.crudgen.domain.enums

import com.squareup.kotlinpoet.ClassName

interface FieldTypeInterface {
    fun value(): ClassName
}