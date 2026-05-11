package com.ghostnexora.vpn.di

import android.content.Context
import com.ghostnexora.vpn.data.local.AppDatabase
import com.ghostnexora.vpn.data.local.DataStoreManager
import com.ghostnexora.vpn.data.local.LogDao
import com.ghostnexora.vpn.data.local.ProfileDao
import com.ghostnexora.vpn.data.repository.ProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt principal.
 * Provee todas las dependencias del grafo de DI en scope Singleton:
 * - AppDatabase (Room)
 * - ProfileDao, LogDao
 * - DataStoreManager
 * - ProfileRepository
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ── Base de datos Room ────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = AppDatabase.getInstance(context)

    // ── DAOs ──────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()

    @Provides
    @Singleton
    fun provideLogDao(db: AppDatabase): LogDao = db.logDao()

    // ── DataStore ─────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideDataStoreManager(
        @ApplicationContext context: Context
    ): DataStoreManager = DataStoreManager(context)

    // ── Repositorio ───────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideProfileRepository(
        profileDao: ProfileDao,
        logDao: LogDao,
        dataStore: DataStoreManager
    ): ProfileRepository = ProfileRepository(profileDao, logDao, dataStore)
}
