package mivs.liturgicalcalendar.ui.news

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mivs.liturgicalcalendar.R
import mivs.liturgicalcalendar.domain.model.NewsItem
import java.text.SimpleDateFormat
import java.util.Locale

class NewsAdapter(private val newsList: List<NewsItem>) :
    RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvNewsDate)
        val tvTitle: TextView = view.findViewById(R.id.tvNewsTitle)
        val tvBody: TextView = view.findViewById(R.id.tvNewsBody)
        val btnAction: Button = view.findViewById(R.id.btnNewsAction)
        val imgNews: android.widget.ImageView = view.findViewById(R.id.imgNews)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val news = newsList[position]

        // 1. Ustawianie tekstów
        holder.tvTitle.text = news.title
        holder.tvBody.text = news.content

        // 2. Formatowanie daty (ZABEZPIECZONE)
        if (news.date != null) {
            try {
                val sdf = SimpleDateFormat("d MMM yyyy", Locale("pl", "PL"))
                holder.tvDate.text = sdf.format(news.date)
            } catch (e: Exception) {
                // Gdyby coś poszło nie tak z formatowaniem
                holder.tvDate.text = "Błąd daty"
            }
        } else {
            // Gdy data w bazie jest pusta (null)
            holder.tvDate.text = ""
        }

        // 3. Obsługa przycisku (Link)
        if (!news.actionLink.isNullOrEmpty()) {
            holder.btnAction.visibility = View.VISIBLE
            holder.btnAction.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.actionLink))
                    holder.itemView.context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            holder.btnAction.visibility = View.GONE
        }
        if (!news.imageUrl.isNullOrEmpty()) {
            // Jeśli jest link, pokaż widok i załaduj zdjęcie przez Glide
            holder.imgNews.visibility = View.VISIBLE

            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(news.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.stat_notify_error)
                .into(holder.imgNews)
        } else {
            // Jeśli brak linku, ukryj ImageView, żeby nie było czarnej dziury
            holder.imgNews.visibility = View.GONE
        }
    }

    override fun getItemCount() = newsList.size
}