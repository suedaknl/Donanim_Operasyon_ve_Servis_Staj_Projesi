package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


@Dao
interface PersonnelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonnel(personnel: Personnel)

    @Update
    suspend fun updatePersonnel(personnel: Personnel)

    @Delete
    suspend fun deletePersonnel(personnel: Personnel)

    @Query("SELECT * FROM personnel_table ORDER BY fullName ASC")
    fun getAllPersonnel(): Flow<List<Personnel>>

    // Kullanıcı adı benzersizlik kontrolü ve ileride Login ekranı için kullanılacak sorgu
    @Query("SELECT * FROM personnel_table WHERE fullname = :username LIMIT 1")
    suspend fun getPersonnelByUsername(username: String): Personnel?

    @Query("SELECT * FROM personnel_table WHERE id = :id LIMIT 1")
    suspend fun getPersonnelById(id: Int): Personnel?

    @Query("SELECT * FROM personnel_table WHERE email = :email LIMIT 1")
    suspend fun getPersonnelByEmail(email: String): Personnel?

    @Query("SELECT * FROM personnel_table WHERE firebaseUid = :firebaseUid LIMIT 1")
    suspend fun getPersonnelByFirebaseUid(firebaseUid: String): Personnel?

    @Query("SELECT * FROM personnel_table ORDER BY fullName ASC")
    suspend fun getAllPersonnelList(): List<Personnel>
}

