package com.example.projetodemandaeletricaindustrial

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EquipamentoAdapter(
    private val lista: MutableList<Equipamento>,
    private val onLongClick: (Int) -> Unit
) : RecyclerView.Adapter<EquipamentoAdapter.EquipamentoViewHolder>() {

    inner class EquipamentoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNome: TextView = view.findViewById(R.id.tvNomeEquipamento)
        val tvDetalhes: TextView = view.findViewById(R.id.tvDetalhesEquipamento)

        // Usando ImageButton conforme definido no seu XML
        val btnDeletar: ImageButton = view.findViewById(R.id.btnDeleteEquipamento)

        init {
            btnDeletar.setOnClickListener {
                // Trocado para adapterPosition para resolver o erro do seu print
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLongClick(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipamentoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_equipment, parent, false)
        return EquipamentoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EquipamentoViewHolder, position: Int) {
        val eq = lista[position]
        holder.tvNome.text = "${eq.descricao} (${eq.tipo})"
        holder.tvDetalhes.text = "Qtd: ${eq.quantidade} | Demanda: ${"%.2f".format(eq.potencia)} kVA"
    }

    override fun getItemCount() = lista.size
}