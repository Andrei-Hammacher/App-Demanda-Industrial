package com.example.projetodemandaeletricaindustrial

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projetodemandaeletricaindustrial.databinding.ActivitySelecaoProjetoBinding
import kotlinx.coroutines.launch

class SelecaoProjetoActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelecaoProjetoBinding
    private val listaProjetos = mutableListOf<Projeto>()
    private lateinit var adapter: ProjetoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelecaoProjetoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Meus Projetos"

        configurarRecyclerView()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun configurarRecyclerView() {
        adapter = ProjetoAdapter(
            listaProjetos,
            onClick  = { projeto -> abrirProjeto(projeto) },
            onDelete = { projeto -> mostrarDialogoExclusao(projeto) }
        )
        binding.recyclerProjetos.layoutManager = LinearLayoutManager(this)
        binding.recyclerProjetos.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        carregarProjetos()
    }

    private fun carregarProjetos() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@SelecaoProjetoActivity)
            val projetosDoBanco = db.projetoDao().buscarTodosProjetos()

            listaProjetos.clear()
            listaProjetos.addAll(projetosDoBanco)
            adapter.notifyDataSetChanged()

            // Atualiza contador e empty state
            val count = listaProjetos.size
            binding.tvContadorProjetos.text =
                if (count == 1) "1 projeto" else "$count projetos"

            // Toggle: lista visível ↔ empty state visível
            val temProjetos = count > 0
            binding.recyclerProjetos.visibility  = if (temProjetos) View.VISIBLE else View.GONE
            binding.layoutEmptyState.visibility  = if (temProjetos) View.GONE   else View.VISIBLE
        }
    }

    private fun abrirProjeto(projeto: Projeto) {
        startActivity(
            Intent(this, ListaCargasActivity::class.java).apply {
                putExtra("PROJETO_ID", projeto.id)
                putExtra("PROJETO_NOME", projeto.nome)
            }
        )
    }

    private fun mostrarDialogoExclusao(projeto: Projeto) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Projeto")
            .setMessage("Deseja apagar '${projeto.nome}' e todos os seus equipamentos?")
            .setPositiveButton("Excluir") { _, _ -> excluirProjeto(projeto) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluirProjeto(projeto: Projeto) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@SelecaoProjetoActivity)
            db.projetoDao().deletarProjeto(projeto)
            Toast.makeText(this@SelecaoProjetoActivity, "Projeto removido", Toast.LENGTH_SHORT).show()
            carregarProjetos()
        }
    }
}