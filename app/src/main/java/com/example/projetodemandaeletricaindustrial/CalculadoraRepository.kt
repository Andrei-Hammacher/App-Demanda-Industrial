package com.example.projetodemandaeletricaindustrial

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

class CalculadoraRepository(private val context: Context) {

    private val gson = Gson()
    // Acesso ao banco de dados para buscar o Maior Motor
    private val db = AppDatabase.getDatabase(context)
    private val equipamentoDao = db.equipamentoDao()

    // --- IMPLEMENTAÇÃO DA OPÇÃO 1: BUSCAR MAIOR MOTOR NO BANCO ---
    /**
     * Busca o equipamento do tipo "Motor" com a maior potência (kVA) para o projeto.
     * Essencial para o cálculo de queda de tensão na partida.
     */
    suspend fun buscarMaiorMotor(projetoId: Int): Equipamento? {
        return equipamentoDao.buscarMaiorMotor(projetoId)
    }

    // --- GRUPO C: MOTORES (DADOS TÉCNICOS DE RENDIMENTO E FP) ---
    private fun carregarDadosMotores(): List<DadosMotor> {
        return try {
            val jsonString = context.assets.open("motores.json")
                .bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<DadosMotor>>() {}.type
            gson.fromJson(jsonString, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun buscarFatoresPorCV(cvDigitado: Double): DadosMotor? {
        return carregarDadosMotores().find { it.cv == cvDigitado }
    }

    // --- AUTOMAÇÃO DO FU (PLANILHA 2) ---
    fun buscarFatorUtilizacao(cvDigitado: Double, tipo: String): Double {
        if (!tipo.equals("Motor", ignoreCase = true)) return 1.0

        // Lógica baseada na sua Planilha 2:
        return when {
            cvDigitado <= 2.5 -> 0.70
            cvDigitado <= 15.0 -> 0.83
            cvDigitado <= 40.0 -> 0.85
            else -> 0.87
        }
    }

    // --- SIMULTANEIDADE (PLANILHA 4) ---
    fun buscarFatorSimultaneidade(qtdEquipamentos: Int, cvReferencia: Double, tipo: String): Double {
        return try {
            val jsonString = context.assets.open("simultaneidade.json")
                .bufferedReader().use { it.readText() }

            val wrapperType = object : TypeToken<SimultaneidadeWrapper>() {}.type
            // Use a chamada direta sem os nomes "json =" para evitar erro de ambiguidade
            val data: SimultaneidadeWrapper = gson.fromJson(jsonString, wrapperType)

            val categoriaNome = when {
                tipo.contains("Soldador", ignoreCase = true) -> "Soldadores"
                cvReferencia <= 2.5 -> "Motores: 3/4 a 2,5 CV"
                cvReferencia <= 15.0 -> "Motores: 3 a 15 CV"
                cvReferencia <= 40.0 -> "Motores: 20 a 40 CV"
                else -> "Acima de 40 CV"
            }

            val categoria = data.fatores_simultaneidade.find { it.categoria == categoriaNome }

            categoria?.tabela?.filter { it.quantidade <= qtdEquipamentos }
                ?.maxByOrNull { it.quantidade }?.fator ?: 1.0

        } catch (e: Exception) {
            1.0
        }
    }

    // --- GRUPO A: ILUMINAÇÃO E TOMADAS ---
    fun buscarFatorGrupoA(tipoSelecionado: String): Double {
        return try {
            val jsonString = context.assets.open("fatores_iluminacao.json")
                .bufferedReader().use { it.readText() }

            val wrapperType = object : TypeToken<Map<String, List<Map<String, Any>>>>() {}.type
            val data: Map<String, List<Map<String, Any>>> = gson.fromJson(jsonString, wrapperType)

            val lista = data["fatores_grupo_a"]
            val item = lista?.find { it["tipo"] == tipoSelecionado }

            (item?.get("fator") as? Double) ?: 1.0
        } catch (e: Exception) {
            1.0
        }
    }
}