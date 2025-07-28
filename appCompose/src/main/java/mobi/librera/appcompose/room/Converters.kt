package mobi.librera.appcompose.room

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

@ProvidedTypeConverter
object Converters {
    //private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromList(list: List<String>): String = Json.encodeToString(list)

    @TypeConverter
    fun toList(data: String): List<String> = Json.decodeFromString(data)


    @TypeConverter
    fun fromMap(list: Map<String, String>): String = Json.encodeToString(list)

    @TypeConverter
    fun toMap(data: String): Map<String, String> = Json.decodeFromString(data)


}