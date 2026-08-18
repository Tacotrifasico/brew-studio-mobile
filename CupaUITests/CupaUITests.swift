import XCTest

final class CupaUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-ui-testing"]
        app.launch()
    }

    func testMainNavigationAndCalculatorInputs() {
        XCTAssertTrue(app.staticTexts["Taller del Brewther"].waitForExistence(timeout: 5))
        app.tabBars.buttons["Preparar"].tap()
        XCTAssertTrue(app.textFields["calculator.coffee"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.textFields["calculator.ratio"].exists)
        XCTAssertTrue(app.textFields["calculator.water"].exists)
        XCTAssertTrue(app.buttons["calculator.prepare"].exists)
        XCTAssertTrue(app.buttons["calculator.manageMethods"].exists)
        app.buttons["calculator.manageMethods"].tap()
        XCTAssertTrue(app.navigationBars["Gestionar métodos"].waitForExistence(timeout: 3))
        app.buttons["Listo"].tap()
    }

    func testCriticalSectionsOpenWithoutPlaceholders() {
        app.tabBars.buttons["Laboratorio"].tap()
        XCTAssertTrue(app.navigationBars["Laboratorio"].waitForExistence(timeout: 3))

        app.tabBars.buttons["Almacén"].tap()
        XCTAssertTrue(app.navigationBars["Almacén"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["Tazas"].exists)
        XCTAssertTrue(app.buttons["inventory.addCoffee"].exists)
        app.buttons["inventory.addCoffee"].tap()
        XCTAssertTrue(app.navigationBars["Agregar café"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.textFields["Nombre del café"].exists)
        app.buttons["Cancelar"].tap()
        app.buttons["Molinos"].tap()
        XCTAssertTrue(app.buttons["grinders.add"].exists)
        app.buttons["grinders.add"].tap()
        XCTAssertTrue(app.navigationBars["Agregar molino"].waitForExistence(timeout: 3))
        app.buttons["Cancelar"].tap()
        app.buttons["Equipos"].tap()
        XCTAssertTrue(app.buttons["equipment.add"].exists)
        app.buttons["equipment.add"].tap()
        XCTAssertTrue(app.navigationBars["Agregar equipo"].waitForExistence(timeout: 3))
        app.buttons["Cancelar"].tap()
        app.buttons["Recetas"].tap()
        XCTAssertTrue(app.buttons["recipes.add"].exists)
        XCTAssertTrue(app.buttons["recipes.import"].exists)
        app.buttons["Técnicas"].tap()
        XCTAssertTrue(app.buttons["techniques.add"].waitForExistence(timeout: 3))
        app.buttons["techniques.add"].tap()
        XCTAssertTrue(app.navigationBars["Nueva técnica"].waitForExistence(timeout: 3))
        app.buttons["Cancelar"].tap()

        app.tabBars.buttons["Cata"].tap()
        XCTAssertTrue(app.navigationBars["Cata"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["tasting.save"].exists)
        XCTAssertTrue(app.buttons["Nueva"].exists)
        XCTAssertTrue(app.buttons["tasting.cooling.start"].exists)
        XCTAssertTrue(app.buttons["tasting.cooling.reset"].exists)
        XCTAssertTrue(app.buttons["tasting.cooling.observe"].exists)
    }

    func testSettingsAndProfileAreReachable() {
        XCTAssertTrue(app.buttons["home.settings"].waitForExistence(timeout: 5))
        app.buttons["home.settings"].tap()
        XCTAssertTrue(app.navigationBars["Configuración"].waitForExistence(timeout: 3))
        app.buttons["Cerrar"].tap()

        app.buttons["home.profile"].tap()
        XCTAssertTrue(app.navigationBars["Brew Studio Hub"].waitForExistence(timeout: 3))
    }

    func testLabAltitudeAndTemperatureUnitFlow() {
        app.tabBars.buttons["Laboratorio"].tap()
        XCTAssertTrue(app.navigationBars["Laboratorio"].waitForExistence(timeout: 3))

        app.buttons["lab.altitude.toggle"].tap()
        XCTAssertTrue(app.buttons["lab.altitude.custom"].waitForExistence(timeout: 2))
        app.buttons["lab.altitude.custom"].tap()
        let city = app.textFields["Ciudad"]
        XCTAssertTrue(city.waitForExistence(timeout: 2))
        city.tap(); city.typeText("CDMX")
        let altitude = app.textFields["Altitud (msnm)"]
        altitude.tap(); altitude.typeText("2240")
        app.buttons["Guardar"].tap()
        XCTAssertTrue(app.staticTexts["lab.altitude.summary"].label.contains("2240"))

        let units = app.segmentedControls["lab.temperature.unit"]
        for _ in 0..<5 where !units.isHittable { app.swipeUp() }
        XCTAssertTrue(units.waitForExistence(timeout: 2))
        units.buttons["°F"].tap()
        XCTAssertTrue(units.buttons["°F"].isSelected)
    }

    func testSwitchingAllTabsKeepsCalculatorState() {
        app.tabBars.buttons["Preparar"].tap()
        let coffee = app.textFields["calculator.coffee"]
        XCTAssertTrue(coffee.waitForExistence(timeout: 3))
        coffee.tap()
        coffee.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: 8) + "18")
        app.tabBars.buttons["Cata"].tap()
        app.tabBars.buttons["Laboratorio"].tap()
        app.tabBars.buttons["Almacén"].tap()
        app.tabBars.buttons["Taller"].tap()
        app.tabBars.buttons["Preparar"].tap()
        XCTAssertEqual(app.textFields["calculator.coffee"].value as? String, "18")
    }
}
