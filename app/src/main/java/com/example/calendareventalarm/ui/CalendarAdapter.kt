package com.example.calendareventalarm.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.calendareventalarm.databinding.ItemCalendarEventBinding
import com.example.calendareventalarm.model.AlarmStatus
import com.example.calendareventalarm.model.CalendarEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarAdapter(
    private var events: List<CalendarEvent> = emptyList()
) : RecyclerView.Adapter<CalendarAdapter.EventViewHolder>() {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    fun updateEvents(newEvents: List<CalendarEvent>) {
        this.events = newEvents
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemCalendarEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    inner class EventViewHolder(private val binding: ItemCalendarEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: CalendarEvent) {
            binding.tvEventTitle.text = event.title
            binding.tvEventTime.text = "Start: ${dateFormat.format(Date(event.startTime))}"
            binding.tvAlarmTime.text = "Alarm: ${timeFormat.format(Date(event.alarmTime))} (15m prior)"

            val (statusText, bgColor, textColor) = when (event.status) {
                AlarmStatus.SCHEDULED -> Triple("Scheduled", "#E8F5E9", "#2E7D32")
                AlarmStatus.SNOOZED -> Triple("Snoozed (5m)", "#FFF3E0", "#E65100")
                AlarmStatus.DISMISSED -> Triple("Dismissed", "#FFEBEE", "#C62828")
                AlarmStatus.FIRED -> Triple("Ringing", "#E1F5FE", "#0277BD")
            }

            binding.chipStatus.text = statusText
            binding.chipStatus.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(bgColor))
            binding.chipStatus.setTextColor(Color.parseColor(textColor))
        }
    }
}
