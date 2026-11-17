package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.CurrencyCode
import com.example.data.model.RoberPathCatalog
import com.example.ui.theme.AccentGoldBanner
import com.example.ui.theme.StatusAlertBg
import com.example.ui.theme.StatusAlertFg
import com.example.ui.theme.StatusInfoBg
import com.example.ui.theme.StatusInfoFg
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingFg
import com.example.ui.theme.StatusVerifiedBg
import com.example.ui.theme.StatusVerifiedFg
import com.example.ui.viewmodel.PortalMode
import com.example.util.PdfReportGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoberPathTopBar(
  title: String,
  subtitle: String,
  currency: CurrencyCode,
  isAdminUser: Boolean,
  portalMode: PortalMode,
  onToggleCurrency: () -> Unit,
  onTogglePortalMode: () -> Unit,
  onSignOut: () -> Unit,
) {
  TopAppBar(
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surface,
      titleContentColor = MaterialTheme.colorScheme.onSurface,
    ),
    navigationIcon = {
      Image(
        painter = painterResource(id = R.drawable.rober_path_logo),
        contentDescription = "Rober Path Emblem",
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .padding(start = 12.dp, end = 6.dp)
          .size(38.dp)
          .clip(CircleShape)
          .border(1.5.dp, AccentGoldBanner, CircleShape),
      )
    },
    title = {
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    },
    actions = {
      FilterChip(
        selected = currency == CurrencyCode.SAR,
        onClick = onToggleCurrency,
        label = { Text(currency.name, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.CurrencyExchange,
            contentDescription = "Switch Currency",
            modifier = Modifier.size(16.dp),
          )
        },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = Modifier.testTag("currency_toggle_chip"),
      )
      if (isAdminUser) {
        IconButton(
          onClick = onTogglePortalMode,
          modifier = Modifier.testTag("portal_mode_switch"),
        ) {
          Icon(
            imageVector = if (portalMode == PortalMode.ADMIN) Icons.Default.Person else Icons.Default.AdminPanelSettings,
            contentDescription = if (portalMode == PortalMode.ADMIN) "Preview Customer Portal" else "Switch to Admin Dashboard",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
      IconButton(
        onClick = onSignOut,
        modifier = Modifier.testTag("sign_out_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Logout,
          contentDescription = "Sign Out",
        )
      }
    },
  )
}

@Composable
fun StatusPill(
  text: String,
  tone: StatusTone = StatusTone.INFO,
  modifier: Modifier = Modifier,
) {
  val (bg, fg) = when (tone) {
    StatusTone.VERIFIED -> StatusVerifiedBg to StatusVerifiedFg
    StatusTone.PENDING -> StatusPendingBg to StatusPendingFg
    StatusTone.ALERT -> StatusAlertBg to StatusAlertFg
    StatusTone.INFO -> StatusInfoBg to StatusInfoFg
  }
  Surface(
    color = bg,
    shape = RoundedCornerShape(50),
    modifier = modifier,
  ) {
    Text(
      text = text,
      color = fg,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

enum class StatusTone { VERIFIED, PENDING, ALERT, INFO }

fun statusToneFor(rawStatus: String): StatusTone = when (rawStatus.uppercase()) {
  "VERIFIED", "ISSUED", "CONFIRMED", "COMPLETED", "READY", "PRESENT", "CONVERTED" -> StatusTone.VERIFIED
  "PENDING", "INQUIRY", "IN_PROGRESS", "VISA_PROCESSING", "SUBMITTED", "DOCS_READY", "PLANNING", "FOLLOW_UP", "UPLOADED" -> StatusTone.PENDING
  "REJECTED", "CANCELLED", "ON_LEAVE" -> StatusTone.ALERT
  else -> StatusTone.INFO
}

@Composable
fun KpiMetricCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.14f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(17.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun FinancialBarChartCard(
  collectedBdt: Long,
  expensesBdt: Long,
  refundsBdt: Long,
  duesBdt: Long,
  currency: CurrencyCode,
  sarRate: Double,
) {
  val items = listOf(
    Triple("Collected", collectedBdt, MaterialTheme.colorScheme.primary),
    Triple("Expenses", expensesBdt, MaterialTheme.colorScheme.secondary),
    Triple("Dues", duesBdt, Color(0xFF1A6B63)),
    Triple("Refunds", refundsBdt, Color(0xFFBA1A1A)),
  )
  val maxVal = (items.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(1L).toFloat()

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text("Financial Overview (${currency.name})", style = MaterialTheme.typography.titleMedium)
      Text(
        "Live collections, operating expenses, customer dues & refunds",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(14.dp))
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(132.dp)
      ) {
        val barWidth = size.width / (items.size * 2f)
        val slotWidth = size.width / items.size
        items.forEachIndexed { index, (_, amount, color) ->
          val ratio = (amount.toFloat() / maxVal).coerceIn(0.05f, 1f)
          val barHeight = size.height * 0.85f * ratio
          val left = slotWidth * index + (slotWidth - barWidth) / 2f
          drawRoundRect(
            color = color.copy(alpha = 0.14f),
            topLeft = Offset(left, 0f),
            size = Size(barWidth, size.height * 0.85f),
            cornerRadius = CornerRadius(14f, 14f),
          )
          drawRoundRect(
            color = color,
            topLeft = Offset(left, size.height * 0.85f - barHeight),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(14f, 14f),
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        items.forEach { (label, amount, color) ->
          Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(color)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
              text = PdfReportGenerator.formatAmount(amount, currency.name, sarRate),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
fun RoberPathBrandBanner(
  headline: String,
  subheadline: String,
  onOpenBrochure: (() -> Unit)? = null,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.linearGradient(
            listOf(Color(0xFF0A472A), Color(0xFF146B43), Color(0xFF1C5B4D))
          )
        )
        .padding(18.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Image(
          painter = painterResource(id = R.drawable.rober_path_logo),
          contentDescription = "Rober Path Official Logo",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(2.dp, AccentGoldBanner, RoundedCornerShape(14.dp))
            .clickable(enabled = onOpenBrochure != null) { onOpenBrochure?.invoke() },
        )
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Verified,
              contentDescription = "Gov Approved",
              tint = AccentGoldBanner,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "GOV. APPROVED • LICENSE #${RoberPathCatalog.LICENSE_NUMBER}",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFF9E4B7),
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = headline,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = subheadline,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.88f),
          )
        }
      }
    }
  }
}

@Composable
fun BrochurePreviewDialog(
  drawableResId: Int,
  title: String,
  onDismiss: () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(shape = RoundedCornerShape(20.dp)) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(10.dp))
        Image(
          painter = painterResource(id = drawableResId),
          contentDescription = title,
          contentScale = ContentScale.FillWidth,
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          onClick = onDismiss,
          color = MaterialTheme.colorScheme.primary,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.align(Alignment.End),
        ) {
          Text(
            text = "Close",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
          )
        }
      }
    }
  }
}

@Composable
fun EmptyStateCard(
  icon: ImageVector,
  title: String,
  description: String,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(38.dp),
      )
      Spacer(modifier = Modifier.height(10.dp))
      Text(title, style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun PassportExpiryWarningBadge(expiry: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(StatusAlertBg)
      .padding(horizontal = 8.dp, vertical = 4.dp),
  ) {
    Icon(
      imageVector = Icons.Default.Warning,
      contentDescription = "Passport Expiring Soon",
      tint = StatusAlertFg,
      modifier = Modifier.size(14.dp),
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = "Under 6 months validity ($expiry)",
      style = MaterialTheme.typography.labelSmall,
      color = StatusAlertFg,
    )
  }
}

fun openWhatsAppOrDial(context: Context, phone: String, message: String? = null) {
  val cleanDigits = phone.replace(Regex("[^0-9]"), "")
  val intent = if (!message.isNullOrBlank()) {
    val encoded = Uri.encode(message)
    Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanDigits?text=$encoded"))
  } else {
    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
  }
  runCatching { context.startActivity(intent) }
}
