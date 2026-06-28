package com.chathala.hala.feature.discover.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chathala.hala.R
import com.chathala.hala.core.data.Countries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFiltersSheet(
    initial: SearchFilters,
    onApply: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var gender by remember { mutableStateOf(initial.gender) }
    var country by remember { mutableStateOf(initial.country) }
    var ageRange by remember { mutableStateOf(initial.minAge.toFloat()..initial.maxAge.toFloat()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.search_filters_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(20.dp))

            // ── الجنس ──
            FilterLabel(stringResource(R.string.search_filter_gender))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                data class Opt(val g: SearchGender, val label: String)
                listOf(
                    Opt(SearchGender.ALL, stringResource(R.string.search_gender_all)),
                    Opt(SearchGender.MALE, stringResource(R.string.search_gender_male)),
                    Opt(SearchGender.FEMALE, stringResource(R.string.search_gender_female))
                ).forEach { o ->
                    FilterChip(
                        selected = gender == o.g,
                        onClick = { gender = o.g },
                        label = { Text(o.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── الدولة ──
            FilterLabel(stringResource(R.string.search_filter_country))
            CountryDropdown(selected = country, onSelect = { country = it })

            Spacer(Modifier.height(20.dp))

            // ── العمر ──
            val from = ageRange.start.toInt()
            val to = ageRange.endInclusive.toInt()
            FilterLabel("${stringResource(R.string.search_filter_age)}: $from - $to")
            RangeSlider(
                value = ageRange,
                onValueChange = { ageRange = it },
                valueRange = AGE_MIN.toFloat()..AGE_MAX.toFloat(),
                steps = (AGE_MAX - AGE_MIN) - 1
            )

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        gender = SearchGender.ALL
                        country = null
                        ageRange = AGE_MIN.toFloat()..AGE_MAX.toFloat()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.search_filters_reset)) }

                Button(
                    onClick = {
                        onApply(
                            SearchFilters(
                                gender = gender,
                                country = country,
                                minAge = ageRange.start.toInt(),
                                maxAge = ageRange.endInclusive.toInt()
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.search_filters_apply)) }
            }
        }
    }
}

@Composable
private fun FilterLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun CountryDropdown(selected: String?, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCountry = Countries.byCode(selected)
    val label = if (selectedCountry != null)
        "${selectedCountry.flag} ${selectedCountry.nameAr}"
    else
        stringResource(R.string.search_gender_all)

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { expanded = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp)
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.search_gender_all)) },
                onClick = { onSelect(null); expanded = false }
            )
            Countries.list.filter { it.code != "OTHER" }.forEach { c ->
                DropdownMenuItem(
                    text = { Text("${c.flag}  ${c.nameAr}") },
                    onClick = { onSelect(c.code); expanded = false }
                )
            }
        }
    }
}
