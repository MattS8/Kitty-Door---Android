package com.ms8.kittydoor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.databinding.Observable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.database.FirebaseDatabase


class LogViewerActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {

    lateinit var adapter: LogViewerAdapter
    lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_KittyDoor_Dark)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)

        // Setup LogViewerAdapter
        val logViewer = findViewById<RecyclerView>(R.id.rcvLog)
        logViewer.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
            reverseLayout = true
        }
        adapter = LogViewerAdapter()
        logViewer.adapter = adapter

        // Setup SwipeRefresh
        swipeRefresh = findViewById(R.id.swipeRefreshLogs)
        swipeRefresh.setOnRefreshListener(this)

        // Setup back button
        val btnBack = findViewById<Button>(R.id.btn_back)
        btnBack.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        AppState.kittyDoorData.debugMessages.addOnPropertyChangedCallback(debugMessagesListener)
        FirebaseDBF.getDebugMessages()
    }

    override fun onPause() {
        super.onPause()
        AppState.kittyDoorData.debugMessages.removeOnPropertyChangedCallback(debugMessagesListener)
    }


    private val debugMessagesListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            AppState.kittyDoorData.debugMessages.get()?.also {
                adapter.setLogs(it)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onRefresh() {
        FirebaseDBF.getDebugMessages()
    }
}