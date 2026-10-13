package dev.lantern.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.lantern.core.Account
import dev.lantern.core.Txn
import dev.lantern.core.balance
import dev.lantern.core.formatMoney
import dev.lantern.core.monthSummary
import dev.lantern.core.newId
import dev.lantern.core.parseMoney

private val defaultCategories = listOf("Food", "Transport", "Home", "Bills", "Health", "Fun", "Shopping", "Salary", "Other")

@Composable
fun MoneyScreen(store: LanternStore) {
    var adding by remember { mutableStateOf(false) }
    val today = store.today
    // The first transaction creates a plain "Cash" account so nothing has to be set up first.
    val accountId = store.accounts.firstOrNull()?.id
    val month = monthSummary(store.txns, today.year, today.monthNumber)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = { FloatingActionButton(onClick = { adding = true }) { Icon(Icons.Rounded.Add, "New transaction") } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Panel {
                    Text("${monthName(today.monthNumber)} ${today.year}", style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Income", style = MaterialTheme.typography.labelMedium); Text(formatMoney(month.incomeMinor), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary) }
                        Column(horizontalAlignment = Alignment.End) { Text("Spent", style = MaterialTheme.typography.labelMedium); Text(formatMoney(-month.expenseMinor), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error) }
                    }
                    val top = month.byCategory.firstOrNull()?.second?.let { -it } ?: 1L
                    month.byCategory.take(5).forEach { (cat, amount) ->
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(cat, style = MaterialTheme.typography.bodyMedium)
                                Text(formatMoney(-amount), style = MaterialTheme.typography.bodyMedium)
                            }
                            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                                Box(Modifier.fillMaxWidth((-amount).toFloat() / top).height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primary))
                            }
                        }
                    }
                }
            }
            if (store.accounts.isNotEmpty()) item {
                Panel {
                    Text("Accounts", style = MaterialTheme.typography.titleSmall)
                    store.accounts.forEach { a ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(a.name, style = MaterialTheme.typography.bodyLarge)
                            Text(formatMoney(balance(a, store.txns)) + " " + a.currency, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            if (store.txns.isEmpty()) item { EmptyHint("No transactions yet", "Tap + to add one. They are saved per year in money/<year>.csv.") }
            items(store.txns.take(200), key = { it.id }) { t ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.category.ifEmpty { "Other" }, style = MaterialTheme.typography.bodyLarge)
                        Text(prettyDate(t.date) + if (t.note.isNotBlank()) "  ·  ${t.note}" else "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Text(formatMoney(t.amountMinor), style = MaterialTheme.typography.titleSmall, color = if (t.amountMinor < 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary)
                    IconButton(onClick = { store.deleteTxn(t.id) }) { Icon(Icons.Rounded.Close, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
    if (adding) AddTxnDialog(onDismiss = { adding = false }) { amount, category, note ->
        val id = accountId ?: newId().also { store.saveAccounts(listOf(Account(it, "Cash"))) }
        store.saveTxn(Txn("", today, amount, id, category, note))
        adding = false
    }
}

@Composable
private fun AddTxnDialog(onDismiss: () -> Unit, onSave: (Long, String, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var income by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("Food") }
    var note by remember { mutableStateOf("") }
    val minor = parseMoney(amount)?.let { if (it < 0) -it else it }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!income, { income = false }, { Text("Spent") })
                    FilterChip(income, { income = true }, { Text("Received") })
                }
                OutlinedTextField(amount, { amount = it }, singleLine = true, placeholder = { Text("Amount, e.g. 12.50") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, { category = it }, singleLine = true, placeholder = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(defaultCategories) { c -> FilterChip(category == c, { category = c }, { Text(c) }) }
                }
                OutlinedTextField(note, { note = it }, singleLine = true, placeholder = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { minor?.let { onSave(if (income) it else -it, category.trim(), note.trim()) } }, enabled = minor != null && minor > 0) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
