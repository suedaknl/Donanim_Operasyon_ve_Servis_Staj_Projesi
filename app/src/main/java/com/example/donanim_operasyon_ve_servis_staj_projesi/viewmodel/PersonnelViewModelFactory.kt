package com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.PersonnelRepository

class PersonnelViewModelFactory(
    private val repository: PersonnelRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PersonnelViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PersonnelViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}