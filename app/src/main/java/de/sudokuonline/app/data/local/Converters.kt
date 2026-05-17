package de.sudokuonline.app.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Type converters for Room database using Gson
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromMoveList(moves: List<StoredMove>): String {
        return gson.toJson(moves)
    }

    @TypeConverter
    fun toMoveList(json: String): List<StoredMove> {
        if (json.isEmpty()) return emptyList()
        return try {
            val type = object : TypeToken<List<StoredMove>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(json: String): List<String> {
        if (json.isEmpty()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * Represents a stored move
 */
data class StoredMove(
    val row: Int,
    val col: Int,
    val symbol: Int,        // 1 = X, 2 = O
    val type: String = "SYMBOL"  // SYMBOL or BOMB
)
