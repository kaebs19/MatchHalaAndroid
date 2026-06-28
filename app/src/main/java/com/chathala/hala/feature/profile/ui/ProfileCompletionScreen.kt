package com.chathala.hala.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chathala.hala.feature.profile.ui.components.AvatarPicker
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.R
import com.chathala.hala.core.data.Country
import com.chathala.hala.core.util.Validators
import com.chathala.hala.feature.profile.ui.components.BirthDateField
import com.chathala.hala.feature.profile.ui.components.CountryField
import com.chathala.hala.feature.profile.ui.components.GenderSelector
import com.chathala.hala.feature.profile.ui.components.InterestsChipGrid
import com.chathala.hala.ui.components.AuthScaffold
import com.chathala.hala.ui.components.FormError
import com.chathala.hala.ui.components.HalaPrimaryButton
import com.chathala.hala.ui.components.TextLink
import java.util.Date

@Composable
fun ProfileCompletionScreen(
    onSkip: () -> Unit,
    onDone: () -> Unit,
    viewModel: ProfileCompletionViewModel = viewModel(factory = ProfileCompletionViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedAvatar by remember { mutableStateOf<String?>(null) }
    var uploadedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var birthDate by remember { mutableStateOf<Date?>(null) }
    var gender by remember { mutableStateOf<String?>(null) }
    var country by remember { mutableStateOf<Country?>(null) }
    var interests by remember { mutableStateOf(setOf<String>()) }

    var birthError by remember { mutableStateOf<String?>(null) }
    var genderError by remember { mutableStateOf<String?>(null) }
    var countryError by remember { mutableStateOf<String?>(null) }
    var interestsError by remember { mutableStateOf<String?>(null) }

    val errGender = stringResource(R.string.err_gender_required)
    val errCountry = stringResource(R.string.err_country_required)
    val errInterests = stringResource(R.string.err_interests_required)

    LaunchedEffect(state.saved) {
        if (state.saved) {
            onDone()
            viewModel.clearSaved()
        }
    }

    AuthScaffold(
        title = stringResource(R.string.profile_title),
        subtitle = stringResource(R.string.profile_subtitle)
    ) {
        SectionLabel(stringResource(R.string.avatar_section_title))
        Text(
            text = stringResource(R.string.avatar_section_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        AvatarPicker(
            selectedAvatar = selectedAvatar,
            uploadedUri = uploadedUri,
            onSelectAvatar = { selectedAvatar = it; uploadedUri = null },
            onUploadImage = { uploadedUri = it; selectedAvatar = null }
        )

        Spacer(Modifier.height(18.dp))
        SectionLabel(stringResource(R.string.field_birthdate))
        BirthDateField(
            selected = birthDate,
            onSelect = { birthDate = it; birthError = null },
            isError = birthError != null,
            errorMessage = birthError
        )

        Spacer(Modifier.height(18.dp))
        SectionLabel(stringResource(R.string.field_gender))
        GenderSelector(
            selected = gender,
            onSelect = { gender = it; genderError = null }
        )
        if (genderError != null) FormError(genderError)

        Spacer(Modifier.height(18.dp))
        SectionLabel(stringResource(R.string.field_country))
        CountryField(
            selected = country,
            onSelect = { country = it; countryError = null },
            isError = countryError != null,
            errorMessage = countryError
        )

        Spacer(Modifier.height(18.dp))
        SectionLabel(stringResource(R.string.field_interests))
        Text(
            text = stringResource(R.string.interests_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        when {
            state.interestsLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp
                    )
                }
            }
            state.interestsError != null -> {
                com.chathala.hala.ui.components.ErrorState(
                    message = state.interestsError ?: "حدث خطأ",
                    onRetry = { viewModel.loadInterests() }
                )
            }
            else -> {
                InterestsChipGrid(
                    items = state.interests,
                    selectedKeys = interests,
                    onToggle = { key ->
                        interests = if (key in interests) interests - key
                        else interests + key
                        interestsError = null
                    }
                )
                if (interestsError != null) FormError(interestsError)
            }
        }

        FormError(state.saveError)

        Spacer(Modifier.height(32.dp))

        HalaPrimaryButton(
            text = stringResource(R.string.btn_save_continue),
            loading = state.saving,
            onClick = {
                birthError = Validators.birthDate(birthDate)
                genderError = if (gender == null) errGender else null
                countryError = if (country == null) errCountry else null
                interestsError = if (interests.isEmpty()) errInterests else null

                if (birthError == null && genderError == null && countryError == null && interestsError == null) {
                    viewModel.save(
                        context = context,
                        birthDate = birthDate!!,
                        gender = gender!!,
                        country = country!!.code,
                        interestKeys = interests.toList(),
                        defaultAvatar = selectedAvatar,
                        uploadUri = uploadedUri
                    )
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextLink(
                text = stringResource(R.string.btn_skip),
                onClick = onSkip
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}
