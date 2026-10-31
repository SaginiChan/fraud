package com.jubileegroup.jbuzz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jubileegroup.jbuzz.ui.theme.JBuzzTheme

/** The parts of the notification that can be edited before it is shown. */
internal data class NotificationFields(val amount: String, val time: String, val paidTo: String)

@Composable
internal fun NotificationDemoScreen(onShowNotification: (NotificationFields) -> Unit) {
    var amount by rememberSaveable { mutableStateOf(ReferenceAmount) }
    var time by rememberSaveable { mutableStateOf(ReferenceTime) }
    var paidTo by rememberSaveable { mutableStateOf(ReferencePaidTo) }
    val focusManager = LocalFocusManager.current

    // The Surface paints the theme's dark background behind the system bars and makes the text light.
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding() // keeps the fields and button clear of the status bar and keyboard
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Edit the notification, then tap Show Notification",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (orange text)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().testTag("field_amount")
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = time,
                onValueChange = { time = it },
                label = { Text("Time") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().testTag("field_time")
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = paidTo,
                onValueChange = { paidTo = it },
                label = { Text("Paid to") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                // Done only closes the keyboard; the button below is what shows the notification.
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth().testTag("field_paid_to")
            )
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    onShowNotification(NotificationFields(amount = amount, time = time, paidTo = paidTo))
                },
                modifier = Modifier.testTag("show_button")
            ) {
                Text(text = "Show Notification")
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun NotificationDemoScreenPreview() {
    JBuzzTheme {
        NotificationDemoScreen(onShowNotification = {})
    }
}

