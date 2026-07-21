package neth.iecal.curbox.data.sync

import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject

class RealtimeClient(
    private val userId: String,
    @Volatile private var accessToken: String,
    private val onChange: () -> Unit,
    private val onConnected: (Boolean) -> Unit = {},
    private val anonKey: String = SupabaseRest.ANON_KEY,
    private val baseWss: String = "wss://pdixkzhncuuxuxwhdwdh.supabase.co/realtime/v1/websocket",
) {
    private val client = OkHttpClient()
    @Volatile private var ws: WebSocket? = null
    private val topic = "realtime:curbox:$userId"
    private val running = AtomicBoolean(false)
    private val reconnecting = AtomicBoolean(false)
    private var ref = 0

    fun start() {
        if (!running.compareAndSet(false, true)) return
        connect()
    }

    fun stop() {
        running.set(false)
        onConnected(false)
        runCatching { ws?.close(1000, null) }
        ws = null
    }

    fun updateToken(token: String) {
        accessToken = token
        val w = ws ?: return
        runCatching {
            w.send(
                JSONObject()
                    .put("topic", topic)
                    .put("event", "access_token")
                    .put("payload", JSONObject().put("access_token", token))
                    .put("ref", (++ref).toString())
                    .toString(),
            )
        }
    }

    private fun connect() {
        if (!running.get()) return
        val req = Request.Builder().url("$baseWss?apikey=$anonKey&vsn=1.0.0").build()
        ws = client.newWebSocket(
            req,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    onConnected(true)
                    join(webSocket)
                    startHeartbeat(webSocket)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val event = runCatching { JSONObject(text).optString("event") }.getOrNull()
                    if (event == "postgres_changes") runCatching { onChange() }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    onConnected(false)
                    reconnectLater()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    onConnected(false)
                    reconnectLater()
                }
            },
        )
    }

    private fun join(webSocket: WebSocket) {
        val change = JSONObject()
            .put("event", "*")
            .put("schema", "public")
            .put("table", "sync_records")
            .put("filter", "user_id=eq.$userId")
        val payload = JSONObject()
            .put("config", JSONObject().put("postgres_changes", JSONArray().put(change)))
            .put("access_token", accessToken)
        webSocket.send(
            JSONObject()
                .put("topic", topic)
                .put("event", "phx_join")
                .put("payload", payload)
                .put("ref", (++ref).toString())
                .toString(),
        )
    }

    private fun startHeartbeat(webSocket: WebSocket) {
        Thread {
            try {
                while (running.get() && ws === webSocket) {
                    Thread.sleep(25_000)
                    val ok = webSocket.send(
                        JSONObject()
                            .put("topic", "phoenix")
                            .put("event", "heartbeat")
                            .put("payload", JSONObject())
                            .put("ref", (++ref).toString())
                            .toString(),
                    )
                    if (!ok) break
                }
            } catch (_: InterruptedException) {
            } catch (_: Exception) {
            }
        }.apply { isDaemon = true }.start()
    }

    private fun reconnectLater() {
        if (!running.get()) return
        if (!reconnecting.compareAndSet(false, true)) return
        ws = null
        Thread {
            try {
                Thread.sleep(3000)
            } catch (_: InterruptedException) {
            }
            reconnecting.set(false)
            if (running.get()) connect()
        }.apply { isDaemon = true }.start()
    }
}
