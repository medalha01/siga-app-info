package app.siga

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import app.siga.databinding.ActivityMainBinding
import app.siga.shared.AndroidPlatform
import app.siga.shared.MainViewModel
import app.siga.shared.TenantRepository
import app.siga.shared.UiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by lazy {
        val repository = TenantRepository(AndroidPlatform(applicationContext))
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repository) as T
            }
        })[MainViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupWebView()
        setupClickListeners()
        setupBackNavigation()
        observeUiState()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.webView, true)

        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = View.GONE
                view?.requestFocus()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnAcessar.setOnClickListener {
            val tenant = binding.inputTenant.text.toString().trim()
            viewModel.onAccessClicked(tenant)
        }
    }

    @SuppressLint("UseKtx")
    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this) {
            if (binding.webView.isVisible && binding.webView.canGoBack()) {
                binding.webView.goBack()
            } else if (binding.webView.isVisible) {
                viewModel.onLogout()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is UiState.Idle -> showLoginScreen()
                    is UiState.Loading -> showLoading()
                    is UiState.WebViewReady -> showWebView(state.url)
                    is UiState.Error -> showError(state.message)
                }
            }
        }
    }

    private fun showLoginScreen() {
        binding.loginContainer.visibility = View.VISIBLE
        binding.webView.visibility = View.GONE
        binding.btnAcessar.isEnabled = true
        binding.progressBar.visibility = View.GONE
        binding.inputTenant.text?.clear()
    }

    private fun showLoading() {
        binding.btnAcessar.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun showWebView(url: String) {
        binding.loginContainer.visibility = View.GONE
        binding.webView.visibility = View.VISIBLE
        binding.webView.loadUrl(url)
        binding.webView.requestFocus()
        binding.btnAcessar.isEnabled = true
        binding.progressBar.visibility = View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        viewModel.clearError()
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }
}
