package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("SELECT * FROM notifications WHERE recipientUid = :recipientUid ORDER BY createdAt DESC")
    fun getNotificationsForUser(recipientUid: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE recipientUid = :recipientUid AND isRead = 0")
    fun getUnreadCount(recipientUid: String): Flow<Int>

    @Query("UPDATE notifications SET isRead = 1 WHERE recipientUid = :recipientUid")
    suspend fun markAllAsRead(recipientUid: String)

    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteById(notificationId: String)

    @Query("DELETE FROM notifications WHERE recipientUid = :recipientUid")
    suspend fun deleteByRecipient(recipientUid: String)

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NotificationEntity?

    @Transaction
    suspend fun upsert(notification: NotificationEntity) {
        insert(notification)
    }
}