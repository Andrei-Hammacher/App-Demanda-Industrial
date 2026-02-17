package com.example.projetodemandaeletricaindustrial

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        // Habilita a seta de voltar na barra superior
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = "Meus Projetos"

        configurarRecyclerView()
    }

    // NOVA FUNÇÃO: Faz a seta "Voltar" funcionar fechando a tela atual
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun configurarRecyclerView() {
        adapter = ProjetoAdapter(
            listaProjetos,
            onClick = { projeto -> abrirProjeto(projeto) },
            onDelete = { projeto -> mostrarDialogoExclusao(projeto) }
        )

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerProjetos)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
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
        }
    }

    private fun abrirProjeto(projeto: Projeto) {
        val intent = Intent(this, ListaCargasActivity::class.java).apply {
            putExtra("PROJETO_ID", projeto.id)
            putExtra("PROJETO_NOME", projeto.nome)
        }
        startActivity(intent)
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