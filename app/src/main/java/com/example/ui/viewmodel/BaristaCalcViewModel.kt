package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.engine.RecipeIngredientInput
import com.example.data.repository.BrewRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class RatioCategory {
    ESPRESSO, INTENSO, BALANCE, CLARIDAD
}

enum class FreshnessState(val label: String, val colorHex: String, val microcopy: String) {
    NoDate("Sin fecha", "#60756A", "Agrega fecha de tostado para calcular frescura."),
    VeryFresh("Muy fresco", "#84AD92", "Muy fresco. Puede tener mucho gas; cuida el bloom."),
    InWindow("En ventana", "#3F7A63", "Buena ventana de uso. Perfil más estable."),
    Ideal("Puntal ideal", "#C28B46", "Punto ideal para muchas preparaciones filtradas."),
    Declining("Bajando", "#B76545", "Va perdiendo expresión. Ajusta molienda o temperatura."),
    Old("Viejo", "#8C5A2B", "Perfil más plano. Úsalo pronto o para recetas con leche/frías.")
}

data class FreshnessResult(
    val daysFromRoast: Int?,
    val daysFromOpen: Int?,
    val freshnessState: FreshnessState,
    val freshnessProgress: Float,
    val recommendation: String,
    val openStatusDetails: String?,
    val openWarning: String? = null
)

fun parseDateDaysDiff(dateStr: String): Int? {
    if (dateStr.isBlank()) return null
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = sdf.parse(dateStr.trim()) ?: return null
        
        val calDate = java.util.Calendar.getInstance().apply { time = parsedDate }
        calDate.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calDate.set(java.util.Calendar.MINUTE, 0)
        calDate.set(java.util.Calendar.SECOND, 0)
        calDate.set(java.util.Calendar.MILLISECOND, 0)
        
        val calToday = java.util.Calendar.getInstance()
        calToday.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calToday.set(java.util.Calendar.MINUTE, 0)
        calToday.set(java.util.Calendar.SECOND, 0)
        calToday.set(java.util.Calendar.MILLISECOND, 0)
        
        val diffMs = calToday.timeInMillis - calDate.timeInMillis
        TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
    } catch (e: Exception) {
        null
    }
}

fun calculateProgress(days: Int): Float {
    if (days < 0) return 0f
    return when {
        days <= 7 -> {
            (days / 7f) * 0.2f
        }
        days <= 21 -> {
            0.2f + ((days - 7) / 14f) * 0.2f
        }
        days <= 35 -> {
            0.4f + ((days - 21) / 14f) * 0.2f
        }
        days <= 60 -> {
            0.6f + ((days - 35) / 25f) * 0.2f
        }
        else -> {
            val excess = (days - 60) / 20f
            (0.8f + excess * 0.2f).coerceAtMost(1.0f)
        }
    }
}

fun calculateBeanFreshness(roastDate: String, firstUseDate: String): FreshnessResult {
    val daysFromRoast = parseDateDaysDiff(roastDate)
    val daysFromOpen = parseDateDaysDiff(firstUseDate)

    if (daysFromRoast == null) {
        return FreshnessResult(
            daysFromRoast = null,
            daysFromOpen = daysFromOpen,
            freshnessState = FreshnessState.NoDate,
            freshnessProgress = 0f,
            recommendation = "Agrega fecha de tostado para calcular frescura.",
            openStatusDetails = if (daysFromOpen != null) "Abierto hace $daysFromOpen días" else "Sin abrir (Hermético)"
        )
    }

    val state = when {
        daysFromRoast < 0 -> FreshnessState.NoDate
        daysFromRoast <= 7 -> FreshnessState.VeryFresh
        daysFromRoast <= 21 -> FreshnessState.InWindow
        daysFromRoast <= 35 -> FreshnessState.Ideal
        daysFromRoast <= 60 -> FreshnessState.Declining
        else -> FreshnessState.Old
    }

    val progress = calculateProgress(daysFromRoast)

    val rec = when (state) {
        FreshnessState.NoDate -> "Agrega fecha de tostado para calcular frescura."
        FreshnessState.VeryFresh -> "Puede tener mucho gas; se sugiere preinfusión larga de 45–50 segundos (bloom)."
        FreshnessState.InWindow -> "Excelente ventana de uso. El perfil de sabor es más estable y dulce."
        FreshnessState.Ideal -> "Punto ideal para filtrados. Buena retención de aromas y extracción equilibrada."
        FreshnessState.Declining -> "Va perdiendo expresión. Ajusta molienda un poco más fina o sube temperatura 1°C."
        FreshnessState.Old -> "Perfil más plano. Úsalo pronto o para recetas con leche/frías donde resalte intensidad."
    }

    val openAlert = if (daysFromOpen != null && daysFromOpen > 14) {
        "Abierto hace $daysFromOpen días. Puede perder aroma más rápido."
    } else null

    return FreshnessResult(
        daysFromRoast = daysFromRoast,
        daysFromOpen = daysFromOpen,
        freshnessState = state,
        freshnessProgress = progress,
        recommendation = rec,
        openStatusDetails = if (daysFromOpen != null) "Abierto hace $daysFromOpen días" else "Sin abrir (Hermético)",
        openWarning = openAlert
    )
}

data class BaristaPreset(
    val id: String,
    val method: String,
    val coffee: Float,
    val ratio: Float,
    val label: String
)

