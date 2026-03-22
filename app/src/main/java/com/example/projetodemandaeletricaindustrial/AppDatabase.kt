package com.example.projetodemandaeletricaindustrial

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Equipamento::class, Projeto::class], version = 5)
abstract class AppDatabase : RoomDatabase() {

    abstract fun equipamentoDao(): EquipamentoDao
    abstract fun projetoDao(): ProjetoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "demanda_industrial_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}