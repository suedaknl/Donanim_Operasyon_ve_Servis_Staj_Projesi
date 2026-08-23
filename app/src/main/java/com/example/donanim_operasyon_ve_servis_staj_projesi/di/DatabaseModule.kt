package com.example.donanim_operasyon_ve_servis_staj_projesi.di

import android.content.Context
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideServiceDao(
        database: AppDatabase
    ): ServiceDao {
        return database.serviceDao()
    }

    @Provides
    fun providePersonnelDao(
        database: AppDatabase
    ): PersonnelDao {
        return database.personnelDao()
    }

    @Provides
    fun provideShiftDao(
        database: AppDatabase
    ): ShiftDao {
        return database.shiftDao()
    }

    @Provides
    fun provideLeaveRequestDao(
        database: AppDatabase
    ): LeaveRequestDao {
        return database.leaveRequestDao()
    }

    @Provides
    fun provideOvertimeDao(
        database: AppDatabase
    ): OvertimeDao {
        return database.overtimeDao()
    }
}