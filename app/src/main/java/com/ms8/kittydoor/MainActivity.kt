package com.ms8.kittydoor

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.CompoundButton
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.Observable
import androidx.drawerlayout.widget.DrawerLayout
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.andrognito.flashbar.Flashbar
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.ms8.kittydoor.AppState.DoorStatus.*
import com.ms8.kittydoor.FirebaseMessageService.Companion.NOTIFICATION_TYPE
import com.ms8.kittydoor.FirebaseMessageService.Companion.TYPE_AUTO_CLOSE_WARNING
import com.ms8.kittydoor.databinding.ActivityMainBinding
import com.ms8.kittydoor.databinding.DrawerOptionsBinding


class MainActivity : AppCompatActivity(), CompoundButton.OnCheckedChangeListener,
    SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var optionsBinding: DrawerOptionsBinding
    private lateinit var drawer : Drawer

    private var doorProgressDrawable : AnimatedVectorDrawableCompat? = null
    private var flashbar: Flashbar? = null

    private var themeStr: String = ""

    override fun onBackPressed() {
        if (drawer.isDrawerOpen) {
            drawer.closeDrawer()
        } else {
            super.onBackPressed()
            moveTaskToBack(true)
        }
    }

    override fun onResume() {
        super.onResume()
        AppState.appData.appInForeground = true

        // Listen for changes to AppState
        AppState.kittyDoorData.status.addOnPropertyChangedCallback(doorStatusListener)
        AppState.kittyDoorData.optionsData.addOnPropertyChangedCallback(doorOptionsListener)
        AppState.kittyDoorData.lightLevel.addOnPropertyChangedCallback(lightLevelListener)
        AppState.kittyDoorData.hwOverrideMode.addOnPropertyChangedCallback(hwOverrideListener)
        AppState.kittyDoorData.overrideAuto.addOnPropertyChangedCallback(overrideAutoListener)

        updateStatusUI()
        updateOptionsUI()
        updateLightLevelUI()
        updateHWOverrideUI()
        updateOverrideAutoUI()

        if (AppState.appData.drawerOpen)
            drawer.openDrawer()
        else
            drawer.closeDrawer()

        // Ensure backend listeners are running
        FirebaseDBF.listenToAllBackendData()
    }

    override fun onPause() {
        super.onPause()

        // Remove to prevent leaks
        AppState.kittyDoorData.status.removeOnPropertyChangedCallback(doorStatusListener)
        AppState.kittyDoorData.optionsData.removeOnPropertyChangedCallback(doorOptionsListener)
        AppState.kittyDoorData.lightLevel.removeOnPropertyChangedCallback(lightLevelListener)
        AppState.kittyDoorData.hwOverrideMode.removeOnPropertyChangedCallback(hwOverrideListener)
        AppState.kittyDoorData.overrideAuto.removeOnPropertyChangedCallback(overrideAutoListener)

        AppState.appData.appInForeground = false
        AppState.appData.drawerOpen = drawer.isDrawerOpen
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPrefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        //themeStr = sharedPrefs.getString(PREFS_THEME, THEME_LIGHT) ?: THEME_LIGHT
        themeStr = THEME_DARK
        setTheme(if (themeStr == THEME_DARK) R.style.Theme_KittyDoor_Dark else R.style.Theme_KittyDoor)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            btnOpen.setOnClickListener(this@MainActivity)
            btnClose.setOnClickListener(this@MainActivity)
            btnCheckLightLevel.setOnClickListener(this@MainActivity)
            btnOptions.setOnClickListener(this@MainActivity)
            btnEnableAuto.setOnClickListener(this@MainActivity)
        }

        doorProgressDrawable = AnimatedVectorDrawableCompat.create(this, R.drawable.av_progress)
        doorProgressDrawable?.registerAnimationCallback(doorProgressViewCallback)

        setupOptions()

