package com.ms8.kittydoor

import android.util.Log
import androidx.databinding.ObservableField
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

object FirebaseDBF {
    // Listeners
    private var kittyDoorListener: KittyDoorListener? = null
    private class KittyDoorListener : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            try {
                val newStatus = snapshot.child("type").value as String
                AppState.kittyDoorData.previousStatus.set(AppState.kittyDoorData.status.get())
                AppState.kittyDoorData.status.set(AppState.statusFromString(newStatus))
            } catch (e: Exception) {
                Log.e(TAG, "kittyDoorListener - $e")
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.w(TAG, "KittyDoorListener - onCancelled")
            kittyDoorListener = null
        }
    }
    private var lightLevelListener: LightLevelListener? = null
    private class LightLevelListener : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            try {
                val newLevel = snapshot.child("level").value as Number
                AppState.kittyDoorData.lightLevel.set(newLevel.toInt())
            } catch (e: Exception) {
                Log.e(TAG, "LightLevelListener - $e")
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.w(TAG, "LightLevelListener - onCancelled")
            lightLevelListener = null
        }
    }
    private var hwOverrideListener: HardwareOverrideListener? = null
    private class HardwareOverrideListener : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            try {
                val newMode = snapshot.child("type").value as Number
                AppState.kittyDoorData.hwOverrideMode.set(newMode.toInt())
            } catch (e: Exception) {
                Log.e(TAG, "HardwareOverrideListener - $e")
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.w(TAG, "HardwareOverrideListener - onCancelled")
            hwOverrideListener = null
        }
    }
    private var doorOptionsListener: DoorOptionsListener? = null
    private class DoorOptionsListener : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            try {
                val snapshotValues = snapshot.value as Map<String, Any?>
                val newOptions = AppState.KittyOptions()
                snapshotValues["openLightLevel"]?.also {
                    newOptions.openLightLevel = (it as Number).toInt()
                }
                snapshotValues["closeLightLevel"]?.also {
                    newOptions.closeLightLevel = (it as Number).toInt()
                }
                snapshotValues["delayOpening"]?.also {
                    newOptions.delayOpening = (it as Boolean)
                }
                snapshotValues["delayClosing"]?.also {
                    newOptions.delayClosing = (it as Boolean)
                }
                snapshotValues["delayOpeningVal"]?.also {
                    newOptions.delayOpeningVal = (it as Number).toLong()
                }
                snapshotValues["delayClosingVal"]?.also {
                    newOptions.delayClosingVal = (it as Number).toLong()
                }
                AppState.kittyDoorData.optionsData.set(newOptions)
            } catch (e: Exception) {
                Log.e(TAG, "kittyDoorListener - $e")
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.w(TAG, "DoorOptionsListener - onCancelled")
            doorOptionsListener = null
        }
    }

    // Listener Hookups
    fun listenToAllBackendData() {
        listenForKittyDoorStatus()
        listenForOptionChanges()
        listenForLightLevelChanges()
        listenForHWOverride()
    }

    fun listenForKittyDoorStatus() {
        val database = FirebaseDatabase.getInstance()

        if (kittyDoorListener == null) {
            Log.d(TAG, "first time listening for kitty door status!")
            kittyDoorListener = KittyDoorListener()
            kittyDoorListener?.let {
                database.reference
                    .child(STATUS)
                    .child(KITTY_DOOR)
                    .addValueEventListener(it)
            }
        }
    }

    fun listenForOptionChanges() {
        val database = FirebaseDatabase.getInstance()

        if (doorOptionsListener == null) {
            doorOptionsListener = DoorOptionsListener()
            doorOptionsListener?.let {
                database.reference
                    .child(SYSTEMS)
                    .child(KITTY_DOOR)
                    .addValueEventListener(it)
            }
        }
    }

    private fun listenForLightLevelChanges() {
        val database = FirebaseDatabase.getInstance()

        if (lightLevelListener == null) {
            lightLevelListener = LightLevelListener()
            lightLevelListener?.let {
                database.reference
                    .child(STATUS)
                    .child(LIGHT_LEVEL)
                    .addValueEventListener(it)
            }
        }
    }

    private fun listenForHWOverride() {
        val database = FirebaseDatabase.getInstance()

        if (hwOverrideListener == null) {
            hwOverrideListener = HardwareOverrideListener()
            hwOverrideListener?.let {
                database.reference
                    .child(STATUS)
                    .child(HW_OVERRIDE)
                    .addValueEventListener(it)
            }
        }
    }
    //

private data class KittyDoorAction (val type: String, val a_timestamp: String)
    // Send Commands
    fun sendOptions(options: AppState.KittyOptions) {
        options.o_timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Calendar.getInstance().time)
        FirebaseDatabase.getInstance().reference
            .child(SYSTEMS)
            .child(KITTY_DOOR)
            .setValue(options)
    }

    fun checkLightLevel() {
        FirebaseDatabase.getInstance().reference
            .child(COMMANDS)
            .child(KITTY_DOOR)
            .setValue(KittyDoorAction(
                "readLightLevel",
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Calendar.getInstance().time)
            ))
    }

    fun openKittyDoor() {
        FirebaseDatabase.getInstance().reference
            .child(COMMANDS)
            .child(KITTY_DOOR)
            .setValue(KittyDoorAction(
                "openKittyDoor",
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Calendar.getInstance().time)
            ))
    }

    fun closeKittyDoor() {
        FirebaseDatabase.getInstance().reference
            .child(COMMANDS)
            .child(KITTY_DOOR)
            .setValue(KittyDoorAction(
                "closeKittyDoor",
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Calendar.getInstance().time)
            ))
    }
    //

    fun addTokenToGarage(token: String) {
        val database = FirebaseDatabase.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        database.reference
            .child(DEVICE_TOKENS)
            .child(ALL_TOKENS)
            .child(KITTY_DOOR)
            .child(token)
            .setValue(uid)
    }

    // Constant Strings
    private const val TAG = "FirebaseDBF"
    private const val SYSTEMS = "systems"
    private const val COMMANDS = "commands"
    private const val KITTY_DOOR = "kitty_door"
    private const val LIGHT_LEVEL = "kitty_door_light_level"
    private const val HW_OVERRIDE = "kitty_door_hw_override"
    private const val DEVICE_TOKENS = "device_tokens"
    private const val ALL_TOKENS = "all_tokens"
    private const val STATUS = "status"
}