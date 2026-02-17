package com.example.projetodemandaeletricaindustrial

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProjetoAdapter(
    private val lista: List<Projeto>,
    private val onClick: (Projeto) -> Unit,
    private val onDelete: (Projeto) -> Unit
) : RecyclerView.Adapter<ProjetoAdapter.ProjetoViewHolder>() {

    inner class ProjetoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNome: TextView = view.findViewById(R.id.tvNomeProjeto)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteProjeto)

        init {
            view.setOnClickListener {
                // Trocado de bindingAdapterPosition para adapterPosition
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onClick(lista[position])
                }
            }

            btnDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDelete(lista[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjetoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_projeto, parent, false)
        return ProjetoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjetoViewHolder, position: Int) {
        holder.tvNome.text = lista[position].nome
    }

    override fun getItemCount() = lista.size
}