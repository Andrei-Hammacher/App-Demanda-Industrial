package com.example.projetodemandaeletricaindustrial

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projetodemandaeletricaindustrial.databinding.ActivityListaCargasBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListaCargasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListaCargasBinding
    private lateinit var adapter: EquipamentoAdapter
    private val listaEquipamentos = mutableListOf<Equipamento>()

    private var projetoId: Int = -1
    private var projetoNome: String? = null

    // Último RelatorioData calculado — atualizado a cada carregarDadosDoBanco()
    private var ultimoRelatorio: RelatorioData? = null

    private val editarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            carregarDadosDoBanco()
            Snackbar.make(binding.root, "Equipamento atualizado com sucesso", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListaCargasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        projetoId = intent.getIntExtra("PROJETO_ID", -1)
        projetoNome = intent.getStringExtra("PROJETO_NOME")
        title = projetoNome ?: "Detalhes do Projeto"

        adapter = EquipamentoAdapter(
            lista    = listaEquipamentos,
            onEdit   = { posicao -> abrirEdicao(posicao) },
            onDelete = { posicao -> mostrarDialogoExclusao(posicao) }
        )

        binding.recyclerCargas.layoutManager = LinearLayoutManager(this)
        binding.recyclerCargas.adapter = adapter

        binding.btnAdicionarCarga.setOnClickListener {
            startActivity(
                Intent(this, AddEquipmentActivity::class.java)
                    .putExtra("PROJETO_ID", projetoId)
            )
        }

        binding.btnExportarPdf.setOnClickListener {
            exportarPdf()
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
            val db = AppDatabase.getDatabase(this@ListaCargasActivity)
            val equipamentos = db.equipamentoDao().listarPorProjeto(projetoId).first()
            val maiorMotor   = db.equipamentoDao().buscarMaiorMotor(projetoId)

            listaEquipamentos.clear()
            listaEquipamentos.addAll(equipamentos)
            adapter.notifyDataSetChanged()
            atualizarResumo(maiorMotor)
        }
    }

    private fun abrirEdicao(posicao: Int) {
        val intent = Intent(this, AddEquipmentActivity::class.java).apply {
            putExtra("PROJETO_ID", projetoId)
            putExtra("EQUIPAMENTO", listaEquipamentos[posicao])
        }
        editarLauncher.launch(intent)
    }

    private fun mostrarDialogoExclusao(posicao: Int) {
        val eq = listaEquipamentos[posicao]
        AlertDialog.Builder(this)
            .setTitle("Remover Equipamento")
            .setMessage("Deseja excluir permanentemente '${eq.descricao}'?")
            .setPositiveButton("Excluir") { _, _ ->
                lifecycleScope.launch {
                    AppDatabase.getDatabase(this@ListaCargasActivity)
                        .equipamentoDao().deletar(eq)
                    carregarDadosDoBanco()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ══════════════════════════════════════════════════════════════════════
    // EXPORTAR PDF
    // ══════════════════════════════════════════════════════════════════════

    private fun exportarPdf() {
        val dados = ultimoRelatorio
        if (dados == null || dados.demandaTotal <= 0.0) {
            Snackbar.make(binding.root,
                "Adicione equipamentos antes de exportar o relatório.",
                Snackbar.LENGTH_SHORT).show()
            return
        }

        val snack = Snackbar.make(binding.root, "Gerando PDF...", Snackbar.LENGTH_INDEFINITE)
        snack.show()

        lifecycleScope.launch {
            try {
                val arquivo = withContext(Dispatchers.IO) {
                    RelatorioGenerator.gerar(this@ListaCargasActivity, dados)
                }

                snack.dismiss()

                val uri = FileProvider.getUriForFile(
                    this@ListaCargasActivity,
                    "${packageName}.fileprovider",
                    arquivo
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Relatório — ${dados.projetoNome}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Compartilhar Relatório PDF"))

            } catch (e: Exception) {
                snack.dismiss()
                Snackbar.make(binding.root, "Erro ao gerar PDF: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // RESUMO + MONTAGEM DO RelatorioData
    // ══════════════════════════════════════════════════════════════════════

    private fun atualizarResumo(maiorMotor: Equipamento?) {
        val repo = CalculadoraRepository(this)

        // ── GRUPO A ────────────────────────────────────────────────────
        val grupoA = listaEquipamentos.filter {
            it.tipo.contains("Iluminação", ignoreCase = true) ||
                    it.tipo.contains("Tomada", ignoreCase = true)
        }.sumOf { it.potencia * it.quantidade }

        val demAdm     = listaEquipamentos.filter { it.tipo.equals("Tomada (Área Administrativa)", ignoreCase = true) }.sumOf { it.potencia * it.quantidade }
        val demSe      = listaEquipamentos.filter { it.tipo.equals("Tomada (Área SE)", ignoreCase = true) }.sumOf { it.potencia * it.quantidade }
        val demIndMono = listaEquipamentos.filter { it.tipo.equals("Tomada (Área Industrial Mono)", ignoreCase = true) }.sumOf { it.potencia * it.quantidade }
        val demIndTri  = listaEquipamentos.filter { it.tipo.equals("Tomada (Área Industrial Tri)", ignoreCase = true) }.sumOf { it.potencia * it.quantidade }
        val demIlum    = listaEquipamentos.filter { it.tipo.contains("Iluminação", ignoreCase = true) }.sumOf { it.potencia * it.quantidade }

        // ── GRUPO B ────────────────────────────────────────────────────
        val grupoB = listaEquipamentos.filter {
            it.tipo.contains("Forno", ignoreCase = true)
        }.sumOf { it.potencia * it.quantidade }

        // ── GRUPO C ────────────────────────────────────────────────────
        val todosMotores = listaEquipamentos.filter { it.tipo.equals("Motor", ignoreCase = true) }
        val demandaGrupoC = calcularDemandaMotoresPorFaixa(todosMotores, repo)

        // ── RETIFICADORES ──────────────────────────────────────────────
        val retificadores = listaEquipamentos.filter { it.tipo.contains("Retificador", ignoreCase = true) }
        val qtdRetif = retificadores.sumOf { it.quantidade }
        val fatorRetif = if (qtdRetif > 0) repo.buscarFatorPorCategoria(qtdRetif, "Retificadores") else 1.0
        val demandaRetificadores = retificadores.sumOf { it.potencia * it.quantidade } * fatorRetif

        // ── SOLDADORES ─────────────────────────────────────────────────
        val soldadores = listaEquipamentos.filter { it.tipo.contains("Solda", ignoreCase = true) }
        val qtdSolda = soldadores.sumOf { it.quantidade }
        val fatorSolda = if (qtdSolda > 0) repo.buscarFatorPorCategoria(qtdSolda, "Soldadores") else 1.0
        val demandaSolda = soldadores.sumOf { it.potencia * it.quantidade } * fatorSolda

        // ── ESPECIAIS ──────────────────────────────────────────────────
        val outrasEspeciais = listaEquipamentos.filter {
            it.tipo.contains("Ar Condicionado", ignoreCase = true) ||
                    it.tipo.contains("Outros", ignoreCase = true)
        }.sumOf { it.potencia * it.quantidade }

        val cargasEspeciais = demandaSolda + outrasEspeciais
        val demandaTotal = grupoA + grupoB + demandaGrupoC + demandaRetificadores + cargasEspeciais

        // ── FP GLOBAL ──────────────────────────────────────────────────
        val potAtivaTotal = grupoA * 1.0 + grupoB * 1.0 +
                demandaGrupoC * 0.85 + demandaRetificadores * 0.85 + cargasEspeciais * 0.85
        val fpGlobal = if (demandaTotal > 0) potAtivaTotal / demandaTotal else 1.0

        // ── RESERVA E TRAFO ────────────────────────────────────────────
        val reservaCarga = demandaTotal * 0.10
        val demandaComReserva = demandaTotal + reservaCarga
        val trafos = listOf(15.0, 30.0, 45.0, 75.0, 112.5, 150.0, 225.0, 300.0, 500.0, 750.0, 1000.0)
        val textoTrafo = trafos.find { it >= demandaComReserva }
            ?.let { "$it kVA" } ?: "Sob Consulta (>1000 kVA)"

        // ── PAINÉIS ────────────────────────────────────────────────────
        val textoSim = buildString {
            append(gerarTextoSimultaneidade(todosMotores, repo))
            if (maiorMotor != null) append("\nCrítico partida: ${maiorMotor.descricao}")
        }
        val textoCcm = gerarTextoPorPainel(todosMotores, { it.ccm },
            { lista -> calcularDemandaMotoresPorFaixa(lista, repo) }, "Nenhum motor cadastrado")
        val textoQdf = gerarTextoPorPainel(
            listaEquipamentos.filter { !it.tipo.equals("Motor", ignoreCase = true) },
            { it.qdf }, { lista -> lista.sumOf { it.potencia * it.quantidade } },
            "Nenhum equipamento cadastrado")

        // ── MONTAR RelatorioData ───────────────────────────────────────
        ultimoRelatorio = RelatorioData(
            projetoNome         = projetoNome ?: "Projeto",
            dataGeracao         = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
            grupoA              = grupoA,
            grupoB              = grupoB,
            demandaGrupoC       = demandaGrupoC,
            demandaRetificadores = demandaRetificadores,
            demandaSolda        = demandaSolda,
            outrasEspeciais     = outrasEspeciais,
            demandaTotal        = demandaTotal,
            demAdm              = demAdm,
            demSe               = demSe,
            demIndMono          = demIndMono,
            demIndTri           = demIndTri,
            demIlum             = demIlum,
            fpGlobal            = fpGlobal,
            reservaCarga        = reservaCarga,
            demandaComReserva   = demandaComReserva,
            trafoSugerido       = textoTrafo,
            textoSimultaneidade = textoSim,
            maiorMotorDescricao = maiorMotor?.descricao,
            textoCcm            = textoCcm,
            textoQdf            = textoQdf,
            equipamentos        = listaEquipamentos.toList()
        )

        // ── ATUALIZAR UI ───────────────────────────────────────────────
        binding.apply {
            tvResumoGrupoA.text    = "Grupo A (Ilum/Tom): ${"%.2f".format(grupoA)} kVA"
            tvResumoGrupoB.text    = "Grupo B (Fornos): ${"%.2f".format(grupoB)} kVA"
            tvResumoGrupoC.text    = "Grupo C (Motores): ${"%.2f".format(demandaGrupoC)} kVA"
            tvResumoEspeciais.text =
                "Retif: ${"%.2f".format(demandaRetificadores)} kVA | " +
                        "Solda: ${"%.2f".format(demandaSolda)} kVA | " +
                        "Outros: ${"%.2f".format(outrasEspeciais)} kVA"
            tvTotalKva.text            = "${"%.2f".format(demandaTotal)} kVA"
            tvFatorSimultaneidade.text = textoSim
            tvFpGlobal.text            = "FP Global: ${"%.2f".format(fpGlobal)}"

            when {
                fpGlobal < 0.92 && demandaTotal > 0 -> {
                    tvAlertaCapacitor.text = "⚠ FP < 0.92 — Banco de capacitores necessário"
                    tvAlertaCapacitor.setTextColor(android.graphics.Color.rgb(255, 180, 180))
                }
                demandaTotal > 0 -> {
                    tvAlertaCapacitor.text = "✓ FP dentro da norma (≥ 0.92)"
                    tvAlertaCapacitor.setTextColor(android.graphics.Color.rgb(180, 255, 180))
                }
                else -> tvAlertaCapacitor.text = ""
            }

            if (demandaTotal > 0) {
                tvReservaCarga.text  = "Reserva (10%): ${"%.2f".format(reservaCarga)} kVA"
                tvDemandaFinal.text  = "Demanda c/ Reserva: ${"%.2f".format(demandaComReserva)} kVA"
                tvTrafoSugerido.text = textoTrafo
            } else {
                tvReservaCarga.text  = "Reserva (10%): 0.00 kVA"
                tvDemandaFinal.text  = "Demanda c/ Reserva: 0.00 kVA"
                tvTrafoSugerido.text = "--"
            }

            tvTomadaAdm.text       = "T1Φ_ADM (Área Admin): ${"%.2f".format(demAdm)} kVA"
            tvTomadaSe.text        = "T1Φ_SE (Subestação): ${"%.2f".format(demSe)} kVA"
            tvTomadaIndMono.text   = "T1Φ_IND (Ind. Mono): ${"%.2f".format(demIndMono)} kVA"
            tvTomadaIndTri.text    = "T3Φ_IND (Ind. Tri): ${"%.2f".format(demIndTri)} kVA"
            tvIluminacaoTotal.text = "Iluminação: ${"%.2f".format(demIlum)} kVA"
            tvResumoCcm.text       = textoCcm
            tvResumoQdf.text       = textoQdf
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // FUNÇÕES DE CÁLCULO
    // ══════════════════════════════════════════════════════════════════════

    private fun gerarTextoPorPainel(
        equipamentos: List<Equipamento>,
        seletor: (Equipamento) -> String,
        calculador: (List<Equipamento>) -> Double,
        vazio: String
    ): String {
        if (equipamentos.isEmpty()) return vazio
        val porPainel = equipamentos.filter { seletor(it).isNotBlank() }
            .groupBy { seletor(it) }.toSortedMap()
        if (porPainel.isEmpty()) return "Sem painel definido"
        return porPainel.entries.joinToString("\n") { (painel, lista) ->
            "$painel: ${"%.2f".format(calculador(lista))} kVA"
        }
    }

    private fun calcularDemandaMotoresPorFaixa(
        motores: List<Equipamento>,
        repo: CalculadoraRepository
    ): Double {
        if (motores.isEmpty()) return 0.0
        val faixas = listOf(
            "Motores: 3/4 a 2,5 CV" to { cv: Double -> cv <= 2.5 },
            "Motores: 3 a 15 CV"    to { cv: Double -> cv > 2.5 && cv <= 15.0 },
            "Motores: 20 a 40 CV"   to { cv: Double -> cv > 15.0 && cv <= 40.0 },
            "Acima de 40 CV"        to { cv: Double -> cv > 40.0 }
        )
        return faixas.sumOf { (cat, filtro) ->
            val grupo = motores.filter { filtro(it.potenciaOriginal) }
            if (grupo.isEmpty()) return@sumOf 0.0
            grupo.sumOf { it.potencia * it.quantidade } *
                    repo.buscarFatorPorCategoria(grupo.sumOf { it.quantidade }, cat)
        }
    }

    private fun gerarTextoSimultaneidade(
        motores: List<Equipamento>,
        repo: CalculadoraRepository
    ): String {
        if (motores.isEmpty()) return "Fator C: N/A"
        val faixas = listOf(
            "Motores: 3/4 a 2,5 CV" to { cv: Double -> cv <= 2.5 },
            "Motores: 3 a 15 CV"    to { cv: Double -> cv > 2.5 && cv <= 15.0 },
            "Motores: 20 a 40 CV"   to { cv: Double -> cv > 15.0 && cv <= 40.0 },
            "Acima de 40 CV"        to { cv: Double -> cv > 40.0 }
        )
        return faixas.mapNotNull { (cat, filtro) ->
            val grupo = motores.filter { filtro(it.potenciaOriginal) }
            if (grupo.isEmpty()) return@mapNotNull null
            val qtd = grupo.sumOf { it.quantidade }
            "$cat: FC=${"%.2f".format(repo.buscarFatorPorCategoria(qtd, cat))} ($qtd un.)"
        }.joinToString("\n")
    }
}