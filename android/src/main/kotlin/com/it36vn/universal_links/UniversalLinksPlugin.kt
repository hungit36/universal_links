package com.it36vn.universal_links

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

class UniversalLinksPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, EventChannel.StreamHandler, ActivityAware, PluginRegistry.NewIntentListener {

    companion object {
        private const val MESSAGES_CHANNEL = "uni_links/messages"
        private const val EVENTS_CHANNEL = "uni_links/events"
    }

    private var changeReceiver: BroadcastReceiver? = null
    private var initialLink: String? = null
    private var latestLink: String? = null
    private var context: Context? = null
    private var initialIntent = true

    private fun handleIntent(context: Context, intent: Intent) {
        val action = intent.action
        val dataString = intent.dataString
        
        if (Intent.ACTION_VIEW == action) {
            if (initialIntent) {
                initialLink = dataString
                initialIntent = false
            }
            latestLink = dataString
            changeReceiver?.onReceive(context, intent)
        }
    }

    private fun createChangeReceiver(events: EventChannel.EventSink): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val dataString = intent.dataString
                if (dataString == null) {
                    events.error("UNAVAILABLE", "Link unavailable", null)
                } else {
                    events.success(dataString)
                }
            }
        }
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        register(flutterPluginBinding.binaryMessenger, this)
    }

    private fun register(messenger: BinaryMessenger, plugin: UniversalLinksPlugin) {
        val methodChannel = MethodChannel(messenger, MESSAGES_CHANNEL)
        methodChannel.setMethodCallHandler(plugin)

        val eventChannel = EventChannel(messenger, EVENTS_CHANNEL)
        eventChannel.setStreamHandler(plugin)
    }

    override fun onDetachedFromEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {}

    override fun onListen(arguments: Any?, eventSink: EventChannel.EventSink) {
        changeReceiver = createChangeReceiver(eventSink)
    }

    override fun onCancel(arguments: Any?) {
        changeReceiver = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "getInitialLink" -> result.success(initialLink)
            "getLatestLink" -> result.success(latestLink)
            else -> result.notImplemented()
        }
    }

    override fun onNewIntent(intent: Intent): Boolean {
        context?.let { handleIntent(it, intent) }
        return false
    }

    override fun onAttachedToActivity(activityPluginBinding: ActivityPluginBinding) {
        activityPluginBinding.addOnNewIntentListener(this)
        context?.let { handleIntent(it, activityPluginBinding.activity.intent) }
    }

    override fun onDetachedFromActivityForConfigChanges() {}

    override fun onReattachedToActivityForConfigChanges(activityPluginBinding: ActivityPluginBinding) {
        activityPluginBinding.addOnNewIntentListener(this)
        context?.let { handleIntent(it, activityPluginBinding.activity.intent) }
    }

    override fun onDetachedFromActivity() {}
}