package ru.nnedition.ymdownloader.api.objects.artist

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

data class ArtistMeta(
    val id: Long,
    val name: String,
    val decomposed: List<ArtistMeta> = emptyList(),
) {
    class Deserializer : JsonDeserializer<ArtistMeta> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ArtistMeta {
            val jsonObject = json.asJsonObject

            val id = jsonObject.get("id").asLong
            val name = jsonObject.get("name").asString

            val decomposed = mutableListOf<ArtistMeta>()

            if (jsonObject.has("decomposed")) {
                jsonObject.get("decomposed").asJsonArray.forEach { element ->
                    if (!element.isJsonObject) return@forEach

                    val jsonObject = element.asJsonObject
                    if (!jsonObject.has("id") || !jsonObject.has("name"))
                        return@forEach

                    decomposed.add(ArtistMeta(
                        id = jsonObject.get("id").asLong,
                        name = jsonObject.get("name").asString
                    ))
                }
            }

            return ArtistMeta(id, name, decomposed)
        }
    }
}