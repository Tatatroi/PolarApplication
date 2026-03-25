package com.application.polarapplication.ui.theme.profile

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProfileManager(context: Context) {
    private val prefs = context.getSharedPreferences("polar_user_profile", Context.MODE_PRIVATE)

    private val _age = MutableStateFlow(prefs.getInt("age", 24))
    val age: StateFlow<Int> = _age

    private val _weight = MutableStateFlow(prefs.getFloat("weight", 75f))
    val weight: StateFlow<Float> = _weight

    private val _height = MutableStateFlow(prefs.getInt("height", 180))
    val height: StateFlow<Int> = _height

    private val _gender = MutableStateFlow(prefs.getString("gender", "Masculin") ?: "Masculin")
    val gender: StateFlow<String> = _gender

    private val _rhr = MutableStateFlow(prefs.getInt("rhr", 55))
    val rhr: StateFlow<Int> = _rhr

    private val _customHrMax = MutableStateFlow(
        if (prefs.contains("customHrMax")) prefs.getInt("customHrMax", 190) else null
    )
    val customHrMax: StateFlow<Int?> = _customHrMax

    private val _profileImageUri = MutableStateFlow(prefs.getString("profileImageUri", null))
    val profileImageUri: StateFlow<String?> = _profileImageUri

    private val _competitionDateMillis = MutableStateFlow(
        if (prefs.contains("competitionDate")) prefs.getLong("competitionDate", -1L) else null
    )
    val competitionDateMillis: StateFlow<Long?> = _competitionDateMillis

    private val _planStartDateMillis = MutableStateFlow(
        if (prefs.contains("planStartDate")) prefs.getLong("planStartDate", -1L) else null
    )
    val planStartDateMillis: StateFlow<Long?> = _planStartDateMillis

    fun saveProfile(
        newAge: Int,
        newWeight: Float,
        newHeight: Int,
        newGender: String,
        newRhr: Int,
        newCustomHrMax: Int?,
        newProfileImageUri: String?,
        newCompetitionDateMillis: Long? = _competitionDateMillis.value,
        newPlanStartDateMillis: Long?   = _planStartDateMillis.value
    ) {
        val editor = prefs.edit()

        editor.putInt("age", newAge)
        editor.putFloat("weight", newWeight)
        editor.putInt("height", newHeight)
        editor.putString("gender", newGender)
        editor.putInt("rhr", newRhr)

        if (newCustomHrMax != null) editor.putInt("customHrMax", newCustomHrMax)
        else editor.remove("customHrMax")

        if (newProfileImageUri != null) editor.putString("profileImageUri", newProfileImageUri)
        else editor.remove("profileImageUri")

        if (newCompetitionDateMillis != null) editor.putLong("competitionDate", newCompetitionDateMillis)
        else editor.remove("competitionDate")

        if (newPlanStartDateMillis != null) editor.putLong("planStartDate", newPlanStartDateMillis)
        else editor.remove("planStartDate")

        editor.apply()

        _age.value                    = newAge
        _weight.value                 = newWeight
        _height.value                 = newHeight
        _gender.value                 = newGender
        _rhr.value                    = newRhr
        _customHrMax.value            = newCustomHrMax
        _profileImageUri.value        = newProfileImageUri
        _competitionDateMillis.value  = newCompetitionDateMillis
        _planStartDateMillis.value    = newPlanStartDateMillis
    }
}