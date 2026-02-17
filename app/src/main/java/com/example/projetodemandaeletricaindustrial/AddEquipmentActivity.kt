package com.example.projetodemandaeletricaindustrial

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.projetodemandaeletricaindustrial.databinding.ActivityAddEquipmentBinding
import kotlinx.coroutines.launch

class AddEquipmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEquipmentBinding
    private lateinit var repository: CalculadoraRepository
    private var projetoIdVinculado: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEquipmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Adicionar Equipamento"

        repository = CalculadoraRepository(this)
        projetoIdVinculado = intent.getIntExtra("PROJETO_ID", -1)

        val tipos = listOf(
            "Motor",
            "Iluminação",
            "Tomada (Área Administrativa)",
            "Tomada (Área Industrial Mono)",
            "Tomada (Área Industrial Tri)",
            "Forno / Estufa (Aquecimento)",
            "Outros"
        )
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tipos)
        binding.spinnerTipoEquipamento.setAdapter(adapterSpinner)

        binding.btnSalvarEquipamento.setOnClickListener {
            salvarNoBanco()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // Função auxiliar para automatizar o FU conforme a Planilha 2
    private fun calcularFatorUtilizacao(cv: Double): Double {
        return when {
            cv <= 2.5 -> 0.70
            cv <= 15.0 -> 0.83  // Cobre a faixa de 3 a 15 CV
            cv <= 40.0 -> 0.85  // Cobre a faixa de 20 a 40 CV
            else -> 0.87        // Acima de 40 CV
        }
    }

    private fun salvarNoBanco() {
        val tipo = binding.spinnerTipoEquipamento.text.toString()
        val descricao = binding.etDescricao.text.toString()
        val quantidade = binding.etQuantidade.text.toString().toIntOrNull() ?: 0
        val potenciaDigitada = binding.etPotencia.text.toString().toDoubleOrNull() ?: 0.0

        if (descricao.isEmpty() || potenciaDigitada <= 0.0 || projetoIdVinculado == -1) {
            Toast.makeText(this, "Erro: Preencha todos os campos corretamente", Toast.LENGTH_SHORT).show()
            return
        }

        var demandaCalculadaUnitaria = 0.0

        when {
            // GRUPO C: Motores
            tipo.equals("Motor", ignoreCase = true) -> {
                val dados = repository.buscarFatoresPorCV(potenciaDigitada)

                // IMPLEMENTAÇÃO DA OPÇÃO 2: FU Automático baseado na potência
                val fu = calcularFatorUtilizacao(potenciaDigitada)
                val potenciaWats = potenciaDigitada * 735.5 // 1 CV = 735.5 Watts

                if (dados != null) {
                    // CENÁRIO 1: Motor encontrado na tabela exata (JSON)
                    // Fórmula: (Watts * FU) / (Rendimento * FP) / 1000 = kVA
                    demandaCalculadaUnitaria = (potenciaWats * fu) / (dados.rendimento * dados.fp) / 1000.0
                } else {
                    // CENÁRIO 2 (CORREÇÃO): Motor não listado (ex: 12 CV, 50 CV)
                    // Usamos valores padrão de mercado para não zerar o cálculo
                    val rendimentoEstimado = 0.85
                    val fpEstimado = 0.83

                    demandaCalculadaUnitaria = (potenciaWats * fu) / (rendimentoEstimado * fpEstimado) / 1000.0
                }
            }

            // GRUPO A: Iluminação e Tomadas
            tipo.contains("Tomada", ignoreCase = true) || tipo.contains("Iluminação", ignoreCase = true) -> {
                val fd = repository.buscarFatorGrupoA(tipo)
                demandaCalculadaUnitaria = (potenciaDigitada / 1000.0) * fd
            }

            // GRUPO B: Aquecimento
            tipo.contains("Aquecimento", ignoreCase = true) || tipo.contains("Forno", ignoreCase = true) -> {
                // Para aquecimento (Grupo B), FD/FU costuma ser 1.0 (100%)
                demandaCalculadaUnitaria = (potenciaDigitada / 1000.0) * 1.0
            }

            else -> demandaCalculadaUnitaria = potenciaDigitada / 1000.0
        }

        val novoEquipamento = Equipamento(
            projetoId = projetoIdVinculado,
            tipo = tipo,
            descricao = descricao,
            quantidade = quantidade,
            potencia = demandaCalculadaUnitaria // Agora salva o valor unitário já calculado em kVA
        )

        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@AddEquipmentActivity)
            database.equipamentoDao().inserir(novoEquipamento)

            runOnUiThread {
                Toast.makeText(this@AddEquipmentActivity, "Salvo! FU usado: ${calcularFatorUtilizacao(potenciaDigitada)}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}