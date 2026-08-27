package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceFeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: ServiceFeedback)

    @Query("SELECT * FROM service_feedbacks WHERE serviceId = :serviceId")
    suspend fun getFeedbackForService(serviceId: Int): ServiceFeedback?

    @Query("SELECT * FROM service_feedbacks ORDER BY timestamp DESC")
    fun getAllFeedbacks(): Flow<List<ServiceFeedback>>

    @Query("SELECT * FROM service_feedbacks WHERE serviceId = :serviceId LIMIT 1")
    fun getFeedbackFlowForService(serviceId: Int): Flow<ServiceFeedback?>

    @Query("SELECT * FROM service_feedbacks WHERE serviceId = :serviceId OR firestoreId = :firestoreId LIMIT 1")
    fun getFeedbackForServiceFlow(serviceId: Int, firestoreId: String): Flow<ServiceFeedback?>
}