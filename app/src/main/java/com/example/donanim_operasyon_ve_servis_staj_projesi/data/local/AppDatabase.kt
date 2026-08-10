package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ServiceRecord::class,
        Personnel::class,
        ServiceNote::class,
        ServicePhoto::class // YENİ: Fotoğraf tablosu eklendi
    ],
    version = 6, // Versiyon 5'ten 6'ya güncellendi
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun serviceDao(): ServiceDao
    abstract fun personnelDao(): PersonnelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}