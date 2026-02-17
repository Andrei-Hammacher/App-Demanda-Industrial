package com.example.projetodemandaeletricaindustrial

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "tabela_equipamentos")
data class Equipamento(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projetoId: Int, // <--- NOVA COLUNA: Liga o motor ao projeto
    val tipo: String,
    val descricao: String,
    val quantidade: Int,
    val potencia: Double
) : Serializable