//        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
//            if (!task.isSuccessful) {
//                Log.w(GarageWidget.TAG, "getInstanceId failed: ${task.exception}")
//                return@addOnCompleteListener
//            }
//
//            val token = task.result?.token ?: return@addOnCompleteListener
//
//            FirebaseDatabaseFunctions.addTokenToGarage(token)
//        }

        // Check if started from FCM Notification
        intent.extras?.getString(NOTIFICATION_TYPE)?.let {
            handleFCMNotification(it)
        }
    }

    private fun updateOverrideAutoUI() {
        val overrideAutoEnabled = AppState.kittyDoorData.overrideAuto.get() ?: return

        val states = arrayOf(intArrayOf(android.R.attr.state_enabled),
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(-android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_pressed))

        val colors = if(!overrideAutoEnabled)
            intArrayOf(
                ContextCompat.getColor(this@MainActivity, R.color.autoEnabled),
                ContextCompat.getColor(this@MainActivity, R.color.autoDisabled),
                ContextCompat.getColor(this@MainActivity, R.color.autoChecked),
                ContextCompat.getColor(this@MainActivity, R.color.autoPressed)
        ) else
            intArrayOf(
                    ContextCompat.getColor(this@MainActivity, R.color.autoDisabled),
                    ContextCompat.getColor(this@MainActivity, R.color.autoDisabled),
                    ContextCompat.getColor(this@MainActivity, R.color.autoDisabled),
                    ContextCompat.getColor(this@MainActivity, R.color.autoDisabledPressed)
            )

        val textColors = if (!overrideAutoEnabled)
            intArrayOf(
                    ContextCompat.getColor(this@MainActivity, R.color.autoText),
                    ContextCompat.getColor(this@MainActivity, R.color.autoTextDisabled),
                    ContextCompat.getColor(this@MainActivity, R.color.autoText),
                    ContextCompat.getColor(this@MainActivity, R.color.autoTextPressed)
            ) else
            intArrayOf(
                    ContextCompat.getColor(this@MainActivity, R.color.autoTextDisabled),
                    ContextCompat.getColor(this@MainActivity, R.color.autoTextDisabled),
                    ContextCompat.getColor(this@MainActivity, R.color.autoTextDisabled),
                    ContextCompat.getColor(this@MainActivity, R.color.autoTextDisabledPressed)
            )

        val openColors = if (overrideAutoEnabled)
            intArrayOf(
                    ContextCompat.getColor(this@MainActivity, R.color.colorOpen),
                    ContextCompat.getColor(this@MainActivity, R.color.colorOpen),
                    ContextCompat.getColor(this@MainActivity, R.color.colorOpen),
                    ContextCompat.getColor(this@MainActivity, R.color.colorOpening)
            ) else
            intArrayOf(
                    ContextCompat.getColor(this@MainActivity, R.color.colorOpenDisabled),
                    ContextCompat.getColor(this@MainActivity, R.color.colorOpenDisabled),
                    ContextCompat.getColor(this@MainActivity, R.color.colorOpenDisabled),
                    ContextCompat.getColor(this@MainActivity, R.color.colorOpenDisabledPressed)
            )
        val closeColors = if (overrideAutoEnabled)
            intArrayOf(
                    ContextCompat.getColor(this@MainActivity, R.color.colorClose),
                    ContextCompat.getColor(this@MainActivity, R.color.colorClose),
                    ContextCompat.getColor(this@MainActivity, R.color.colorClose),
                    ContextCompat.getColor(this@MainActivity, R.color.colorClosing)
            ) else
            intArrayOf(
                    ContextCompat.getColor(this@MainActivity, R.color.colorCloseDisabled),
                    ContextCompat.getColor(this@MainActivity, R.color.colorCloseDisabled),
                    ContextCompat.getColor(this@MainActivity, R.color.colorCloseDisabled),
                    ContextCompat.getColor(this@MainActivity, R.color.colorCloseDisabledPressed)
            )

        binding.btnOpen.backgroundTintList = ColorStateList(states, openColors)
        binding.btnClose.backgroundTintList = ColorStateList(states, closeColors)
        binding.btnEnableAuto.backgroundTintList = ColorStateList(states, colors)
        binding.btnEnableAuto.setTextColor(ColorStateList(states,textColors))
    }

    private fun updateHWOverrideUI() {
        val hwEnabled = AppState.kittyDoorData.hwOverrideMode.get() ?: return != 0

        binding.tvHardwareOverrideVal.text = getString(if (hwEnabled) R.string.enabled else R.string.disabled)
        binding.tvHardwareOverrideVal.setTextColor(ContextCompat.getColor(this, if (hwEnabled) R.color.hw_enabled else R.color.hw_disabled))
    }

    private fun updateLightLevelUI() {
        val lightLevel = AppState.kittyDoorData.lightLevel.get() ?: return
        val openLightLevel = AppState.kittyDoorData.optionsData.get()?.openLightLevel ?: return
        val closeLightLevel = AppState.kittyDoorData.optionsData.get()?.closeLightLevel ?: return

        binding.tvLightLevel.text = lightLevel.toString()
        when {
            lightLevel >= openLightLevel -> binding.tvLightLevel.setTextColor(ContextCompat.getColor(this, R.color.colorOpen))
            lightLevel <= closeLightLevel -> binding.tvLightLevel.setTextColor(ContextCompat.getColor(this, R.color.colorClose))
            else -> binding.tvLightLevel.setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
        }
    }

    private fun updateOptionsUI() {
        val options = AppState.kittyDoorData.optionsData.get()

        optionsBinding.swDelayClose.apply {
            isChecked = options?.delayClosing == true
        }
        optionsBinding.swDelayOpen.apply {
            isChecked = options?.delayOpening == true
        }
        optionsBinding.etCloseLightLevel.setText(options?.closeLightLevel.toString())
        optionsBinding.etOpenLightLevel.setText(options?.openLightLevel.toString())
        optionsBinding.sbCloseAfter.apply {
            progress = getProgressVal(options?.delayClosingVal ?: 0)
        }
        optionsBinding.sbOpenAfter.apply {
            progress = getProgressVal(options?.delayOpeningVal ?: 0)
        }
        optionsBinding.swTheme.apply {
            text = if (themeStr == THEME_DARK) getString(R.string.dark_theme) else getString(R.string.light_theme)
            isChecked = themeStr == THEME_DARK
        }

        val autoCloseEnabled = options?.delayClosing == true
        val autoOpenEnabled = options?.delayOpening == true

        optionsBinding.sbCloseAfter.isEnabled = autoCloseEnabled
        optionsBinding.tvCloseAfter.isEnabled = autoCloseEnabled
        optionsBinding.tvCloseAfterVal.isEnabled = autoCloseEnabled
        optionsBinding.sbOpenAfter.isEnabled = autoOpenEnabled
        optionsBinding.tvOpenAfter.isEnabled = autoOpenEnabled
        optionsBinding.tvOpenAfterVal.isEnabled = autoOpenEnabled

        optionsBinding.tvOpenAfterVal.text = getTimeStrFromProgress(optionsBinding.sbOpenAfter.progress)
        optionsBinding.tvCloseAfterVal.text = getTimeStrFromProgress(optionsBinding.sbCloseAfter.progress)
    }

    private fun updateStatusUI() {
        val status = AppState.kittyDoorData.status.get() ?: return
        val statusColor = ContextCompat.getColor(this, AppState.colorFromStatus(status))

        // Set status view properties
        binding.tvStatus.apply {
            text = status.name
            setTextColor(statusColor)
        }

        // Show/hide progress view
        val showProgressView = status == OPENING || status == CLOSING
        binding.progGarageStatus.apply {
            if (showProgressView) {
                doorProgressDrawable?.setTint(statusColor)
                setImageDrawable(doorProgressDrawable)
                doorProgressDrawable?.start()
            }

            animate()
                .alpha(if (showProgressView) 1f else 0f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun setupOptions() {
        optionsBinding = DrawerOptionsBinding.inflate(layoutInflater)
        updateOptionsUI()

        optionsBinding.swDelayOpen.apply {
            setOnCheckedChangeListener(this@MainActivity)
        }
        optionsBinding.swDelayClose.apply {
            setOnCheckedChangeListener(this@MainActivity)
        }

        optionsBinding.sbCloseAfter.apply {
            setOnSeekBarChangeListener(this@MainActivity)
        }
        optionsBinding.sbOpenAfter.apply {
            setOnSeekBarChangeListener(this@MainActivity)
        }
        optionsBinding.swTheme.apply {
            setOnCheckedChangeListener(this@MainActivity)
        }
        optionsBinding.etOpenLightLevel.apply {
            setOnFocusChangeListener { _: View, hasFocus: Boolean -> if (!hasFocus) updateOpenLightLevel() }
        }
        optionsBinding.etCloseLightLevel.apply {
            setOnFocusChangeListener { _: View, hasFocus: Boolean -> if (!hasFocus) updateCLoseLightLevel() }
        }

        optionsBinding.spaceStatusBar.minimumHeight = resources
            .getDimensionPixelSize(resources.getIdentifier("status_bar_height", "dimen", "android"))

        optionsBinding.optionsRoot
            .setBackgroundColor(getBackgroundColor())

        optionsBinding.btnViewDoorLog.setOnClickListener(this@MainActivity)

        // Set up Options drawer
        drawer = DrawerBuilder()
            .withActivity(this)
            .withDrawerGravity(Gravity.END)
            .withFullscreen(true)
            .withCloseOnClick(false)
            .withTranslucentStatusBar(false)
            .withCustomView(optionsBinding.optionsRoot)
            .build()
        drawer.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)
        drawer.closeDrawer()
    }

    private fun showLogViewer() {
        startActivity(Intent(this, LogViewerActivity::class.java).also {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    private fun updateCLoseLightLevel() {
        val options = AppState.kittyDoorData.optionsData.get() ?: return
        val prevLightLevel = options.closeLightLevel
        val curLightLevel = optionsBinding.etCloseLightLevel.text.toString().toInt()
        Log.d(TAG, "prevLightLevel = $prevLightLevel and curLightLevel = $curLightLevel")
        if (prevLightLevel != curLightLevel) {
            options.closeLightLevel = curLightLevel
            FirebaseDBF.sendOptions(options)
        }
    }

    private fun updateOpenLightLevel() {
        val options = AppState.kittyDoorData.optionsData.get() ?: return
        val prevLightLevel = options.openLightLevel
        val curLightLevel = optionsBinding.etOpenLightLevel.text.toString().toInt()
        Log.d(TAG, "prevLightLevel = $prevLightLevel and curLightLevel = $curLightLevel")
        if (prevLightLevel != curLightLevel) {
            options.openLightLevel = curLightLevel
            FirebaseDBF.sendOptions(options)
        }
    }

    private fun toggleDelayViews(swID: Int, isChecked: Boolean) {
        val options = AppState.kittyDoorData.optionsData.get() ?: return
        Log.d(TAG, "changing from ${options.delayOpening} to $isChecked")
        val sendToFirebase = if (swID == R.id.swDelayClose)
            options.delayClosing != isChecked else options.delayOpening != isChecked
        if (swID == R.id.swDelayClose)
            options.delayClosing = isChecked else options.delayOpening = isChecked

        updateOptionsUI()
        if (sendToFirebase)
            FirebaseDBF.sendOptions(options)
    }

    private fun toggleTheme(isChecked: Boolean) {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(PREFS_THEME, if (isChecked) THEME_DARK else THEME_LIGHT)
            .apply()
        recreate()
    }

    private fun showDoorFailureNotification() {
        flashbar = Flashbar.Builder(this@MainActivity)
            .icon(R.drawable.ic_warning_yellow_24dp)
            .iconColorFilter(ContextCompat.getColor(this@MainActivity, R.color.warningYellow))
            .showIcon()
            .title(R.string.auto_close_warning_title)
            .message(R.string.auto_close_warning_desc)
            .positiveActionText(android.R.string.ok)
            .positiveActionText(android.R.string.ok)
            .positiveActionTapListener(object : Flashbar.OnActionTapListener {
                override fun onActionTapped(bar: Flashbar) {
                    flashbar?.dismiss()
                }
            })
            .barDismissListener(flashbarDismissListener)
            .backgroundColor(getBackgroundColor())
            .titleAppearance(R.style.TextAppearance_MaterialComponents_Headline5)
            .messageAppearance(R.style.TextAppearance_MaterialComponents_Body2)
            .positiveActionTextAppearance(R.style.AppTheme_TextAppearance_Flashbar_Warning_Positive)
            .negativeActionTextAppearance(R.style.TextAppearance_MaterialComponents_Body1)
            .build()
        flashbar?.show()
    }

    private fun showHardwareOverrideNotification() {
        flashbar = Flashbar.Builder(this@MainActivity)
            .icon(R.drawable.ic_warning_yellow_24dp)
            .iconColorFilter(ContextCompat.getColor(this@MainActivity, R.color.warningYellow))
            .showIcon()
            .title(R.string.hw_override_enabled)
            .message(R.string.hw_override_enabled_msg)
            .positiveActionText(android.R.string.ok)
                .positiveActionTapListener(object : Flashbar.OnActionTapListener {
                    override fun onActionTapped(bar: Flashbar) {
                        flashbar?.dismiss()
                    }
                })
            .barDismissListener(flashbarDismissListener)
            .backgroundColor(getBackgroundColor())
            .titleAppearance(R.style.TextAppearance_MaterialComponents_Headline5)
            .messageAppearance(R.style.TextAppearance_MaterialComponents_Body2)
            .positiveActionTextAppearance(R.style.AppTheme_TextAppearance_Flashbar_Warning_Positive)
            .negativeActionTextAppearance(R.style.TextAppearance_MaterialComponents_Body1)
            .build()
        flashbar?.show()
    }

    private fun getBackgroundColor(): Int {
        return if (themeStr == THEME_DARK)
            ContextCompat.getColor(this, R.color.colorBackgroundDark)
        else
            ContextCompat.getColor(this, R.color.colorBackground)
    }

    private fun getProgressVal(timeMillis: Long): Int = (timeMillis / (30 * 60 * 1000)).toInt()
    private fun getTimeMillis(progress: Int): Long = (30 * (progress) * 60 * 1000).toLong()
    private fun getTimeStrFromProgress(progress: Int): String { return "${progress * 30} min" }

    companion object {
        const val TAG = "MainActivity"
        const val PREFS = "com.ms8.kittydoor.PREFS"
        const val PREFS_THEME = "PREFS_THEME"
        const val THEME_DARK = "THEME_DARK"
        const val THEME_LIGHT = "THEME_LIGHT"
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnCheckLightLevel -> FirebaseDBF.checkLightLevel()
            R.id.btnOpen -> openKittyDoor()
            R.id.btnClose -> closeKittyDoor()
            R.id.btnOptions -> drawer.openDrawer()
            R.id.btnEnableAuto -> enableAuto()
            R.id.btnViewDoorLog -> showLogViewer()
        }
    }

    private fun enableAuto() {
        Log.d(TAG, "### TEST")
        AppState.kittyDoorData.optionsData.get()?.also {
            AppState.kittyDoorData.overrideAuto.set(false)
            FirebaseDBF.sendOptions(it)
        }
    }

    private fun closeKittyDoor() {
        val hwOverride = AppState.kittyDoorData.hwOverrideMode.get() ?: return
        if (hwOverride != 0) {
            showHardwareOverrideNotification()
        } else {
            AppState.kittyDoorData.overrideAuto.set(true)
            FirebaseDBF.closeKittyDoor()
        }
    }

    private fun openKittyDoor() {
        val hwOverride = AppState.kittyDoorData.hwOverrideMode.get() ?: return
        if (hwOverride != 0) {
            showHardwareOverrideNotification()
        } else {
            AppState.kittyDoorData.overrideAuto.set(true)
            FirebaseDBF.openKittyDoor()
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        Log.d(TAG, "onCheckChanged!")
        when(buttonView.id) {
            R.id.swDelayClose -> toggleDelayViews(R.id.swDelayClose, isChecked)
            R.id.swDelayOpen -> toggleDelayViews(R.id.swDelayOpen, isChecked)
            R.id.swTheme -> toggleTheme(isChecked)
            else -> Log.e(TAG, "unknown ID: ${buttonView.id}")
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        when (seekBar.id) {
            R.id.sbCloseAfter -> optionsBinding.tvCloseAfterVal.text = getTimeStrFromProgress(progress + 1)
            R.id.sbOpenAfter -> optionsBinding.tvOpenAfterVal.text = getTimeStrFromProgress(progress + 1)
        }
    }



    override fun onStopTrackingTouch(seekBar: SeekBar) {
        when (seekBar.id) {
            R.id.sbCloseAfter -> updateCloseAfter()
            R.id.sbOpenAfter -> updateOpenAfter()
        }
    }

    private fun updateCloseAfter() {
        val options = AppState.kittyDoorData.optionsData.get() ?: return
        val progress = optionsBinding.sbCloseAfter.progress
        val prevClosingVal = options.delayClosingVal
        val newClosingVal = getTimeMillis(progress)
        Log.d(TAG, "prevClosingVal = $prevClosingVal and newClosingVal = $newClosingVal")

        if (prevClosingVal != newClosingVal) {
            options.delayClosingVal = newClosingVal
            FirebaseDBF.sendOptions(options)
        }
    }

    private fun updateOpenAfter() {
        val options = AppState.kittyDoorData.optionsData.get() ?: return
        val progress = optionsBinding.sbOpenAfter.progress
        val prevOpeningVal = options.delayOpeningVal
        val newOpeningVal = getTimeMillis(progress)
        Log.d(TAG, "prevClosingVal = $prevOpeningVal and newClosingVal = $newOpeningVal")

        if (prevOpeningVal != newOpeningVal) {
            options.delayOpeningVal = newOpeningVal
            FirebaseDBF.sendOptions(options)
        }
    }

    private fun handleFCMNotification(type: String) {
        when (type) {
            TYPE_AUTO_CLOSE_WARNING -> showDoorFailureNotification()
            else -> Log.e(TAG, "Unknown FCM notification type")
        }
    }

    private val hwOverrideListener = object: Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            updateHWOverrideUI()
        }
    }

    private val overrideAutoListener = object: Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            updateOverrideAutoUI()
        }
    }



    private val lightLevelListener = object: Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            updateLightLevelUI()
        }
    }

    private val doorOptionsListener  = object: Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            updateOptionsUI()
        }
    }

    private val doorStatusListener = object: Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            val previousStatus = AppState.kittyDoorData.previousStatus.get()
            val currentStatus = AppState.kittyDoorData.status.get()

            when {
                previousStatus == OPENING && currentStatus == CLOSED ->
                {
                    Flashbar.Builder(this@MainActivity)
                        .icon(R.drawable.ic_error_white_24dp)
                        .title(R.string.kitty_door_closed)
                        .message(R.string.kittydoor_closed_msg)
                        .dismissOnTapOutside()
                        .show()
                }

                previousStatus == CLOSING && currentStatus == OPEN -> {
                    Flashbar.Builder(this@MainActivity)
                        .icon(R.drawable.ic_error_white_24dp)
                        .title(R.string.kitty_door_open)
                        .message(R.string.kitty_door_open_msg)
                        .dismissOnTapOutside()
                        .show()
                }
            }

            AppState.kittyDoorData.previousStatus.set(currentStatus)
            updateStatusUI()
        }

    }

    private val doorProgressViewCallback = object : Animatable2Compat.AnimationCallback() {
        override fun onAnimationEnd(drawable: Drawable?) {
            binding.progGarageStatus.post { doorProgressDrawable?.start() }
        }
    }

    private val flashbarDismissListener = object : Flashbar.OnBarDismissListener {
        override fun onDismissProgress(bar: Flashbar, progress: Float) {}

        override fun onDismissed(bar: Flashbar, event: Flashbar.DismissEvent) {
            flashbar = null
        }

        override fun onDismissing(bar: Flashbar, isSwiped: Boolean) { }
    }

    // UNUSED
    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    //
}