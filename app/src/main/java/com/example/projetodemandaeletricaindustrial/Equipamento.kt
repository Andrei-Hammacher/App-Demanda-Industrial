package com.example.projetodemandaeletricaindustrial

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "tabela_equipamentos")
data class Equipamento(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projetoId: Int,
    val tipo: String,
    val descricao: String,
    val quantidade: Int,
    val potencia: Double,               // Demanda unitária calculada em kVA
    val potenciaOriginal: Double = 0.0, // Valor original digitado (CV para motores, W para demais)
    val ccm: String = "",               // Painel de motores (CCM1–CCM5). Vazio para não-motores.
    val qdf: String = ""                // Painel de equipamentos não-motores (QDF1–QDF5). Vazio para motores.
) : Serializable