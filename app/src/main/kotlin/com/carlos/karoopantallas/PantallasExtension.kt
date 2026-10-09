package com.carlos.karoopantallas

import com.carlos.karoopantallas.render.Palette
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import timber.log.Timber

/**
 * Servicio que el Karoo descubre gracias al intent-filter del AndroidManifest.
 * Registra las pantallas 1B (oscura) y 1C (clara) como campos de datos gráficos.
 * El id "pantallas" y los typeId deben coincidir con res/xml/extension_info.xml.
 */
class PantallasExtension : KarooExtension("pantallas", "1.1") {

    private val karooSystem by lazy { KarooSystemService(applicationContext) }

    override val types by lazy {
        listOf(
            RutaScreen(karooSystem, extension, "ruta-oscura", Palette.DARK),
            RutaScreen(karooSystem, extension, "ruta-clara", Palette.LIGHT),
        )
    }

    override fun onCreate() {
        super.onCreate()
        if (Timber.treeCount == 0) Timber.plant(Timber.DebugTree())
        karooSystem.connect { connected ->
            Timber.i("Karoo Pantallas conectado: $connected")
        }
    }

    override fun onDestroy() {
        karooSystem.disconnect()
        super.onDestroy()
    }
}
