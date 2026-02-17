package com.example.projetodemandaeletricaindustrial

data class SimultaneidadeWrapper(
    val fatores_simultaneidade: List<CategoriaSimultaneidade>
)

data class CategoriaSimultaneidade(
    val categoria: String,
    val tabela: List<FatorSimultaneidadeItem>
)

data class FatorSimultaneidadeItem(
    val quantidade: Int,
    val fator: Double
)