package me.rerere.rikkahub.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "ProactiveAlarmReceiver"

// 一个简单的全局 IO 协程作用域，用来在广播里跑后台任务
private val proactiveScope = CoroutineScope(Dispatchers.IO)

class ProactiveAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val conversationId = intent.getStringExtra("conversation_id") ?: return
        Log.i(TAG, "Alarm triggered for conversation: $conversationId")

        // 延长 Receiver 生命周期，允许异步完成
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        proactiveScope.launch {
            try {
                ProactiveGenerationRunner.run(appContext, conversationId)
            } finally {
                // 告诉系统：这次广播处理完了
                pendingResult.finish()
            }
        }
    }
}
