package com.example.mapapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mapapp.databinding.ItemXmlFileBinding

class FilesAdapter(
    private val fileClickListener: (file: String, view: View) -> Unit,
) : RecyclerView.Adapter<FilesAdapter.ViewHolder>() {

    private var list: MutableList<String> = mutableListOf()

    fun setData(data: List<String>) {
        list.clear()
        list.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemXmlFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    inner class ViewHolder(private val binding: ItemXmlFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(fileName: String) {
            binding.tvFileName.text = fileName
            binding.btnMore.setOnClickListener { fileClickListener(fileName, binding.root) }
        }
    }

    override fun getItemCount() = list.size
}