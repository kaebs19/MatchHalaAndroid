package com.chathala.hala.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.R
import com.chathala.hala.core.data.Countries
import com.chathala.hala.core.data.Country
import com.chathala.hala.feature.profile.ui.components.BirthDateField
import com.chathala.hala.feature.profile.ui.components.CountryField
import com.chathala.hala.feature.profile.ui.components.GenderSelector
import com.chathala.hala.feature.profile.ui.components.InterestsChipGrid
import com.chathala.hala.ui.components.FormError
import com.chathala.hala.ui.components.HalaPrimaryButton
import com.chathala.hala.ui.components.HalaTextField

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProfileViewModel = viewModel(factory = EditProfileViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            onSaved()
            viewModel.clearSaved()
        }
    }

    val handleBack: () -> Unit = {
        if (state.hasUnsavedChanges) showDiscardDialog = true else onBack()
    }
    BackHandler(enabled = true, onBack = handleBack)

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.edit_profile_discard_title)) },
            text = { Text(stringResource(R.string.edit_profile_discard_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) {
                    Text(
                        text = stringResource(R.string.edit_profile_discard_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.edit_profile_discard_cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = handleBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = stringResource(R.string.edit_profile_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 8.dp).weight(1f)
            )
            if (state.hasImage) {
                com.chathala.hala.ui.components.TextLink(
                    text = stringResource(R.string.edit_profile_delete_photo),
                    onClick = { viewModel.deletePhoto() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        LaunchedEffect(Unit) {
            viewModel.message.collect { msg ->
                // يُعرض Toast بسيط عند تحديث/حذف الصورة
                android.widget.Toast.makeText(
                    /* NOTE: using plain Toast here since EditProfile doesn't host a snackbar */
                    null, msg, android.widget.Toast.LENGTH_SHORT
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HalaTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = stringResource(R.string.field_name),
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) }
            )

            HalaTextField(
                value = state.bio,
                onValueChange = viewModel::setBio,
                label = stringResource(R.string.profile_section_bio),
                leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null) },
                singleLine = false
            )

            SectionLabel(stringResource(R.string.field_gender))
            GenderSelector(
                selected = state.gender,
                onSelect = viewModel::setGender
            )

            SectionLabel(stringResource(R.string.field_birthdate))
            BirthDateField(
                selected = state.birthDate,
                onSelect = viewModel::setBirthDate
            )

            SectionLabel(stringResource(R.string.field_country))
            CountryField(
                selected = state.country?.let { code -> Countries.list.firstOrNull { it.code == code } },
                onSelect = { c: Country -> viewModel.setCountry(c.code) }
            )

            SectionLabel(stringResource(R.string.field_interests))
            if (state.loadingInterests) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            } else {
                InterestsChipGrid(
                    items = state.availableInterests,
                    selectedKeys = state.interests,
                    onToggle = viewModel::toggleInterest
                )
            }

            FormError(state.error)

            Spacer(Modifier.height(8.dp))

            HalaPrimaryButton(
                text = stringResource(R.string.edit_profile_save),
                loading = state.saving,
                onClick = { viewModel.save() }
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onBackground
    )
}
