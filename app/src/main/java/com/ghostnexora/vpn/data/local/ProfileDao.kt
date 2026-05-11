package com.ghostnexora.vpn.data.local

import androidx.room.*
import com.ghostnexora.vpn.data.model.VpnProfile
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones CRUD sobre la tabla vpn_profiles.
 * Todas las operaciones de lectura exponen Flow para reactividad automática.
 */
@Dao
interface ProfileDao {

    // ── Queries ──────────────────────────────────────────────────────────

    /** Todos los perfiles, ordenados por nombre */
    @Query("SELECT * FROM vpn_profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<VpnProfile>>

    /** Solo perfiles activos */
    @Query("SELECT * FROM vpn_profiles WHERE enabled = 1 ORDER BY name ASC")
    fun getEnabledProfiles(): Flow<List<VpnProfile>>

    /** Perfiles favoritos */
    @Query("SELECT * FROM vpn_profiles WHERE is_favorite = 1 ORDER BY name ASC")
    fun getFavoriteProfiles(): Flow<List<VpnProfile>>

    /** Buscar por nombre o host */
    @Query("""
        SELECT * FROM vpn_profiles 
        WHERE name LIKE '%' || :query || '%' 
           OR host LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchProfiles(query: String): Flow<List<VpnProfile>>

    /** Perfil por ID (suspend para uso puntual) */
    @Query("SELECT * FROM vpn_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): VpnProfile?

    /** Perfil por ID (Flow para observación reactiva) */
    @Query("SELECT * FROM vpn_profiles WHERE id = :id LIMIT 1")
    fun observeProfileById(id: String): Flow<VpnProfile?>

    /** Contar todos los perfiles */
    @Query("SELECT COUNT(*) FROM vpn_profiles")
    fun getProfileCount(): Flow<Int>

    /** Último perfil usado */
    @Query("""
        SELECT * FROM vpn_profiles 
        WHERE last_used != '' 
        ORDER BY last_used DESC 
        LIMIT 1
    """)
    suspend fun getLastUsedProfile(): VpnProfile?

    // ── Insert / Update ───────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: VpnProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<VpnProfile>)

    @Update
    suspend fun updateProfile(profile: VpnProfile)

    /** Actualiza solo el campo lastUsed al conectar */
    @Query("UPDATE vpn_profiles SET last_used = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: String)

    /** Alterna el estado favorito */
    @Query("UPDATE vpn_profiles SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    /** Habilitar o deshabilitar un perfil */
    @Query("UPDATE vpn_profiles SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    // ── Delete ────────────────────────────────────────────────────────────

    @Delete
    suspend fun deleteProfile(profile: VpnProfile)

    @Query("DELETE FROM vpn_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: String)

    @Query("DELETE FROM vpn_profiles")
    suspend fun deleteAllProfiles()
}
