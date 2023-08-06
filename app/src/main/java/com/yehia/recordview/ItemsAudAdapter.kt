package com.yehia.recordview

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.yehia.recordview.databinding.ItemXBinding

public class ItemsAudAdapter(
    private val activity: Activity,
) : RecyclerView.Adapter<ItemsAudAdapter.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemXBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return 50
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.recordPlays.setAudioTarget(
            "https://www.eroshen.com/up2/uploads/1445668835931.mp3?fbclid=IwAR1G3L02lfn2HBf__WcNAIXaqxwqcXPw7aUdzHPRFSwZzU7nRmPOjTNHwHQ",
            activity,"3000"
        )
    }

    class ViewHolder(val binding: ItemXBinding) : RecyclerView.ViewHolder(binding.root)
}