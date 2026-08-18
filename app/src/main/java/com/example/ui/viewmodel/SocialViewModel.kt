package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.Recipe
import com.example.data.database.Technique
import com.example.data.remote.*
import com.example.data.remote.models.RemoteInboxItem
import com.example.data.remote.models.RemoteShare
import com.example.data.remote.models.RemoteActivityLog
import com.example.data.repository.AuthRepository
import com.example.data.repository.SocialRepository
import com.example.data.repository.SyncRepository
import com.example.feature.social.data.*
import com.example.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SocialUiState(
    val isLoggedIn: Boolean = false,
    val userId: String? = null,
    val displayName: String = "",
    val handle: String = "",
    val avatarColor: String = "#3F7A63",
    val email: String = "",
    
    // Feed data
    val feed: List<RemoteShare> = emptyList(),
    val isFeedLoading: Boolean = false,
    val feedError: String? = null,

    // Inbox data
    val inbox: List<RemoteInboxItem> = emptyList(),
    val isInboxLoading: Boolean = false,
    val inboxError: String? = null,

    // Activity log
    val activity: List<RemoteActivityLog> = emptyList(),

    // Sync operations state
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,

    // Auth screen transitions state
    val isAuthLoading: Boolean = false,
    val authError: String? = null,
    val isRegistrationSuccess: Boolean = false,

    // Stats
    val recipesCount: Int = 0,
    val techniquesCount: Int = 0
)

class SocialViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val sessionManager = SessionManager(application)
    
    private val authRemoteSource = AuthRemoteDataSource()
    private val socialRemoteSource = SocialRemoteDataSource()
    private val recipeRemoteSource = RecipeRemoteDataSource()
    private val techniqueRemoteSource = TechniqueRemoteDataSource()

    val authRepo = AuthRepository(authRemoteSource, sessionManager)
    val socialRepo = SocialRepository(
        remoteSource = socialRemoteSource,
        authRepo = authRepo,
        recipeDao = database.recipeDao(),
        techniqueDao = database.techniqueDao(),
        techniqueStepDao = database.techniqueStepDao()
    )
    val syncRepo = SyncRepository(
        authRepo = authRepo,
        recipeDao = database.recipeDao(),
        techniqueDao = database.techniqueDao(),
        techniqueStepDao = database.techniqueStepDao(),
        beanDao = database.beanDao(),
        recipeRemoteSource = recipeRemoteSource,
        techniqueRemoteSource = techniqueRemoteSource
    )

    // Domain Repositories & Use Cases
    val domainAuthRepo = AuthRepositoryImpl(authRepo, sessionManager)
    val domainRecipeRepo = RecipeRepositoryImpl(database.recipeDao(), database.recipeIngredientDao(), database.recipeStepDao())
    val domainTechniqueRepo = TechniqueRepositoryImpl(database.techniqueDao(), database.techniqueStepDao())
    val domainSocialRepo = SocialRepositoryImpl(socialRemoteSource)

    val shareRecipeUseCase = ShareRecipeUseCase(domainAuthRepo, domainRecipeRepo, domainSocialRepo)
    val shareTechniqueUseCase = ShareTechniqueUseCase(domainAuthRepo, domainTechniqueRepo, domainSocialRepo)
    val importRecipeShareUseCase = ImportRecipeShareUseCase(domainAuthRepo, domainRecipeRepo, domainSocialRepo)
    val importTechniqueShareUseCase = ImportTechniqueShareUseCase(domainAuthRepo, domainTechniqueRepo, domainSocialRepo)
    val forkRecipeShareUseCase = ForkRecipeShareUseCase(domainAuthRepo, domainRecipeRepo, domainSocialRepo)
    val forkTechniqueShareUseCase = ForkTechniqueShareUseCase(domainAuthRepo, domainTechniqueRepo, domainSocialRepo)
    val toggleLikeUseCase = ToggleShareLikeUseCase(domainAuthRepo, domainSocialRepo)

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    init {
        // Hydrate from SharedPreferences session
        _uiState.update { 
            it.copy(
                isLoggedIn = authRepo.isLoggedIn(),
                userId = authRepo.getUserId(),
                displayName = authRepo.getCachedDisplayName(),
                handle = authRepo.getCachedHandle(),
                avatarColor = authRepo.getCachedAvatarColor(),
                email = authRepo.getCachedEmail() ?: ""
            )
        }

        // Fetch counts from Room lists to populate stats
        viewModelScope.launch {
            database.recipeDao().getRecipesCount().collect { count ->
                _uiState.update { it.copy(recipesCount = count) }
            }
        }
        viewModelScope.launch {
            database.techniqueDao().getTechniquesCount().collect { count ->
                _uiState.update { it.copy(techniquesCount = count) }
            }
        }

        // Trigger safe remote fetch on startup
        if (authRepo.isLoggedIn()) {
            fetchRemoteData()
        }
    }

    fun fetchRemoteData() {
        fetchFeed()
        fetchInbox()
        fetchProfile()
        fetchActivity()
    }

    // AUTH ACTIONS

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null) }
            val result = authRepo.signIn(email, password)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoggedIn = true,
                        userId = authRepo.getUserId(),
                        displayName = authRepo.getCachedDisplayName(),
                        handle = authRepo.getCachedHandle(),
                        avatarColor = authRepo.getCachedAvatarColor(),
                        email = authRepo.getCachedEmail() ?: "",
                        isAuthLoading = false
                    )
                }
                fetchRemoteData()
                triggerSync() // Auto-run sync on successful login
            } else {
                _uiState.update { 
                    it.copy(
                        isAuthLoading = false, 
                        authError = result.exceptionOrNull()?.message ?: "Error al autenticar"
                    ) 
                }
            }
        }
    }

    fun register(email: String, password: String, displayName: String, handle: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null, isRegistrationSuccess = false) }
            val result = authRepo.signUp(email, password, displayName, handle)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoggedIn = true,
                        userId = authRepo.getUserId(),
                        displayName = authRepo.getCachedDisplayName(),
                        handle = authRepo.getCachedHandle(),
                        avatarColor = authRepo.getCachedAvatarColor(),
                        email = authRepo.getCachedEmail() ?: "",
                        isRegistrationSuccess = true,
                        isAuthLoading = false
                    )
                }
                fetchRemoteData()
                triggerSync()
            } else {
                _uiState.update { 
                    it.copy(
                        isAuthLoading = false, 
                        authError = result.exceptionOrNull()?.message ?: "Error al registrarse"
                    ) 
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.signOut()
            _uiState.update {
                SocialUiState( // Reset to guest state
                    isLoggedIn = false,
                    displayName = "",
                    handle = "",
                    feed = it.feed // retain feed cache for seamless guest reading
                )
            }
        }
    }

    fun loginDemo() {
        authRepo.loginDemo()
        _uiState.update {
            it.copy(
                isLoggedIn = true,
                userId = authRepo.getUserId(),
                displayName = authRepo.getCachedDisplayName(),
                handle = authRepo.getCachedHandle(),
                avatarColor = authRepo.getCachedAvatarColor(),
                email = authRepo.getCachedEmail() ?: "demo@brewstudio.app",
                authError = null
            )
        }
    }

    fun updateProfile(displayName: String, handle: String, avatarColor: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null) }
            val result = authRepo.updateProfile(displayName, handle, avatarColor)
            if (result.isSuccess) {
                val p = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        displayName = p.displayName,
                        handle = p.handle ?: "",
                        avatarColor = p.avatarColor ?: "#3F7A63",
                        isAuthLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authError = result.exceptionOrNull()?.message ?: "Error al guardar perfil"
                    )
                }
            }
        }
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            val result = authRepo.fetchCurrentProfile()
            if (result.isSuccess) {
                val p = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        displayName = p.displayName,
                        handle = p.handle ?: "",
                        avatarColor = p.avatarColor ?: "#3F7A63"
                    )
                }
            }
        }
    }

    // SOCIAL ACTIONS

    fun fetchFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFeedLoading = true, feedError = null) }
            val result = socialRepo.getFeed()
            if (result.isSuccess) {
                _uiState.update { it.copy(feed = result.getOrThrow(), isFeedLoading = false) }
            } else {
                _uiState.update { 
                    it.copy(
                        isFeedLoading = false, 
                        feedError = result.exceptionOrNull()?.message ?: "Error al descargar feed"
                    ) 
                }
            }
        }
    }

    fun fetchInbox() {
        viewModelScope.launch {
            if (!authRepo.isLoggedIn()) return@launch
            _uiState.update { it.copy(isInboxLoading = true, inboxError = null) }
            val result = socialRepo.getInbox()
            if (result.isSuccess) {
                _uiState.update { it.copy(inbox = result.getOrThrow(), isInboxLoading = false) }
            } else {
                _uiState.update { 
                    it.copy(
                        isInboxLoading = false, 
                        inboxError = result.exceptionOrNull()?.message ?: "Error de bandeja"
                    ) 
                }
            }
        }
    }

    fun fetchActivity() {
        viewModelScope.launch {
            if (!authRepo.isLoggedIn()) return@launch
            val result = socialRepo.getActivityTimeline()
            if (result.isSuccess) {
                _uiState.update { it.copy(activity = result.getOrThrow()) }
            }
        }
    }

    fun likeShare(shareId: String) {
        viewModelScope.launch {
            val result = socialRepo.likeShare(shareId)
            if (result.isSuccess) {
                // Instantly update counter locally for fast UI response
                _uiState.update { state ->
                    val updatedFeed = state.feed.map { item ->
                        if (item.id == shareId) {
                            val currentLikes = item.likesCount?.firstOrNull()?.count ?: 0
                            item.copy(likesCount = listOf(com.example.data.remote.models.CountWrapper(currentLikes + 1)))
                        } else item
                    }
                    state.copy(feed = updatedFeed)
                }
                fetchActivity()
            }
        }
    }

    fun unlikeShare(shareId: String) {
        viewModelScope.launch {
            val result = socialRepo.unlikeShare(shareId)
            if (result.isSuccess) {
                _uiState.update { state ->
                    val updatedFeed = state.feed.map { item ->
                        if (item.id == shareId) {
                            val currentLikes = item.likesCount?.firstOrNull()?.count ?: 1
                            item.copy(likesCount = listOf(com.example.data.remote.models.CountWrapper(kotlin.math.max(0, currentLikes - 1))))
                        } else item
                    }
                    state.copy(feed = updatedFeed)
                }
                fetchActivity()
            }
        }
    }

    fun saveShare(shareId: String) {
        viewModelScope.launch {
            socialRepo.saveShare(shareId)
            fetchActivity()
        }
    }

    fun importShare(share: RemoteShare, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            // Register in domain social repository first to ensure share payload is available
            val brewShare = mapRemoteShareToBrewShare(share)
            domainSocialRepo.publish(brewShare)

            val isRecipe = share.entityType == "recipe"
            val result = if (isRecipe) {
                importRecipeShareUseCase(share.id)
            } else {
                importTechniqueShareUseCase(share.id)
            }

            if (result.isSuccess) {
                // Also trigger remote import for backend syncing
                socialRepo.importShare(share)
                onResult(true, "Copia registrada exitosamente en tu biblioteca.")
                fetchActivity()
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Error al importar copia")
            }
        }
    }

    fun forkShare(share: RemoteShare, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val brewShare = mapRemoteShareToBrewShare(share)
            domainSocialRepo.publish(brewShare)

            val isRecipe = share.entityType == "recipe"
            val result = if (isRecipe) {
                forkRecipeShareUseCase(share.id)
            } else {
                forkTechniqueShareUseCase(share.id)
            }

            if (result.isSuccess) {
                socialRepo.forkShare(share)
                onResult(true, "Variante editable (Fork) guardada exitosamente en tu biblioteca.")
                fetchActivity()
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Error al forquear entidad")
            }
        }
    }

    private fun mapRemoteShareToBrewShare(share: RemoteShare): com.example.domain.model.BrewShare {
        val snap = share.payloadSnapshotJson
        val isRecipe = share.entityType == "recipe"
        val uid = authRepo.getUserId() ?: "local_user"

        val origAuthorId = share.originalAuthorUserId ?: share.fromUserId
        val origAuthorName = share.originalAuthorName ?: share.fromName ?: "Barista"
        val origEntityId = share.originalEntityId ?: share.entityId

        val attribution = com.example.domain.model.Attribution(
            required = true,
            mode = null,
            originalAuthorUserId = origAuthorId,
            originalAuthorName = origAuthorName,
            originalEntityId = origEntityId
        )

        val payload = if (isRecipe) {
            val domainRecipe = com.example.domain.model.DomainRecipe(
                id = share.entityId,
                ownerUserId = share.fromUserId,
                name = share.name,
                method = snap["method"] as? String ?: "V60",
                ingredientsSummary = snap["ingredientsSummary"] as? String ?: "",
                stepsSummary = snap["stepsSummary"] as? String ?: "",
                tags = snap["tags"] as? String ?: "",
                originalAuthorUserId = origAuthorId,
                originalAuthorName = origAuthorName,
                originalEntityId = origEntityId,
                attribution = attribution
            )
            com.example.domain.model.SharedPayload.RecipePayload(domainRecipe)
        } else {
            val domainTech = com.example.domain.model.PreparationTechnique(
                id = share.entityId,
                ownerUserId = share.fromUserId,
                name = share.name,
                method = snap["method"] as? String ?: "V60",
                coffeeGrams = (snap["coffeeGrams"] as? Number)?.toDouble() ?: (snap["doseG"] as? Number)?.toDouble() ?: 15.0,
                waterMl = (snap["waterMl"] as? Number)?.toDouble() ?: 240.0,
                grind = (snap["grind"] as? Number)?.toDouble() ?: (snap["grindValue"] as? Number)?.toDouble() ?: 18.0,
                temperatureC = (snap["temperature"] as? Number)?.toDouble() ?: (snap["temperatureC"] as? Number)?.toDouble() ?: 93.0,
                totalTimeSeconds = (snap["totalTimeSeconds"] as? Number)?.toInt() ?: 180,
                executionMode = snap["executionMode"] as? String ?: "GUIDED",
                executionSteps = emptyList(),
                originalAuthorUserId = origAuthorId,
                originalAuthorName = origAuthorName,
                originalEntityId = origEntityId,
                attribution = attribution
            )
            com.example.domain.model.SharedPayload.TechniquePayload(domainTech)
        }

        return com.example.domain.model.BrewShare(
            id = share.id,
            entityType = if (isRecipe) com.example.domain.model.ShareEntityType.RECIPE else com.example.domain.model.ShareEntityType.TECHNIQUE,
            entityId = share.entityId,
            name = share.name,
            subtitle = share.subtitle,
            fromUserId = share.fromUserId,
            fromDisplayName = share.fromName ?: "Barista",
            fromHandle = share.fromHandle,
            targetUserId = share.targetUserId,
            visibility = if (share.visibility == "DIRECT" || share.visibility == "direct") com.example.domain.model.ShareVisibility.DIRECT else com.example.domain.model.ShareVisibility.PUBLIC,
            message = share.message,
            attribution = attribution,
            payload = payload
        )
    }

    fun shareRecipeToFeed(recipe: Recipe, message: String, targetUserId: String? = null, visibility: String = "public", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = socialRepo.shareRecipe(recipe, message, targetUserId, visibility)
            if (result.isSuccess) {
                onResult(true, "Receta compartida exitosamente.")
                fetchFeed()
                fetchActivity()
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Error al compartir")
            }
        }
    }

    fun shareTechniqueToFeed(tech: Technique, steps: List<com.example.data.database.TechniqueStep>, message: String, targetUserId: String? = null, visibility: String = "public", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = socialRepo.shareTechnique(tech, steps, message, targetUserId, visibility)
            if (result.isSuccess) {
                onResult(true, "Técnica compartida exitosamente.")
                fetchFeed()
                fetchActivity()
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Error al compartir técnica")
            }
        }
    }

    // SYNC OPERATIONS

    fun triggerSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = null) }
            val result = syncRepo.synchronizeAll()
            if (result.isSuccess) {
                _uiState.update { 
                    it.copy(isSyncing = false, syncMessage = result.getOrThrow()) 
                }
                fetchRemoteData()
            } else {
                _uiState.update { 
                    it.copy(
                        isSyncing = false, 
                        syncMessage = "Fallo en sincronización: " + (result.exceptionOrNull()?.message ?: "Error")
                    ) 
                }
            }
        }
    }
}
