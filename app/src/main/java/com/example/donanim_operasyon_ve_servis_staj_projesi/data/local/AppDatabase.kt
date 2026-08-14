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
    version = 13, // Versiyon 12'den 13'e çıkarıldı
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

        // YENİ EKLENEN GÜVENLİ MİGRATİON (12 -> 13): personnel_table tablosuna gender sütunu ekleniyor
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE personnel_table ADD COLUMN gender TEXT NOT NULL DEFAULT 'ERKEK'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "service_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}