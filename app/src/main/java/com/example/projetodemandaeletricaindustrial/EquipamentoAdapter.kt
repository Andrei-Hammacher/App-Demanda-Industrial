package com.example.projetodemandaeletricaindustrial

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class EquipamentoAdapter(
    private val lista: MutableList<Equipamento>,
    private val onEdit: (Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<EquipamentoAdapter.EquipamentoViewHolder>() {

    inner class EquipamentoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: com.google.android.material.card.MaterialCardView =
            view.findViewById(R.id.cardEquipamento)
        val viewStripe: View        = view.findViewById(R.id.viewStripe)
        val tvNome: TextView        = view.findViewById(R.id.tvNomeEquipamento)
        val tvTipo: TextView        = view.findViewById(R.id.tvTipoEquipamento)
        val tvDetalhes: TextView    = view.findViewById(R.id.tvDetalhesEquipamento)
        val tvDemandaChip: TextView = view.findViewById(R.id.tvDemandaChip)
        val btnEditar: ImageButton  = view.findViewById(R.id.btnEditarEquipamento)
        val btnDeletar: ImageButton = view.findViewById(R.id.btnDeleteEquipamento)

        init {
            btnEditar.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) onEdit(pos)
            }
            btnDeletar.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) onDelete(pos)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipamentoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_equipment, parent, false)
        return EquipamentoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EquipamentoViewHolder, position: Int) {
        val eq  = lista[position]
        val ctx = holder.itemView.context

        // ── Cor do grupo ────────────────────────────────────────────────
        val stripeColorRes = when {
            eq.tipo.equals("Motor", ignoreCase = true)                          -> R.color.stripe_grupo_c
            eq.tipo.contains("Iluminação", ignoreCase = true) ||
                    eq.tipo.contains("Tomada", ignoreCase = true)                       -> R.color.stripe_grupo_a
            eq.tipo.contains("Forno", ignoreCase = true)                        -> R.color.stripe_grupo_b
            eq.tipo.contains("Retificador", ignoreCase = true)                  -> R.color.stripe_retificador
            eq.tipo.contains("Solda", ignoreCase = true) ||
                    eq.tipo.contains("Ar Condicionado", ignoreCase = true)              -> R.color.stripe_especial
            else                                                                 -> R.color.stripe_outros
        }
        val bgColorRes = when {
            eq.tipo.equals("Motor", ignoreCase = true)                          -> R.color.stripe_grupo_c_bg
            eq.tipo.contains("Iluminação", ignoreCase = true) ||
                    eq.tipo.contains("Tomada", ignoreCase = true)                       -> R.color.stripe_grupo_a_bg
            eq.tipo.contains("Forno", ignoreCase = true)                        -> R.color.stripe_grupo_b_bg
            eq.tipo.contains("Retificador", ignoreCase = true)                  -> R.color.stripe_retificador_bg
            eq.tipo.contains("Solda", ignoreCase = true) ||
                    eq.tipo.contains("Ar Condicionado", ignoreCase = true)              -> R.color.stripe_especial_bg
            else                                                                 -> R.color.stripe_outros_bg
        }

        val stripeColor = ContextCompat.getColor(ctx, stripeColorRes)
        val bgColor     = ContextCompat.getColor(ctx, bgColorRes)

        holder.viewStripe.setBackgroundColor(stripeColor)
        holder.card.setCardBackgroundColor(bgColor)

        // ── Nome: descrição + painel ────────────────────────────────────
        val ccmLabel = if (eq.ccm.isNotBlank()) "  [${eq.ccm}]" else ""
        val qdfLabel = if (eq.qdf.isNotBlank()) "  [${eq.qdf}]" else ""
        holder.tvNome.text = "${eq.descricao}$ccmLabel$qdfLabel"

        // ── Tipo em linha separada (label menor) ────────────────────────
        holder.tvTipo.text = eq.tipo
        holder.tvTipo.setTextColor(stripeColor)

        // ── Detalhes: qtd + potência original ───────────────────────────
        val potTexto = when {
            eq.tipo.equals("Motor", ignoreCase = true) ->
                "${"%.1f".format(eq.potenciaOriginal)} CV/un"
            eq.tipo.equals("Iluminação com Reator", ignoreCase = true) ->
                "${"%.0f".format(eq.potenciaOriginal)} W/reator"
            else ->
                "${"%.0f".format(eq.potenciaOriginal)} W/un"
        }
        holder.tvDetalhes.text = "Qtd: ${eq.quantidade}  ·  $potTexto"

        // ── Chip de demanda total ───────────────────────────────────────
        val demandaTotal = eq.potencia * eq.quantidade
        holder.tvDemandaChip.text = "${"%.2f".format(demandaTotal)} kVA"
        holder.tvDemandaChip.setTextColor(stripeColor)

        // Fundo do chip com alpha da cor do grupo
        val chipBg = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 40f
            setColor(android.graphics.Color.argb(30,
                android.graphics.Color.red(stripeColor),
                android.graphics.Color.green(stripeColor),
                android.graphics.Color.blue(stripeColor)))
        }
        holder.tvDemandaChip.background = chipBg
    }

    override fun getItemCount() = lista.size
}