package com.shorashim.feature.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shorashim.R
import com.shorashim.core.common.AuthValidator

@Composable
fun ClaimUsernameDialog(
    isOpen: Boolean,
    onConfirm: (username: String) -> Unit,
) {
    if (!isOpen) return

    var username by remember { mutableStateOf("") }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = { /* Force choosing username */ },
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(R.string.auth_claim_username_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.auth_claim_username_desc),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        errorRes = null
                    },
                    label = { Text(stringResource(R.string.auth_username_label)) },
                    placeholder = { Text(stringResource(R.string.auth_username_hint)) },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Outlined.AlternateEmail,
                            contentDescription = null,
                        )
                    },
                    isError = errorRes != null,
                    supportingText = errorRes?.let { { Text(stringResource(it)) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val err = AuthValidator.validateUsername(username)
                    if (err != null) {
                        errorRes = err
                    } else {
                        onConfirm(username.trim().removePrefix("@"))
                    }
                },
                enabled = username.isNotBlank(),
            ) {
                Text(stringResource(R.string.auth_claim_username_button))
            }
        },
    )
}
