import SwiftUI

struct LoginView: View {
    var isLoading: Bool
    let onAccess: (String) -> Void
    @State private var tenant = ""

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "building.2")
                .resizable()
                .scaledToFit()
                .frame(width: 100, height: 100)
                .foregroundColor(Color(red: 0, green: 0.76, blue: 0.57))

            Text("Acesso ao Sistema")
                .font(.title)
                .fontWeight(.bold)

            TextField("Nome do ambiente", text: $tenant)
                .textFieldStyle(.roundedBorder)
                .textInputAutocapitalization(.never)
                .disableAutocorrection(true)
                .padding(.horizontal, 32)

            Button(action: {
                onAccess(tenant.trimmingCharacters(in: .whitespaces))
            }) {
                if isLoading {
                    ProgressView()
                        .tint(.white)
                } else {
                    Text("Entrar")
                        .fontWeight(.bold)
                }
            }
            .frame(maxWidth: .infinity, minHeight: 50)
            .background(Color(red: 0, green: 0.76, blue: 0.57))
            .foregroundColor(.white)
            .cornerRadius(8)
            .padding(.horizontal, 32)
            .disabled(isLoading)

            Spacer()
            Spacer()
        }
    }
}
