package com.example.data.remote

import com.example.data.remote.models.*
import retrofit2.Response
import retrofit2.http.*

interface SupabaseApi {

    // GO-TRUE AUTH HEADERS AND ENDPOINTS

    @Headers("Prefer: return=representation")
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Body request: SignUpRequest
    ): Response<AuthResponse>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Body request: SignInRequest
    ): Response<AuthResponse>


    // PROFILES TABLE CRUD

    @GET("rest/v1/profiles")
    suspend fun getProfile(
        @Query("id") idFilter: String, // format: "eq.UUID"
        @Query("select") select: String = "*"
    ): Response<List<RemoteProfile>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/profiles")
    suspend fun upsertProfile(
        @Body profile: RemoteProfile
    ): Response<RemoteProfile>


    // BEANS TABLE CRUD

    @GET("rest/v1/beans")
    suspend fun getBeans(
        @Query("user_id") uidFilter: String // "eq.UUID"
    ): Response<List<RemoteBean>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/beans")
    suspend fun insertBean(
        @Body bean: RemoteBean
    ): Response<RemoteBean>


    // RECIPES TABLE CRUD

    @GET("rest/v1/recipes")
    suspend fun getRecipes(
        @Query("user_id") uidFilter: String // "eq.UUID"
    ): Response<List<RemoteRecipe>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/recipes")
    suspend fun insertRecipe(
        @Body recipe: RemoteRecipe
    ): Response<RemoteRecipe>


    // TECHNIQUES TABLE CRUD

    @GET("rest/v1/techniques")
    suspend fun getTechniques(
        @Query("user_id") uidFilter: String // "eq.UUID"
    ): Response<List<RemoteTechnique>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/techniques")
    suspend fun insertTechnique(
        @Body technique: RemoteTechnique
    ): Response<RemoteTechnique>


    // TECHNIQUE STEPS CRUD

    @GET("rest/v1/technique_steps")
    suspend fun getTechniqueSteps(
        @Query("technique_id") techniqueIdFilter: String // "eq.UUID"
    ): Response<List<RemoteTechniqueStep>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/technique_steps")
    suspend fun insertTechniqueSteps(
        @Body steps: List<RemoteTechniqueStep>
    ): Response<List<RemoteTechniqueStep>>


    // SHARES / ACTIVITY FEED CRUD

    @GET("rest/v1/shares")
    suspend fun getPublicFeed(
        @Query("visibility") visibilityFilter: String = "eq.public",
        @Query("select") select: String = "*,likes_count:share_likes(count),saves_count:share_saves(count)",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<RemoteShare>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/shares")
    suspend fun shareEntity(
        @Body share: RemoteShare
    ): Response<RemoteShare>


    // SHARE LIKES CRUD

    @POST("rest/v1/share_likes")
    suspend fun likeShare(
        @Body like: Map<String, String> // e.g. {"share_id": "...", "user_id": "..."}
    ): Response<Unit>

    @DELETE("rest/v1/share_likes")
    suspend fun unlikeShare(
        @Query("share_id") shareIdFilter: String, // "eq.UUID"
        @Query("user_id") userIdFilter: String // "eq.UUID"
    ): Response<Unit>


    // SHARE SAVES CRUD

    @POST("rest/v1/share_saves")
    suspend fun saveShare(
        @Body save: Map<String, String>
    ): Response<Unit>


    // INBOX ITEMS

    @GET("rest/v1/inbox_items")
    suspend fun getInbox(
        @Query("target_user_id") uidFilter: String, // "eq.UUID"
        @Query("select") select: String = "*,share:shares(*,likes_count:share_likes(count),saves_count:share_saves(count))",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<RemoteInboxItem>>


    // ACTIVITY TIMELINE

    @GET("rest/v1/activity_log")
    suspend fun getActivityTimeline(
        @Query("user_id") uidFilter: String, // "eq.UUID"
        @Query("order") order: String = "created_at.desc"
    ): Response<List<RemoteActivityLog>>

    @POST("rest/v1/activity_log")
    suspend fun logActivity(
        @Body activity: RemoteActivityLog
    ): Response<Unit>


    // CUSTOM RPC ALIGNMENT ENDPOINTS (PARTE 1 EXECUTIONS)

    @POST("rest/v1/rpc/import_share_as_recipe")
    suspend fun rpcImportAsRecipe(
        @Body payload: RpcShareIdPayload
    ): Response<String> // returns uuid

    @POST("rest/v1/rpc/import_share_as_technique")
    suspend fun rpcImportAsTechnique(
        @Body payload: RpcShareIdPayload
    ): Response<String>

    @POST("rest/v1/rpc/fork_share_as_recipe")
    suspend fun rpcForkAsRecipe(
        @Body payload: RpcShareIdPayload
    ): Response<String>

    @POST("rest/v1/rpc/fork_share_as_technique")
    suspend fun rpcForkAsTechnique(
        @Body payload: RpcShareIdPayload
    ): Response<String>
}
