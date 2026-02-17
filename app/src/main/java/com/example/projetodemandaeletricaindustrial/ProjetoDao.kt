package com.example.projetodemandaeletricaindustrial

import androidx.room.*

@Dao
interface ProjetoDao {
    @Query("SELECT * FROM tabela_projetos ORDER BY data DESC")
    suspend fun buscarTodosProjetos(): List<Projeto>

    @Insert
    suspend fun inserirProjeto(projeto: Projeto): Long // Retorna o ID do projeto criado

    @Delete
    suspend fun deletarProjeto(projeto: Projeto)
}