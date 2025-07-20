package com.example.deardango

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class DiaryEntryAdapter(
    private val entries: MutableList<DiaryEntry>,
    private val onEntryClick: (DiaryEntry) -> Unit
) : RecyclerView.Adapter<DiaryEntryAdapter.DiaryEntryViewHolder>() {

    class DiaryEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryEntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diary_entry, parent, false)
        return DiaryEntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiaryEntryViewHolder, position: Int) {
        val entry = entries[position]
        holder.tvTitle.text = entry.title
        holder.tvDate.text = formatDate(entry.timestamp)

        holder.itemView.setOnClickListener {
            onEntryClick(entry)
        }
    }

    override fun getItemCount(): Int = entries.size

    fun updateEntries(newEntries: List<DiaryEntry>) {
        entries.clear()
        entries.addAll(newEntries)
        notifyDataSetChanged()
    }

    private fun formatDate(date: Date): String {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.time

        return when {
            isSameDay(date, today) -> "Today"
            isSameDay(date, yesterday) -> "Yesterday"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}