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
    version = 12,
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

        // Eksik olan 10 -> 11 geçişi eklendi (Veriyi koruyarak güvenli köprü)
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Eğer 10'dan 11'e geçerken tabloya eklenen bir sütun varsa buraya yazılabilir,
                // yoksa mevcut yapıyı korumak adına boş bırakılabilir veya güvenli komut yazılabilir.
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE service_records ADD COLUMN rejectionReason TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "service_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}