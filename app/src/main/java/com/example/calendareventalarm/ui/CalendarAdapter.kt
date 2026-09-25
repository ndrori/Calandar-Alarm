package com.example.calendareventalarm.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.calendareventalarm.R
import com.example.calendareventalarm.data.PreferencesManager
import com.example.calendareventalarm.databinding.ItemCalendarEventBinding
import com.example.calendareventalarm.model.AlarmStatus
import com.example.calendareventalarm.model.CalendarEvent
import com.example.calendareventalarm.utils.LocaleUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

class CalendarAdapter(
    private var events: List<CalendarEvent> = emptyList()
) : RecyclerView.Adapter<CalendarAdapter.EventViewHolder>() {

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
            val context = binding.root.context
            val activeLocale = LocaleUtils.getLocale(PreferencesManager(context).getLanguageCode())
            val timeFormat = SimpleDateFormat("h:mm a", activeLocale)
            val dateFormat = SimpleDateFormat("MMM d, h:mm a", activeLocale)

            val leadMins = max(0, ((event.startTime - event.alarmTime) / (60 * 1000.0)).roundToInt())
            val leadLabel = if (leadMins > 0) {
                context.getString(R.string.alarm_lead_time_minutes, leadMins)
            } else {
                context.getString(R.string.alarm_lead_time_now)
            }

            binding.tvEventTitle.text = event.title
            binding.tvEventTime.text = dateFormat.format(Date(event.startTime))
            binding.tvAlarmTime.text = "${timeFormat.format(Date(event.alarmTime))} ($leadLabel)"

            val scheduledStatusText = if (leadMins > 0) {
                "${context.getString(R.string.status_scheduled)} (${leadMins}m)"
            } else {
                "${context.getString(R.string.status_scheduled)} (0m)"
            }

            val (statusText, bgColor, textColor) = when (event.status) {
                AlarmStatus.SCHEDULED -> Triple(scheduledStatusText, "#E8F5E9", "#2E7D32")
                AlarmStatus.SNOOZED -> Triple(context.getString(R.string.snooze_button), "#FFF3E0", "#E65100")
                AlarmStatus.DISMISSED -> Triple(context.getString(R.string.status_dismissed), "#FFEBEE", "#C62828")
                AlarmStatus.FIRED -> Triple(context.getString(R.string.status_fired), "#E1F5FE", "#0277BD")
            }

            binding.chipStatus.text = statusText
            binding.chipStatus.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(bgColor))
            binding.chipStatus.setTextColor(Color.parseColor(textColor))
        }
    }
}
