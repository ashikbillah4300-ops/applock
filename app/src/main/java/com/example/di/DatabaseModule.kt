package com.example.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.GlassLockDatabase
import com.example.data.local.HiddenAppDao
import com.example.data.local.IntruderEventDao
import com.example.data.local.LockedAppDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `hidden_apps` (
                    `packageHash` TEXT NOT NULL,
                    `encryptedPayload` TEXT NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`packageHash`)
                )
                """.trimIndent()
            )
        }
    }

    @Provides
    @Singleton
    fun provideGlassLockDatabase(
        @ApplicationContext context: Context
    ): GlassLockDatabase {
        return Room.databaseBuilder(
            context,
            GlassLockDatabase::class.java,
            "glasslock_db"
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideLockedAppDao(db: GlassLockDatabase): LockedAppDao = db.lockedAppDao()

    @Provides
    fun provideIntruderEventDao(db: GlassLockDatabase): IntruderEventDao = db.intruderEventDao()

    @Provides
    fun provideHiddenAppDao(db: GlassLockDatabase): HiddenAppDao = db.hiddenAppDao()
}
