package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ServiceRecord::class, Personnel::class],
    version = 4, // YENİ EKLENEN: Versiyon 3'ten 4'e güncellendi
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
                    "app_database" // Eğer kendi projende buradaki veritabanı adın farklıysa (örneğin "service_db"), onu aynı bırakabilirsin.
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}