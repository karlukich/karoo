package com.carlos.karoopantallas

import android.content.Context
import android.widget.RemoteViews
import com.carlos.karoopantallas.data.Fmt
import com.carlos.karoopantallas.data.Snapshot
import com.carlos.karoopantallas.data.consumerFlow
import com.carlos.karoopantallas.data.dataPoints
import com.carlos.karoopantallas.data.streamDataFlow
import com.carlos.karoopantallas.render.Palette
import com.carlos.karoopantallas.render.RutaData
import com.carlos.karoopantallas.render.RutaRenderer
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.DataType.Type
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.UserProfile
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Pantalla 1B (oscura) o 1C (clara): un campo gráfico pensado para ocupar una página entera.
 * Se redibuja una vez por segundo, que es el máximo que admite el Karoo para vistas de extensiones.
 */
@OptIn(FlowPreview::class)
class RutaScreen(
    private val karoo: KarooSystemService,
    extension: String,
    typeId: String,
    private val palette: Palette,
) : DataTypeImpl(extension, typeId) {

    private val dataTypes = listOf(
        Type.SPEED, Type.AVERAGE_SPEED,
        Type.SMOOTHED_3S_AVERAGE_POWER, Type.NORMALIZED_POWER,
        Type.HEART_RATE, Type.ELEVATION_GRADE,
        Type.DISTANCE, Type.ELEVATION_GAIN,
    )

    /** Un campo gráfico también necesita un valor numérico; emitimos el tiempo para que figure como activo. */
    override fun startStream(emitter: Emitter<StreamState>) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            emitter.onNext(StreamState.Streaming(DataPoint(dataTypeId, mapOf(DataType.Field.SINGLE to 0.0))))
            karoo.streamDataFlow(Type.ELAPSED_TIME).collect { state ->
                val v = (state as? StreamState.Streaming)?.dataPoint?.singleValue ?: 0.0
                emitter.onNext(StreamState.Streaming(DataPoint(dataTypeId, mapOf(DataType.Field.SINGLE to v))))
            }
        }
        emitter.setCancellable { job.cancel() }
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        // Ocupamos todo el campo, sin la cabecera estándar del Karoo
        emitter.onNext(UpdateGraphicConfig(showHeader = false))

        val renderer = RutaRenderer(context)
        val (w, h) = config.viewSize

        val job = CoroutineScope(Dispatchers.IO).launch {
            val profile: Flow<UserProfile?> = karoo.consumerFlow<UserProfile>()
                .map<UserProfile, UserProfile?> { it }
                .onStart { emit(null) }
            val points = karoo.dataPoints(dataTypes).onStart { emit(emptyMap()) }

            combine(points, profile) { p, prof -> Snapshot(p, prof, null, null) }
                .sample(1000)
                .collect { snap ->
                    try {
                        val bmp = renderer.render(w, h, toData(snap), palette)
                        val views = RemoteViews(context.packageName, R.layout.pantalla)
                        views.setImageViewBitmap(R.id.imagen, bmp)
                        emitter.updateView(views)
                    } catch (e: Exception) {
                        Timber.e(e, "Error dibujando $typeId")
                    }
                }
        }
        emitter.setCancellable { job.cancel() }
    }

    private fun toData(s: Snapshot): RutaData {
        val power = s.v(Type.SMOOTHED_3S_AVERAGE_POWER)
        val hr = s.v(Type.HEART_RATE)
        val grade = s.v(Type.ELEVATION_GRADE)
        return RutaData(
            speed = Fmt.dec1(s.speed(s.v(Type.SPEED))),
            speedUnit = s.speedUnit,
            avgSpeed = Fmt.dec1(s.speed(s.v(Type.AVERAGE_SPEED))),
            power = Fmt.int(power),
            powerZone = s.powerZone(power),
            powerZoneCount = s.powerZoneCount,
            normPower = Fmt.int(s.v(Type.NORMALIZED_POWER)),
            hr = Fmt.int(hr),
            hrZone = s.hrZone(hr),
            hrZoneCount = s.hrZoneCount,
            grade = grade,
            gradeText = Fmt.dec1(grade),
            distance = Fmt.dist(s.dist(s.v(Type.DISTANCE))),
            distUnit = s.distUnit,
            elevation = Fmt.int(s.elev(s.v(Type.ELEVATION_GAIN))),
            elevUnit = s.elevUnit,
        )
    }
}
