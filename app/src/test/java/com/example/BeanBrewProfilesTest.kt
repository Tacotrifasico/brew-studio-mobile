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
    @Test fun profilesArePerMethodAndDoNotEraseOtherAssociations() {
        val first = BeanBrewProfiles.write("{}", BeanBrewProfile("Prensa francesa", 26, 90))
        val second = BeanBrewProfiles.write(first, BeanBrewProfile("V60", 22, 94))
        assertEquals(26, BeanBrewProfiles.read(second, " PRENSA FRANCESA ")!!.clicks)
        assertEquals(94, BeanBrewProfiles.read(second, "v60")!!.temperatureC)
        assertNull(BeanBrewProfiles.read(second, "AeroPress"))
        assertEquals(BeanBrewProfiles.key("Método"), BeanBrewProfiles.key("metodo"))
        val changed = BeanBrewProfiles.write(second, BeanBrewProfile("V60", 20, 91))
        assertEquals(90, BeanBrewProfiles.read(changed, "Prensa francesa")!!.temperatureC)
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
        assertEquals(87, BeanBrewProfiles.read(read.brewProfilesJSON, "AeroPress")!!.temperatureC)
        val other = bean.copy(id = java.util.UUID.randomUUID().toString(), name = "Otro grano", brewProfilesJSON = "{}")
        runBlocking { db.beanDao().insertBean(other) }
        await { model.state.value.beansList.any { it.id == other.id } }
        model.selectCalculatorBean(other.id)
        model.onActionPrepare()
        assertEquals(93, model.state.value.activePrepTemp)
        assertEquals(18, model.state.value.activePrepClicks)
        model.selectCalculatorBean(bean.id)
        model.startTimer()
        model.selectCalculatorBean(other.id)
        model.onActionPrepare()
        assertEquals(bean.id, model.state.value.activePrepBeanId)
        assertEquals(91, model.state.value.activePrepTemp)
        model.cancelPreparation()
    }

    private fun await(condition: () -> Boolean) {
        repeat(300) { shadowOf(Looper.getMainLooper()).idle(); if (condition()) return; Thread.sleep(10) }
        assertTrue("Timed out waiting for Room/ViewModel", condition())
    }
}
