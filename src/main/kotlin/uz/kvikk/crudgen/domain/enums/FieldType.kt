package uz.kvikk.crudgen.domain.enums

import com.squareup.kotlinpoet.ClassName

enum class FieldType : FieldTypeInterface {


    ID {
        override fun value(): ClassName = ClassName("", "String")
    },

    STRING {
        override fun value(): ClassName = ClassName("", "String")
    },

    INT {
        override fun value(): ClassName = ClassName("", "Int")
    },

    BOOLEAN {
        override fun value(): ClassName = ClassName("", "Boolean")
    },

    BYTE_ARRAY {
        override fun value(): ClassName = ClassName("", "ByteArray")
    },

    BYTE {
        override fun value(): ClassName = ClassName("", "Byte")
    },

    LONG {
        override fun value(): ClassName = ClassName("", "Long")
    },

    FLOAT {
        override fun value(): ClassName = ClassName("", "Float")
    },

    DOUBLE {
        override fun value(): ClassName = ClassName("", "Double")
    },

    BIG_DECIMAL {
        override fun value(): ClassName = ClassName("java.math", "BigDecimal")
    },

    TIME {
        override fun value(): ClassName = ClassName("java.sql", "Time")
    },

    DATE_SQL {
        override fun value(): ClassName = ClassName("java.sql", "Date")
    },

    DATE_JAVA {
        override fun value(): ClassName = ClassName("java.util", "Date")
    },

    TIMESTAMP_SQL {
        override fun value(): ClassName = ClassName("java.sql", "Timestamp")
    }



}