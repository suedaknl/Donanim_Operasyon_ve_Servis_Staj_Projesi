package com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.NotificationEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var activeRecipientUid: String? = null

    fun startSync(recipientUid: String) {
        if (recipientUid.isBlank()) return
        if (activeRecipientUid == recipientUid) return

        activeRecipientUid = recipientUid
        notificationRepository.startSync(recipientUid)

        viewModelScope.launch {
            launch {
                notificationRepository.getNotifications(recipientUid).collectLatest { list ->
                    _notifications.value = list
                }
            }
            launch {
                notificationRepository.getUnreadCount(recipientUid).collectLatest { count ->
                    _unreadCount.value = count
                }
            }
        }
    }

    fun markAsRead(notificationId: String, recipientUid: String) {
        if (recipientUid.isBlank()) return
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId, recipientUid)
        }
    }

    fun markAllAsRead(recipientUid: String) {
        if (recipientUid.isBlank()) return
        viewModelScope.launch {
            notificationRepository.markAllAsRead(recipientUid)
        }
    }

    fun deleteNotification(notificationId: String) {
        if (notificationId.isBlank()) return
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
        }
    }

    fun clearAllNotifications(recipientUid: String) {
        if (recipientUid.isBlank()) return
        viewModelScope.launch {
            notificationRepository.clearAllNotifications(recipientUid)
        }
    }

    override fun onCleared() {
        super.onCleared()
        notificationRepository.stopSync()
    }
}