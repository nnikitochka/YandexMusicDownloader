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
        private val valueMap = entries.associateBy { it.num }
        fun fromInt(value: Int): Quality? = valueMap[value]
    }
}