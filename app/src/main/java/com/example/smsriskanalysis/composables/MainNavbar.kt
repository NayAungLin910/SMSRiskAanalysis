package com.example.smsriskanalysis.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun MainNavbar(
    isSelectionMode: Boolean = false,
    onSelectCLick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            TextButton(onClick = onDoneClick) {
                Text(
                    text = "Done",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            TextButton(onClick = { expanded = true }) {
                Text(
                    text = "Edit",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            DropdownMenu(
                expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Select") },
                    onClick= {
                        expanded = false
                        onSelectCLick()
                    }
                )
            }
        }
    }
}