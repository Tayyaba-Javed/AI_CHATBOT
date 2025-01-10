package com.example.aichatbot.adapters


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.aichatbot.R
import com.example.aichatbot.database.ChatTable


class ChatAdapter : ListAdapter<ChatTable, RecyclerView.ViewHolder>(ChatTableDiffCallback()) {
    companion object {
        private const val VIEW_TYPE_SENDER = 0
        private const val VIEW_TYPE_RECEIVER = 1

    }
    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isSender) {
            VIEW_TYPE_SENDER
        } else {
            VIEW_TYPE_RECEIVER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENDER -> {
                val view =
                    LayoutInflater.from(parent.context).inflate(R.layout.sender_item, parent, false)
                SenderViewHolder(view)
            }

            VIEW_TYPE_RECEIVER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.receiver_item, parent, false)
                ReceiverViewHolder(view)
            }


            else -> throw IllegalArgumentException("Invalid view type")
        }
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chat = getItem(position)
        when (holder) {
            is SenderViewHolder -> {
                holder.bind(chat)
            }

            is ReceiverViewHolder -> {
                holder.bind(chat, holder.itemView.context)
            }

        }
    }

    class SenderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageText: TextView = view.findViewById(R.id.senderText)

        fun bind(chat: ChatTable) {
            messageText.text = chat.message
        }
    }

    class ReceiverViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageText: TextView = view.findViewById(R.id.responseText)
        fun bind(chat: ChatTable, context: Context) {
            messageText.text = chat.message

        }

    }

    class ChatTableDiffCallback : DiffUtil.ItemCallback<ChatTable>() {
        override fun areItemsTheSame(oldItem: ChatTable, newItem: ChatTable): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: ChatTable, newItem: ChatTable): Boolean {
            return oldItem == newItem
        }
    }
}