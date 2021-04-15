package com.ms8.kittydoor

import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.Observable
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.ms8.kittydoor.FirebaseMessageService.Companion.NOTIFICATION_TYPE
import com.ms8.kittydoor.databinding.ActivitySplashScreenBinding

class SplashScreen : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private var progressView : AnimatedVectorDrawableCompat? = null

    private val progressViewCallback = object : Animatable2Compat.AnimationCallback() {
        override fun onAnimationEnd(drawable: Drawable?) {
            binding.progressBar.post { progressView?.start() }
        }
    }

    private val backendListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            val status = AppState.kittyDoorData.status.get()
            val options = AppState.kittyDoorData.optionsData.get()
            Log.d(TAG, "## - Received [status = $status | options = $options]")
            if (AppState.kittyDoorData.status.get() != null && AppState.kittyDoorData.optionsData.get() != null) {
                Log.d(TAG, "## - Received All Backend Data!")
                AppState.kittyDoorData.status.removeOnPropertyChangedCallback(this)
                AppState.kittyDoorData.optionsData.removeOnPropertyChangedCallback(this)
                nextActivity()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressView = AnimatedVectorDrawableCompat.create(this, R.drawable.av_progress)
        progressView?.registerAnimationCallback(progressViewCallback)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_id))
            .requestEmail()
            .build()

        auth = FirebaseAuth.getInstance()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        signIn()
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser != null) {
            if (AppState.kittyDoorData.status.get() != null && AppState.kittyDoorData.optionsData.get() != null)
                nextActivity()
            else
                fetchBackendData()
        } else {
            Log.w(TAG, "onResume not signed in!!")
        }
    }

    override fun onPause() {
        super.onPause()

        if (auth.currentUser != null) {
            AppState.kittyDoorData.status.removeOnPropertyChangedCallback(backendListener)
            AppState.kittyDoorData.optionsData.removeOnPropertyChangedCallback(backendListener)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RC_SIGN_IN -> handleGoogleSignIn(data, resultCode)
        }
    }

    private fun handleGoogleSignIn(data: Intent?, resultCode: Int) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            firebaseAuthWithGoogle(task!!.getResult(ApiException::class.java)!!)
        } catch (e : Exception) {
            Log.e(TAG, "onActivityResult - ${e.message}")
            stopProgressView()
//            MaterialAlertDialogBuilder(this)
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id)

        startProgressView()

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")

                    startProgressView()

                    fetchBackendData()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)

                    MaterialAlertDialogBuilder(this@SplashScreen)
                        .setTitle(R.string.err_failed_signin_title)
                        .setMessage(R.string.err_failed_signin_msg)
                        .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int -> signIn() }
                        .show()

                    stopProgressView()

                    binding.progressBar.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
            }
    }

    private fun startProgressView() {
        binding.progressBar.setImageDrawable(progressView)
        progressView?.start()
        binding.progressBar.animate()
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(AccelerateInterpolator())
            .start()
    }

    private fun stopProgressView() {
        //binding.progressBar.setImageDrawable(progressView)
        progressView?.stop()
        binding.progressBar.animate()
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(AccelerateInterpolator())
            .start()
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun fetchBackendData() {
        if (binding.progressBar.alpha != 1f)
            startProgressView()
        Log.d(TAG, "fetching backend data...")
        AppState.kittyDoorData.status.addOnPropertyChangedCallback(backendListener)
        AppState.kittyDoorData.optionsData.addOnPropertyChangedCallback(backendListener)
        FirebaseDBF.listenForKittyDoorStatus()
        FirebaseDBF.listenForOptionChanges()

    }

    private fun nextActivity() {
        val nextActivityIntent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("EXIT", true)
        intent.extras?.getString(NOTIFICATION_TYPE)?.let {
            nextActivityIntent.putExtra(NOTIFICATION_TYPE, it)
        }
        startActivity(nextActivityIntent)
    }

    companion object {
        const val TAG = "SplashScreen"

        const val RC_SIGN_IN = 88
    }
}