package mivs.liturgicalcalendar.ui.news

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import mivs.liturgicalcalendar.R
import mivs.liturgicalcalendar.domain.model.NewsItem

class NewsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_news)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Konfiguracja paska narzędzi (powrót)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish() // Zamyka Activity i wraca do Kalendarza
        }

        recyclerView = findViewById(R.id.recyclerViewNews)
        progressBar = findViewById(R.id.progressBarNews)

        recyclerView.layoutManager = LinearLayoutManager(this)

        fetchNewsFromFirebase()
    }

    private fun fetchNewsFromFirebase() {
        progressBar.visibility = View.VISIBLE // Pokaż kręciołek

        val db = FirebaseFirestore.getInstance()

        // Pamiętaj: Kolekcja musi nazywać się "news" w Firebase!
        db.collection("news")
            .whereEqualTo("isVisible", true) // Pobieramy tylko widoczne
            .orderBy("date", Query.Direction.DESCENDING) // Najnowsze na górze
            .limit(20) // Limit dla bezpieczeństwa i szybkości
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE

                // --- ZMIANA: BEZPIECZNE POBIERANIE DANYCH ---
                val newsList = mutableListOf<NewsItem>()

                for (document in documents) {
                    try {
                        // Próbujemy zamienić każdy dokument osobno
                        val item = document.toObject(NewsItem::class.java)
                        newsList.add(item)
                    } catch (e: Exception) {
                        // Jeśli dokument ma błąd (np. zły typ daty), ignorujemy go
                        e.printStackTrace()
                        //Toast.makeText(this, "Błąd w dokumencie ${document.id}: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                // ---------------------------------------------

                if (newsList.isEmpty()) {
                    Toast.makeText(this, "Brak nowych wiadomości", Toast.LENGTH_SHORT).show()
                } else {
                    recyclerView.adapter = NewsAdapter(newsList)
                }
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Błąd pobierania: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }
}