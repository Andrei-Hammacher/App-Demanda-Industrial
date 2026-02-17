package com.example.projetodemandaeletricaindustrial

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Aumentamos a versão para 2 e incluímos a Entidade Projeto
@Database(entities = [Equipamento::class, Projeto::class], version = 2)
abstract class AppDatabase : RoomDatabase() {

    abstract fun equipamentoDao(): EquipamentoDao
    abstract fun projetoDao(): ProjetoDao // Adicionamos o novo DAO aqui

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
                    .fallbackToDestructiveMigration() // Isso evita erros de versão durante o desenvolvimento
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}