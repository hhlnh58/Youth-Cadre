package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        WorkspaceEntity::class,
        PersonEntity::class,
        PortfolioEntity::class,
        ProjectEntity::class,
        TaskEntity::class,
        CalendarEventEntity::class,
        NoteEntity::class,
        DocumentEntity::class,
        DecisionEntity::class,
        SettlementEntity::class,
        QuickLinkEntity::class,
        ChatMessageEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class CadreDatabase : RoomDatabase() {
    abstract fun cadreDao(): CadreDao

    companion object {
        @Volatile
        private var INSTANCE: CadreDatabase? = null

        fun getDatabase(context: Context): CadreDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CadreDatabase::class.java,
                    "youth_cadre_os.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
