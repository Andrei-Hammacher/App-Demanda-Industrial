package com.example.projetodemandaeletricaindustrial

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipamentoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(equipamento: Equipamento)

    @Update
    suspend fun atualizar(equipamento: Equipamento)

    @Delete
    suspend fun deletar(equipamento: Equipamento)

    // Lista todos os equipamentos vinculados ao ID deste projeto
    @Query("SELECT * FROM tabela_equipamentos WHERE projetoId = :projetoId")
    fun listarPorProjeto(projetoId: Int): Flow<List<Equipamento>>

    // OPÇÃO 1: Busca o motor de maior potência individual (Crítico para partida)
    @Query("SELECT * FROM tabela_equipamentos WHERE projetoId = :projetoId AND tipo = 'Motor' ORDER BY potencia DESC LIMIT 1")
    suspend fun buscarMaiorMotor(projetoId: Int): Equipamento?

    // OPÇÃO 3: Soma para o Fator de Potência Global
    // Dica: Use coalesce para evitar que retorne null caso a lista esteja vazia
    @Query("SELECT TOTAL(potencia * quantidade) FROM tabela_equipamentos WHERE projetoId = :projetoId")
    fun obterDemandaTotalProjeto(projetoId: Int): Flow<Double>
}