package com.odontoart.rotas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun ParitySectionHeader(title: String, description: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            if (!description.isNullOrBlank()) {
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}

@Composable
internal fun ParityInfoCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(text, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun ParityErrorCard(text: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun ParityStatusPill(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

internal fun formatDateBr(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    return runCatching {
        val date = if (value.length >= 10) LocalDate.parse(value.substring(0, 10)) else LocalDate.parse(value)
        date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR")))
    }.getOrElse { value }
}

internal fun formatDateTimeBr(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    return runCatching {
        val instant = runCatching { Instant.parse(value) }.getOrElse { OffsetDateTime.parse(value).toInstant() }
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }.getOrElse { value }
}

internal fun stripBasicHtml(value: String): String = value
    .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
    .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")
    .replace(Regex("<[^>]+>"), "")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .trim()
