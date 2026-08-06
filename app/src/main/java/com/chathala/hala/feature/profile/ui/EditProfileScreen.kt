package com.chathala.hala.feature.profile.ui

import com.chathala.hala.core.i18n.S

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chathala.hala.core.util.showToast
import kotlinx.coroutines.launch
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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
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
import com.chathala.hala.ui.components.HalaAsyncImage

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProfileViewModel = viewModel(factory = EditProfileViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // اختيار صورة جديدة للملف الشخصي
    val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val part = com.chathala.hala.core.util.MediaUploadHelper.uriToImagePart(
                    context = context,
                    uri = uri,
                    fieldName = "profileImage"
                )
                if (part != null) viewModel.uploadPhoto(part)
                else context.showToast(S.get(R.string.profile_photo_read_failed))
            }
        }
    }
    val pickPhoto: () -> Unit = {
        photoPicker.launch(
            androidx.activity.result.PickVisualMediaRequest(
                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

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
            title = { Text(S.get(R.string.edit_profile_discard_title)) },
            text = { Text(S.get(R.string.edit_profile_discard_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) {
                    Text(
                        text = S.get(R.string.edit_profile_discard_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(S.get(R.string.edit_profile_discard_cancel))
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
                text = S.get(R.string.edit_profile_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 8.dp).weight(1f)
            )
            if (state.hasImage) {
                com.chathala.hala.ui.components.TextLink(
                    text = S.get(R.string.edit_profile_delete_photo),
                    onClick = { viewModel.deletePhoto() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        LaunchedEffect(Unit) {
            // رسائل تحديث/حذف الصورة — Toast بسياق صحيح (الشاشة لا تستضيف snackbar)
            viewModel.message.collect { msg -> context.showToast(msg) }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // صورة الملف الشخصي — تغيير مباشر من هنا
            EditablePhoto(
                imageUrl = state.imageUrl,
                name = state.name,
                uploading = state.uploading,
                onPick = pickPhoto
            )

            // تحذير المحتوى — بارز قبل الحقول
            ContentWarningCard()

            HalaTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = S.get(R.string.field_name),
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) }
            )

            HalaTextField(
                value = state.bio,
                onValueChange = viewModel::setBio,
                label = S.get(R.string.profile_section_bio),
                leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null) },
                singleLine = false
            )

            SectionLabel(S.get(R.string.field_gender))
            GenderSelector(
                selected = state.gender,
                onSelect = viewModel::setGender
            )

            SectionLabel(S.get(R.string.field_birthdate))
            BirthDateField(
                selected = state.birthDate,
                onSelect = viewModel::setBirthDate
            )

            SectionLabel(S.get(R.string.field_country))
            CountryField(
                selected = state.country?.let { code -> Countries.list.firstOrNull { it.code == code } },
                onSelect = { c: Country -> viewModel.setCountry(c.code) }
            )

            SectionLabel(S.get(R.string.field_interests))
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
                text = S.get(R.string.edit_profile_save),
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

/** صورة الملف الشخصي مع إمكانية تغييرها بالنقر. */
@Composable
private fun EditablePhoto(
    imageUrl: String?,
    name: String?,
    uploading: Boolean,
    onPick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                    .clickable(enabled = !uploading, onClick = onPick),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uploading -> CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp)
                    )
                    else -> HalaAsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        fallbackName = name,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            // شارة الكاميرا
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                    .clickable(enabled = !uploading, onClick = onPick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = S.get(R.string.profile_change_photo),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = S.get(R.string.profile_tap_change_photo),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** تحذير بارز بشأن المحتوى المخالف (صور/أسماء/نبذة). */
@Composable
private fun ContentWarningCard() {
    val warn = Color(0xFFE53935)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(warn.copy(alpha = 0.10f))
            .border(1.dp, warn.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(warn.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = warn,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = S.get(R.string.profile_warning_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = warn
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = S.get(R.string.profile_warning_body) +
                    S.get(R.string.profile_warning_tail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )
        }
    }
}
