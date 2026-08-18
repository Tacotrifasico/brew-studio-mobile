import SwiftUI
import UIKit

enum CupaPalette {
    enum Light {
        static let background: UInt = 0xF4F1EA, backgroundAlt: UInt = 0xEBE6DC, card: UInt = 0xFFFFFF
        static let text: UInt = 0x1A1C1A, secondaryText: UInt = 0x5A655D, border: UInt = 0xE2DDD2
        static let forest: UInt = 0x234E3C, terracotta: UInt = 0xAD4F36, gold: UInt = 0xA95600
        static let espresso: UInt = 0xA9472E, clarity: UInt = 0x2E5A44, onAccent: UInt = 0xFFFFFF
    }

    enum Dark {
        static let background: UInt = 0x111512, backgroundAlt: UInt = 0x1A211D, card: UInt = 0x202823
        static let text: UInt = 0xF1F4F1, secondaryText: UInt = 0xB8C2BB, border: UInt = 0x38443D
        static let forest: UInt = 0x6FC59B, terracotta: UInt = 0xF09A7D, gold: UInt = 0xFFB45C
        static let espresso: UInt = 0xF09A7D, clarity: UInt = 0x78CDA3, onAccent: UInt = 0x111512
    }
}

enum CupaTheme {
    static let background = adaptive(light: CupaPalette.Light.background, dark: CupaPalette.Dark.background)
    static let backgroundAlt = adaptive(light: CupaPalette.Light.backgroundAlt, dark: CupaPalette.Dark.backgroundAlt)
    static let card = adaptive(light: CupaPalette.Light.card, dark: CupaPalette.Dark.card)
    static let text = adaptive(light: CupaPalette.Light.text, dark: CupaPalette.Dark.text)
    static let secondaryText = adaptive(light: CupaPalette.Light.secondaryText, dark: CupaPalette.Dark.secondaryText)
    static let border = adaptive(light: CupaPalette.Light.border, dark: CupaPalette.Dark.border)
    static let forest = adaptive(light: CupaPalette.Light.forest, dark: CupaPalette.Dark.forest)
    static let terracotta = adaptive(light: CupaPalette.Light.terracotta, dark: CupaPalette.Dark.terracotta)
    static let gold = adaptive(light: CupaPalette.Light.gold, dark: CupaPalette.Dark.gold)
    static let espresso = adaptive(light: CupaPalette.Light.espresso, dark: CupaPalette.Dark.espresso)
    static let clarity = adaptive(light: CupaPalette.Light.clarity, dark: CupaPalette.Dark.clarity)
    static let onAccent = adaptive(light: CupaPalette.Light.onAccent, dark: CupaPalette.Dark.onAccent)

    private static func adaptive(light: UInt, dark: UInt) -> Color {
        Color(uiColor: UIColor { traits in UIColor(hex: traits.userInterfaceStyle == .dark ? dark : light) })
    }
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

extension UIColor {
    convenience init(hex: UInt, alpha: CGFloat = 1) {
        self.init(red: CGFloat((hex >> 16) & 0xff) / 255, green: CGFloat((hex >> 8) & 0xff) / 255, blue: CGFloat(hex & 0xff) / 255, alpha: alpha)
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
