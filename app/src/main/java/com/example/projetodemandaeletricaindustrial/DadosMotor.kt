package com.example.projetodemandaeletricaindustrial

// Esta classe permite que o GSON converta seu JSON em objetos Kotlin
data class DadosMotor(
    val cv: Double,
    val rendimento: Double,
    val fp: Double
)