data class BaristaCalcState(
    // Calculator variables
    val method: String = "V60",
    val coffee: Float = 15.0f,
    val ratio: Float = 16.0f,
    val water: Int = 240,

    val coffeeInput: String = "15.0",
    val ratioInput: String = "16.0",
    val waterInput: String = "240",

    val microcopy: String = "Listo para preparar.",
    val ratioCategory: RatioCategory = RatioCategory.BALANCE,
    val snackbarMessage: String? = null,

    // Storage lists loaded from DB flows
    val beansList: List<Bean> = emptyList(),
    val equipmentList: List<Instrument> = emptyList(),
    val grindersList: List<Instrument> = emptyList(),
    val techniquesList: List<Technique> = emptyList(),
    val recipesList: List<Recipe> = emptyList(),
    val catasList: List<Cata> = emptyList(),
    val cupsList: List<Cup> = emptyList(),
    val experimentsList: List<LabExperiment> = emptyList(),

    // Counts for dashboard
    val beansCount: Int = 0,
    val equipmentCount: Int = 0,
    val grindersCount: Int = 0,
    val techniquesCount: Int = 0,
    val recipesCount: Int = 0,
    val cupsCount: Int = 0,
    val experimentsCount: Int = 0,

    // Active Preparation View States
    val activePrepMethod: String = "V60",
    val activePrepCoffee: Float = 15f,
    val activePrepWater: Int = 240,
    val activePrepRatio: Float = 16f,
    val activePrepTemp: Int = 92,
    val activePrepGrinder: String = "Comandante C40",
    val activePrepClicks: Int = 24,
    val activePrepBean: String = "Finca El Paraíso",
    val activePrepTechniqueName: String = "Estándar V60",
    
    // Live execution state
    val timerRunning: Boolean = false,
    val timerPaused: Boolean = false,
    val elapsedSeconds: Int = 0,
    val activeStepIndex: Int = 0,
    val activePrepSteps: List<TechniqueStep> = emptyList(),

    // Active Cata View States
    val cataMinutesElapsed: Int = 0,
    val selectedExpectedNotes: String = "Frutal, Cacao, Floral",
    val selectedFoundNotes: String = "",
    val cataTexture: String = "sedosa", // "ligera", "sedosa", "jugosa", "redonda", "densa", "seca"
    val cataCleanliness: String = "alta", // "baja", "media", "alta", "muy alta"
    val cataPersistence: String = "media", // "corta", "media", "larga"
    val cataFreeNotes: String = "",
    val cataRating: Float = 4.0f,

    // Active Lab Playground States
    val labMethod: String = "V60",
    val labCoffee: Float = 15f,
    val labWater: Int = 240,
    val labRatio: Float = 16f,
    val labTemp: Int = 92,
    val labGrinder: String = "Comandante C40",
    val labClicks: Int = 24,
    val labBean: String = "Finca El Paraíso",
    val labBeanFreshness: String = "en ventana", // "muy fresco", "en ventana", "punto ideal", "bajando", "viejo"
    val labEstTimeSeconds: Int = 180,
    val labNotes: String = "",

    // Diagnostic/hypotheses results (100% offline)
    val labPreviewIntensity: String = "Balanceado",
    val labPreviewBody: String = "Medio",
    val labPreviewClarity: String = "Alta",
    val labPreviewExtraction: String = "Extracción Ideal",
    val labPreviewRiesgos: String = "Ninguno detectado. Taza en ventana dorada.",
    val labRecommendationText: String = "Relación ideal para resaltar dulzura."
)

class BaristaCalcViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = BrewRepository(
        ratioPresetDao = database.ratioPresetDao(),
        ratioLastUsedDao = database.ratioLastUsedDao(),
        beanDao = database.beanDao(),
        instrumentDao = database.instrumentDao(),
        grinderProfileDao = database.grinderProfileDao(),
        grinderMethodSettingDao = database.grinderMethodSettingDao(),
        techniqueDao = database.techniqueDao(),
        techniqueStepDao = database.techniqueStepDao(),
        recipeDao = database.recipeDao(),
        cataDao = database.cataDao(),
        cupDao = database.cupDao(),
        labExperimentDao = database.labExperimentDao(),
        recipeIngredientDao = database.recipeIngredientDao()
    )

    private val _state = MutableStateFlow(BaristaCalcState())
    val state: StateFlow<BaristaCalcState> = _state.asStateFlow()

    private val baseRatios = mapOf(
        "V60" to 16.0f,
        "AeroPress" to 13.0f,
        "Prensa francesa" to 15.0f,
        "Chemex" to 16.0f,
        "Espresso" to 2.0f,
        "Moka" to 10.0f,
        "Cold brew" to 8.0f
    )

    val presets = listOf(
        BaristaPreset("1", "V60", 15.0f, 16.0f, "V60 · 15g · 1:16"),
        BaristaPreset("2", "AeroPress", 18.0f, 13.0f, "AeroPress · 18g · 1:13"),
        BaristaPreset("3", "Prensa francesa", 20.0f, 15.0f, "Prensa · 20g · 1:15"),
        BaristaPreset("4", "Chemex", 24.0f, 16.0f, "Chemex · 24g · 1:16"),
        BaristaPreset("5", "Espresso", 18.0f, 2.0f, "Espresso · 18g · 1:2"),
        BaristaPreset("6", "Moka", 18.0f, 10.0f, "Moka · 18g · 1:10"),
        BaristaPreset("7", "Cold brew", 50.0f, 8.0f, "Cold brew · 50g · 1:8")
    )

    private var timerJob: Job? = null
    private var cataTimerJob: Job? = null

    init {
        // Collect DB changes and update states
        viewModelScope.launch {
            repository.allBeans.collect { list ->
                _state.update { it.copy(beansList = list) }
            }
        }
        viewModelScope.launch {
            repository.allInstruments.collect { list ->
                _state.update { it.copy(equipmentList = list) }
            }
        }
        viewModelScope.launch {
            repository.allGrinders.collect { list ->
                _state.update { it.copy(grindersList = list) }
            }
        }
        viewModelScope.launch {
            repository.allTechniques.collect { list ->
                _state.update { it.copy(techniquesList = list) }
            }
        }
        viewModelScope.launch {
            repository.allRecipes.collect { list ->
                _state.update { it.copy(recipesList = list) }
            }
        }
        viewModelScope.launch {
            repository.allCatas.collect { list ->
                _state.update { it.copy(catasList = list) }
            }
        }
        viewModelScope.launch {
            repository.allCups.collect { list ->
                _state.update { it.copy(cupsList = list) }
            }
        }
        viewModelScope.launch {
            repository.allExperiments.collect { list ->
                _state.update { it.copy(experimentsList = list) }
            }
        }

        // Collect counts
        viewModelScope.launch {
            repository.beansCount.collect { c -> _state.update { it.copy(beansCount = c) } }
        }
        viewModelScope.launch {
            repository.instrumentsCount.collect { c -> _state.update { it.copy(equipmentCount = c, grindersCount = c) } }
        }
        viewModelScope.launch {
            repository.techniquesCount.collect { c -> _state.update { it.copy(techniquesCount = c) } }
        }
        viewModelScope.launch {
            repository.recipesCount.collect { c -> _state.update { it.copy(recipesCount = c) } }
        }
        viewModelScope.launch {
            repository.cupsCount.collect { c -> _state.update { it.copy(cupsCount = c) } }
        }
        viewModelScope.launch {
            repository.experimentsCount.collect { c -> _state.update { it.copy(experimentsCount = c) } }
        }

        // Populate Demo Data on Startup if empty
        viewModelScope.launch {
            delay(800)
            if (_state.value.beansCount == 0) {
                insertDemoData()
            }
            updateSensoryCategory(_state.value.ratio)
        }
    }

    private suspend fun insertDemoData() {
        // Beans demo
        repository.insertBean(Bean(
            roaster = "Brewther Roasters",
            name = "Finca El Paraíso",
            origin = "Colombia (Anaeróbico)",
            altitude = "1,900 msnm",
            process = "Doble Fermentación",
            roastDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() - 4 * 24 * 3600 * 1000)),
            firstUseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            notes = "Notas intensas a fresa fresca, durazno, yogurt de coco.",
            status = "abierto",
            stockGrams = 250f
        ))
        repository.insertBean(Bean(
            roaster = "Taller de Origen",
            name = "Geisha Esmeralda",
            origin = "Panamá (Jaramillo)",
            altitude = "1,750 msnm",
            process = "Lavado",
            roastDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() - 10 * 24 * 3600 * 1000)),
            firstUseDate = "",
            notes = "Notas florales nítidas a jazmín, bergamota y té negro.",
            status = "cerrado",
            stockGrams = 150f
        ))

        // Grinder demo
        repository.insertInstrument(Instrument(
            name = "Comandante C40 MK4",
            type = "GRINDER",
            brand = "Comandante",
            model = "C40 MK4",
            notes = "Muelas cónicas de acero nitrurado de alta precisión."
        ))
        repository.insertInstrument(Instrument(
            name = "Timemore Chestnut C3",
            type = "GRINDER",
            brand = "Timemore",
            model = "Chestnut C3",
            notes = "Perfecto para molido medio y V60 diario."
        ))

        // Equipment demo
        repository.insertInstrument(Instrument(name = "Hario V60 Decanter", type = "BREWER_METHOD", notes = "Vidrio borosilicato tradicional."))
        repository.insertInstrument(Instrument(name = "Kalita Wave 185", type = "BREWER_METHOD", notes = "Extractor de fondo plano para tazas balanceadas."))
        repository.insertInstrument(Instrument(name = "Fellow Stagg EKG", type = "KETTLE", notes = "Hervidor eléctrico con control térmico."))

        // Technique demo
        val techId = UUID.randomUUID().toString()
        val defaultMethodUuid = "11111111-1111-4000-8000-000000000001"
        val tech = Technique(
            id = techId,
            name = "V60 Tetsu Kasuya 4:6",
            methodId = defaultMethodUuid,
            doseG = 15.0f,
            waterMl = 240,
            ratio = 16.0f,
            temperatureC = 92,
            executionMode = "GUIDED",
            grindValue = 24.0,
            grindDescription = "Media Gruesa (24 clicks)",
            grindUnit = "CLICKS",
            notes = "Método diseñado por el Campeón Mundial Tetsu Kasuya. Divide el agua en pasajes de 40% y 60% para modular dulzura/acidez y cuerpo.",
            totalTimeSeconds = 210
        )
        val steps = listOf(
            TechniqueStep(stepNumber = 1, techniqueId = techId, title = "Bloom de gases", durationSeconds = 45, waterAddedMl = 50, waterAccumulatedMl = 50, intensity = "alta", gesture = "tap", stepNote = "Vierte rápido en espiral para mojar todo el café."),
            TechniqueStep(stepNumber = 2, techniqueId = techId, title = "Ajuste de Dulzura", durationSeconds = 45, waterAddedMl = 70, waterAccumulatedMl = 120, intensity = "media", gesture = "tap", stepNote = "Vierte suavemente al centro."),
            TechniqueStep(stepNumber = 3, techniqueId = techId, title = "Ajuste de Acidez", durationSeconds = 40, waterAddedMl = 40, waterAccumulatedMl = 160, intensity = "baja", gesture = "tap", stepNote = "Vierte suave."),
            TechniqueStep(stepNumber = 4, techniqueId = techId, title = "Estructurar Cuerpo", durationSeconds = 40, waterAddedMl = 40, waterAccumulatedMl = 200, intensity = "baja", gesture = "tap", stepNote = "Vierte suave."),
            TechniqueStep(stepNumber = 5, techniqueId = techId, title = "Claridad Final", durationSeconds = 40, waterAddedMl = 40, waterAccumulatedMl = 240, intensity = "baja", gesture = "tap", stepNote = "Completa el total de agua.")
        )
        repository.insertTechnique(tech, steps)
    }

    // --- BIDIRECTIONAL CALCULATORS ---
    fun onCoffeeChanged(input: String) {
        val rawInput = input.replace(',', '.')
        _state.update { it.copy(coffeeInput = input) }
        val parsed = rawInput.toFloatOrNull()
        if (parsed != null && parsed >= 1.0f) {
            val calcWater = (parsed * _state.value.ratio).toInt()
            _state.update { it.copy(
                coffee = parsed,
                water = calcWater,
                waterInput = calcWater.toString(),
                microcopy = "Listo para preparar con ${parsed}g de café."
            ) }
        }
    }

    fun onRatioChanged(input: String) {
        val rawInput = input.replace(',', '.')
        _state.update { it.copy(ratioInput = input) }
        val parsed = rawInput.toFloatOrNull()
        if (parsed != null && parsed >= 1.0f) {
            updateSensoryCategory(parsed)
            val calcWater = (_state.value.coffee * parsed).toInt()
            _state.update { it.copy(
                ratio = parsed,
                water = calcWater,
                waterInput = calcWater.toString(),
                microcopy = "Ratio ajustado a 1:${parsed}."
            ) }
        }
    }

    fun onWaterChanged(input: String) {
        _state.update { it.copy(waterInput = input) }
        val parsed = input.toIntOrNull()
        if (parsed != null && parsed >= 1) {
            val calcCoffee = Math.round((parsed / _state.value.ratio) * 10f) / 10f
            _state.update { it.copy(
                coffee = calcCoffee,
                coffeeInput = calcCoffee.toString(),
                water = parsed,
                microcopy = "Ajustado agua total a ${parsed}ml."
            ) }
        }
    }

    fun onMethodSelected(method: String) {
        val ratio = baseRatios[method] ?: 15.0f
        updateSensoryCategory(ratio)
        val calcWater = (_state.value.coffee * ratio).toInt()
        _state.update { it.copy(
            method = method,
            ratio = ratio,
            ratioInput = ratio.toString(),
            water = calcWater,
            waterInput = calcWater.toString(),
            microcopy = "Método cambiado a $method. Ratio sugerido 1:$ratio."
        ) }
    }

    fun applyPreset(preset: BaristaPreset) {
        updateSensoryCategory(preset.ratio)
        val calcWater = (preset.coffee * preset.ratio).toInt()
        _state.update { it.copy(
            method = preset.method,
            coffee = preset.coffee,
            coffeeInput = preset.coffee.toString(),
            ratio = preset.ratio,
            ratioInput = preset.ratio.toString(),
            water = calcWater,
            waterInput = calcWater.toString(),
            microcopy = "Se cargó el preset: ${preset.label}."
        ) }
    }

    fun adjustCoffee(amount: Float) {
        val newVal = (_state.value.coffee + amount).coerceAtLeast(1.0f)
        onCoffeeChanged(String.format(Locale.US, "%.1f", newVal))
    }

    fun adjustRatio(amount: Float) {
        val newVal = (_state.value.ratio + amount).coerceAtLeast(1.0f)
        onRatioChanged(String.format(Locale.US, "%.1f", newVal))
    }

    fun adjustWater(amount: Int) {
        val newVal = (_state.value.water + amount).coerceAtLeast(1)
        onWaterChanged(newVal.toString())
    }

    fun resetRatioToMethodBase() {
        val base = baseRatios[_state.value.method] ?: 15.0f
        onRatioChanged(base.toString())
    }

    fun onCoffeeFocusLost() {
        val check = _state.value.coffeeInput.toFloatOrNull()
        if (check == null || check < 1.0f) {
            onCoffeeChanged("15.0")
        }
    }

    fun onRatioFocusLost() {
        val check = _state.value.ratioInput.toFloatOrNull()
        if (check == null || check < 1.0f) {
            onRatioChanged("15.0")
        }
    }

    fun onWaterFocusLost() {
        val check = _state.value.waterInput.toIntOrNull()
        if (check == null || check < 1) {
            onWaterChanged("240")
        }
    }

    private fun updateSensoryCategory(ratio: Float) {
        val cat = when {
            ratio <= 3.0f -> RatioCategory.ESPRESSO
            ratio <= 10.0f -> RatioCategory.INTENSO
            ratio <= 16.0f -> RatioCategory.BALANCE
            else -> RatioCategory.CLARIDAD
        }
        _state.update { it.copy(ratioCategory = cat) }
    }

    // --- ACCIONES DE COMPARTIR VARIABLES ---
    fun onActionPrepare() {
        val currentVal = _state.value
        // Envia variables a Preparar
        _state.update { it.copy(
            activePrepMethod = currentVal.method,
            activePrepCoffee = currentVal.coffee,
            activePrepWater = currentVal.water,
            activePrepRatio = currentVal.ratio,
            activePrepTemp = 93,
            activePrepTechniqueName = "${currentVal.method} Estándar",
            activePrepSteps = generateQuickSteps(currentVal.method, currentVal.water)
        ) }
        showToast("Enviado a Preparar: Extracción de ${currentVal.coffee}g para ${currentVal.water}ml.")
    }

    fun onActionLab() {
        // Envia variables a Laboratorio
        _state.update { it.copy(
            labMethod = it.method,
            labCoffee = it.coffee,
            labWater = it.water,
            labRatio = it.ratio,
            labTemp = 93,
            labClicks = 18
        ) }
        calculateOfflineLabHypothesis()
        showToast("Enviado a Laboratorio de Variables.")
    }

    fun onActionFavorite() {
        viewModelScope.launch {
            repository.insertRecipe(Recipe(
                name = "Favorito ${_state.value.method} 1:${_state.value.ratio}",
                recipeKind = "BLACK_COFFEE",
                intention = "Agregado desde Barista Calc."
            ))
            showToast("Receta guardada en Favoritos del Almacén.")
        }
    }

    // --- PLAYBACK PREPARATION CONTROLLER ---
    fun loadPrepTechnique(techId: String) {
        viewModelScope.launch {
            val tech = _state.value.techniquesList.find { it.id == techId }
            if (tech != null) {
                val dbSteps = repository.getStepsForTechniqueSync(techId)
                val finalSteps = if (dbSteps.isNotEmpty()) dbSteps else generateQuickSteps(tech.methodId, tech.waterMl)
                _state.update { it.copy(
                    activePrepMethod = tech.methodId,
                    activePrepCoffee = tech.doseG,
                    activePrepWater = tech.waterMl,
                    activePrepRatio = tech.ratio,
                    activePrepTemp = tech.temperatureC,
                    activePrepTechniqueName = tech.name,
                    activePrepGrinder = tech.grindDescription ?: "Manual",
                    activePrepClicks = (tech.grindValue ?: 18.0).toInt(),
                    activePrepSteps = finalSteps
                ) }
            }
        }
    }

    fun startTimer() {
        if (_state.value.activePrepSteps.isEmpty()) {
            _state.update { it.copy(activePrepSteps = generateQuickSteps(it.activePrepMethod, it.activePrepWater)) }
        }

        _state.update { it.copy(timerRunning = true, timerPaused = false, elapsedSeconds = 0, activeStepIndex = 0) }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.timerRunning) {
                delay(1000)
                if (!_state.value.timerPaused) {
                    val currentElapsed = _state.value.elapsedSeconds + 1
                    _state.update { it.copy(elapsedSeconds = currentElapsed) }
                    
                    // check steps transition
                    val stepsList = _state.value.activePrepSteps
                    val totalDurationSoFar = stepsList.take(_state.value.activeStepIndex + 1).sumOf { it.durationSeconds }
                    
                    if (currentElapsed >= totalDurationSoFar) {
                        if (_state.value.activeStepIndex < stepsList.size - 1) {
                            _state.update { it.copy(activeStepIndex = it.activeStepIndex + 1) }
                            showToast("¡Siguiente paso de preparación!")
                        } else {
                            // finished
                            stopTimer()
                            showToast("¡Extracción de café completada con éxito!")
                        }
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        _state.update { it.copy(timerPaused = true) }
    }

    fun resumeTimer() {
        _state.update { it.copy(timerPaused = false) }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _state.update { it.copy(timerRunning = false, timerPaused = false) }
    }

    fun advanceStep() {
        val stepsList = _state.value.activePrepSteps
        if (_state.value.activeStepIndex < stepsList.size - 1) {
            val prevStepsSum = stepsList.take(_state.value.activeStepIndex + 1).sumOf { it.durationSeconds }
            _state.update { it.copy(
                activeStepIndex = it.activeStepIndex + 1,
                elapsedSeconds = prevStepsSum
            ) }
        } else {
            stopTimer()
        }
    }

    fun previousStep() {
        if (_state.value.activeStepIndex > 0) {
            val prevStepIndex = _state.value.activeStepIndex - 1
            val prevStepsSum = _state.value.activePrepSteps.take(prevStepIndex).sumOf { it.durationSeconds }
            _state.update { it.copy(
                activeStepIndex = prevStepIndex,
                elapsedSeconds = prevStepsSum
            ) }
        }
    }

    fun deleteTechnique(techId: String) {
        viewModelScope.launch {
            val tech = _state.value.techniquesList.find { it.id == techId }
            if (tech != null) {
                repository.deleteTechnique(tech)
                showToast("Técnica eliminada.")
            }
        }
    }

    fun createAndSaveTechnique(name: String, method: String, coffee: Float, water: Int, ratio: Float, temp: Int, grinder: String, clicks: Int, notes: String, stepTitles: List<String>, stepTimes: List<Int>, stepWaters: List<Int>) {
        viewModelScope.launch {
            val sumTime = stepTimes.sum()
            val techId = UUID.randomUUID().toString()
            val defaultMethodUuid = "11111111-1111-4000-8000-000000000001"
            val tech = Technique(
                id = techId,
                name = name,
                methodId = defaultMethodUuid,
                doseG = coffee,
                waterMl = water,
                ratio = ratio,
                temperatureC = temp,
                executionMode = "GUIDED",
                grindValue = clicks.toDouble(),
                grindDescription = grinder,
                notes = notes,
                totalTimeSeconds = sumTime
            )
            var accumulated = 0
            val steps = stepTitles.mapIndexed { idx, title ->
                accumulated += stepWaters.getOrElse(idx) { 0 }
                TechniqueStep(
                    techniqueId = techId,
                    stepNumber = idx + 1,
                    title = title,
                    durationSeconds = stepTimes.getOrElse(idx) { 30 },
                    waterAddedMl = stepWaters.getOrElse(idx) { 0 },
                    waterAccumulatedMl = accumulated,
                    intensity = if (idx == 0) "alta" else "media",
                    gesture = "tap",
                    stepNote = "Paso manual de extracción"
                )
            }
            repository.insertTechnique(tech, steps)
            showToast("Técnica '$name' creada y guardada en el Almacén.")
        }
    }

    // --- CATA & SENSORY STAGE ---
    fun setCataTexture(text: String) { _state.update { it.copy(cataTexture = text) } }
    fun setCataCleanliness(clean: String) { _state.update { it.copy(cataCleanliness = clean) } }
    fun setCataPersistence(per: String) { _state.update { it.copy(cataPersistence = per) } }
    fun setCataRating(r: Float) { _state.update { it.copy(cataRating = r) } }
    fun updateCataFoundNotes(notesSymbolSeparated: String) { _state.update { it.copy(selectedFoundNotes = notesSymbolSeparated) } }

    fun startCataMinutesTimer() {
        _state.update { it.copy(cataMinutesElapsed = 0) }
        cataTimerJob?.cancel()
        cataTimerJob = viewModelScope.launch {
            while (true) {
                delay(60000) // update every simulated/real minute
                _state.update { it.copy(cataMinutesElapsed = it.cataMinutesElapsed + 1) }
            }
        }
    }

    fun stopCataTimer() {
        cataTimerJob?.cancel()
    }

    fun getCupLifeStateLabel(): String {
        val minutes = _state.value.cataMinutesElapsed
        return when {
            minutes < 4 -> "Abierta (Gran calor y preinfusión volátil)"
            minutes < 10 -> "Ideal (Temperatura baja, resalta dulzura)"
            minutes < 16 -> "Cae (Se disipa la complejidad)"
            else -> "Agotada (Fría y de acidez plana)"
        }
    }

    fun saveCup(notesFound: String, notesExpected: String, score: Float, comment: String) {
        viewModelScope.launch {
            val cupId = UUID.randomUUID().toString()
            val cup = Cup(
                id = cupId,
                executedDoseG = _state.value.activePrepCoffee,
                executedWaterMl = _state.value.activePrepWater,
                executedRatio = _state.value.activePrepRatio,
                executedTemperatureC = _state.value.activePrepTemp,
                executedGrindSetting = _state.value.activePrepClicks.toString(),
                executedDurationSeconds = _state.value.elapsedSeconds.coerceAtLeast(120),
                cupLifeSeconds = _state.value.cataMinutesElapsed * 60,
                cupLifeState = "FRESH",
                rating = score.toDouble(),
                comment = comment,
                beanNameSnapshot = _state.value.activePrepBean.ifBlank { "Grano de la Casa" },
                recipeNameSnapshot = "Personal V60",
                techniqueNameSnapshot = _state.value.activePrepTechniqueName
            )
            repository.insertCup(cup)
            val cata = Cata(
                id = UUID.randomUUID().toString(),
                cupId = cupId,
                activeFlavorFamily = "FRUITY",
                selectedFlavorNotesJson = notesFound,
                sensoryWheelDescriptorsJson = "[]",
                textureLevel = "MEDIUM",
                cleanlinessLevel = "HIGH",
                persistenceLevel = "MEDIUM",
                overallScore = score.toDouble(),
                totalScaScore = score.toDouble() * 20.0,
                evaluatorNotes = comment,
                evaluatedAt = currentIso8601()
            )
            repository.insertCata(cata)
            showToast("Cup/Taza catada con éxito y registrada en el Almacén.")
        }
    }

    fun deleteCup(cup: Cup) {
        viewModelScope.launch {
            repository.deleteCup(cup)
            showToast("Taza eliminada del registro histórico.")
        }
    }

    fun pullCataToLab() {
        // Transfers cata read settings to Lab Screen variables
        _state.update { it.copy(
            labMethod = it.activePrepMethod,
            labCoffee = it.activePrepCoffee,
            labWater = it.activePrepWater,
            labRatio = it.activePrepRatio,
            labTemp = it.activePrepTemp,
            labClicks = it.activePrepClicks,
            labNotes = "Cargado de cata sensorial. Textura: ${it.cataTexture}, Limpieza: ${it.cataCleanliness}."
        ) }
        calculateOfflineLabHypothesis()
        showToast("Datos de cata sensorial llevados al Laboratorio de Hipótesis.")
    }

    // --- LABORATORIO DE VARIABLES (OFLLINE HYPOTHESIS) ---
    fun updateLabValue(
        methodName: String,
        coffee: Float,
        water: Int,
        ratio: Float,
        temp: Int,
        clicks: Int,
        bean: String,
        freshness: String,
        notes: String
    ) {
        _state.update { it.copy(
            labMethod = methodName,
            labCoffee = coffee,
            labWater = water,
            labRatio = ratio,
            labTemp = temp,
            labClicks = clicks,
            labBean = bean,
            labBeanFreshness = freshness,
            labNotes = notes
        ) }
        calculateOfflineLabHypothesis()
    }

    fun updateLabVariables(
        method: String? = null,
        coffee: Float? = null,
        water: Int? = null,
        ratio: Float? = null,
        temperature: Int? = null,
        clicks: Int? = null,
        bean: String? = null,
        freshness: String? = null,
        estTimeSeconds: Int? = null,
        notes: String? = null
    ) {
        _state.update { current ->
            current.copy(
                labMethod = method ?: current.labMethod,
                labCoffee = coffee ?: current.labCoffee,
                labWater = water ?: current.labWater,
                labRatio = ratio ?: current.labRatio,
                labTemp = temperature ?: current.labTemp,
                labClicks = clicks ?: current.labClicks,
                labBean = bean ?: current.labBean,
                labBeanFreshness = freshness ?: current.labBeanFreshness,
                labEstTimeSeconds = estTimeSeconds ?: current.labEstTimeSeconds,
                labNotes = notes ?: current.labNotes
            )
        }
        calculateOfflineLabHypothesis()
    }

    fun calculateOfflineLabHypothesis() {
        val s = _state.value
        val ratio = s.labRatio
        val temp = s.labTemp
        val clicks = s.labClicks
        val freshness = s.labBeanFreshness

        // 1. Intensity calculation
        val intensity = when {
            ratio <= 10.0f -> "Extradensa (Fuerte & Expresiva)"
            ratio <= 13.5f -> "Marcada e Intensa"
            ratio <= 16.5f -> "Equilibrada (Punto Dulce)"
            else -> "Ligera / Estilo Té"
        }

        // 2. Body calculation
        val body = when {
            ratio <= 11.0f -> "Alto y viscoso"
            clicks < 15 -> "Espeso y denso"
            clicks > 25 -> "Ligero y cristalino"
            else -> "Sedoso & Redondo"
        }

        // 3. Clarity calculation
        val clarity = when {
            clicks > 25 -> "Nítida (Excelente separación)"
            ratio >= 16f -> "Alta claridad sensorial"
            else -> "Baja, prima la intensidad sobre notas individuales"
        }

        // 4. Extraction estimation
        val extraction = when {
            temp >= 96 && clicks <= 12 -> "Sobre-extracción Extrema (Riesgo amargo/seco)"
            temp < 86 && ratio <= 12 -> "Sub-extracción (Agria y salada)"
            temp in 88..94 && clicks in 14..26 -> "Extracción Ideal del Brewther"
            else -> "Hipótesis aceptable. Verifique molienda."
        }

        // 5. Risks and diagnostic
        val risks = StringBuilder()
        if (temp > 95) risks.append("• Alta temperatura puede evaporar notas florales y dejar amargor.\n")
        if (temp < 87) risks.append("• Temperatura baja acentuará acidez frágil.\n")
        if (clicks < 13) risks.append("• Molienda fina obstruirá paso, causando taza turbia y astringencia.\n")
        if (clicks > 28) risks.append("• Molienda gruesa puede dar canalización y taza aguada.\n")
        if (freshness == "muy fresco") risks.append("• Grano joven (turbulencia de CO2). Necesitas preinfusión larga de 50s.\n")
        if (freshness == "viejo") risks.append("• Grano antiguo (perdió gas). Sube temperatura y muele más fino.\n")
        if (risks.isEmpty()) risks.append("Taza perfectamente calibrada. ¡Fórmula óptima!")

        val rec = when {
            ratio <= 3.0f -> "Hypótesis Corto Espresso: Produce alta concentración de aceites."
            freshness == "muy fresco" -> "Aumenta el tiempo del bloom para drenar dióxido de carbono."
            temp >= 94 -> "Vierte suave para no agitar de más y evitar sabores astringentes."
            else -> "Mantenimiento ideal del ciclo brew -> taste -> diagnose -> adjust."
        }

        _state.update { it.copy(
            labPreviewIntensity = intensity,
            labPreviewBody = body,
            labPreviewClarity = clarity,
            labPreviewExtraction = extraction,
            labPreviewRiesgos = risks.toString().trim(),
            labRecommendationText = rec
        ) }
    }

    fun playLabIdeaAsPrep() {
        // Mandar hipótesis directa a Preparar
        _state.update { it.copy(
            activePrepMethod = it.labMethod,
            activePrepCoffee = it.labCoffee,
            activePrepWater = it.labWater,
            activePrepRatio = it.labRatio,
            activePrepTemp = it.labTemp,
            activePrepTechniqueName = "Idea de Laboratorio",
            activePrepGrinder = "Manual",
            activePrepClicks = it.labClicks,
            activePrepSteps = generateQuickSteps(it.labMethod, it.labWater)
        ) }
        showToast("¡Hipótesis de Laboratorio enviada a Preparar!")
    }

    fun saveLabExperiment() {
        viewModelScope.launch {
            val s = _state.value
            val exp = LabExperiment(
                coffeeGrams = s.labCoffee,
                waterMl = s.labWater,
                ratio = s.labRatio,
                temperatureC = s.labTemp,
                grindSetting = s.labClicks.toString(),
                beanFreshnessDays = 7,
                estimatedTimeSeconds = s.labEstTimeSeconds,
                experimentHypothesis = s.labPreviewExtraction,
                experimentNotes = "Intensidad: ${s.labPreviewIntensity}. Notas: ${s.labNotes}",
                conclusionNotes = ""
            )
            repository.insertExperiment(exp)
            showToast("Experimento guardado en el archivo del Laboratorios.")
        }
    }

    fun saveLabAsRecipe(recipeName: String) {
        viewModelScope.launch {
            val s = _state.value
            val recipe = Recipe(
                name = recipeName,
                recipeKind = "BLACK_COFFEE",
                intention = "Fórmula calibrada en laboratorio: ${s.labNotes}"
            )
            repository.insertRecipe(recipe)
            showToast("Receta '$recipeName' guardada en favoritos.")
        }
    }

    fun saveLabAsTechnique(techniqueName: String) {
        viewModelScope.launch {
            val s = _state.value
            val defaultMethodUuid = "11111111-1111-4000-8000-000000000001"
            val technique = Technique(
                name = techniqueName,
                methodId = defaultMethodUuid,
                doseG = s.labCoffee,
                waterMl = s.labWater,
                ratio = s.labRatio,
                temperatureC = s.labTemp,
                grindValue = s.labClicks.toDouble(),
                grindDescription = s.labGrinder.ifBlank { "Manual" },
                notes = "Diseñada en Laboratorio. Hipótesis: ${s.labPreviewExtraction}.",
                totalTimeSeconds = s.labEstTimeSeconds
            )
            val steps = generateQuickSteps(s.labMethod, s.labWater)
            repository.insertTechnique(technique, steps)
            showToast("Técnica '$techniqueName' registrada en el Almacén.")
        }
    }

    fun resetLabVariables() {
        _state.update { it.copy(
            labMethod = "V60",
            labCoffee = 15f,
            labWater = 240,
            labRatio = 16f,
            labTemp = 92,
            labClicks = 24,
            labBean = "Finca El Paraíso",
            labBeanFreshness = "en ventana",
            labEstTimeSeconds = 180,
            labNotes = ""
        ) }
        calculateOfflineLabHypothesis()
        showToast("Variables de laboratorio reseteadas.")
    }

    fun deleteExperiment(experiment: LabExperiment) {
        viewModelScope.launch {
            repository.deleteExperiment(experiment)
            showToast("Experimento eliminado.")
        }
    }

    // --- ALMACÉN / STORAGE INVENTORY ---
    fun saveBean(
        id: String? = null,
        roaster: String,
        name: String,
        origin: String,
        altitude: String,
        process: String,
        roastDate: String,
        firstUseDate: String,
        notes: String,
        status: String,
        stockGrams: Float
    ) {
        viewModelScope.launch {
            val actualId = id ?: UUID.randomUUID().toString()
            val bean = Bean(
                id = actualId,
                roaster = roaster,
                name = name,
                origin = origin,
                altitude = altitude,
                process = process,
                roastDate = roastDate,
                firstUseDate = firstUseDate,
                notes = notes,
                status = status,
                stockGrams = stockGrams
            )
            repository.insertBean(bean)
            if (id == null) {
                showToast("Grano de café '$name' ingresado al Almacén.")
            } else {
                showToast("Grano de café '$name' actualizado.")
            }
        }
    }

    fun selectBeanForBrewing(bean: Bean) {
        val f = calculateBeanFreshness(bean.roastDate, bean.firstUseDate)
        _state.update { it.copy(
            activePrepBean = bean.name,
            selectedExpectedNotes = bean.notes,
            microcopy = "Preparando con ${bean.name} (${f.freshnessState.label}, tueste: ${bean.roastDate})"
        ) }
        showToast("Grano '${bean.name}' seleccionado para Preparar.")
    }

    fun selectBeanForLab(bean: Bean) {
        val f = calculateBeanFreshness(bean.roastDate, bean.firstUseDate)
        _state.update { current ->
            current.copy(
                labBean = bean.name,
                labBeanFreshness = when (f.freshnessState) {
                    FreshnessState.VeryFresh -> "muy fresco"
                    FreshnessState.InWindow -> "en ventana"
                    FreshnessState.Ideal -> "punto ideal"
                    FreshnessState.Declining -> "bajando"
                    FreshnessState.Old -> "viejo"
                    FreshnessState.NoDate -> "en ventana"
                },
                labNotes = "Grano: ${bean.name}. Proceso: ${bean.process}. ${bean.notes}"
            )
        }
        calculateOfflineLabHypothesis()
        showToast("Grano '${bean.name}' cargado en el Laboratorio.")
    }

    fun markBeanAsOpened(bean: Bean) {
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val updated = bean.copy(firstUseDate = todayStr, status = "abierto")
            repository.insertBean(updated)
            showToast("Café '${bean.name}' abierto hoy.")
        }
    }

    fun markBeanAsFinished(bean: Bean) {
        viewModelScope.launch {
            val updated = bean.copy(status = "terminado", stockGrams = 0f)
            repository.insertBean(updated)
            showToast("Café '${bean.name}' marcado como terminado.")
        }
    }

    fun addBean(roaster: String, name: String, origin: String, altitude: String, process: String, date: String, notes: String, stock: Float) {
        saveBean(
            id = null,
            roaster = roaster,
            name = name,
            origin = origin,
            altitude = altitude,
            process = process,
            roastDate = date,
            firstUseDate = "",
            notes = notes,
            status = "cerrado",
            stockGrams = stock
        )
    }

    fun deleteBean(bean: Bean) {
        viewModelScope.launch {
            repository.deleteBean(bean)
            showToast("Café retirado del Almacén.")
        }
    }

    fun addEquipment(name: String, type: String, notes: String) {
        viewModelScope.launch {
            repository.insertInstrument(Instrument(name = name, type = type, notes = notes))
            showToast("Equipo '$name' registrado.")
        }
    }

    fun deleteEquipment(equipment: Instrument) {
        viewModelScope.launch {
            repository.deleteInstrument(equipment)
            showToast("Equipo eliminado.")
        }
    }

    fun addGrinder(brand: String, model: String, clicks: String, calibracion: String) {
        viewModelScope.launch {
            repository.insertInstrument(Instrument(name = "$brand $model".trim(), type = "GRINDER", brand = brand, model = model, notes = calibracion))
            showToast("Molino '$model' guardado.")
        }
    }

    fun deleteGrinder(grinder: Instrument) {
        viewModelScope.launch {
            repository.deleteInstrument(grinder)
            showToast("Molino eliminado.")
        }
    }

    fun addRecipe(
        name: String,
        recipeKind: String = "BLACK_COFFEE",
        ingredientsSummary: String = "",
        stepsSummary: String = "",
        intention: String = "",
        suggestedMethod: String = "",
        tags: String = "",
        isFavorite: Boolean = false,
        ingredientsList: List<RecipeIngredientInput> = emptyList()
    ) {
        viewModelScope.launch {
            val recipe = Recipe(
                name = name,
                recipeKind = recipeKind,
                ingredientsSummary = ingredientsSummary,
                stepsSummary = stepsSummary,
                intention = intention,
                suggestedMethodId = if (suggestedMethod.isNotBlank()) suggestedMethod else null,
                tags = tags,
                isFavorite = isFavorite
            )
            repository.insertRecipe(recipe, ingredientsList)
            showToast("Receta '$name' archivada en el Almacén.")
        }
    }

    fun toggleRecipeFavorite(recipe: Recipe) {
        viewModelScope.launch {
            repository.insertRecipe(recipe.copy(isFavorite = !recipe.isFavorite))
        }
    }

    fun addExperiment(
        methodName: String,
        coffeeGrams: Float,
        waterMl: Int,
        ratio: Float,
        temp: Int,
        grindSize: String,
        notes: String
    ) {
        viewModelScope.launch {
            val exp = LabExperiment(
                coffeeGrams = coffeeGrams,
                waterMl = waterMl,
                ratio = ratio,
                temperatureC = temp,
                grindSetting = grindSize,
                beanFreshnessDays = 7,
                estimatedTimeSeconds = 180,
                experimentHypothesis = "Prueba de extracción",
                experimentNotes = notes,
                conclusionNotes = ""
            )
            repository.insertExperiment(exp)
            showToast("Experimento archivado en el Almacén.")
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.deleteRecipe(recipe)
            showToast("Receta de favoritos eliminada.")
        }
    }

    // --- PRESET UTILITIES ---
    private fun generateQuickSteps(method: String, waterMl: Int): List<TechniqueStep> {
        return when (method) {
            "Espresso" -> listOf(
                TechniqueStep(stepNumber = 1, techniqueId = "", title = "Extracción de Presión", durationSeconds = 30, waterAddedMl = waterMl, waterAccumulatedMl = waterMl, intensity = "alta", gesture = "tap", stepNote = "Manten la presión uniforme.")
            )
            "AeroPress" -> listOf(
                TechniqueStep(stepNumber = 1, techniqueId = "", title = "Preinfusión (Bloom)", durationSeconds = 30, waterAddedMl = 40, waterAccumulatedMl = 40, intensity = "alta", gesture = "tap", stepNote = "Remueve por 10 segundos."),
                TechniqueStep(stepNumber = 2, techniqueId = "", title = "Vertido de volumen", durationSeconds = 40, waterAddedMl = waterMl - 40, waterAccumulatedMl = waterMl, intensity = "media", gesture = "tap", stepNote = "Pon el émbolo para vacío."),
                TechniqueStep(stepNumber = 3, techniqueId = "", title = "Presión continua", durationSeconds = 30, waterAddedMl = 0, waterAccumulatedMl = waterMl, intensity = "alta", gesture = "tap", stepNote = "Presiona despacio.")
            )
            else -> listOf(
                TechniqueStep(stepNumber = 1, techniqueId = "", title = "Preinfusión Bloom", durationSeconds = 35, waterAddedMl = 50, waterAccumulatedMl = 50, intensity = "alta", gesture = "tap", stepNote = "Moja todo el grano uniformemente."),
                TechniqueStep(stepNumber = 2, techniqueId = "", title = "Primer Vertido", durationSeconds = 45, waterAddedMl = (waterMl - 50) / 2, waterAccumulatedMl = 50 + (waterMl - 50) / 2, intensity = "media", gesture = "tap", stepNote = "Vierte en círculos suaves."),
                TechniqueStep(stepNumber = 3, techniqueId = "", title = "Segundo Vertido final", durationSeconds = 40, waterAddedMl = waterMl - (50 + (waterMl - 50) / 2), waterAccumulatedMl = waterMl, intensity = "baja", gesture = "tap", stepNote = "Completa la secuencia.")
            )
        }
    }

    // --- SNACKBAR & NOTIFICATION UTILITIES ---
    fun clearSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }

    fun showToast(msg: String) {
        _state.update { it.copy(snackbarMessage = msg) }
    }
}
