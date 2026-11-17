package com.example.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Booking
import com.example.data.model.ItineraryDay
import com.example.data.model.RoberPathCatalog
import com.example.ui.components.BrochurePreviewDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone

@Composable
fun SafarItineraryScreen(activeBooking: Booking?) {
  var phaseFilter by remember { mutableStateOf("ALL") }
  var showPoster by remember { mutableStateOf(false) }
  val checkedTips = remember { mutableStateMapOf<Int, Boolean>() }

  val days = remember(phaseFilter) {
    when (phaseFilter) {
      "MAKKAH" -> RoberPathCatalog.fourteenDayItinerary.filter { it.dayNumber <= 7 }
      "MADINAH" -> RoberPathCatalog.fourteenDayItinerary.filter { it.dayNumber >= 8 }
      else -> RoberPathCatalog.fourteenDayItinerary
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("ওমরাহ সফর • 14-Day Itinerary", style = MaterialTheme.typography.titleLarge)
          Text(
            text = "Makkah 7 Nights (মক্কা ৭ রাত) • Madinah 6 Nights (মদিনা ৬ রাত)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        OutlinedButton(
          onClick = { showPoster = true },
          modifier = Modifier.testTag("view_itinerary_poster_button"),
        ) {
          Icon(Icons.Default.Image, contentDescription = "View Itinerary Poster", modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Guide")
        }
      }
    }

    if (activeBooking != null) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Your Assigned Travel Logistics (${activeBooking.bookingCode})",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            LogisticsRow(Icons.Default.Flight, "Flight", activeBooking.flightInfo.ifBlank { "Scheduled via Dhaka" })
            LogisticsRow(Icons.Default.Hotel, "Makkah Hotel", activeBooking.hotelMakkah.ifBlank { "Assigned 4/5 Star" })
            LogisticsRow(Icons.Default.Mosque, "Madinah Hotel", activeBooking.hotelMadinah.ifBlank { "Assigned 4/5 Star" })
            LogisticsRow(Icons.Default.DirectionsBus, "Transport", activeBooking.transportInfo.ifBlank { "AC Haramain Coach" })
            LogisticsRow(Icons.Default.PersonPin, "Moallem", activeBooking.moallemName.ifBlank { "Kafela Director" })
          }
        }
      }
    }

    item {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = phaseFilter == "ALL", onClick = { phaseFilter = "ALL" }, label = { Text("All 14 Days") })
        FilterChip(selected = phaseFilter == "MAKKAH", onClick = { phaseFilter = "MAKKAH" }, label = { Text("Makkah (7N)") })
        FilterChip(selected = phaseFilter == "MADINAH", onClick = { phaseFilter = "MADINAH" }, label = { Text("Madinah (6N)") })
      }
    }

    items(days, key = { it.dayNumber }) { day ->
      ItineraryDayCard(day)
    }

    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("প্রয়োজনীয় পরামর্শ • Pilgrim Preparation Tips", style = MaterialTheme.typography.titleMedium)
          Spacer(modifier = Modifier.height(6.dp))
          RoberPathCatalog.travelTips.forEachIndexed { idx, tip ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Checkbox(
                checked = checkedTips[idx] == true,
                onCheckedChange = { checkedTips[idx] = it },
              )
              Text(tip, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }
  }

  if (showPoster) {
    BrochurePreviewDialog(
      drawableResId = R.drawable.rober_path_itinerary,
      title = "Rober Path 14-Day Umrah Safar Guide",
      onDismiss = { showPoster = false },
    )
  }
}

@Composable
private fun LogisticsRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.width(8.dp))
    Text("$label: ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    Text(value, style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun ItineraryDayCard(day: ItineraryDay) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "D${day.dayNumber}",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onPrimary,
          fontWeight = FontWeight.Bold,
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(day.titleEn, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
          StatusPill(
            text = day.city,
            tone = if (day.dayNumber <= 7) StatusTone.VERIFIED else StatusTone.INFO,
          )
        }
        Text(day.titleBn, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(day.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}
