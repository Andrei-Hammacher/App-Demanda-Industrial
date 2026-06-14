package com.example.projetodemandaeletricaindustrial

import android.os.Bundle
import android.view.View
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
    private var equipamentoEditando: Equipamento? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEquipmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repository = CalculadoraRepository(this)
        projetoIdVinculado = intent.getIntExtra("PROJETO_ID", -1)
        equipamentoEditando = intent.getSerializableExtra("EQUIPAMENTO") as? Equipamento

        val modoEdicao = equipamentoEditando != null
        title = if (modoEdicao) "Editar Equipamento" else "Adicionar Equipamento"
        binding.btnSalvarEquipamento.text = if (modoEdicao) "Salvar Alterações" else "Salvar Equipamento"

        configurarSpinners()
        if (modoEdicao) preencherCampos(equipamentoEditando!!)

        binding.spinnerTipoEquipamento.setOnItemClickListener { _, _, _, _ ->
            atualizarCamposParaTipo(binding.spinnerTipoEquipamento.text.toString())
        }
        binding.btnSalvarEquipamento.setOnClickListener { salvarNoBanco() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun configurarSpinners() {
        val tipos = listOf(
            "Motor",
            "Iluminação",
            "Iluminação com Reator",
            "Tomada (Área Administrativa)",
            "Tomada (Área SE)",                // ← Planilha1: Área Subestação, FD=1.0
            "Tomada (Área Industrial Mono)",
            "Tomada (Área Industrial Tri)",
            "Forno Resistivo",                 // ← Planilha2/4: FU=1.0, FC=1.0
            "Forno de Indução",                // ← Planilha2/4: FU=1.0, FC=1.0 (tratado igual ao resistivo)
            "Retificador",
            "Ar Condicionado",
            "Máquina de Solda",
            "Outros"
        )
        binding.spinnerTipoEquipamento.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tipos)
        )

        // CCM1–CCM5 para motores (Planilha: CCM1–CCM5)
        val opcoesCcm = listOf("CCM1", "CCM2", "CCM3", "CCM4", "CCM5")
        binding.spinnerCcm.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, opcoesCcm)
        )
        binding.spinnerCcm.setText("CCM1", false)

        // QDF1–QDF5 para equipamentos não-motores (Planilha: QDF1–QDF5)
        val opcoesQdf = listOf("QDF1", "QDF2", "QDF3", "QDF4", "QDF5")
        binding.spinnerQdf.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, opcoesQdf)
        )
        binding.spinnerQdf.setText("QDF1", false)
    }

    private fun preencherCampos(eq: Equipamento) {
        binding.spinnerTipoEquipamento.setText(eq.tipo, false)
        binding.etDescricao.setText(eq.descricao)
        binding.etQuantidade.setText(eq.quantidade.toString())
        binding.etPotencia.setText(eq.potenciaOriginal.toString())

        if (eq.tipo.equals("Motor", ignoreCase = true) && eq.ccm.isNotBlank())
            binding.spinnerCcm.setText(eq.ccm, false)

        if (!eq.tipo.equals("Motor", ignoreCase = true) && eq.qdf.isNotBlank())
            binding.spinnerQdf.setText(eq.qdf, false)

        // --- NOVO: LER OS DADOS CUSTOMIZADOS DO BANCO ---
        // Se o motor tem um FP salvo no banco, escreve na caixinha
        if (eq.fpCustomizado != null) {
            binding.etFatorPotencia.setText(eq.fpCustomizado.toString())
        }
        // Se o motor tem um Rendimento salvo no banco, escreve na caixinha
        if (eq.rendimentoCustomizado != null) {
            binding.etRendimento.setText(eq.rendimentoCustomizado.toString())
        }

        // Recupera FP do reator: fp = potenciaOriginal / (potencia × 1000)
        if (eq.tipo.equals("Iluminação com Reator", ignoreCase = true) && eq.potencia > 0 && eq.fpCustomizado == null)
            binding.etFatorPotencia.setText("%.4f".format(eq.potenciaOriginal / (eq.potencia * 1000.0)))

        atualizarCamposParaTipo(eq.tipo)
    }

    private fun atualizarCamposParaTipo(tipo: String) {
        val ehMotor  = tipo.equals("Motor", ignoreCase = true)
        val ehReator = tipo.equals("Iluminação com Reator", ignoreCase = true)

        // CCM apenas para motores; QDF para todos os demais
        binding.tilCcm.visibility = if (ehMotor) View.VISIBLE else View.GONE
        binding.tilQdf.visibility = if (!ehMotor) View.VISIBLE else View.GONE

        // --- É AQUI QUE A MÁGICA ACONTECE ---
        // Mostra a linha com os novos campos se for Motor ou Reator
        binding.llDadosAvancados.visibility = if (ehReator || ehMotor) View.VISIBLE else View.GONE

        // O rendimento só faz sentido para Motores, então escondemos do Reator
        binding.tilRendimento.visibility = if (ehMotor) View.VISIBLE else View.GONE

        // Atualiza as dicas (hints) dinamicamente
        binding.tilPotencia.hint = when {
            ehMotor  -> "Potência (CV)"
            ehReator -> "Potência do Reator (W)"
            else     -> "Potência (W)"
        }

        binding.tilQuantidade.hint = if (ehReator) "Quantidade de Lâmpadas" else "Quantidade"

        // Se for motor avisa que é opcional, se for reator é obrigatório
        binding.tilFatorPotencia.hint = if (ehMotor) "FP (opcional)" else "FP do Reator"
    }

    private fun calcularFatorUtilizacao(cv: Double): Double {
        return when {
            cv <= 2.5  -> 0.70
            cv <= 15.0 -> 0.83
            cv <= 40.0 -> 0.85
            else       -> 0.87
        }
    }

    private fun salvarNoBanco() {
        val tipo             = binding.spinnerTipoEquipamento.text.toString()
        val descricao        = binding.etDescricao.text.toString()
        val quantidade       = binding.etQuantidade.text.toString().toIntOrNull() ?: 0
        val potenciaDigitada = binding.etPotencia.text.toString().toDoubleOrNull() ?: 0.0
        val ccm              = if (tipo.equals("Motor", ignoreCase = true))
            binding.spinnerCcm.text.toString().trim() else ""
        val qdf              = if (!tipo.equals("Motor", ignoreCase = true))
            binding.spinnerQdf.text.toString().trim() else ""
        val fpDigitado = binding.etFatorPotencia.text.toString().toDoubleOrNull()
        val rendimentoDigitado = binding.etRendimento.text.toString().toDoubleOrNull()

        if (descricao.isEmpty() || potenciaDigitada <= 0.0 || projetoIdVinculado == -1) {
            Toast.makeText(this, "Erro: Preencha todos os campos corretamente", Toast.LENGTH_SHORT).show()
            return
        }
        if (tipo.equals("Motor", ignoreCase = true) && ccm.isEmpty()) {
            Toast.makeText(this, "Selecione o painel de origem (CCM) do motor", Toast.LENGTH_SHORT).show()
            return
        }

        var demandaCalculadaUnitaria = 0.0

        when {
            // ── MOTOR ─────────────────────────────────────────────────────────
            tipo.equals("Motor", ignoreCase = true) -> {
                val dadosPadrao = repository.buscarFatoresPorCV(potenciaDigitada)
                val fu = calcularFatorUtilizacao(potenciaDigitada)
                val potenciaWats = potenciaDigitada * 735.5

                val rendimentoDigitado = binding.etRendimento.text.toString().toDoubleOrNull()

                val fpSeguranca = 0.83
                val rendimentoSeguranca = 0.85

                val fpFinal = fpDigitado ?: dadosPadrao?.fp ?: fpSeguranca
                val rendimentoFinal = rendimentoDigitado ?: dadosPadrao?.rendimento ?: rendimentoSeguranca

                demandaCalculadaUnitaria = (potenciaWats * fu) / (rendimentoFinal * fpFinal) / 1000.0
            }

            // ── ILUMINAÇÃO COM REATOR (Planilha1) ─────────────────────────────
            tipo.equals("Iluminação com Reator", ignoreCase = true) -> {
                val fpReator = binding.etFatorPotencia.text.toString().toDoubleOrNull()
                if (fpReator == null || fpReator <= 0.0 || fpReator > 1.0) {
                    Toast.makeText(this,
                        "Informe o Fator de Potência do Reator (entre 0 e 1, ex: 0.92)",
                        Toast.LENGTH_LONG).show()
                    return
                }
                demandaCalculadaUnitaria = (potenciaDigitada / fpReator) / 1000.0
            }

            // ── ILUMINAÇÃO SIMPLES e TOMADAS (inclui Área SE) ─────────────────
            tipo.contains("Iluminação", ignoreCase = true) ||
                    tipo.contains("Tomada", ignoreCase = true) -> {
                val fd = repository.buscarFatorGrupoA(tipo)
                demandaCalculadaUnitaria = (potenciaDigitada / 1000.0) * fd
            }

            // ── FORNO RESISTIVO e FORNO DE INDUÇÃO (Planilha2: FU=1.0) ────────
            // A simultaneidade FC=1.0 é aplicada no resumo via tabela Fornos resistivos
            tipo.contains("Forno", ignoreCase = true) -> {
                demandaCalculadaUnitaria = potenciaDigitada / 1000.0
            }

            // ── RETIFICADOR ───────────────────────────────────────────────────
            tipo.contains("Retificador", ignoreCase = true) -> {
                demandaCalculadaUnitaria = potenciaDigitada / 1000.0
            }

            // ── AR CONDICIONADO ───────────────────────────────────────────────
            tipo.contains("Ar Condicionado", ignoreCase = true) -> {
                demandaCalculadaUnitaria = potenciaDigitada / 1000.0
            }

            // ── MÁQUINA DE SOLDA (Planilha2: FU=1.0; Planilha4 controla FC) ──
            tipo.contains("Solda", ignoreCase = true) -> {
                demandaCalculadaUnitaria = potenciaDigitada / 1000.0
            }

            else -> demandaCalculadaUnitaria = potenciaDigitada / 1000.0
        }

        val equipamento = Equipamento(
            id               = equipamentoEditando?.id ?: 0,
            projetoId        = equipamentoEditando?.projetoId ?: projetoIdVinculado,
            tipo             = tipo,
            descricao        = descricao,
            quantidade       = quantidade,
            potencia         = demandaCalculadaUnitaria,
            potenciaOriginal = potenciaDigitada,
            ccm              = ccm,
            qdf              = qdf,
            fpCustomizado    = fpDigitado,
            rendimentoCustomizado = rendimentoDigitado

        )

        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(this@AddEquipmentActivity).equipamentoDao()
            if (equipamentoEditando != null) dao.atualizar(equipamento)
            else dao.inserir(equipamento)

            runOnUiThread {
                val acao = if (equipamentoEditando != null) "Atualizado" else "Salvo"
                val msg = when {
                    tipo.equals("Motor", ignoreCase = true) -> {
                        val fu = calcularFatorUtilizacao(potenciaDigitada)
                        "$acao! [$ccm] Demanda/un: ${"%.3f".format(demandaCalculadaUnitaria)} kVA (FU: $fu)"
                    }
                    tipo.equals("Iluminação com Reator", ignoreCase = true) -> {
                        "$acao! $quantidade lâmpadas × ${"%.4f".format(demandaCalculadaUnitaria)} kVA/un = ${"%.3f".format(demandaCalculadaUnitaria * quantidade)} kVA total"
                    }
                    else -> "Equipamento ${"${acao.lowercase()}".replaceFirstChar { it.uppercase() }} com sucesso!"
                }
                Toast.makeText(this@AddEquipmentActivity, msg, Toast.LENGTH_LONG).show()
                // Sinaliza sucesso para ListaCargasActivity mostrar Snackbar
                setResult(RESULT_OK)
                finish()
            }
        }
    }
}