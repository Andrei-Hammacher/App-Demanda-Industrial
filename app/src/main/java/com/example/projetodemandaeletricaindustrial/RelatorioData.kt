package com.example.projetodemandaeletricaindustrial

/**
 * Agrupa todos os valores já calculados em ListaCargasActivity
 * para passar ao RelatorioGenerator sem recalcular nada.
 */
data class RelatorioData(
    // Identificação
    val projetoNome: String,
    val dataGeracao: String,

    // Grupos de demanda (kVA)
    val grupoA: Double,
    val grupoB: Double,
    val demandaGrupoC: Double,
    val demandaRetificadores: Double,
    val demandaSolda: Double,
    val outrasEspeciais: Double,
    val demandaTotal: Double,

    // Detalhamento Grupo A
    val demAdm: Double,
    val demSe: Double,
    val demIndMono: Double,
    val demIndTri: Double,
    val demIlum: Double,

    // FP e transformador
    val fpGlobal: Double,
    val reservaCarga: Double,
    val demandaComReserva: Double,
    val trafoSugerido: String,

    // Simultaneidade
    val textoSimultaneidade: String,
    val maiorMotorDescricao: String?,

    // Painéis
    val textoCcm: String,
    val textoQdf: String,

    // Lista completa de equipamentos
    val equipamentos: List<Equipamento>
)