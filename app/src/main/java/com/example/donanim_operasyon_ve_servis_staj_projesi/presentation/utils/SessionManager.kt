package com.example.donanim_operasyon_ve_servis_staj_projesi.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_session", Context.MODE_PRIVATE)

    fun saveSession(isRememberMe: Boolean, role: String, personnelId: Int = 0) {
        prefs.edit().apply {
            putBoolean("IS_REMEMBER_ME", isRememberMe)
            putString("USER_ROLE", role)
            putInt("PERSONNEL_ID", personnelId)
            apply()
        }
    }

    fun isRememberMe(): Boolean = prefs.getBoolean("IS_REMEMBER_ME", false)
    fun getUserRole(): String? = prefs.getString("USER_ROLE", null)
    fun getPersonnelId(): Int = prefs.getInt("PERSONNEL_ID", 0)

    // --- YENİ: Kullanıcı adını hatırlamak için eklendi ---
    fun saveLastUsername(username: String) {
        prefs.edit().putString("LAST_USERNAME", username).apply()
    }

    fun getLastUsername(): String = prefs.getString("LAST_USERNAME", "") ?: ""
    // -----------------------------------------------------

    fun clearSession() {
        // Çıkış yapıldığında e-postayı kaybetmemek için önce yedeğini alıyoruz
        val lastUser = getLastUsername()

        prefs.edit().clear().apply() // Tüm oturum bilgilerini (hatırla, rol vs.) güvenle sil

        // Sadece e-postayı (kullanıcı adını) geri yazıyoruz
        saveLastUsername(lastUser)
    }
}