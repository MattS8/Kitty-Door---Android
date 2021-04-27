package com.ms8.kittydoor

import android.util.Log
import androidx.databinding.ObservableField
import kotlin.properties.ObservableProperty

object AppState {
    val kittyDoorData = KittyDoorData()
    val appData = AppData()

    data class KittyDoorData (
        val status: ObservableField<DoorStatus?> = ObservableField(),
        val previousStatus: ObservableField<DoorStatus?> = ObservableField(),
        val hwOverrideMode: ObservableField<Int?> = ObservableField(),
        val lightLevel: ObservableField<Int?> = ObservableField(),
        val optionsData: ObservableField<KittyOptions?> = ObservableField(),
        val overrideAuto: ObservableField<Boolean> = ObservableField(false)
    )

    data class KittyOptions(
        var openLightLevel: Int = 0,
        var closeLightLevel: Int = 0,
        var delayOpening: Boolean = true,
        var delayClosing: Boolean = true,
        var delayOpeningVal: Long = 120,
        var delayClosingVal: Long = 120,
        var o_timestamp: String = "",
        var command: String = "_none_",
        var overrideAuto: Boolean = false
    )

    data class AppData (
            var appInForeground: Boolean = false,
            var drawerOpen: Boolean = false
    )

    enum class DoorStatus {CLOSED, CLOSING, OPEN, OPENING, PAUSED}
    fun colorFromStatus(garageStatus: DoorStatus): Int {
        return when (garageStatus) {
            DoorStatus.CLOSED -> R.color.colorClose
            DoorStatus.CLOSING -> R.color.colorClosing
            DoorStatus.OPEN -> R.color.colorOpen
            DoorStatus.OPENING -> R.color.colorOpening
            DoorStatus.PAUSED -> R.color.colorPaused
        }
    }

    fun statusFromString(status: String): DoorStatus {
        return when (status) {
            DoorStatus.CLOSED.name -> DoorStatus.CLOSED
            DoorStatus.CLOSING.name -> DoorStatus.CLOSING
            DoorStatus.OPEN.name -> DoorStatus.OPEN
            DoorStatus.OPENING.name -> DoorStatus.OPENING
            DoorStatus.PAUSED.name -> DoorStatus.PAUSED
            else ->
            {
                Log.e(TAG, "statusFromString - not a valid string: $status")
                DoorStatus.CLOSED
            }
        }
    }

    const val TAG = "AppState"
}