package com.ms8.kittydoor

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class LogViewerAdapter: RecyclerView.Adapter<LogViewerAdapter.ViewHolder>() {
    private var logs = ArrayList<FirebaseDBF.FirebaseDebugMessage>()

    @SuppressLint("NotifyDataSetChanged")
    fun setLogs(newLogs : ArrayList<FirebaseDBF.FirebaseDebugMessage>) {
        logs = newLogs
        notifyDataSetChanged()
    }

    fun addLog(newLog: FirebaseDBF.FirebaseDebugMessage) {
        logs.add(newLog)
        notifyItemInserted(logs.size-1)
    }

    fun addLogs(newLogs: Collection<FirebaseDBF.FirebaseDebugMessage>) {
        val oldEnd = logs.size-1
        logs.addAll(newLogs)
        notifyItemRangeInserted(oldEnd, newLogs.size)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(firebaseLog: FirebaseDBF.FirebaseDebugMessage) {
            val tvMessage = itemView.findViewById<TextView>(R.id.tvMessage)
            tvMessage.text = firebaseLog.message
            tvMessage.setTextColor(ContextCompat.getColor(itemView.context,
                when {
                    firebaseLog.message.contains("OPEN") -> R.color.colorOpen
                    firebaseLog.message.contains("CLOSED") -> R.color.colorClose
                    else -> R.color.white
                }
            ))
            val timestampStr = firebaseLog.timestamp + ": "
            itemView.findViewById<TextView>(R.id.tvTimeStamp).text = timestampStr
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_log_entry, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(logs[position])
    }

    override fun getItemCount() = logs.size
}