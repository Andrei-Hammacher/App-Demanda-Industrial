package com.example.projetodemandaeletricaindustrial

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CalculadoraRepository(private val context: Context) {

    private val gson = Gson()
    private val db = AppDatabase.getDatabase(context)
    private val equipamentoDao = db.equipamentoDao()

    suspend fun buscarMaiorMotor(projetoId: Int): Equipamento? {
        return equipamentoDao.buscarMaiorMotor(projetoId)
    }

    // --- GRUPO C: MOTORES ---
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

    fun buscarFatorUtilizacao(cvDigitado: Double, tipo: String): Double {
        if (!tipo.equals("Motor", ignoreCase = true)) return 1.0
        return when {
            cvDigitado <= 2.5  -> 0.70
            cvDigitado <= 15.0 -> 0.83
            cvDigitado <= 40.0 -> 0.85
            else               -> 0.87
        }
    }

    // --- SIMULTANEIDADE (PLANILHA 4) ---
    // Carrega o JSON de simultaneidade uma única vez por chamada
    private fun carregarSimultaneidade(): SimultaneidadeWrapper? {
        return try {
            val jsonString = context.assets.open("simultaneidade.json")
                .bufferedReader().use { it.readText() }
            val wrapperType = object : TypeToken<SimultaneidadeWrapper>() {}.type
            gson.fromJson(jsonString, wrapperType)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Busca o fator de simultaneidade pelo NOME EXATO da categoria.
     * Permite que cada grupo de motores (por faixa de CV) consulte sua própria linha da Planilha4.
     */
    fun buscarFatorPorCategoria(qtdEquipamentos: Int, nomeCategoria: String): Double {
        val data = carregarSimultaneidade() ?: return 1.0
        val categoria = data.fatores_simultaneidade.find { it.categoria == nomeCategoria }
        return categoria?.tabela
            ?.filter { it.quantidade <= qtdEquipamentos }
            ?.maxByOrNull { it.quantidade }?.fator ?: 1.0
    }

    /**
     * Versão original mantida para compatibilidade — determina a categoria pelo CV de referência.
     * Usada pelo AddEquipmentActivity para exibir o FU no Toast.
     */
    fun buscarFatorSimultaneidade(qtdEquipamentos: Int, cvReferencia: Double, tipo: String): Double {
        val nomeCategoria = when {
            tipo.contains("Soldador", ignoreCase = true)     -> "Soldadores"
            tipo.contains("Retificador", ignoreCase = true)  -> "Retificadores"
            tipo.contains("Forno", ignoreCase = true)        -> "Fornos resistivos"
            cvReferencia <= 2.5                              -> "Motores: 3/4 a 2,5 CV"
            cvReferencia <= 15.0                             -> "Motores: 3 a 15 CV"
            cvReferencia <= 40.0                             -> "Motores: 20 a 40 CV"
            else                                             -> "Acima de 40 CV"
        }
        return buscarFatorPorCategoria(qtdEquipamentos, nomeCategoria)
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