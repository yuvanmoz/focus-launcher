package com.focuslauncher.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.focuslauncher.core.database.entity.FocusProfileEntity
import com.focuslauncher.core.database.entity.ProfileAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM focus_profiles")
    fun getAllProfilesFlow(): Flow<List<FocusProfileEntity>>

    @Query("SELECT * FROM focus_profiles")
    suspend fun getAllProfiles(): List<FocusProfileEntity>

    @Query("SELECT * FROM focus_profiles WHERE id = :profileId")
    suspend fun getProfileById(profileId: Long): FocusProfileEntity?

    @Query("SELECT * FROM focus_profiles WHERE id = :profileId")
    fun getProfileByIdFlow(profileId: Long): Flow<FocusProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: FocusProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: FocusProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: FocusProfileEntity)

    // Allowed Apps mapping
    @Query("SELECT * FROM profile_apps WHERE profileId = :profileId")
    fun getAllowedAppsFlow(profileId: Long): Flow<List<ProfileAppEntity>>

    @Query("SELECT * FROM profile_apps WHERE profileId = :profileId")
    suspend fun getAllowedApps(profileId: Long): List<ProfileAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllowedApps(apps: List<ProfileAppEntity>)

    @Query("DELETE FROM profile_apps WHERE profileId = :profileId")
    suspend fun clearAllowedApps(profileId: Long)

    @Transaction
    suspend fun updateAllowedAppsForProfile(profileId: Long, apps: List<ProfileAppEntity>) {
        clearAllowedApps(profileId)
        if (apps.isNotEmpty()) {
            insertAllowedApps(apps)
        }
    }
}
