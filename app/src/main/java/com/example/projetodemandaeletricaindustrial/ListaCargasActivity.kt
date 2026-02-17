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

        val demandaTotal = grupoA + grupoB + demandaGrupoC

        binding.apply {
            tvResumoGrupoA.text = "Grupo A (Ilum/Tom): ${"%.2f".format(grupoA)} kVA"
            tvResumoGrupoB.text = "Grupo B (Aquecim): ${"%.2f".format(grupoB)} kVA"
            tvResumoGrupoC.text = "Grupo C (Motores): ${"%.2f".format(demandaGrupoC)} kVA"
            tvTotalKva.text = "Demanda Total: ${"%.2f".format(demandaTotal)} kVA"

            val categoria = definirNomeCategoria(maiorPotenciaCv)
            tvFatorSimultaneidade.text = "Fator C: ${"%.2f".format(fatorC)} (Cat: $categoria)"
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