package com.darkjade.streamlib.data.repository

import android.content.Context
import com.darkjade.streamlib.data.db.StreamLibDatabase
import com.darkjade.streamlib.data.db.entity.ProfileEntity

class ProfileRepository(context: Context) {
    private val db = StreamLibDatabase.getInstance(context)
    private val dao = db.profileDao()

    fun observeAll() = dao.observeAll()

    suspend fun ensureDefaultProfile(): ProfileEntity {
        val existing = dao.getDefault()
        if (existing != null) return existing
        val id = dao.insert(ProfileEntity(name = "Profile 1", isDefault = true))
        return dao.getById(id)!!
    }

    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun addProfile(name: String) = dao.insert(ProfileEntity(name = name, isDefault = false))
    suspend fun updateProfile(profile: ProfileEntity) = dao.update(profile)
    suspend fun deleteProfile(profile: ProfileEntity) = dao.delete(profile)
}
