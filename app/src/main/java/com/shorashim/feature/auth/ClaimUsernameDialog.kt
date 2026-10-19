package com.shorashim.feature.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.sp
import com.shorashim.R
import com.shorashim.core.common.AuthValidator
import com.shorashim.core.ui.component.ShorashimDialog

@Composable
fun ClaimUsernameDialog(
    isOpen: Boolean,
    onConfirm: (username: String) -> Unit,
) {
    if (!isOpen) return

    var username by remember { mutableStateOf("") }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    ShorashimDialog(
        onDismissRequest = { /* Force choosing username */ },
        icon = Icons.Outlined.AlternateEmail,
        title = stringResource(R.string.auth_claim_username_title),
        subtitle = stringResource(R.string.auth_claim_username_desc),
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
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    text = stringResource(R.string.auth_claim_username_button),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorRes = null
                },
                label = { Text(stringResource(R.string.auth_username_label)) },
                placeholder = { Text(stringResource(R.string.auth_username_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.AlternateEmail,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                isError = errorRes != null,
                supportingText = errorRes?.let { { Text(stringResource(it)) } },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
