package ru.nnedition.ymdownloader.api.objects

enum class Quality(
    val num: Int,
    val shortName: String,
) {
    LOW(1, "lq"),
    NORMAL(2, "nq"),
    HIGH(3, "hq"),
    LOSSLESS(4, "lossless");

    override fun toString() = shortName

    companion object {
        fun fromInt(value: Int): Quality? = entries.associateBy { it.num }[value]

        fun fromShort(value: String): Quality? = entries.associateBy { it.shortName }[value]
    }
}