package com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {

    // Mevcut oturum açmış kullanıcıyı döndürür
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    // Coroutine (suspend) destekli, güvenli giriş fonksiyonu
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            // await() metodu ile callback cehenneminden kurtulup işlemi asenkron bekliyoruz
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Giriş başarılı ancak kullanıcı nesnesi boş döndü."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Şifre sıfırlama e-postası gönderme fonksiyonu (Coroutine destekli)
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Firebase'den dönen hataları yakala
            Result.failure(e)
        }
    }

    // Çıkış yapma
    fun signOut() {
        auth.signOut()
    }
}