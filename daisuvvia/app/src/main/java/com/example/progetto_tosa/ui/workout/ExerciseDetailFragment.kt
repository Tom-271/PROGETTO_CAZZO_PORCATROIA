package com.example.progetto_tosa.ui.workout

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.widget.ImageButton
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.progetto_tosa.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout

class ExerciseDetailFragment : DialogFragment() {

    private var haiPremuto = false
    private lateinit var videoContainer: View
    private lateinit var webView: WebView
    private lateinit var overlay: View
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var headerImageContainer: View
    private lateinit var descriptionImage: ImageView
    private var pageCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_TransparentDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_exercise_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleView       = view.findViewById<TextView>(R.id.tvDetailTitle)
        headerImageContainer = view.findViewById(R.id.headerImageContainer)
        descriptionImage     = view.findViewById(R.id.descriptionImage)
        videoContainer       = view.findViewById(R.id.videoContainer)
        webView              = view.findViewById(R.id.webViewDetail)
        overlay              = view.findViewById(R.id.videoOverlay)
        val descView         = view.findViewById<TextView>(R.id.tvDetailDescription)
        val sub2View         = view.findViewById<TextView>(R.id.subtitle2)
        val desc2View        = view.findViewById<TextView>(R.id.description2)
        val descrTotView     = view.findViewById<TextView>(R.id.descrizioneTotale)
        val buttonMore       = view.findViewById<MaterialButton>(R.id.buttonMore)
        viewPager            = view.findViewById(R.id.viewPagerImages)
        tabLayout            = view.findViewById(R.id.tabLayoutIndicator)
        val buttonExit = view.findViewById<ImageButton>(R.id.buttonExit)

        val title       = requireArguments().getString(ARG_TITLE)!!
        val video       = requireArguments().getString(ARG_VIDEO)!!
        val desc        = requireArguments().getString(ARG_DESC)!!
        val sub2        = requireArguments().getString(ARG_SUB2)!!
        val desc2       = requireArguments().getString(ARG_DESC2)!!
        val img1Res     = requireArguments().getInt(ARG_IMG1)
        val img2Res     = requireArguments().getInt(ARG_IMG2)
        val descrTotStr = requireArguments().getString(ARG_DESCRIZIONE_TOTALE)


