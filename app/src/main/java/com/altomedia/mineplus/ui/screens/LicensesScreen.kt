package com.altomedia.mineplus.ui.screens

import androidx.compose.runtime.Composable

/**
 * Open Source Licenses — lists every third-party library and the native
 * mining engine bundled into MinePlus with its license.
 */
@Composable
fun LicensesScreen() {
    LegalPage(
        title = "Open Source Licenses",
        sections = listOf(
            "X11 Mining Engine (sphlib)" to
                "MinePlus bundles a native X11 mining engine derived from the " +
                "public-domain sphlib library (implementations of the SHS family " +
                "by Paulo S. L. M. Barreto and the X11 hashing chain used by " +
                "Dash). sphlib is released to the public domain. " +
                "No license fee or attribution is required for public-domain code.",
            "Android Jetpack (Compose, Lifecycle, Navigation, DataStore)" to
                "Licensed under the Apache License, Version 2.0 (the \"License\"). " +
                "You may obtain a copy of the License at " +
                "https://www.apache.org/licenses/LICENSE-2.0",
            "Dagger / Hilt (dependency injection)" to
                "Copyright (c) The Dagger Authors. Licensed under the Apache " +
                "License, Version 2.0.",
            "Gson" to
                "Copyright 2008 Google Inc. Licensed under the Apache License, " +
                "Version 2.0.",
            "Kotlin Coroutines / kotlinx" to
                "Copyright (c) 2018 JetBrains s.r.o. Licensed under the Apache " +
                "License, Version 2.0.",
            "Kotlin Standard Library" to
                "Copyright (c) 2010-2026 JetBrains s.r.o. and Kotlin Programming " +
                "Language contributors. Licensed under the Apache License, " +
                "Version 2.0.",
            "cpuminer-multi (build recipe)" to
                "The native build recipe references cpuminer-multi, which is " +
                "distributed under the GNU General Public License version 2 (GPL-2.0). " +
                "Any GPL-licensed binary produced from that recipe is distributed " +
                "in source form alongside MinePlus so that its license obligations " +
                "are satisfied. See native/ for the recipe and sources."
        )
    )
}