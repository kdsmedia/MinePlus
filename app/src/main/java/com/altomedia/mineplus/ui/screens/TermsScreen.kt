package com.altomedia.mineplus.ui.screens

import androidx.compose.runtime.Composable

/** TERMS — terms of use for the MinePlus application. */
@Composable
fun TermsScreen() {
    LegalPage(
        title = "Terms of Use",
        sections = listOf(
            "Acceptance of terms" to
                "By installing and using MinePlus, you agree to these terms. " +
                "If you do not agree, please uninstall the application.",
            "Use of the service" to
                "MinePlus is provided as an X11 mining controller that connects " +
                "to the mining pool (NiceHash Stratum) you configure. You are " +
                "responsible for complying with the pool's terms of service and " +
                "applicable laws in your jurisdiction.",
            "No warranty" to
                "The application is provided \"as is\" without warranty of any " +
                "kind, express or implied, including but not limited to " +
                "merchantability and fitness for a particular purpose.",
            "Limitation of liability" to
                "ALTOMEDIA shall not be liable for any direct, indirect, " +
                "incidental or consequential damages arising from the use of, " +
                "or inability to use, this application or from mining losses, " +
                "device damage, or power consumption.",
            "Mining responsibilities" to
                "Mining consumes significant CPU/battery resources. You are " +
                "responsible for device temperature, battery health and " +
                "electricity costs. Use the Device protection settings to limit " +
                "risks.",
            "Changes to these terms" to
                "We may update these terms from time to time. Continued use of " +
                "the application after changes constitutes acceptance of the " +
                "updated terms.",
            "Contact" to
                "For questions about these terms, contact ALTOMEDIA through the " +
                "GitHub repository hosting MinePlus."
        )
    )
}