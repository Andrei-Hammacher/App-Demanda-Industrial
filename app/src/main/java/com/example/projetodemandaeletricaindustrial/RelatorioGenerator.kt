package com.example.projetodemandaeletricaindustrial

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RelatorioGenerator {

    // ── Layout A4 ─────────────────────────────────────────────────────────
    private const val PW = 595
    private const val PH = 842
    private const val ML = 36f
    private const val MR = 559f
    private const val CONTENT_W = 523f
    private const val MB = 808f

    // ── Paleta principal ──────────────────────────────────────────────────
    private val COR_PRIMARY     = Color.rgb(13, 71, 161)
    private val COR_PRIMARY_DK  = Color.rgb(10, 47, 110)
    private val COR_BG_SECTION  = Color.rgb(227, 242, 253)
    private val COR_BG_TOTAL    = Color.rgb(13, 71, 161)
    private val COR_WARNING     = Color.rgb(198, 40, 40)
    private val COR_TEXT        = Color.rgb(13, 27, 42)
    private val COR_TEXT_SEC    = Color.rgb(84, 110, 122)
    private val COR_DIVIDER     = Color.rgb(224, 232, 240)
    private val COR_TABLE_HEAD  = Color.rgb(227, 242, 253)
    private val COR_ROW_ALT     = Color.rgb(248, 250, 252)

    // ── Cores por grupo (mesmas do app) ───────────────────────────────────
    private val COR_GRUPO_A   = Color.rgb(21, 101, 192)   // azul — iluminação/tomadas
    private val COR_GRUPO_B   = Color.rgb(230, 81, 0)     // laranja — fornos
    private val COR_GRUPO_C   = Color.rgb(46, 125, 50)    // verde — motores
    private val COR_RETIF     = Color.rgb(106, 27, 154)   // roxo — retificadores
    private val COR_SOLDA     = Color.rgb(198, 40, 40)    // vermelho — solda/especiais
    private val COR_OUTROS    = Color.rgb(69, 90, 100)    // cinza — outros

    // ── Paints ────────────────────────────────────────────────────────────
    private fun pText(size: Float, color: Int = COR_TEXT, bold: Boolean = false) = Paint().apply {
        textSize = size; this.color = color
        typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD
        else android.graphics.Typeface.DEFAULT
        isAntiAlias = true
    }
    private fun pFill(color: Int) = Paint().apply {
        this.color = color; style = Paint.Style.FILL; isAntiAlias = true
    }
    private fun pStroke(color: Int, width: Float = 1f) = Paint().apply {
        this.color = color; style = Paint.Style.STROKE; strokeWidth = width; isAntiAlias = true
    }

    // ── Cor de grupo por nome ─────────────────────────────────────────────
    private fun corDoGrupo(nomeGrupo: String): Int = when {
        nomeGrupo.contains("Motor",        ignoreCase = true) -> COR_GRUPO_C
        nomeGrupo.contains("Iluminac",     ignoreCase = true) -> COR_GRUPO_A
        nomeGrupo.contains("Tomada",       ignoreCase = true) -> COR_GRUPO_A
        nomeGrupo.contains("Grupo A",      ignoreCase = true) -> COR_GRUPO_A
        nomeGrupo.contains("Grupo B",      ignoreCase = true) -> COR_GRUPO_B
        nomeGrupo.contains("Grupo C",      ignoreCase = true) -> COR_GRUPO_C
        nomeGrupo.contains("Forno",        ignoreCase = true) -> COR_GRUPO_B
        nomeGrupo.contains("Retificad",    ignoreCase = true) -> COR_RETIF
        nomeGrupo.contains("Solda",        ignoreCase = true) -> COR_SOLDA
        nomeGrupo.contains("Ar Condic",    ignoreCase = true) -> COR_SOLDA
        else                                                   -> COR_OUTROS
    }

    // ── Ponto de entrada ──────────────────────────────────────────────────
    fun gerar(context: Context, dados: RelatorioData): File {
        val doc = PdfDocument()
        val s = DrawState(doc, dados)

        s.novaPage()
        desenharCabecalho(s, dados)
        desenharResumoExecutivo(s, dados)
        desenharTabelaGrupos(s, dados)
        desenharDetalhamentoTomadas(s, dados)
        desenharPaineis(s, dados)
        desenharListaEquipamentos(s, dados)
        s.fecharPage()

        val nome = "Relatorio_${dados.projetoNome.replace(" ", "_")}_${
            SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        }.pdf"
        val arquivo = File(context.getExternalFilesDir(null), nome)
        arquivo.outputStream().use { doc.writeTo(it) }
        doc.close()
        return arquivo
    }

    // ══════════════════════════════════════════════════════════════════════
    // SEÇÕES
    // ══════════════════════════════════════════════════════════════════════

    private fun desenharCabecalho(s: DrawState, d: RelatorioData) {
        val cv = s.cv

        // Barra principal + faixa decorativa inferior
        cv.drawRect(0f, 0f, PW.toFloat(), 68f, pFill(COR_PRIMARY_DK))
        cv.drawRect(0f, 68f, PW.toFloat(), 72f, pFill(COR_PRIMARY))

        // Bloco colorido de acento no lado esquerdo
        cv.drawRect(0f, 0f, 6f, 72f, pFill(Color.rgb(0, 188, 212)))

        cv.drawText("DemandaCalc", ML + 4f, 30f, pText(20f, Color.WHITE, bold = true))
        cv.drawText("Relatorio de Demanda Eletrica Industrial",
            ML + 4f, 52f, pText(11f, Color.rgb(179, 212, 245)))
        cv.drawText(d.dataGeracao, MR - 115f, 42f, pText(9f, Color.rgb(179, 212, 245)))

        s.y = 86f
        cv.drawText("PROJETO", ML, s.y, pText(8f, COR_TEXT_SEC))
        s.y += 14f
        cv.drawText(d.projetoNome, ML, s.y, pText(17f, COR_TEXT, bold = true))
        s.y += 16f
        cv.drawLine(ML, s.y, MR, s.y, pFill(COR_DIVIDER))
        s.y += 16f
    }

    private fun desenharResumoExecutivo(s: DrawState, d: RelatorioData) {
        val cv = s.cv

        secTitulo(cv, "RESUMO EXECUTIVO", s.y)
        s.y += 22f

        // ── Card principal de demanda total ────────────────────────────
        val cardH = 80f
        cv.drawRoundRect(RectF(ML, s.y, MR, s.y + cardH), 12f, 12f, pFill(COR_BG_TOTAL))

        // Faixa lateral ciano de acento
        cv.drawRoundRect(RectF(ML, s.y, ML + 6f, s.y + cardH), 12f, 12f,
            pFill(Color.rgb(0, 188, 212)))

        // Divisor vertical interno entre demanda e FP
        cv.drawLine(MR - 170f, s.y + 14f, MR - 170f, s.y + cardH - 14f,
            pFill(Color.argb(80, 255, 255, 255)))

        cv.drawText("DEMANDA TOTAL", ML + 16f, s.y + 22f,
            pText(8f, Color.rgb(179, 212, 245)))
        cv.drawText("${"%.2f".format(d.demandaTotal)} kVA",
            ML + 16f, s.y + 56f, pText(28f, Color.WHITE, bold = true))

        // FP Global com barra de indicação
        val fpOk = d.fpGlobal >= 0.92 || d.demandaTotal == 0.0
        val fpCor = if (fpOk) Color.rgb(165, 214, 167) else Color.rgb(239, 154, 154)
        cv.drawText("FATOR DE POTENCIA GLOBAL", MR - 162f, s.y + 22f,
            pText(7f, Color.rgb(179, 212, 245)))
        cv.drawText("${"%.3f".format(d.fpGlobal)}", MR - 162f, s.y + 52f,
            pText(22f, Color.WHITE, bold = true))

        // Barra de FP (0 → 1.0, marcador em 0.92)
        val barX = MR - 162f
        val barY = s.y + 60f
        val barW = 140f
        val barH2 = 5f
        cv.drawRoundRect(RectF(barX, barY, barX + barW, barY + barH2),
            3f, 3f, pFill(Color.argb(60, 255, 255, 255)))
        val barFill = (d.fpGlobal.coerceIn(0.0, 1.0) * barW).toFloat()
        cv.drawRoundRect(RectF(barX, barY, barX + barFill, barY + barH2),
            3f, 3f, pFill(fpCor))
        // Marcador 0.92
        val marcador92 = barX + (0.92f * barW)
        cv.drawLine(marcador92, barY - 2f, marcador92, barY + barH2 + 2f,
            Paint().apply { color = Color.WHITE; strokeWidth = 1.5f })

        val alertaFp = if (!fpOk) "ATENCAO: Banco de capacitores necessario"
        else "FP dentro da norma (>= 0.92)"
        cv.drawText(alertaFp, MR - 162f, s.y + 76f, pText(7f, fpCor))

        s.y += cardH + 12f

        // ── Três boxes de dimensionamento ──────────────────────────────
        val colW = CONTENT_W / 3f
        miniInfoBox(cv, "Reserva de Crescimento (10%)",
            "${"%.2f".format(d.reservaCarga)} kVA",
            ML, s.y, colW - 6f, borderColor = COR_DIVIDER)
        miniInfoBox(cv, "Demanda com Reserva",
            "${"%.2f".format(d.demandaComReserva)} kVA",
            ML + colW, s.y, colW - 6f, borderColor = COR_DIVIDER)
        // Trafo sugerido com destaque especial
        miniInfoBoxTrafo(cv, d.trafoSugerido, ML + colW * 2f, s.y, colW - 2f)
        s.y += 52f

        cv.drawLine(ML, s.y, MR, s.y, pFill(COR_DIVIDER))
        s.y += 14f

        // Motor crítico
        if (!d.maiorMotorDescricao.isNullOrBlank()) {
            // Fundo sutil para o alerta
            cv.drawRoundRect(RectF(ML, s.y - 11f, MR, s.y + 7f),
                4f, 4f, pFill(Color.rgb(255, 235, 238)))
            cv.drawRect(ML, s.y - 11f, ML + 3f, s.y + 7f, pFill(COR_WARNING))
            cv.drawText("ATENCAO - Critico para partida: ${d.maiorMotorDescricao}",
                ML + 8f, s.y, pText(9f, COR_WARNING, bold = true))
            s.y += 20f
        }

        // Simultaneidade
        val simLinhas = d.textoSimultaneidade.split("\n")
            .filter { it.isNotBlank() && it != "Fator C: N/A" && !it.contains("Critico") }
        if (simLinhas.isNotEmpty()) {
            cv.drawText("Fatores de Simultaneidade aplicados:",
                ML, s.y, pText(8f, COR_TEXT_SEC))
            s.y += 13f
            simLinhas.forEach { linha ->
                cv.drawText("  $linha", ML, s.y, pText(9f, COR_TEXT_SEC))
                s.y += 13f
            }
        }
        s.y += 8f
    }

    private fun desenharTabelaGrupos(s: DrawState, d: RelatorioData) {
        s.verificarEspaco(160f)
        secTitulo(s.cv, "DEMANDA POR GRUPO DE EQUIPAMENTOS", s.y)
        s.y += 22f

        data class GrupoLinha(val nome: String, val demanda: Double, val cor: Int)
        val total = d.demandaTotal

        val grupos = buildList {
            add(GrupoLinha("Grupo A - Iluminacao e Tomadas",           d.grupoA,               COR_GRUPO_A))
            add(GrupoLinha("Grupo B - Fornos Resistivos e de Inducao", d.grupoB,               COR_GRUPO_B))
            add(GrupoLinha("Grupo C - Motores Eletricos",              d.demandaGrupoC,        COR_GRUPO_C))
            if (d.demandaRetificadores > 0)
                add(GrupoLinha("Retificadores",                        d.demandaRetificadores, COR_RETIF))
            if (d.demandaSolda > 0)
                add(GrupoLinha("Maquinas de Solda",                    d.demandaSolda,         COR_SOLDA))
            if (d.outrasEspeciais > 0)
                add(GrupoLinha("Ar Condicionado / Outros",             d.outrasEspeciais,      COR_OUTROS))
        }

        val cv = s.cv
        val rowH = 26f
        val BAR_MAX_W = 140f  // largura máxima da barra de proporção
        // Colunas: [badge+nome | kVA | % | barra]
        val cNome  = ML + 14f
        val cKva   = ML + 240f
        val cPct   = ML + 330f
        val cBarra = ML + 375f

        // Cabeçalho
        cv.drawRect(ML, s.y - 13f, MR, s.y + rowH - 13f, pFill(COR_TABLE_HEAD))
        listOf(cNome to "Grupo / Tipo", cKva to "Demanda (kVA)",
            cPct to "% Total", cBarra to "Proporcao").forEach { (x, h) ->
            cv.drawText(h, x, s.y, pText(8f, COR_PRIMARY, bold = true))
        }
        s.y += rowH

        grupos.forEachIndexed { idx, g ->
            s.verificarEspaco(rowH + 2f)
            if (idx % 2 == 1)
                cv.drawRect(ML, s.y - 13f, MR, s.y + rowH - 13f, pFill(COR_ROW_ALT))

            // Badge colorido (quadrado 8×8)
            cv.drawRect(ML + 2f, s.y - 8f, ML + 10f, s.y, pFill(g.cor))

            cv.drawText(g.nome, cNome, s.y, pText(8f, COR_TEXT))

            val kvaStr = "${"%.2f".format(g.demanda)} kVA"
            cv.drawText(kvaStr, cKva, s.y, pText(9f, COR_TEXT, bold = true))

            val pct = if (total > 0) g.demanda / total else 0.0
            cv.drawText("${"%.1f".format(pct * 100)}%", cPct, s.y, pText(9f, COR_TEXT_SEC))

            // Barra proporcional colorida
            val barW = (pct * BAR_MAX_W).toFloat().coerceAtLeast(0f)
            val barY = s.y - 8f
            // Fundo da barra
            cv.drawRoundRect(RectF(cBarra, barY, cBarra + BAR_MAX_W, barY + 10f),
                4f, 4f, pFill(COR_DIVIDER))
            // Preenchimento
            if (barW > 0)
                cv.drawRoundRect(RectF(cBarra, barY, cBarra + barW, barY + 10f),
                    4f, 4f, pFill(g.cor))

            cv.drawLine(ML, s.y + rowH - 13f, MR, s.y + rowH - 13f,
                Paint().apply { color = COR_DIVIDER; strokeWidth = 0.5f })
            s.y += rowH
        }

        // Linha de total
        cv.drawRect(ML, s.y - 13f, MR, s.y + rowH - 13f, pFill(COR_BG_SECTION))
        cv.drawText("TOTAL GERAL", cNome, s.y, pText(9f, COR_PRIMARY, bold = true))
        cv.drawText("${"%.2f".format(total)} kVA", cKva, s.y,
            pText(10f, COR_PRIMARY, bold = true))
        cv.drawText("100%", cPct, s.y, pText(9f, COR_PRIMARY, bold = true))
        s.y += rowH + 14f
    }

    private fun desenharDetalhamentoTomadas(s: DrawState, d: RelatorioData) {
        if (d.grupoA <= 0.0) return
        s.verificarEspaco(120f)
        secTitulo(s.cv, "DETALHAMENTO - TOMADAS E ILUMINACAO", s.y)
        s.y += 22f

        val cols = listOf(ML, ML + 300f, MR)
        val linhas = buildList {
            if (d.demAdm > 0)     add(listOf("T1fi_ADM - Area Administrativa",       f2(d.demAdm)))
            if (d.demSe > 0)      add(listOf("T1fi_SE  - Subestacao",                f2(d.demSe)))
            if (d.demIndMono > 0) add(listOf("T1fi_IND - Industrial Monofasico",     f2(d.demIndMono)))
            if (d.demIndTri > 0)  add(listOf("T3fi_IND - Industrial Trifasico",      f2(d.demIndTri)))
            if (d.demIlum > 0)    add(listOf("Iluminacao (simples e com reator)",    f2(d.demIlum)))
        }
        if (linhas.isNotEmpty()) {
            desenharTabelaSimples(s, cols,
                headers = listOf("Tipo de Carga", "Demanda (kVA)", ""),
                linhas = linhas,
                rodape = listOf("Total Grupo A", f2(d.grupoA)))
        }
        s.y += 14f
    }

    private fun desenharPaineis(s: DrawState, d: RelatorioData) {
        val temCcm = d.textoCcm.isNotBlank() && !d.textoCcm.contains("Nenhum")
                && !d.textoCcm.contains("Sem")
        val temQdf = d.textoQdf.isNotBlank() && !d.textoQdf.contains("Nenhum")
                && !d.textoQdf.contains("Sem")
        if (!temCcm && !temQdf) return

        s.verificarEspaco(90f)
        secTitulo(s.cv, "DEMANDA POR PAINEL ELETRICO", s.y)
        s.y += 22f

        val cv = s.cv
        val colW = CONTENT_W / 2f - 8f

        // Dois painéis lado a lado (CCM esquerda, QDF direita)
        if (temCcm) {
            // Box CCM — borda verde (cor dos motores)
            cv.drawRoundRect(RectF(ML, s.y - 4f, ML + colW, s.y + 100f),
                8f, 8f, pFill(Color.rgb(232, 245, 233)))
            cv.drawRoundRect(RectF(ML, s.y - 4f, ML + colW, s.y + 100f),
                8f, 8f, pStroke(COR_GRUPO_C, 1.5f))
            cv.drawRect(ML, s.y - 4f, ML + 4f, s.y + 100f, pFill(COR_GRUPO_C))

            cv.drawText("Motores por CCM", ML + 10f, s.y + 10f,
                pText(9f, COR_GRUPO_C, bold = true))
            var lineY = s.y + 24f
            d.textoCcm.split("\n").filter { it.isNotBlank() }.forEach { linha ->
                val partes = linha.split(":")
                if (partes.size == 2) {
                    cv.drawText(partes[0].trim(), ML + 10f, lineY, pText(9f, COR_TEXT, bold = true))
                    cv.drawText(partes[1].trim(), ML + 10f, lineY + 12f, pText(10f, COR_GRUPO_C))
                    lineY += 26f
                }
            }
        }

        if (temQdf) {
            val xQdf = if (temCcm) ML + colW + 16f else ML
            // Box QDF — borda azul
            cv.drawRoundRect(RectF(xQdf, s.y - 4f, xQdf + colW, s.y + 100f),
                8f, 8f, pFill(Color.rgb(227, 242, 253)))
            cv.drawRoundRect(RectF(xQdf, s.y - 4f, xQdf + colW, s.y + 100f),
                8f, 8f, pStroke(COR_GRUPO_A, 1.5f))
            cv.drawRect(xQdf, s.y - 4f, xQdf + 4f, s.y + 100f, pFill(COR_GRUPO_A))

            cv.drawText("Equipamentos por QDF", xQdf + 10f, s.y + 10f,
                pText(9f, COR_GRUPO_A, bold = true))
            var lineY = s.y + 24f
            d.textoQdf.split("\n").filter { it.isNotBlank() }.forEach { linha ->
                val partes = linha.split(":")
                if (partes.size == 2) {
                    cv.drawText(partes[0].trim(), xQdf + 10f, lineY, pText(9f, COR_TEXT, bold = true))
                    cv.drawText(partes[1].trim(), xQdf + 10f, lineY + 12f, pText(10f, COR_GRUPO_A))
                    lineY += 26f
                }
            }
        }

        s.y += 112f
        s.cv.drawLine(ML, s.y, MR, s.y, pFill(COR_DIVIDER))
        s.y += 14f
    }

    private fun desenharListaEquipamentos(s: DrawState, d: RelatorioData) {
        if (d.equipamentos.isEmpty()) return
        s.verificarEspaco(80f)
        secTitulo(s.cv, "LISTA COMPLETA DE EQUIPAMENTOS (${d.equipamentos.size} itens)", s.y)
        s.y += 22f

        val cols = listOf(ML + 14f, ML + 170f, ML + 300f, ML + 345f, ML + 408f, ML + 473f)
        val cv = s.cv
        val rowH = 20f

        // Cabeçalho
        cv.drawRect(ML, s.y - 13f, MR, s.y + rowH - 13f, pFill(COR_TABLE_HEAD))
        listOf(cols[0] to "Descricao", cols[1] to "Tipo", cols[2] to "Qtd",
            cols[3] to "Pot.Orig.", cols[4] to "Dem.Unit.", cols[5] to "Dem.Total"
        ).forEach { (x, h) ->
            cv.drawText(h, x, s.y, pText(8f, COR_PRIMARY, bold = true))
        }
        s.y += rowH

        var totalKva = 0.0
        d.equipamentos.forEachIndexed { idx, eq ->
            s.verificarEspaco(rowH + 4f)

            val cor = corDoGrupo(eq.tipo)
            if (idx % 2 == 1)
                cv.drawRect(ML, s.y - 13f, MR, s.y + rowH - 13f, pFill(COR_ROW_ALT))

            // Stripe colorida à esquerda
            cv.drawRect(ML, s.y - 13f, ML + 4f, s.y + rowH - 13f, pFill(cor))

            val painel = when {
                eq.ccm.isNotBlank() -> " [${eq.ccm}]"
                eq.qdf.isNotBlank() -> " [${eq.qdf}]"
                else -> ""
            }
            val desc = (eq.descricao + painel).let {
                if (it.length > 22) it.take(21) + ".." else it
            }
            val tipo = eq.tipo.let {
                if (it.length > 18) it.take(17) + ".." else it
            }
            val potOrig = if (eq.tipo.equals("Motor", ignoreCase = true))
                "${"%.1f".format(eq.potenciaOriginal)} CV"
            else
                "${"%.0f".format(eq.potenciaOriginal)} W"

            val demTotal = eq.potencia * eq.quantidade
            totalKva += demTotal

            cv.drawText(desc,               cols[0], s.y, pText(8f, COR_TEXT))
            cv.drawText(tipo,               cols[1], s.y, pText(8f, COR_TEXT_SEC))
            cv.drawText("${eq.quantidade}", cols[2], s.y, pText(8f, COR_TEXT))
            cv.drawText(potOrig,            cols[3], s.y, pText(8f, COR_TEXT))
            cv.drawText("${"%.3f".format(eq.potencia)} kVA", cols[4], s.y, pText(8f, COR_TEXT))
            cv.drawText("${"%.3f".format(demTotal)} kVA",    cols[5], s.y, pText(8f, COR_TEXT, bold = true))

            cv.drawLine(ML, s.y + rowH - 13f, MR, s.y + rowH - 13f,
                Paint().apply { color = COR_DIVIDER; strokeWidth = 0.5f })
            s.y += rowH
        }

        // Rodapé da tabela com total
        cv.drawRect(ML, s.y - 13f, MR, s.y + rowH - 13f, pFill(COR_BG_SECTION))
        cv.drawText("TOTAL (${ d.equipamentos.size } equipamentos)",
            cols[0], s.y, pText(9f, COR_PRIMARY, bold = true))
        cv.drawText("${"%.2f".format(totalKva)} kVA",
            cols[5], s.y, pText(9f, COR_PRIMARY, bold = true))
        s.y += rowH
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS DE DESENHO
    // ══════════════════════════════════════════════════════════════════════

    private fun secTitulo(cv: Canvas, texto: String, y: Float) {
        cv.drawRect(ML, y - 12f, ML + 4f, y + 5f, pFill(COR_PRIMARY))
        cv.drawText(texto, ML + 10f, y, pText(10f, COR_PRIMARY, bold = true))
    }

    private fun miniInfoBox(
        cv: Canvas, label: String, valor: String,
        x: Float, y: Float, w: Float, borderColor: Int = COR_DIVIDER
    ) {
        cv.drawRoundRect(RectF(x, y, x + w, y + 46f), 8f, 8f, pFill(Color.rgb(247, 250, 253)))
        cv.drawRoundRect(RectF(x, y, x + w, y + 46f), 8f, 8f, pStroke(borderColor, 1f))
        // Label em duas linhas se necessário
        if (label.length > 20) {
            val split = label.lastIndexOf(' ', label.length / 2 + 4).takeIf { it > 0 } ?: (label.length / 2)
            cv.drawText(label.substring(0, split), x + 8f, y + 13f, pText(7f, COR_TEXT_SEC))
            cv.drawText(label.substring(split + 1), x + 8f, y + 23f, pText(7f, COR_TEXT_SEC))
        } else {
            cv.drawText(label, x + 8f, y + 16f, pText(7f, COR_TEXT_SEC))
        }
        cv.drawText(valor, x + 8f, y + 38f, pText(11f, COR_TEXT))
    }

    /** Box do trafo com destaque visual especial — é a info mais importante. */
    private fun miniInfoBoxTrafo(cv: Canvas, valor: String, x: Float, y: Float, w: Float) {
        // Fundo azul claro + borda azul sólida
        cv.drawRoundRect(RectF(x, y, x + w, y + 46f), 8f, 8f, pFill(COR_BG_SECTION))
        cv.drawRoundRect(RectF(x, y, x + w, y + 46f), 8f, 8f, pStroke(COR_PRIMARY, 2f))
        // Faixa de cor no topo
        cv.drawRoundRect(RectF(x, y, x + w, y + 6f), 8f, 8f, pFill(COR_PRIMARY))
        cv.drawRect(x, y + 3f, x + w, y + 6f, pFill(COR_PRIMARY))

        cv.drawText("Transformador Sugerido", x + 8f, y + 20f, pText(7f, COR_TEXT_SEC))
        cv.drawText(valor, x + 8f, y + 38f, pText(13f, COR_PRIMARY, bold = true))
    }

    private fun desenharTabelaSimples(
        s: DrawState, cols: List<Float>,
        headers: List<String>, linhas: List<List<String>>,
        rodape: List<String>? = null
    ) {
        val cv = s.cv
        val rowH = 18f

        cv.drawRect(ML, s.y - 12f, MR, s.y + rowH - 12f, pFill(COR_TABLE_HEAD))
        headers.forEachIndexed { i, h ->
            if (i < cols.size) cv.drawText(h, cols[i] + 4f, s.y, pText(8f, COR_PRIMARY, bold = true))
        }
        s.y += rowH

        linhas.forEachIndexed { idx, linha ->
            s.verificarEspaco(rowH + 4f)
            val cor = if (linha.isNotEmpty()) corDoGrupo(linha[0]) else COR_OUTROS
            // Stripe colorida
            cv.drawRect(ML, s.y - 12f, ML + 3f, s.y + rowH - 12f, pFill(cor))
            if (idx % 2 == 1)
                cv.drawRect(ML + 3f, s.y - 12f, MR, s.y + rowH - 12f, pFill(COR_ROW_ALT))
            linha.forEachIndexed { i, cel ->
                if (i < cols.size) cv.drawText(cel, cols[i] + 4f, s.y, pText(8f, COR_TEXT))
            }
            cv.drawLine(ML, s.y + rowH - 12f, MR, s.y + rowH - 12f,
                Paint().apply { color = COR_DIVIDER; strokeWidth = 0.5f })
            s.y += rowH
        }

        if (rodape != null) {
            cv.drawRect(ML, s.y - 12f, MR, s.y + rowH - 12f, pFill(COR_BG_SECTION))
            rodape.forEachIndexed { i, cel ->
                if (i < cols.size && cel.isNotBlank())
                    cv.drawText(cel, cols[i] + 4f, s.y, pText(9f, COR_PRIMARY, bold = true))
            }
            s.y += rowH
        }
    }

    private fun f2(v: Double) = "${"%.2f".format(v)} kVA"

    // ══════════════════════════════════════════════════════════════════════
    // ESTADO DE DESENHO
    // ══════════════════════════════════════════════════════════════════════
    class DrawState(private val doc: PdfDocument, private val dados: RelatorioData) {
        var y = 0f
        var pagina = 0
        private lateinit var page: PdfDocument.Page
        lateinit var cv: Canvas

        fun novaPage() {
            if (pagina > 0) {
                desenharRodape()
                doc.finishPage(page)
            }
            pagina++
            val info = PdfDocument.PageInfo.Builder(PW, PH, pagina).create()
            page = doc.startPage(info)
            cv = page.canvas
            cv.drawRect(0f, 0f, PW.toFloat(), PH.toFloat(),
                Paint().apply { color = Color.WHITE })
            y = 36f
        }

        fun fecharPage() {
            desenharRodape()
            doc.finishPage(page)
        }

        private fun desenharRodape() {
            cv.drawLine(ML, MB + 8f, MR, MB + 8f,
                Paint().apply { color = COR_DIVIDER; strokeWidth = 0.5f })
            cv.drawText(
                "DemandaCalc  |  ${dados.projetoNome}  |  ${dados.dataGeracao}  |  Pag. $pagina",
                ML, MB + 20f,
                Paint().apply {
                    textSize = 7f; color = COR_TEXT_SEC
                    typeface = android.graphics.Typeface.DEFAULT; isAntiAlias = true
                }
            )
        }

        private fun desenharCabecalhoContinuacao() {
            cv.drawRect(0f, 0f, PW.toFloat(), 26f,
                Paint().apply { color = COR_PRIMARY_DK })
            cv.drawRect(0f, 0f, 6f, 26f,
                Paint().apply { color = Color.rgb(0, 188, 212) })
            cv.drawText("DemandaCalc  |  ${dados.projetoNome}",
                ML + 4f, 17f, Paint().apply {
                    textSize = 9f; color = Color.WHITE
                    typeface = android.graphics.Typeface.DEFAULT_BOLD; isAntiAlias = true
                })
            cv.drawText("Pag. $pagina", MR - 45f, 17f,
                Paint().apply {
                    textSize = 9f; color = Color.rgb(179, 212, 245); isAntiAlias = true
                })
            y = 38f
        }

        fun verificarEspaco(necessario: Float) {
            if (y + necessario > MB) {
                novaPage()
                desenharCabecalhoContinuacao()
            }
        }
    }
}