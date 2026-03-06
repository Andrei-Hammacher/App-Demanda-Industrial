package com.example.projetodemandaeletricaindustrial

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projetodemandaeletricaindustrial.databinding.ActivityListaCargasBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ListaCargasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListaCargasBinding
    private lateinit var adapter: EquipamentoAdapter
    private val listaEquipamentos = mutableListOf<Equipamento>()

    private var projetoId: Int = -1
    private var projetoNome: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListaCargasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        projetoId = intent.getIntExtra("PROJETO_ID", -1)
        projetoNome = intent.getStringExtra("PROJETO_NOME")
        title = projetoNome ?: "Detalhes do Projeto"

        adapter = EquipamentoAdapter(listaEquipamentos) { posicao ->
            mostrarDialogoExclusao(posicao)
        }

        binding.recyclerCargas.layoutManager = LinearLayoutManager(this)
        binding.recyclerCargas.adapter = adapter

        binding.btnAdicionarCarga.setOnClickListener {
            val intent = Intent(this, AddEquipmentActivity::class.java)
            intent.putExtra("PROJETO_ID", projetoId)
            startActivity(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        carregarDadosDoBanco()
    }

    private fun carregarDadosDoBanco() {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@ListaCargasActivity)

            // CORREÇÃO 1: O nome do método no DAO agora é listarPorProjeto
            // Como ele retorna um Flow, usamos .first() para pegar a lista atual uma única vez
            val equipamentosDoBanco = database.equipamentoDao().listarPorProjeto(projetoId).first()

            listaEquipamentos.clear()
            listaEquipamentos.addAll(equipamentosDoBanco)

            adapter.notifyDataSetChanged()
            atualizarResumo()

            // IMPLEMENTAÇÃO DA OPÇÃO 1: Buscar o maior motor para alerta
            verificarMaiorMotor(projetoId)
        }
    }

    private fun verificarMaiorMotor(projetoId: Int) {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@ListaCargasActivity)
            val maiorMotor = database.equipamentoDao().buscarMaiorMotor(projetoId)

            if (maiorMotor != null) {
                // Exemplo de como mostrar o alerta na UI
                binding.tvFatorSimultaneidade.append("\n⚠️ Crítico Partida: ${maiorMotor.descricao}")
            }
        }
    }

    private fun mostrarDialogoExclusao(posicao: Int) {
        val equipamento = listaEquipamentos[posicao]

        AlertDialog.Builder(this)
            .setTitle("Remover Equipamento")
            .setMessage("Deseja excluir permanentemente este item?")
            .setPositiveButton("Excluir") { _, _ ->
                lifecycleScope.launch {
                    val database = AppDatabase.getDatabase(this@ListaCargasActivity)
                    database.equipamentoDao().deletar(equipamento)
                    carregarDadosDoBanco()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun atualizarResumo() {
        val repository = CalculadoraRepository(this)

        val grupoA = listaEquipamentos.filter {
            it.tipo.contains("Iluminação", ignoreCase = true) ||
                    it.tipo.contains("Tomada", ignoreCase = true)
        }.sumOf { it.potencia * it.quantidade }

        val grupoB = listaEquipamentos.filter {
            it.tipo.contains("Aquecimento", ignoreCase = true) ||
                    it.tipo.contains("Forno", ignoreCase = true)
        }.sumOf { it.potencia * it.quantidade }

        val motores = listaEquipamentos.filter { it.tipo.equals("Motor", ignoreCase = true) }
        val somaMotoresBruta = motores.sumOf { it.potencia * it.quantidade }
        val qtdMotores = motores.sumOf { it.quantidade }

        // Aqui pegamos a potência individual máxima para o fator de simultaneidade
        val maiorPotenciaCv = motores.maxOfOrNull { it.potencia } ?: 0.0

        val fatorC = repository.buscarFatorSimultaneidade(qtdMotores, maiorPotenciaCv, "Motor")
        val demandaGrupoC = somaMotoresBruta * fatorC

        // --- NOVIDADE: GRUPOS D e E (Cargas Especiais e Outros) ---
        val cargasEspeciais = listaEquipamentos.filter {
            it.tipo.contains("Ar Condicionado", ignoreCase = true) ||
                    it.tipo.contains("Solda", ignoreCase = true) ||
                    it.tipo.contains("Outros", ignoreCase = true)
        }.sumOf { it.potencia * it.quantidade }

        // Atualizamos a demanda total somando as cargas especiais
        val demandaTotal = grupoA + grupoB + demandaGrupoC + cargasEspeciais

        // --- IMPLEMENTAÇÃO DA OPÇÃO 3: CÁLCULO DO FATOR DE POTÊNCIA GLOBAL ---

        // 1. Calculamos a Potência Ativa (kW) estimada de cada grupo
        val pGrupoA = grupoA * 1.0
        val pGrupoB = grupoB * 1.0
        val pGrupoC = demandaGrupoC * 0.85
        // Ar Condicionado e Solda também são cargas mais indutivas, usamos média de 0.85
        val pEspeciais = cargasEspeciais * 0.85

        val potenciaAtivaTotal = pGrupoA + pGrupoB + pGrupoC + pEspeciais

        // 2. FP Global = Potência Ativa Total (kW) / Potência Aparente Total (kVA)
        val fpGlobal = if (demandaTotal > 0) potenciaAtivaTotal / demandaTotal else 1.0

        // --- CÁLCULO: RESERVA DE CARGA E TRANSFORMADOR ---
        val reservaCarga = demandaTotal * 0.10 // 10% de folga para expansão futura
        val demandaComReserva = demandaTotal + reservaCarga

        // Tabela padrão de Transformadores Comerciais (Trifásicos em kVA)
        val trafosComerciais = listOf(15.0, 30.0, 45.0, 75.0, 112.5, 150.0, 225.0, 300.0, 500.0, 750.0, 1000.0)

        // Busca o primeiro transformador que seja maior ou igual à nossa demanda final com reserva
        val trafoIdeal = trafosComerciais.find { it >= demandaComReserva }
        val textoTrafo = if (trafoIdeal != null) "$trafoIdeal kVA" else "Sob Consulta (>1000 kVA)"

        binding.apply {
            tvResumoGrupoA.text = "Grupo A (Ilum/Tom): ${"%.2f".format(grupoA)} kVA"
            tvResumoGrupoB.text = "Grupo B (Aquecim): ${"%.2f".format(grupoB)} kVA"
            tvResumoGrupoC.text = "Grupo C (Motores): ${"%.2f".format(demandaGrupoC)} kVA"

            // --- ADICIONADO NA INTERFACE ---
            tvResumoEspeciais.text = "Grupo D/E (Especiais): ${"%.2f".format(cargasEspeciais)} kVA"

            tvTotalKva.text = "Demanda Total: ${"%.2f".format(demandaTotal)} kVA"

            val categoria = definirNomeCategoria(maiorPotenciaCv)
            tvFatorSimultaneidade.text = "Fator C: ${"%.2f".format(fatorC)} (Cat: $categoria)"

            // --- EXIBIÇÃO DA OPÇÃO 3 NA TELA ---
            tvFpGlobal.text = "FP Global: ${"%.2f".format(fpGlobal)}"

            if (fpGlobal < 0.92 && demandaTotal > 0) {
                tvAlertaCapacitor.text = "⚠️ Atenção: FP < 0.92. Necessário Banco de Capacitores."
                tvAlertaCapacitor.setTextColor(android.graphics.Color.RED)
            } else if (demandaTotal > 0) {
                tvAlertaCapacitor.text = "✅ FP dentro da norma (≥ 0.92)"
                tvAlertaCapacitor.setTextColor(android.graphics.Color.parseColor("#388E3C")) // Verde padrão
            } else {
                // Limpa o texto se não houver demanda
                tvAlertaCapacitor.text = ""
            }

            // --- PREENCHENDO OS NOVOS CAMPOS DE RESERVA E TRAFO ---
            if (demandaTotal > 0) {
                tvReservaCarga.text = "Reserva Crescimento (10%): ${"%.2f".format(reservaCarga)} kVA"
                tvDemandaFinal.text = "Demanda c/ Reserva: ${"%.2f".format(demandaComReserva)} kVA"
                tvTrafoSugerido.text = "Trafo Sugerido: $textoTrafo"
            } else {
                tvReservaCarga.text = "Reserva Crescimento (10%): 0.00 kVA"
                tvDemandaFinal.text = "Demanda c/ Reserva: 0.00 kVA"
                tvTrafoSugerido.text = "Trafo Sugerido: --"
            }
        }
    }

    private fun definirNomeCategoria(cv: Double): String {
        return when {
            cv <= 0.0 -> "N/A"
            cv <= 2.5 -> "3/4 a 2,5 CV"
            cv <= 15.0 -> "3 a 15 CV"
            cv <= 40.0 -> "20 a 40 CV"
            else -> "Acima de 40 CV"
        }
    }
}