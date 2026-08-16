package com.whitecall.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.whitecall.app.data.local.dao.BlockedCallDao
import com.whitecall.app.data.local.dao.GroupDao
import com.whitecall.app.data.local.dao.WhiteListDao
import com.whitecall.app.data.local.entity.BlockedCallEntity
import com.whitecall.app.data.local.entity.GroupEntity
import com.whitecall.app.data.local.entity.WhiteListEntity

@Database(
    entities = [WhiteListEntity::class, BlockedCallEntity::class, GroupEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun whiteListDao(): WhiteListDao
    abstract fun blockedCallDao(): BlockedCallDao
    abstract fun groupDao(): GroupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "whitecall_database.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
