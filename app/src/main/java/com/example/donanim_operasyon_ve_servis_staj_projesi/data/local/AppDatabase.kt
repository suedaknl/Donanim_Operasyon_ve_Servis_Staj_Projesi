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
        OvertimeEntity::class,
        NotificationEntity::class,
        ServiceFeedback::class
    ],
    version = 23,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun serviceDao(): ServiceDao
    abstract fun personnelDao(): PersonnelDao
    abstract fun closingSignatureDao(): ClosingSignatureDao
    abstract fun shiftDao(): ShiftDao
    abstract fun leaveRequestDao(): LeaveRequestDao
    abstract fun overtimeDao(): OvertimeDao
    abstract fun notificationDao(): NotificationDao
    abstract fun serviceFeedbackDao(): ServiceFeedbackDao

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

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE service_records ADD COLUMN assignmentType TEXT NOT NULL DEFAULT 'DIRECT'")
                db.execSQL("ALTER TABLE service_records ADD COLUMN poolAssignmentDeadline INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE shifts ADD COLUMN firestoreId TEXT")
                db.execSQL("ALTER TABLE leave_requests ADD COLUMN firestoreId TEXT")
                db.execSQL("ALTER TABLE overtimes ADD COLUMN firestoreId TEXT")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS notifications (
                        id TEXT PRIMARY KEY NOT NULL,
                        recipientUid TEXT NOT NULL,
                        role TEXT NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        body TEXT NOT NULL,
                        targetId TEXT,
                        createdAt INTEGER NOT NULL,
                        isRead INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_leave_requests_firestoreId ON leave_requests(firestoreId)")
            }
        }

        // --- MİGRATİON (22 -> 23): ServiceFeedbacks Tablosu ve Kolonları ---
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS service_feedbacks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        serviceId INTEGER NOT NULL,
                        firestoreId TEXT,
                        rating INTEGER NOT NULL,
                        quality INTEGER NOT NULL DEFAULT 0,
                        staff INTEGER NOT NULL DEFAULT 0,
                        speed INTEGER NOT NULL DEFAULT 0,
                        comment TEXT,
                        timestamp INTEGER NOT NULL
                    )
                """)
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
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_19_20,
                        MIGRATION_20_21,
                        MIGRATION_21_22,
                        MIGRATION_22_23
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}