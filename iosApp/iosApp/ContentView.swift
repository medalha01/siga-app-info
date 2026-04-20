import SwiftUI
import Shared

struct ContentView: View {
    @StateObject private var viewModelWrapper = ViewModelWrapper()

    var body: some View {
        NavigationStack {
            ZStack {
                switch viewModelWrapper.state {
                case .idle:
                    LoginView(
                        isLoading: false,
                        onAccess: { tenant in
                            viewModelWrapper.viewModel.onAccessClicked(tenant: tenant)
                        }
                    )
                case .loading:
                    LoginView(
                        isLoading: true,
                        onAccess: { _ in }
                    )
                case .webViewReady(let url):
                    WebViewScreen(
                        url: url,
                        onLogout: { viewModelWrapper.viewModel.onLogout() }
                    )
                case .error(let message):
                    LoginView(
                        isLoading: false,
                        onAccess: { tenant in
                            viewModelWrapper.viewModel.onAccessClicked(tenant: tenant)
                        }
                    )
                    .onAppear {
                        viewModelWrapper.showError(message)
                    }
                }
            }
            .alert("Erro", isPresented: $viewModelWrapper.showingError) {
                Button("OK") { viewModelWrapper.viewModel.clearError() }
            } message: {
                Text(viewModelWrapper.errorMessage)
            }
        }
    }
}
