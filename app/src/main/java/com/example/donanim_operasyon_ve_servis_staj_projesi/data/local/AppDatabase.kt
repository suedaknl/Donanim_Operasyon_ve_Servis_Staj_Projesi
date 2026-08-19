package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ServiceRecord::class,
        Personnel::class,
        ServiceNote::class,
        ServicePhoto::class,
        ServiceClosingSignature::class
    ],
    version = 14, // Versiyon 13'ten 14'e çıkarıldı (latitude ve longitude eklendi)
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun serviceDao(): ServiceDao
    abstract fun personnelDao(): PersonnelDao
    abstract fun closingSignatureDao(): ClosingSignatureDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE service_records ADD COLUMN firestoreId TEXT")
                db.execSQL("ALTER TABLE service_records ADD COLUMN assignedPersonnelUid TEXT")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE service_records ADD COLUMN firestoreId TEXT")
                db.execSQL("ALTER TABLE service_records ADD COLUMN assignedPersonnelUid TEXT")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE service_records ADD COLUMN rejectionReason TEXT")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE personnel_table ADD COLUMN gender TEXT NOT NULL DEFAULT 'ERKEK'")
            }
        }

        // YENİ EKLENEN GÜVENLİ MİGRATİON (13 -> 14): service_records tablosuna latitude ve longitude ekleniyor
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE service_records ADD COLUMN latitude REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE service_records ADD COLUMN longitude REAL DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "service_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14 // YENİ MİGRATİON BURAYA EKLENDİ
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}