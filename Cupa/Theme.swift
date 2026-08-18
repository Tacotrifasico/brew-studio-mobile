import SwiftUI
import UIKit

enum CupaTheme {
    static let background = adaptive(light: 0xF4F1EA, dark: 0x111512)
    static let backgroundAlt = adaptive(light: 0xEBE6DC, dark: 0x1A211D)
    static let card = adaptive(light: 0xFFFFFF, dark: 0x202823)
    static let text = adaptive(light: 0x1A1C1A, dark: 0xF1F4F1)
    static let secondaryText = adaptive(light: 0x5A655D, dark: 0xB8C2BB)
    static let border = adaptive(light: 0xE2DDD2, dark: 0x38443D)
    static let forest = adaptive(light: 0x234E3C, dark: 0x6FC59B)
    static let terracotta = adaptive(light: 0xC86D51, dark: 0xF09A7D)
    static let gold = adaptive(light: 0xA95600, dark: 0xFFB45C)

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
