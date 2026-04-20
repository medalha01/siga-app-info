import SwiftUI
import Shared

enum AppState {
    case idle
    case loading
    case webViewReady(String)
    case error(String)
}

@MainActor
class ViewModelWrapper: ObservableObject {
    let viewModel: MainViewModel
    @Published var state: AppState = .idle
    @Published var showingError = false
    @Published var errorMessage = ""

    init() {
        let platform = IosPlatform()
        let repository = TenantRepository(platform: platform)
        viewModel = MainViewModel(repository: repository)

        viewModel.observeUiState().collect { [weak self] uiState in
            guard let self, let uiState else { return }
            DispatchQueue.main.async {
                switch uiState {
                case is UiState.Idle:
                    self.state = .idle
                case is UiState.Loading:
                    self.state = .loading
                case let ready as UiState.WebViewReady:
                    self.state = .webViewReady(ready.url)
                case let error as UiState.Error:
                    self.state = .error(error.message)
                default:
                    break
                }
            }
        }
    }

    func showError(_ message: String) {
        errorMessage = message
        showingError = true
    }
}
