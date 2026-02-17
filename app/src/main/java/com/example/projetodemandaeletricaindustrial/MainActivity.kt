package com.example.projetodemandaeletricaindustrial

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.projetodemandaeletricaindustrial.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Botão para criar um novo cálculo do zero
        binding.btnNovoProjeto.setOnClickListener {
            mostrarDialogoNovoProjeto()
        }

        // Botão para ver projetos que já foram salvos
        binding.btnVerProjetos.setOnClickListener {
            val intent = Intent(this, SelecaoProjetoActivity::class.java)
            startActivity(intent)
        }
    }

    private fun mostrarDialogoNovoProjeto() {
        val inputNome = EditText(this)
        inputNome.hint = "Ex: Galpão Industrial A"

        AlertDialog.Builder(this)
            .setTitle("Novo Projeto")
            .setMessage("Digite o nome da instalação para o cálculo de demanda:")
            .setView(inputNome)
            .setPositiveButton("Criar") { _, _ ->
                val nome = inputNome.text.toString()
                if (nome.isNotEmpty()) {
                    salvarProjetoNoBanco(nome)
                } else {
                    Toast.makeText(this, "O nome não pode ser vazio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun salvarProjetoNoBanco(nome: String) {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@MainActivity)
            val novoProjeto = Projeto(nome = nome)

            // Inserimos o projeto e pegamos o ID gerado pelo Room
            val idGerado = database.projetoDao().inserirProjeto(novoProjeto)

            // Abrimos a lista de cargas passando o ID do projeto recém-criado
            val intent = Intent(this@MainActivity, ListaCargasActivity::class.java)
            intent.putExtra("PROJETO_ID", idGerado.toInt())
            intent.putExtra("PROJETO_NOME", nome)
            startActivity(intent)
        }
    }
}