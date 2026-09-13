package com.altomedia.mineplus.ui.screens

import androidx.compose.runtime.Composable

/** PRIVACY — what MinePlus collects and how it protects user data. */
@Composable
fun PrivacyScreen() {
    LegalPage(
        title = "Privacy Policy",
        sections = listOf(
            "Data stored on your device" to
                "MinePlus stores your mining configuration locally on your " +
                "device using Android DataStore. Your NiceHash wallet address " +
                "and password are encrypted before being stored, using keys " +
                "held in the Android Keystore. They never leave the device " +
                "except as part of the mining protocol itself.",
            "What we do not collect" to
                "MinePlus does not collect analytics, crash reports, personal " +
                "identifiers, contacts, location or browsing data. There is no " +
                "telemetry and no advertising SDK.",
            "Network activity" to
                "When you start mining, MinePlus connects directly to the " +
                "Stratum server you configured (by default NiceHash). The server " +
                "receives your worker login (wallet address + worker name) as " +
                "required by the mining protocol and the shares you submit. " +
                "This behavior is inherent to mining and is not under our control.",
            "Notification access" to
                "Event notifications are shown only to keep you informed about " +
                "mining status. You can disable notifications at any time in " +
                "Settings → Application → Notifications.",
            "Your choices" to
                "You can stop mining, disable auto-start, or completely delete " +
                "the app (which erases all stored configuration) at any time."
        )
    )
}