package de.sudokuonline.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room Database for AI Learning
 *
 * Stores:
 * - Learned positions with best moves and win rates
 * - Complete game histories for analysis
 * - Move sequences that led to wins/losses
 */
@Database(
    entities = [
        LearnedPositionEntity::class,
        GameHistoryEntity::class,
        MoveSequenceEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AIDatabase : RoomDatabase() {

    abstract fun learnedPositionDao(): LearnedPositionDao
    abstract fun gameHistoryDao(): GameHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AIDatabase? = null

        fun getInstance(context: Context): AIDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AIDatabase::class.java,
                    "ai_learning_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
