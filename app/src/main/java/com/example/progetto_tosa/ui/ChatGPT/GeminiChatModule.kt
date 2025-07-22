package com.example.progetto_tosa.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.progetto_tosa.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/* ============================================================
   COSTANTI & UTIL
   ============================================================ */

private const val BASE_URL = "https://generativelanguage.googleapis.com/"

// --- Guard rail prompt ---
private val SYSTEM_PROMPT = """
Sei un assistente che risponde SOLO su palestra, allenamento (esercizi, esecuzioni, programmazione),
muscolazione, riabilitazione leggera, cardio e alimentazione sportiva.
Se la domanda è fuori tema, rifiuta gentilmente e invita l'utente a tornare all'argomento.
Rispondi in italiano, in modo pratico e conciso.
""".trimIndent()


// parole chiave ampie (puoi aggiungerne/editarle liberamente)
private val KEYWORDS = listOf(
    // macro argomenti
    "palestra","gym","allen","workout","scheda","programma","routine","split","sessione",
    "eserciz","esecuz","ripetiz","serie","set","forza","ipertrofia","massa","cut","bulk",
    "cardio","hiit","corsa","tapis","ellittica","vogatore","cycling","spin bike",
    // muscoli / distretti
    "petto","chest","dorsali","schiena","lat","trapezi","deltoidi","spalle","bicipiti",
    "tricipiti","avambracci","addominali","core","obliqui","glutei","quadricipiti",
    "femorali","polpacci","calf","colonna","cervicale","lombari",
    // attrezzi / macchine
    "manubri","bilanciere","cavi","pulegge","macchina","multifunzione","smith","leg press",
    "lat machine","pectorals","butterfly","cable row","rowing","panatta","technogym",
    // esercizi specifici
    "panca piana","bench press","incline press","decline press","chest press",
    "croci","fly","pullover","rematore","bent over row","deadlift","stacco","stacchi",
    "squat","front squat","leg extension","leg curl","hip thrust","affondi","lunge",
    "trazioni","pull up","chin up","lat pulldown","scrollate","shrug","military press",
    "shoulder press","lateral raise","alzate laterali","alzate frontali","curl","hammer curl",
    "french press","pushdown","dip","calf raise","plank","crunch","sit up","russian twist",
    "stretching","mobilità","foam roller","scarico","deload",
    // alimentazione
    "aliment","dieta","nutriz","macro","calorie","proteine","carbo","grassi",
    "integrator","integratori","creatina","whey","bcaa","omega","vitamine","minerali",
    "pre workout","post workout","meal prep","pasto","pasti","deficit","surplus","cibo"
)

private fun isOnTopic(text: String): Boolean {
    val t = text.lowercase()
    return KEYWORDS.any { t.contains(it) }
}

/* ============================================================
   DATA
   ============================================================ */

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val role: Role,
    val content: String
) {
    enum class Role { USER, ASSISTANT }
}

/* ============================================================
   ADAPTER
   ============================================================ */

class MessageAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(Diff) {

    object Diff : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(o: ChatMessage, n: ChatMessage) = o.id == n.id
        override fun areContentsTheSame(o: ChatMessage, n: ChatMessage) = o == n
    }

    override fun getItemViewType(pos: Int) =
        if (getItem(pos).role == ChatMessage.Role.USER) 1 else 2

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (vt == 1)
            UserVH(inf.inflate(R.layout.item_msg_user, parent, false))
        else
            AssistantVH(inf.inflate(R.layout.item_msg_assistant, parent, false))
    }

    override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
        val m = getItem(pos)
        when (h) {
            is UserVH -> h.bind(m)
            is AssistantVH -> h.bind(m)
        }
    }

    class UserVH(v: View) : RecyclerView.ViewHolder(v) {
        private val tv: TextView = v.findViewById(R.id.tvText)
        fun bind(m: ChatMessage) { tv.text = m.content }
    }

    class AssistantVH(v: View) : RecyclerView.ViewHolder(v) {
        private val tv: TextView = v.findViewById(R.id.tvText)
        fun bind(m: ChatMessage) { tv.text = m.content }
    }
}

/* ============================================================
   GEMINI API
   ============================================================ */

data class GeminiRequest(val contents: List<GeminiContent>)
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)
data class GeminiPart(val text: String)

data class GeminiResponse(val candidates: List<Candidate>?) {
    data class Candidate(val content: GeminiContent?)
}

interface GeminiService {
    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun chat(
        @Header("X-goog-api-key") apiKey: String,
        @Body body: GeminiRequest
    ): GeminiResponse
}

/* ============================================================
   REPOSITORY
   ============================================================ */

