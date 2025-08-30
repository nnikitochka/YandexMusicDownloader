package ru.nnedition.lrclib.line

open class MetaLine(
    val key: String,
    open val value: String,
) : Line {
    companion object {
        private val registry = mutableMapOf<String, (String) -> MetaLine?>()

        fun register(key: String, factory: (String) -> MetaLine?) {
            registry[key] = factory
        }

        init {
            register("ar") { Artists(it) }
            register("ti") { Title(it) }
            register("al") { Album(it) }
            register("by") { Writers(it) }
            register("offset") { offset -> offset.toLongOrNull()?.let { Offset(offset) } }
        }

        fun fromKey(key: String, value: String): MetaLine? = this.registry[key]?.invoke(value)
    }

    override fun toString() = "[$key:$value]"

    data class Artists(
        override val value: String
    ) : MetaLine("ar", value)

    data class Title(
        override val value: String
    ) : MetaLine("ti", value)

    data class Album(
        override val value: String
    ) : MetaLine("al", value)

    data class Writers(
        override val value: String
    ) : MetaLine("by", value)

    data class Offset(
        override val value: String
    ) : MetaLine("offset", value)
}