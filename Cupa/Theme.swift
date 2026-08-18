import SwiftUI

enum CupaTheme {
    static let background = Color(hex: 0xF4F1EA)
    static let backgroundAlt = Color(hex: 0xEBE6DC)
    static let card = Color.white
    static let text = Color(hex: 0x1A1C1A)
    static let secondaryText = Color(hex: 0x5A655D)
    static let border = Color(hex: 0xE2DDD2)
    static let forest = Color(hex: 0x234E3C)
    static let terracotta = Color(hex: 0xC86D51)
    static let gold = Color(hex: 0xD97706)
}

extension Color {
    init(hex: UInt, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xff) / 255,
            green: Double((hex >> 8) & 0xff) / 255,
            blue: Double(hex & 0xff) / 255,
            opacity: alpha
        )
    }
}

struct CupaCard<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        content
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(CupaTheme.card)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(CupaTheme.border, lineWidth: 1)
            }
    }
}

struct SectionHeader: View {
    let eyebrow: String
    let title: String
    let subtitle: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(eyebrow.uppercased())
                .font(.caption.weight(.bold))
                .tracking(1.4)
                .foregroundStyle(CupaTheme.terracotta)
            Text(title)
                .font(.largeTitle.bold())
                .foregroundStyle(CupaTheme.text)
            Text(subtitle)
                .font(.subheadline)
                .foregroundStyle(CupaTheme.secondaryText)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