class ChatRepository(
    private val service: GeminiService,
    private val apiKey: String
) {
    suspend fun ask(history: List<ChatMessage>, userMsg: String): String {

        val system = GeminiContent(
            role = "user", // Gemini non ha "system", uso "user" come contesto iniziale
            parts = listOf(GeminiPart(SYSTEM_PROMPT))
        )

        val all = history + ChatMessage(role = ChatMessage.Role.USER, content = userMsg)

        val contents = listOf(system) + all.map {
            GeminiContent(
                parts = listOf(GeminiPart(text = it.content)),
                role = if (it.role == ChatMessage.Role.USER) "user" else "model"
            )
        }

        val resp = service.chat(apiKey, GeminiRequest(contents))
        val reply = resp.candidates?.firstOrNull()
            ?.content?.parts?.firstOrNull()?.text.orEmpty()

        // post-filter: se esce dal tema
        return if (isOnTopic(reply)) reply
        else "Rimaniamo su palestra, allenamento o alimentazione 😉"
    }

    companion object {
        fun create(apiKey: String): ChatRepository {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(OkHttpClient.Builder().build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return ChatRepository(retrofit.create(GeminiService::class.java), apiKey)
        }
    }
}

/* ============================================================
   VIEWMODEL
   ============================================================ */

class ChatViewModel(private val repo: ChatRepository) : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    fun send(text: String) {
        if (text.isBlank()) return

        // niente filtro pre‑invio (solo avviso se vuoi)
        add(ChatMessage(role = ChatMessage.Role.USER, content = text))

        viewModelScope.launch {
            try {
                val reply = repo.ask(_messages.value ?: emptyList(), text)
                add(ChatMessage(role = ChatMessage.Role.ASSISTANT, content = reply))
            } catch (e: Exception) {
                add(ChatMessage(role = ChatMessage.Role.ASSISTANT, content = "Errore: ${e.message}"))
            }
        }
    }

    private fun add(m: ChatMessage) {
        _messages.value = (_messages.value ?: emptyList()) + m
    }
}

class ChatVMFactory(private val repo: ChatRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(c: Class<T>): T {
        if (c.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}

/* ============================================================
   OVERLAY
   ============================================================ */

class ChatOverlay(
    parent: ViewGroup,
    private val vm: ChatViewModel,
    lifecycleOwner: LifecycleOwner
) {
    private val root: View = LayoutInflater.from(parent.context)
        .inflate(R.layout.layout_chat_overlay, parent, false)

    private val overlayRoot: View = root.findViewById(R.id.chatOverlayRoot)
    private val card: View = root.findViewById(R.id.chatCard)
    private val rv: RecyclerView = root.findViewById(R.id.rvMessages)
    private val et: TextInputEditText = root.findViewById(R.id.etMessage)
    private val btnSend: MaterialButton = root.findViewById(R.id.btnSend)
    private val btnClose: ImageView = root.findViewById(R.id.btnClose)

    private val adapter = MessageAdapter()
    private var prepared = false

    init {
        parent.addView(root)
        overlayRoot.isClickable = true
        overlayRoot.isFocusable = true
        overlayRoot.bringToFront()

        rv.layoutManager = LinearLayoutManager(parent.context).apply { stackFromEnd = true }
        rv.adapter = adapter

        btnClose.setOnClickListener { hide() }
        btnSend.setOnClickListener { send() }

        vm.messages.observe(lifecycleOwner) {
            adapter.submitList(it)
            rv.scrollToPosition(adapter.itemCount - 1)
        }
    }

    fun show() {
        if (overlayRoot.isVisible) return
        overlayRoot.isVisible = true
        overlayRoot.alpha = 0f
        overlayRoot.animate().alpha(1f).setDuration(150).start()

        card.post {
            prepareIfNeeded()
            card.animate()
                .translationX(0f)
                .setDuration(250)
                .start()
        }
    }

    fun hide() {
        if (!overlayRoot.isVisible) return
        card.animate()
            .translationX(-card.width.toFloat() - 50f)
            .setDuration(250)
            .withEndAction {
                overlayRoot.animate()
                    .alpha(0f)
                    .setDuration(120)
                    .withEndAction { overlayRoot.isVisible = false }
                    .start()
            }
            .start()
    }

    fun toggle() = if (overlayRoot.isVisible) hide() else show()

    private fun prepareIfNeeded() {
        if (prepared) return
        card.translationX = -card.width.toFloat() - 50f
        prepared = true
    }

    private fun send() {
        val t = et.text?.toString().orEmpty()
        if (t.isBlank()) return
        et.setText("")
        vm.send(t)
    }
}

/* ============================================================
   SECRETS
   ============================================================ */
object Secrets {
    const val GEMINI_KEY = "AIzaSyC6OqHQay1bhcKRrt3PZXusd6FiGs45GIs"
}
