package com.example.social

import com.example.domain.model.*
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.RecipeRepository
import com.example.domain.repository.SocialRepository
import com.example.domain.repository.TechniqueRepository
import com.example.domain.usecase.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FakeAuthRepository : AuthRepository {
    private val _currentUser = MutableStateFlow<BrewUser?>(null)
    override val currentUser: Flow<BrewUser?> = _currentUser

    fun setUser(user: BrewUser?) {
        _currentUser.value = user
    }

    override fun getCurrentUserSync(): BrewUser? = _currentUser.value

    override suspend fun signIn(email: String, password: String): Result<BrewUser> {
        val user = BrewUser("usr_1", "Emiliano", "@emiliano")
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun signOut() {
        _currentUser.value = null
    }
}

class FakeRecipeRepository : RecipeRepository {
    val recipes = mutableMapOf<String, DomainRecipe>()

    override fun observeMine(): Flow<List<DomainRecipe>> {
        return MutableStateFlow(recipes.values.toList())
    }

    override suspend fun get(id: String): DomainRecipe? = recipes[id]

    override suspend fun save(recipe: DomainRecipe) {
        recipes[recipe.id] = recipe
    }
}

class FakeTechniqueRepository : TechniqueRepository {
    val techniques = mutableMapOf<String, PreparationTechnique>()

    override fun observeMine(): Flow<List<PreparationTechnique>> {
        return MutableStateFlow(techniques.values.toList())
    }

    override suspend fun get(id: String): PreparationTechnique? = techniques[id]

    override suspend fun save(technique: PreparationTechnique) {
        techniques[technique.id] = technique
    }
}

class FakeSocialRepository : SocialRepository {
    val shares = mutableMapOf<String, BrewShare>()

    override fun observeFeed(): Flow<List<BrewShare>> {
        return MutableStateFlow(shares.values.filter { it.visibility == ShareVisibility.PUBLIC })
    }

    override fun observeInbox(userId: String): Flow<List<BrewShare>> {
        return MutableStateFlow(shares.values.filter { it.visibility == ShareVisibility.DIRECT && it.targetUserId == userId })
    }

    override suspend fun publish(share: BrewShare): Result<BrewShare> {
        shares[share.id] = share
        return Result.success(share)
    }

    override suspend fun toggleLike(shareId: String, userId: String): Result<Boolean> {
        val share = shares[shareId] ?: return Result.failure(IllegalArgumentException("Not found"))
        val likes = share.likes.toMutableSet()
        val isLiked: Boolean
        if (likes.contains(userId)) {
            likes.remove(userId)
            isLiked = false
        } else {
            likes.add(userId)
            isLiked = true
        }
        shares[shareId] = share.copy(likes = likes)
        return Result.success(isLiked)
    }

    override suspend fun importShare(shareId: String, currentUserId: String): Result<BrewShare> {
        val share = shares[shareId] ?: return Result.failure(IllegalArgumentException("Not found"))
        val saves = share.saves.toMutableSet()
        saves.add(currentUserId)
        val updated = share.copy(saves = saves)
        shares[shareId] = updated
        return Result.success(updated)
    }

    override suspend fun forkShare(shareId: String, currentUserId: String): Result<BrewShare> {
        return importShare(shareId, currentUserId)
    }

    override suspend fun getShare(shareId: String): BrewShare? = shares[shareId]
}

class SocialSystemUnitTest {

    private lateinit var authRepo: FakeAuthRepository
    private lateinit var recipeRepo: FakeRecipeRepository
    private lateinit var techniqueRepo: FakeTechniqueRepository
    private lateinit var socialRepo: FakeSocialRepository

    private lateinit var shareRecipeUseCase: ShareRecipeUseCase
    private lateinit var shareTechniqueUseCase: ShareTechniqueUseCase
    private lateinit var importRecipeShareUseCase: ImportRecipeShareUseCase
    private lateinit var importTechniqueShareUseCase: ImportTechniqueShareUseCase
    private lateinit var forkRecipeShareUseCase: ForkRecipeShareUseCase
    private lateinit var forkTechniqueShareUseCase: ForkTechniqueShareUseCase
    private lateinit var toggleLikeUseCase: ToggleShareLikeUseCase

    private val emiliano = BrewUser("user_emiliano", "Emiliano", "@emiliano-brew")
    private val ana = BrewUser("user_ana", "Ana", "@ana-barista")
    private val luis = BrewUser("user_luis", "Luis", "@luis-coffee")

    @Before
    fun setUp() {
        authRepo = FakeAuthRepository()
        recipeRepo = FakeRecipeRepository()
        techniqueRepo = FakeTechniqueRepository()
        socialRepo = FakeSocialRepository()

        shareRecipeUseCase = ShareRecipeUseCase(authRepo, recipeRepo, socialRepo)
        shareTechniqueUseCase = ShareTechniqueUseCase(authRepo, techniqueRepo, socialRepo)
        importRecipeShareUseCase = ImportRecipeShareUseCase(authRepo, recipeRepo, socialRepo)
        importTechniqueShareUseCase = ImportTechniqueShareUseCase(authRepo, techniqueRepo, socialRepo)
        forkRecipeShareUseCase = ForkRecipeShareUseCase(authRepo, recipeRepo, socialRepo)
        forkTechniqueShareUseCase = ForkTechniqueShareUseCase(authRepo, techniqueRepo, socialRepo)
        toggleLikeUseCase = ToggleShareLikeUseCase(authRepo, socialRepo)
    }

    @Test
    fun testCannotShareWithoutAuthentication() = runBlocking {
        authRepo.setUser(null)
        val result = shareRecipeUseCase("recipe_1", ShareDestination.Feed, "Hola")
        assertTrue(result.isFailure)
        assertEquals("Debe estar autenticado para compartir", result.exceptionOrNull()?.message)
    }

    @Test
    fun testCannotShareOthersEntity() = runBlocking {
        authRepo.setUser(ana)
        val recipe = DomainRecipe(
            id = "recipe_1",
            ownerUserId = emiliano.id,
            name = "V60 Cítrico"
        )
        recipeRepo.save(recipe)

        val result = shareRecipeUseCase("recipe_1", ShareDestination.Feed, "Intento compartir de otro")
        assertTrue(result.isFailure)
        assertEquals("No puede publicar como propia una receta de otro usuario", result.exceptionOrNull()?.message)
    }

    @Test
    fun testShareRecipeToFeedSuccess() = runBlocking {
        authRepo.setUser(emiliano)
        val recipe = DomainRecipe(
            id = "rec_emiliano_1",
            ownerUserId = emiliano.id,
            name = "V60 Lavado Cítrico",
            method = "V60",
            ingredients = listOf(RecipeIngredient(name = "Café Etiopía", amount = 20f)),
            steps = listOf(RecipeStepItem(instruction = "Vertido inicial", stepNumber = 1))
        )
        recipeRepo.save(recipe)

        val result = shareRecipeUseCase("rec_emiliano_1", ShareDestination.Feed, "Muy recomendado")
        assertTrue(result.isSuccess)

        val share = result.getOrThrow()
        assertEquals(ShareEntityType.RECIPE, share.entityType)
        assertEquals("V60 Lavado Cítrico", share.name)
        assertEquals(ShareVisibility.PUBLIC, share.visibility)
        assertNull(share.targetUserId)
        assertEquals(emiliano.id, share.fromUserId)
        assertEquals("Muy recomendado", share.message)
        assertEquals(emiliano.id, share.attribution.originalAuthorUserId)
    }

    @Test
    fun testShareRecipeDirectToUser() = runBlocking {
        authRepo.setUser(emiliano)
        val recipe = DomainRecipe(
            id = "rec_emiliano_2",
            ownerUserId = emiliano.id,
            name = "AeroPress Espresso Style"
        )
        recipeRepo.save(recipe)

        val result = shareRecipeUseCase("rec_emiliano_2", ShareDestination.Direct(ana.id, ana.displayName), "Prueba esto Ana")
        assertTrue(result.isSuccess)

        val share = result.getOrThrow()
        assertEquals(ShareVisibility.DIRECT, share.visibility)
        assertEquals(ana.id, share.targetUserId)
    }

    @Test
    fun testImportRecipePreservesOriginalAuthorAndSavesInRecipeRepo() = runBlocking {
        // Emiliano publishes recipe
        authRepo.setUser(emiliano)
        val recipe = DomainRecipe(
            id = "rec_emiliano_3",
            ownerUserId = emiliano.id,
            name = "Cold Brew Concentrado"
        )
        recipeRepo.save(recipe)
        val share = shareRecipeUseCase("rec_emiliano_3", ShareDestination.Feed, "Disfruten").getOrThrow()

        // Ana imports the recipe
        authRepo.setUser(ana)
        val importResult = importRecipeShareUseCase(share.id)
        assertTrue(importResult.isSuccess)

        val importedRecipe = importResult.getOrThrow()
        assertNotEquals(recipe.id, importedRecipe.id)
        assertEquals(ana.id, importedRecipe.ownerUserId)
        assertEquals("Cold Brew Concentrado", importedRecipe.name)
        assertEquals(emiliano.id, importedRecipe.originalAuthorUserId)
        assertEquals(emiliano.displayName, importedRecipe.originalAuthorName)
        assertEquals(AttributionMode.IMPORT, importedRecipe.attribution?.mode)

        // Verify it is saved in Ana's RecipeRepository
        assertNotNull(recipeRepo.get(importedRecipe.id))
    }

    @Test
    fun testForkRecipeAppendsForkSuffixAndSavesInRecipeRepo() = runBlocking {
        authRepo.setUser(emiliano)
        val recipe = DomainRecipe(
            id = "rec_emiliano_4",
            ownerUserId = emiliano.id,
            name = "Prensa Francesa Intensa"
        )
        recipeRepo.save(recipe)
        val share = shareRecipeUseCase("rec_emiliano_4", ShareDestination.Feed, "Prensa").getOrThrow()

        authRepo.setUser(ana)
        val forkResult = forkRecipeShareUseCase(share.id)
        assertTrue(forkResult.isSuccess)

        val forkedRecipe = forkResult.getOrThrow()
        assertEquals(ana.id, forkedRecipe.ownerUserId)
        assertEquals("Prensa Francesa Intensa · fork", forkedRecipe.name)
        assertEquals(emiliano.id, forkedRecipe.originalAuthorUserId)
        assertEquals(AttributionMode.FORK, forkedRecipe.attribution?.mode)
    }

    @Test
    fun testCriticalAuthorshipChainThroughMultipleForks() = runBlocking {
        // Step 1: Emiliano creates Recipe A
        authRepo.setUser(emiliano)
        val recipeA = DomainRecipe(
            id = "rec_A",
            ownerUserId = emiliano.id,
            name = "Receta Original A",
            originalAuthorUserId = emiliano.id,
            originalAuthorName = emiliano.displayName,
            originalEntityId = "rec_A"
        )
        recipeRepo.save(recipeA)
        val share1 = shareRecipeUseCase("rec_A", ShareDestination.Feed, "Comparto A").getOrThrow()

        // Step 2: Ana imports Recipe A -> Recipe B
        authRepo.setUser(ana)
        val recipeB = importRecipeShareUseCase(share1.id).getOrThrow()
        val share2 = shareRecipeUseCase(recipeB.id, ShareDestination.Feed, "Comparto B").getOrThrow()

        // Step 3: Luis forks Recipe B -> Recipe C
        authRepo.setUser(luis)
        val recipeC = forkRecipeShareUseCase(share2.id).getOrThrow()

        // VERIFY: Luis is owner, Ana was received from, BUT original author is STILL Emiliano!
        assertEquals(luis.id, recipeC.ownerUserId)
        assertEquals(emiliano.id, recipeC.originalAuthorUserId)
        assertEquals(emiliano.displayName, recipeC.originalAuthorName)
        assertEquals("rec_A", recipeC.originalEntityId)
        assertEquals(ana.id, recipeC.socialSource?.fromUserId)
    }

    @Test
    fun testImportTechniquePreservesAllExecutionStepsAndParams() = runBlocking {
        authRepo.setUser(emiliano)
        val tech = PreparationTechnique(
            id = "tech_1",
            ownerUserId = emiliano.id,
            name = "V60 3 Vertidos",
            method = "V60",
            coffeeGrams = 18.0,
            waterMl = 300.0,
            grind = 16.0,
            temperatureC = 92.0,
            totalTimeSeconds = 190,
            executionMode = "GUIDED",
            executionSteps = listOf(
                ExecutionStep(stepNumber = 1, title = "Bloom", durationSeconds = 30, waterAddedMl = 50, waterAccumulatedMl = 50),
                ExecutionStep(stepNumber = 2, title = "Vertido 1", durationSeconds = 45, waterAddedMl = 125, waterAccumulatedMl = 175)
            )
        )
        techniqueRepo.save(tech)
        val share = shareTechniqueUseCase("tech_1", ShareDestination.Feed, "Mi técnica favorita").getOrThrow()

        authRepo.setUser(ana)
        val importResult = importTechniqueShareUseCase(share.id)
        assertTrue(importResult.isSuccess)

        val importedTech = importResult.getOrThrow()
        assertNotEquals(tech.id, importedTech.id)
        assertEquals(ana.id, importedTech.ownerUserId)
        assertEquals("V60 3 Vertidos", importedTech.name)
        assertEquals(18.0, importedTech.coffeeGrams!!, 0.01)
        assertEquals(300.0, importedTech.waterMl!!, 0.01)
        assertEquals(2, importedTech.executionSteps.size)
        assertEquals("Bloom", importedTech.executionSteps[0].title)

        // Check that technique is saved in TechniqueRepository
        assertNotNull(techniqueRepo.get(importedTech.id))
    }

    @Test
    fun testToggleLike() = runBlocking {
        authRepo.setUser(emiliano)
        val recipe = DomainRecipe(id = "rec_like", ownerUserId = emiliano.id, name = "Recipe Like")
        recipeRepo.save(recipe)
        val share = shareRecipeUseCase("rec_like", ShareDestination.Feed, null).getOrThrow()

        authRepo.setUser(ana)
        val likeResult1 = toggleLikeUseCase(share.id)
        assertTrue(likeResult1.isSuccess)
        assertTrue(likeResult1.getOrThrow()) // Liked

        val shareAfterLike = socialRepo.getShare(share.id)!!
        assertTrue(shareAfterLike.likes.contains(ana.id))

        // Toggle again to unlike
        val likeResult2 = toggleLikeUseCase(share.id)
        assertTrue(likeResult2.isSuccess)
        assertFalse(likeResult2.getOrThrow()) // Unliked

        val shareAfterUnlike = socialRepo.getShare(share.id)!!
        assertFalse(shareAfterUnlike.likes.contains(ana.id))
    }
}
