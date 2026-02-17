package com.example.projetodemandaeletricaindustrial

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "tabela_projetos")
data class Projeto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nome: String,
    val data: Long = System.currentTimeMillis()
) : Serializable