        titleView.text        = title
        descView.text         = desc
        descriptionImage.setImageResource(requireArguments().getInt(ARG_DESCRIPTION_IMAGE))
        descrTotView.text     = descrTotStr

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled  = true
            mediaPlaybackRequiresUserGesture = false
        }
        webView.webViewClient   = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        val videoId = Uri.parse(video).getQueryParameter("v") ?: video.substringAfterLast("/")
        webView.loadUrl(
            "https://www.youtube.com/embed/$videoId?autoplay=1&modestbranding=1&playsinline=1"
        )

        overlay.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video)))
        }

        setInitialVisibility(view)

        buttonMore.setOnClickListener {
            if (!haiPremuto) {
                headerImageContainer.visibility = View.GONE
                videoContainer.visibility       = View.GONE
                descView.visibility             = View.GONE

                sub2View.text           = sub2
                desc2View.text          = desc2
                sub2View.visibility     = View.VISIBLE
                desc2View.visibility    = View.VISIBLE
                descrTotView.visibility = View.VISIBLE

                setupCarousel(listOf(img1Res, img2Res))

                viewPager.visibility = View.VISIBLE
                tabLayout.visibility = View.VISIBLE
                buttonMore.text      = "Di meno..."
                haiPremuto = true
            } else {
                resetVisibility(view)
            }
        }

        ChangeTheme(view)

        buttonExit.setOnClickListener {
            dismiss() // Chiude il dialogo, utilissimo!!! è proprio una feature dei dialog fragment, una cosa velocissima rispetto all'eliminare tutti i dati dalla recycle ecc
        }
    }

    private fun setInitialVisibility(root: View) {
        root.findViewById<View>(R.id.headerImageContainer).visibility    = View.VISIBLE
        root.findViewById<View>(R.id.videoContainer).visibility          = View.VISIBLE
        root.findViewById<WebView>(R.id.webViewDetail).visibility        = View.VISIBLE
        root.findViewById<View>(R.id.videoOverlay).visibility            = View.VISIBLE
        root.findViewById<TextView>(R.id.tvDetailDescription).visibility = View.VISIBLE
        root.findViewById<TextView>(R.id.subtitle2).visibility           = View.GONE
        root.findViewById<TextView>(R.id.description2).visibility        = View.GONE
        root.findViewById<TextView>(R.id.descrizioneTotale).visibility   = View.GONE
        viewPager.visibility = View.GONE
        tabLayout.visibility = View.GONE
    }

    private fun resetVisibility(root: View) {
        pageCallback?.let { viewPager.unregisterOnPageChangeCallback(it) }
        pageCallback = null

        root.findViewById<View>(R.id.headerImageContainer).visibility    = View.VISIBLE
        root.findViewById<View>(R.id.videoContainer).visibility          = View.VISIBLE
        root.findViewById<TextView>(R.id.tvDetailDescription).visibility = View.VISIBLE
        root.findViewById<TextView>(R.id.subtitle2).visibility           = View.GONE
        root.findViewById<TextView>(R.id.description2).visibility        = View.GONE
        root.findViewById<TextView>(R.id.descrizioneTotale).visibility   = View.GONE
        viewPager.visibility = View.GONE
        tabLayout.visibility = View.GONE
        root.findViewById<MaterialButton>(R.id.buttonMore).text = "Di più..."
        haiPremuto = false
    }

    private fun ChangeTheme(root: View) {
        val night       = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val bgColorId   = if (night == Configuration.UI_MODE_NIGHT_YES) R.color.black else R.color.white
        val textColorId = if (night == Configuration.UI_MODE_NIGHT_YES) R.color.white else R.color.black
        val bgColor     = ContextCompat.getColor(requireContext(), bgColorId)
        val txtColor    = ContextCompat.getColor(requireContext(), textColorId)

        root.findViewById<MaterialCardView>(R.id.detailCardInner)
            .setCardBackgroundColor(bgColor)
        root.findViewById<TextView>(R.id.tvDetailDescription)
            ?.setTextColor(txtColor)
        root.findViewById<TextView>(R.id.description2)
            ?.setTextColor(txtColor)
        root.findViewById<TextView>(R.id.descrizioneTotale)
            ?.setTextColor(txtColor)

        // background dei dots: bianco in light, nero in dark
        root.findViewById<TabLayout>(R.id.tabLayoutIndicator)
            ?.setBackgroundColor(bgColor)
    }

    private fun setupCarousel(imgs: List<Int>) {
        viewPager.adapter = object : RecyclerView.Adapter<ImageHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                ImageHolder(ImageView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                })

            override fun onBindViewHolder(holder: ImageHolder, position: Int) {
                holder.image.setImageResource(imgs[position % imgs.size])
            }

            override fun getItemCount() = Int.MAX_VALUE
        }

        val dp = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics
        ).toInt()

        fun dot(colorInt: Int) = ShapeDrawable(OvalShape()).apply {
            intrinsicWidth  = dp
            intrinsicHeight = dp
            paint.isAntiAlias = true
            paint.color = colorInt
        }

        // pallino blu per il selezionato, bianco per gli altri
        val selectedColor   = ContextCompat.getColor(requireContext(), R.color.sky)
        val unselectedColor = Color.WHITE

        val sel   = dot(selectedColor)
        val unsel = dot(unselectedColor)

        tabLayout.removeAllTabs()
        imgs.forEachIndexed { i, _ ->
            tabLayout.addTab(tabLayout.newTab().setIcon(if (i == 0) sel else unsel))
        }
        tabLayout.tabIconTint = null

        val realCount = imgs.size
        val fakeCount = Int.MAX_VALUE
        val startPos  = fakeCount / 2 - (fakeCount / 2) % realCount
        viewPager.offscreenPageLimit = realCount
        viewPager.setCurrentItem(startPos, false)

        pageCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(pos: Int) {
                val idx = pos % realCount
                for (j in 0 until tabLayout.tabCount) {
                    tabLayout.getTabAt(j)?.icon = if (j == idx) sel else unsel
                }
            }
        }
        viewPager.registerOnPageChangeCallback(pageCallback!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pageCallback?.let { viewPager.unregisterOnPageChangeCallback(it) }
    }

    private class ImageHolder(val image: ImageView) : RecyclerView.ViewHolder(image)

    companion object {
        private const val ARG_TITLE              = "ARG_TITLE"
        private const val ARG_DESCRIPTION_IMAGE  = "ARG_DESCRIPTION_IMAGE"
        private const val ARG_VIDEO              = "ARG_VIDEO"
        private const val ARG_DESC               = "ARG_DESC"
        private const val ARG_SUB2               = "ARG_SUB2"
        private const val ARG_DESC2              = "ARG_DESC2"
        private const val ARG_IMG1               = "ARG_IMG1"
        private const val ARG_IMG2               = "ARG_IMG2"
        private const val ARG_DESCRIZIONE_TOTALE = "ARG_DESCRIZIONE_TOTALE"

        fun newInstance(
            title: String,
            videoUrl: String,
            description: String,
            subtitle2: String,
            description2: String,
            image1Res: Int,
            image2Res: Int,
            descriptionImage: Int,
            descrizioneTotale: String
        ) = ExerciseDetailFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_VIDEO, videoUrl)
                putString(ARG_DESC, description)
                putString(ARG_SUB2, subtitle2)
                putString(ARG_DESC2, description2)
                putInt(ARG_IMG1, image1Res)
                putInt(ARG_IMG2, image2Res)
                putInt(ARG_DESCRIPTION_IMAGE, descriptionImage)
                putString(ARG_DESCRIZIONE_TOTALE, descrizioneTotale)
            }
        }
    }

}
