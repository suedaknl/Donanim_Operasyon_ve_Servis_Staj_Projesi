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
        ServiceClosingSignature::class,
        ShiftEntity::class,
        LeaveRequestEntity::class,
        OvertimeEntity::class
    ],
    version = 20, // <--- Versiyon 20'ye yükseltildi
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun serviceDao(): ServiceDao
    abstract fun personnelDao(): PersonnelDao
    abstract fun closingSignatureDao(): ClosingSignatureDao
    abstract fun shiftDao(): ShiftDao
    abstract fun leaveRequestDao(): LeaveRequestDao
    abstract fun overtimeDao(): OvertimeDao

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

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE service_records ADD COLUMN latitude REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE service_records ADD COLUMN longitude REAL DEFAULT NULL")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE personnel_table ADD COLUMN currentLatitude REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE personnel_table ADD COLUMN currentLongitude REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE personnel_table ADD COLUMN lastLocationUpdate INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE service_records ADD COLUMN assignedPersonnelName TEXT DEFAULT NULL"
                )
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE service_records ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE service_records ADD COLUMN archivedAt INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE service_closing_signatures ADD COLUMN signatureData TEXT"
                )
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS shifts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        personnelId INTEGER NOT NULL,
                        shiftDate TEXT NOT NULL,
                        startTime TEXT NOT NULL,
                        endTime TEXT NOT NULL,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS leave_requests (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        personnelId INTEGER NOT NULL,
                        startDate TEXT NOT NULL,
                        endDate TEXT NOT NULL,
                        leaveType TEXT NOT NULL,
                        description TEXT NOT NULL,
                        status TEXT NOT NULL,
                        adminNote TEXT,
                        createdAt INTEGER NOT NULL,
                        reviewedAt INTEGER
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS overtimes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        personnelId INTEGER NOT NULL,
                        serviceRecordId INTEGER,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER NOT NULL,
                        durationMinutes INTEGER NOT NULL,
                        description TEXT,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }

        // İŞ HAVUZU İÇİN YENİ MİGRASYON (19 -> 20)
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE service_records ADD COLUMN assignmentType TEXT NOT NULL DEFAULT 'DIRECT'")
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
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        AppDatabase.MIGRATION_15_16,
                        AppDatabase.MIGRATION_16_17,
                        AppDatabase.MIGRATION_17_18,
                        AppDatabase.MIGRATION_18_19,
                        AppDatabase.MIGRATION_19_20 // <--- Migrasyon zincirine eklendi
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}