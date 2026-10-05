package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.example.data.database.*
import com.example.ui.viewmodel.BaristaCalcViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BeanBrewProfilesTest {
    @Test fun fahrenheitStepsInclude195AndPersistFractionalCelsiusWithoutLosingOtherMethods() {
        val initial = BeanBrewProfile("V60", 22, 91)
        assertEquals("196 °F", initial.temperatureText(true))
        val favorite = initial.copy(temperatureC = initial.steppedTemperature(-1, true))
        assertEquals("195 °F", favorite.temperatureText(true))
        assertEquals(195.0, favorite.temperatureC * 1.8 + 32, 0.000001)
        val json = BeanBrewProfiles.write(BeanBrewProfiles.write("{}", BeanBrewProfile("AeroPress", 18, 88)), favorite)
        val restored = BeanBrewProfiles.read(json, "V60")!!
        assertEquals("195 °F", restored.temperatureText(true))
        assertEquals(88.0, BeanBrewProfiles.read(json, "AeroPress")!!.temperatureC, 0.0)
        assertEquals("194 °F", restored.copy(temperatureC = restored.steppedTemperature(-1, true)).temperatureText(true))
        assertEquals("196 °F", restored.copy(temperatureC = restored.steppedTemperature(1, true)).temperatureText(true))
        assertEquals(92.0, initial.steppedTemperature(1, false), 0.0)
        assertEquals(100.0, BeanBrewProfile("V60", 22, 100).steppedTemperature(1, true), 0.0)
        // Old integer JSON still decodes after adding precision.
        assertEquals(91.0, BeanBrewProfiles.read("{\"v60\":{\"methodName\":\"V60\",\"clicks\":22,\"temperatureC\":91}}", "V60")!!.temperatureC, 0.0)
    }
    @Test fun methodStartingPointsStaySeparateFromSavedBeanFavorites() {
        val expected = mapOf("V60" to (22 to 92), "AeroPress" to (18 to 88), "Prensa francesa" to (28 to 94),
            "Chemex" to (26 to 93), "Espresso" to (8 to 93), "Moka" to (12 to 90), "Cold brew" to (32 to 20))
        expected.forEach { (method, values) ->
            val profile = BeanBrewProfiles.resolve("{}", method)
            assertEquals(values.first, profile.clicks); assertEquals(values.second.toDouble(), profile.temperatureC, 0.0)
            assertNull(BeanBrewProfiles.read("{}", method))
        }
        val stored = BeanBrewProfiles.write("{}", BeanBrewProfile("V60", 31, 89))
        assertEquals(31, BeanBrewProfiles.resolve(stored, "v60").clicks)
        assertEquals(18, BeanBrewProfiles.resolve(stored, "AeroPress").clicks)
        assertEquals(89.0, BeanBrewProfiles.resolve(stored, "V60").temperatureC, 0.0)
    }

    @Test fun switchingMethodsRecallsBeanFavoritesImmediatelyWithoutPrepareButton() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val bean = Bean(roaster = "", name = "Por método", origin = "", altitude = "", process = "", roastDate = "", firstUseDate = "", notes = "", stockGrams = 100f,
            brewProfilesJSON = BeanBrewProfiles.write(BeanBrewProfiles.write("{}", BeanBrewProfile("V60", 27, 91)), BeanBrewProfile("AeroPress", 16, 86)))
        runBlocking { AppDatabase.getDatabase(app).beanDao().insertBean(bean) }
        val model = BaristaCalcViewModel(app)
        await { model.state.value.beansList.any { it.id == bean.id } }
        model.selectCalculatorBean(bean.id)
        model.onMethodSelected("AeroPress")
        assertEquals(16, model.state.value.activePrepClicks); assertEquals(86, model.state.value.activePrepTemp)
        model.onMethodSelected("Chemex")
        assertEquals(26, model.state.value.activePrepClicks); assertEquals(93, model.state.value.activePrepTemp)
        assertNull(BeanBrewProfiles.read(model.state.value.beansList.first { it.id == bean.id }.brewProfilesJSON, "Chemex"))
        model.onMethodSelected("V60")
        assertEquals(27, model.state.value.activePrepClicks); assertEquals(91, model.state.value.activePrepTemp)
        model.onActionLab()
        assertEquals(27, model.state.value.labClicks); assertEquals(91, model.state.value.labTemp)
    }

    @Test fun profilesArePerMethodAndDoNotEraseOtherAssociations() {
        val first = BeanBrewProfiles.write("{}", BeanBrewProfile("Prensa francesa", 26, 90))
        val second = BeanBrewProfiles.write(first, BeanBrewProfile("V60", 22, 94))
        assertEquals(26, BeanBrewProfiles.read(second, " PRENSA FRANCESA ")!!.clicks)
        assertEquals(94.0, BeanBrewProfiles.read(second, "v60")!!.temperatureC, 0.0)
        assertNull(BeanBrewProfiles.read(second, "AeroPress"))
        assertEquals(BeanBrewProfiles.key("Método"), BeanBrewProfiles.key("metodo"))
        val changed = BeanBrewProfiles.write(second, BeanBrewProfile("V60", 20, 91))
        assertEquals(90.0, BeanBrewProfiles.read(changed, "Prensa francesa")!!.temperatureC, 0.0)
        assertEquals(20, BeanBrewProfiles.read(changed, "V60")!!.clicks)
        assertNull(BeanBrewProfiles.read("not json", "V60"))
    }

    @Test fun migrationAddsProfilesWithoutLosingInventory() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(app)
            .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE beans (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL)")
                    db.execSQL("INSERT INTO beans VALUES ('existing', 'Mi café')")
                }
                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build())
        val db = helper.writableDatabase
        MIGRATION_9_10.migrate(db)
        db.query("SELECT name, brewProfilesJSON FROM beans WHERE id = 'existing'").use {
            assertTrue(it.moveToFirst()); assertEquals("Mi café", it.getString(0)); assertEquals("{}", it.getString(1))
        }
        helper.close()
    }

    @Test fun inventoryAndCalculatorSharePersistentProfilesAndTransferToLabAndPreparation() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = AppDatabase.getDatabase(app)
        val bean = Bean(roaster = "Tostador", name = "Perfil persistido", origin = "", altitude = "", process = "", roastDate = "", firstUseDate = "", notes = "", stockGrams = 100f)
        runBlocking { db.beanDao().insertBean(bean) }
        val model = BaristaCalcViewModel(app)
        await { model.state.value.beansList.any { it.id == bean.id } }
        model.selectCalculatorBean(bean.id)
        var saved = false
        model.saveBeanBrewProfile(bean.id, "V60", 22, 91) { saved = it }
        await { saved && model.state.value.beansList.first { it.id == bean.id }.brewProfilesJSON != "{}" }
        model.onMethodSelected("V60"); model.onActionLab(); model.onActionPrepare()
        assertEquals(bean.id, model.state.value.labBeanId)
        assertEquals(22, model.state.value.labClicks)
        assertEquals(91, model.state.value.activePrepTemp)
        assertEquals(bean.id, model.state.value.activePrepBeanId)
        saved = false
        model.saveBeanBrewProfile(bean.id, "AeroPress", 15, 87) { saved = it }
        await { saved && BeanBrewProfiles.read(model.state.value.beansList.first { it.id == bean.id }.brewProfilesJSON, "AeroPress") != null }
        model.onMethodSelected("AeroPress"); model.onActionPrepare()
        assertEquals(87, model.state.value.activePrepTemp)
        model.onMethodSelected("V60"); model.onActionPrepare()
        assertEquals(91, model.state.value.activePrepTemp)
        val restored = BaristaCalcViewModel(app)
        await { restored.state.value.beansList.any { it.id == bean.id } }
        assertEquals(bean.id, restored.state.value.calculatorBeanId)
        restored.onMethodSelected("V60"); restored.onActionLab()
        assertEquals(22, restored.state.value.labClicks)
        val read = runBlocking { db.beanDao().getBeanById(bean.id) }!!
        assertEquals(87.0, BeanBrewProfiles.read(read.brewProfilesJSON, "AeroPress")!!.temperatureC, 0.0)
        val other = bean.copy(id = java.util.UUID.randomUUID().toString(), name = "Otro grano", brewProfilesJSON = "{}")
        runBlocking { db.beanDao().insertBean(other) }
        await { model.state.value.beansList.any { it.id == other.id } }
        model.selectCalculatorBean(other.id)
        model.onActionPrepare()
        assertEquals(92, model.state.value.activePrepTemp)
        assertEquals(22, model.state.value.activePrepClicks)
        model.selectCalculatorBean(bean.id)
        model.startTimer()
        model.selectCalculatorBean(other.id)
        model.onActionPrepare()
        assertEquals(bean.id, model.state.value.activePrepBeanId)
        assertEquals(91, model.state.value.activePrepTemp)
        model.cancelPreparation()
    }

    @Test fun ronpotreroIsDefaultPermanentSampleAndPreservesUserProfiles() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val model = BaristaCalcViewModel(app)
        await { model.state.value.beansList.any { it.isSample } }
        assertEquals(SampleCoffee.ID, model.state.value.calculatorBeanId)
        val sample = model.state.value.beansList.first { it.isSample }
        assertEquals("Ronpotrero", sample.name)
        assertNull(sample.ownerUserId)
        assertEquals(92.0, BeanBrewProfiles.read(sample.brewProfilesJSON, "V60")!!.temperatureC, 0.0)
        var saved = false
        model.saveBeanBrewProfile(sample.id, "V60", 25, 91) { saved = it }
        await { saved && BeanBrewProfiles.read(model.state.value.beansList.first { it.isSample }.brewProfilesJSON, "V60")?.clicks == 25 }
        model.deleteBean(sample); model.markBeanAsFinished(sample)
        val reopened = BaristaCalcViewModel(app)
        await { reopened.state.value.beansList.any { it.isSample } }
        val after = reopened.state.value.beansList.filter { it.isSample }
        assertEquals(1, after.size)
        assertEquals(25, BeanBrewProfiles.read(after[0].brewProfilesJSON, "V60")!!.clicks)
        assertEquals("SYNCED", after[0].syncStatus)
        model.selectCalculatorBean(null)
        val explicitlyEmpty = BaristaCalcViewModel(app)
        await { explicitlyEmpty.state.value.beansList.any { it.isSample } }
        assertNull(explicitlyEmpty.state.value.calculatorBeanId)
    }

    private fun await(condition: () -> Boolean) {
        repeat(300) { shadowOf(Looper.getMainLooper()).idle(); if (condition()) return; Thread.sleep(10) }
        assertTrue("Timed out waiting for Room/ViewModel", condition())
    }
}
