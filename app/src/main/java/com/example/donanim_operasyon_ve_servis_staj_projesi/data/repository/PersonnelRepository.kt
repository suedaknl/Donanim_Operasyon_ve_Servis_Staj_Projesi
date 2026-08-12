package com.example.donanim_operasyon_ve_servis_staj_projesi.repository

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.PersonnelDao
import kotlinx.coroutines.flow.Flow

class PersonnelRepository(private val personnelDao: PersonnelDao) {

    fun getAllPersonnel(): Flow<List<Personnel>> {
        return personnelDao.getAllPersonnel()
    }

    suspend fun insertPersonnel(personnel: Personnel) {
        personnelDao.insertPersonnel(personnel)
    }

    suspend fun updatePersonnel(personnel: Personnel) {
        personnelDao.updatePersonnel(personnel)
    }

    suspend fun deletePersonnel(personnel: Personnel) {
        personnelDao.deletePersonnel(personnel)
    }

    // Kullanıcı adını veritabanında arayan fonksiyon
    suspend fun getPersonnelByUsername(username: String): Personnel? {
        return personnelDao.getPersonnelByUsername(username)
    }

    suspend fun getPersonnelById(id: Int): Personnel? {
        return personnelDao.getPersonnelById(id)
    }

    suspend fun getPersonnelByEmail(email: String): Personnel? {
        return personnelDao.getPersonnelByEmail(email)
    }

}