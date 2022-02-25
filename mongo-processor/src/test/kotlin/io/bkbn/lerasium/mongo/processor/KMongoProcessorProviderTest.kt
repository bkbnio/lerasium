package io.bkbn.lerasium.mongo.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.bkbn.lerasium.utils.TestUtils.errorMessage
import io.bkbn.lerasium.utils.TestUtils.kotlinCode
import io.bkbn.lerasium.utils.TestUtils.kspGeneratedSources
import io.bkbn.lerasium.utils.TestUtils.readTrimmed
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class KMongoProcessorProviderTest : DescribeSpec({
  describe("Document Generation") {
    it("Can generate a simple document") {
      // arrange
      val compilation = KotlinCompilation().apply {
        sources = listOf(simpleSourceFile)
        symbolProcessorProviders = listOf(KMongoProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserDocument.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.model.Entity
        import io.bkbn.lerasium.generated.models.UserResponse
        import java.util.UUID
        import kotlin.String
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import kotlinx.serialization.Contextual
        import kotlinx.serialization.SerialName
        import kotlinx.serialization.Serializable

        @Serializable
        public data class UserEntity(
          @Contextual
          @SerialName("_id")
          public val id: UUID,
          public var name: String,
          public var createdAt: LocalDateTime,
          public var updatedAt: LocalDateTime
        ) : Entity<UserResponse> {
          public override fun toResponse(): UserResponse = with(::UserResponse) {
            val propertiesByName = UserEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                else -> propertiesByName[it.name]?.get(this@UserEntity)
              }
            }
            callBy(params)
          }
        }
        """.trimIndent()
      )
    }
    it("Can generate a simple nested document") {
      // arrange
      val compilation = KotlinCompilation().apply {
        sources = listOf(nestedDocumentFile)
        symbolProcessorProviders = listOf(KMongoProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserDocument.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.model.Entity
        import io.bkbn.lerasium.generated.models.UserPreferencesResponse
        import io.bkbn.lerasium.generated.models.UserResponse
        import java.util.UUID
        import kotlin.Boolean
        import kotlin.Int
        import kotlin.String
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import kotlinx.serialization.Contextual
        import kotlinx.serialization.SerialName
        import kotlinx.serialization.Serializable

        @Serializable
        public data class UserEntity(
          @Contextual
          @SerialName("_id")
          public val id: UUID,
          public var name: String,
          public var age: Int,
          public var preferences: UserPreferencesEntity,
          public var createdAt: LocalDateTime,
          public var updatedAt: LocalDateTime
        ) : Entity<UserResponse> {
          public override fun toResponse(): UserResponse = with(::UserResponse) {
            val propertiesByName = UserEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                "preferences" -> preferences.toResponse()
                else -> propertiesByName[it.name]?.get(this@UserEntity)
              }
            }
            callBy(params)
          }
        }

        @Serializable
        public data class UserPreferencesEntity(
          public var status: String,
          public var subscribed: Boolean
        ) : Entity<UserPreferencesResponse> {
          public override fun toResponse(): UserPreferencesResponse = with(::UserPreferencesResponse) {
            val propertiesByName = UserPreferencesEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                else -> propertiesByName[it.name]?.get(this@UserPreferencesEntity)
              }
            }
            callBy(params)
          }
        }
        """.trimIndent()
      )
    }
    it("Can generate a deeply nested document") {
      // arrange
      val compilation = KotlinCompilation().apply {
        sources = listOf(deeplyNestedDocument)
        symbolProcessorProviders = listOf(KMongoProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserDocument.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import io.bkbn.lerasium.core.model.Entity
        import io.bkbn.lerasium.generated.models.UserInfoResponse
        import io.bkbn.lerasium.generated.models.UserPreferencesResponse
        import io.bkbn.lerasium.generated.models.UserResponse
        import io.bkbn.lerasium.generated.models.UserStuffResponse
        import java.util.UUID
        import kotlin.Boolean
        import kotlin.reflect.full.memberProperties
        import kotlin.reflect.full.valueParameters
        import kotlinx.datetime.LocalDateTime
        import kotlinx.serialization.Contextual
        import kotlinx.serialization.SerialName
        import kotlinx.serialization.Serializable

        @Serializable
        public data class UserEntity(
          @Contextual
          @SerialName("_id")
          public val id: UUID,
          public var preferences: UserPreferencesEntity,
          public var createdAt: LocalDateTime,
          public var updatedAt: LocalDateTime
        ) : Entity<UserResponse> {
          public override fun toResponse(): UserResponse = with(::UserResponse) {
            val propertiesByName = UserEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                "preferences" -> preferences.toResponse()
                else -> propertiesByName[it.name]?.get(this@UserEntity)
              }
            }
            callBy(params)
          }
        }

        @Serializable
        public data class UserPreferencesEntity(
          public var stuff: UserStuffEntity
        ) : Entity<UserPreferencesResponse> {
          public override fun toResponse(): UserPreferencesResponse = with(::UserPreferencesResponse) {
            val propertiesByName = UserPreferencesEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                "stuff" -> stuff.toResponse()
                else -> propertiesByName[it.name]?.get(this@UserPreferencesEntity)
              }
            }
            callBy(params)
          }
        }

        @Serializable
        public data class UserStuffEntity(
          public var info: UserInfoEntity
        ) : Entity<UserStuffResponse> {
          public override fun toResponse(): UserStuffResponse = with(::UserStuffResponse) {
            val propertiesByName = UserStuffEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                "info" -> info.toResponse()
                else -> propertiesByName[it.name]?.get(this@UserStuffEntity)
              }
            }
            callBy(params)
          }
        }

        @Serializable
        public data class UserInfoEntity(
          public var isCool: Boolean
        ) : Entity<UserInfoResponse> {
          public override fun toResponse(): UserInfoResponse = with(::UserInfoResponse) {
            val propertiesByName = UserInfoEntity::class.memberProperties.associateBy { it.name }
            val params = valueParameters.associateWith {
              when (it.name) {
                else -> propertiesByName[it.name]?.get(this@UserInfoEntity)
              }
            }
            callBy(params)
          }
        }
        """.trimIndent()
      )
    }
  }
  describe("Dao Generation") {
    it("can build a simple crud dao") {
      // arrange
      val compilation = KotlinCompilation().apply {
        sources = listOf(simpleSourceFile)
        symbolProcessorProviders = listOf(KMongoProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserDao.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import com.mongodb.client.MongoCollection
        import com.mongodb.client.MongoDatabase
        import io.bkbn.lerasium.core.dao.Dao
        import io.bkbn.lerasium.core.model.CountResponse
        import io.bkbn.lerasium.generated.models.UserCreateRequest
        import io.bkbn.lerasium.generated.models.UserResponse
        import io.bkbn.lerasium.generated.models.UserUpdateRequest
        import java.util.UUID
        import kotlin.Int
        import kotlin.Unit
        import kotlin.collections.List
        import kotlinx.datetime.Clock
        import kotlinx.datetime.TimeZone
        import kotlinx.datetime.toLocalDateTime
        import org.litote.kmongo.deleteOneById
        import org.litote.kmongo.findOneById
        import org.litote.kmongo.getCollection
        import org.litote.kmongo.save

        public class UserDao(
          db: MongoDatabase
        ) : Dao<UserEntity, UserResponse, UserCreateRequest, UserUpdateRequest> {
          private val collection: MongoCollection<UserEntity> = db.getCollection()

          public override fun create(request: UserCreateRequest): UserResponse {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val entity = UserEntity(
              id = UUID.randomUUID(),
              createdAt = now,
              updatedAt = now,
              name = request.name,
            )
            collection.save(entity)
            return entity.toResponse()
          }

          public override fun read(id: UUID): UserResponse {
            val entity = collection.findOneById(id) ?: error("PLACEHOLDER")
            return entity.toResponse()
          }

          public override fun update(id: UUID, request: UserUpdateRequest): UserResponse {
            val entity = collection.findOneById(id) ?: error("PLACEHOLDER")
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            request.name?.let {
              entity.name = it
            }
            entity.updatedAt = now
            collection.save(entity)
            return entity.toResponse()
          }

          public override fun delete(id: UUID): Unit {
            collection.deleteOneById(id)
          }

          public override fun countAll(): CountResponse {
            val count = collection.countDocuments()
            return CountResponse(count)
          }

          public override fun getAll(chunk: Int, offset: Int): List<UserResponse> {
            val entities = collection.find().skip(chunk * offset).limit(chunk)
            return entities.toList().map { entity ->
              entity.toResponse()
            }
          }
        }
        """.trimIndent()
      ) { it.replace("PLACEHOLDER", errorMessage) }
    }
    it("Can build a dao with a unique index") {
      // arrange
      val simpleSourceFile = SourceFile.kotlin(
        "Spec.kt", """
        package test

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.mongo.Document
        import io.bkbn.lerasium.persistence.Index

        @Domain("User")
        interface User {
          val name: String
        }

        @Document
        interface UserDoc : User {
          @Index(unique = true)
          override val name: String
        }
        """.trimIndent()
      )

      val compilation = KotlinCompilation().apply {
        sources = listOf(simpleSourceFile)
        symbolProcessorProviders = listOf(KMongoProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserDao.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import com.mongodb.client.MongoCollection
        import com.mongodb.client.MongoDatabase
        import io.bkbn.lerasium.core.dao.Dao
        import io.bkbn.lerasium.core.model.CountResponse
        import io.bkbn.lerasium.generated.models.UserCreateRequest
        import io.bkbn.lerasium.generated.models.UserResponse
        import io.bkbn.lerasium.generated.models.UserUpdateRequest
        import java.util.UUID
        import kotlin.Int
        import kotlin.Unit
        import kotlin.collections.List
        import kotlinx.datetime.Clock
        import kotlinx.datetime.TimeZone
        import kotlinx.datetime.toLocalDateTime
        import org.litote.kmongo.deleteOneById
        import org.litote.kmongo.ensureUniqueIndex
        import org.litote.kmongo.findOneById
        import org.litote.kmongo.getCollection
        import org.litote.kmongo.save

        public class UserDao(
          db: MongoDatabase
        ) : Dao<UserEntity, UserResponse, UserCreateRequest, UserUpdateRequest> {
          private val collection: MongoCollection<UserEntity> = db.getCollection()

          init {
            collection.ensureUniqueIndex(UserEntity::name)
          }

          public override fun create(request: UserCreateRequest): UserResponse {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val entity = UserEntity(
              id = UUID.randomUUID(),
              createdAt = now,
              updatedAt = now,
              name = request.name,
            )
            collection.save(entity)
            return entity.toResponse()
          }

          public override fun read(id: UUID): UserResponse {
            val entity = collection.findOneById(id) ?: error("PLACEHOLDER")
            return entity.toResponse()
          }

          public override fun update(id: UUID, request: UserUpdateRequest): UserResponse {
            val entity = collection.findOneById(id) ?: error("PLACEHOLDER")
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            request.name?.let {
              entity.name = it
            }
            entity.updatedAt = now
            collection.save(entity)
            return entity.toResponse()
          }

          public override fun delete(id: UUID): Unit {
            collection.deleteOneById(id)
          }

          public override fun countAll(): CountResponse {
            val count = collection.countDocuments()
            return CountResponse(count)
          }

          public override fun getAll(chunk: Int, offset: Int): List<UserResponse> {
            val entities = collection.find().skip(chunk * offset).limit(chunk)
            return entities.toList().map { entity ->
              entity.toResponse()
            }
          }
        }
        """.trimIndent()
      ) { it.replace("PLACEHOLDER", errorMessage) }
    }
    it("Can build a dao with a composite index") {
      // arrange
      val simpleSourceFile = SourceFile.kotlin(
        "Spec.kt", """
        package test

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.mongo.Document
        import io.bkbn.lerasium.persistence.CompositeIndex

        @Domain("User")
        interface User {
          val name: String
          val favoriteFood: String
        }

        @Document
        @CompositeIndex(fields = ["name", "favoriteFood"])
        interface UserDoc : User
        """.trimIndent()
      )

      val compilation = KotlinCompilation().apply {
        sources = listOf(simpleSourceFile)
        symbolProcessorProviders = listOf(KMongoProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserDao.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import com.mongodb.client.MongoCollection
        import com.mongodb.client.MongoDatabase
        import io.bkbn.lerasium.core.dao.Dao
        import io.bkbn.lerasium.core.model.CountResponse
        import io.bkbn.lerasium.generated.models.UserCreateRequest
        import io.bkbn.lerasium.generated.models.UserResponse
        import io.bkbn.lerasium.generated.models.UserUpdateRequest
        import java.util.UUID
        import kotlin.Int
        import kotlin.Unit
        import kotlin.collections.List
        import kotlinx.datetime.Clock
        import kotlinx.datetime.TimeZone
        import kotlinx.datetime.toLocalDateTime
        import org.litote.kmongo.deleteOneById
        import org.litote.kmongo.ensureIndex
        import org.litote.kmongo.findOneById
        import org.litote.kmongo.getCollection
        import org.litote.kmongo.save

        public class UserDao(
          db: MongoDatabase
        ) : Dao<UserEntity, UserResponse, UserCreateRequest, UserUpdateRequest> {
          private val collection: MongoCollection<UserEntity> = db.getCollection()

          init {
            collection.ensureIndex(UserEntity::name, UserEntity::favoriteFood)
          }

          public override fun create(request: UserCreateRequest): UserResponse {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val entity = UserEntity(
              id = UUID.randomUUID(),
              createdAt = now,
              updatedAt = now,
              name = request.name,
              favoriteFood = request.favoriteFood,
            )
            collection.save(entity)
            return entity.toResponse()
          }

          public override fun read(id: UUID): UserResponse {
            val entity = collection.findOneById(id) ?: error("PLACEHOLDER")
            return entity.toResponse()
          }

          public override fun update(id: UUID, request: UserUpdateRequest): UserResponse {
            val entity = collection.findOneById(id) ?: error("PLACEHOLDER")
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            request.name?.let {
              entity.name = it
            }
            request.favoriteFood?.let {
              entity.favoriteFood = it
            }
            entity.updatedAt = now
            collection.save(entity)
            return entity.toResponse()
          }

          public override fun delete(id: UUID): Unit {
            collection.deleteOneById(id)
          }

          public override fun countAll(): CountResponse {
            val count = collection.countDocuments()
            return CountResponse(count)
          }

          public override fun getAll(chunk: Int, offset: Int): List<UserResponse> {
            val entities = collection.find().skip(chunk * offset).limit(chunk)
            return entities.toList().map { entity ->
              entity.toResponse()
            }
          }
        }
        """.trimIndent()
      ) { it.replace("PLACEHOLDER", errorMessage) }
    }
    it("Can build a dao with nested objects") {
      // arrange
      val compilation = KotlinCompilation().apply {
        sources = listOf(nestedDocumentFile)
        symbolProcessorProviders = listOf(KMongoProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserDao.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import com.mongodb.client.MongoCollection
        import com.mongodb.client.MongoDatabase
        import io.bkbn.lerasium.core.dao.Dao
        import io.bkbn.lerasium.core.model.CountResponse
        import io.bkbn.lerasium.generated.models.UserCreateRequest
        import io.bkbn.lerasium.generated.models.UserResponse
        import io.bkbn.lerasium.generated.models.UserUpdateRequest
        import java.util.UUID
        import kotlin.Int
        import kotlin.Unit
        import kotlin.collections.List
        import kotlinx.datetime.Clock
        import kotlinx.datetime.TimeZone
        import kotlinx.datetime.toLocalDateTime
        import org.litote.kmongo.deleteOneById
        import org.litote.kmongo.findOneById
        import org.litote.kmongo.getCollection
        import org.litote.kmongo.save

        public class UserDao(
          db: MongoDatabase
        ) : Dao<UserEntity, UserResponse, UserCreateRequest, UserUpdateRequest> {
          private val collection: MongoCollection<UserEntity> = db.getCollection()

          public override fun create(request: UserCreateRequest): UserResponse {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val entity = UserEntity(
              id = UUID.randomUUID(),
              createdAt = now,
              updatedAt = now,
              name = request.name,
              age = request.age,
              preferences = request.preferences.let { preferences ->
                UserPreferencesEntity(
                  status = preferences.status,
                  subscribed = preferences.subscribed,
                )
              }
            )
            collection.save(entity)
            return entity.toResponse()
          }

          public override fun read(id: UUID): UserResponse {
            val entity = collection.findOneById(id) ?: error("PLACEHOLDER")
            return entity.toResponse()
          }

          public override fun update(id: UUID, request: UserUpdateRequest): UserResponse {
            val entity = collection.findOneById(id) ?: error("PLACEHOLDER")
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            request.name?.let {
              entity.name = it
            }
            request.age?.let {
              entity.age = it
            }
            request.preferences?.let {
              entity.preferences.let { preferences ->
                it.status?.let {
                  preferences.status = it
                }
                it.subscribed?.let {
                  preferences.subscribed = it
                }
              }
            }
            entity.updatedAt = now
            collection.save(entity)
            return entity.toResponse()
          }

          public override fun delete(id: UUID): Unit {
            collection.deleteOneById(id)
          }

          public override fun countAll(): CountResponse {
            val count = collection.countDocuments()
            return CountResponse(count)
          }

          public override fun getAll(chunk: Int, offset: Int): List<UserResponse> {
            val entities = collection.find().skip(chunk * offset).limit(chunk)
            return entities.toList().map { entity ->
              entity.toResponse()
            }
          }
        }
        """.trimIndent()
      ) { it.replace("PLACEHOLDER", errorMessage) }
    }
    it("Can build a dao with deeply nested objects") {
      // arrange
      val compilation = KotlinCompilation().apply {
        sources = listOf(deeplyNestedDocument)
        symbolProcessorProviders = listOf(KMongoProcessorProvider())
        inheritClassPath = true
      }

      // act
      val result = compilation.compile()

      // assert
      result shouldNotBe null
      result.kspGeneratedSources shouldHaveSize 2
      result.kspGeneratedSources.first { it.name == "UserDao.kt" }.readTrimmed() shouldBe kotlinCode(
        """
        package io.bkbn.lerasium.generated.entity

        import com.mongodb.client.MongoCollection
        import com.mongodb.client.MongoDatabase
        import io.bkbn.lerasium.core.dao.Dao
        import io.bkbn.lerasium.core.model.CountResponse
        import io.bkbn.lerasium.generated.models.UserCreateRequest
        import io.bkbn.lerasium.generated.models.UserResponse
        import io.bkbn.lerasium.generated.models.UserUpdateRequest
        import java.util.UUID
        import kotlin.Int
        import kotlin.Unit
        import kotlin.collections.List
        import kotlinx.datetime.Clock
        import kotlinx.datetime.TimeZone
        import kotlinx.datetime.toLocalDateTime
        import org.litote.kmongo.deleteOneById
        import org.litote.kmongo.findOneById
        import org.litote.kmongo.getCollection
        import org.litote.kmongo.save

        public class UserDao(
          db: MongoDatabase
        ) : Dao<UserEntity, UserResponse, UserCreateRequest, UserUpdateRequest> {
          private val collection: MongoCollection<UserEntity> = db.getCollection()

          public override fun create(request: UserCreateRequest): UserResponse {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val entity = UserEntity(
              id = UUID.randomUUID(),
              createdAt = now,
              updatedAt = now,
              preferences = request.preferences.let { preferences ->
                UserPreferencesEntity(
                  stuff = preferences.stuff.let { stuff ->
                    UserStuffEntity(
                      info = stuff.info.let { info ->
                        UserInfoEntity(
                          isCool = info.isCool,
                        )
                      }
                    )
                  }
                )
              }
            )
            collection.save(entity)
            return entity.toResponse()
          }

          public override fun read(id: UUID): UserResponse {
            val entity = collection.findOneById(id) ?: error("PLACEHOLDER")
            return entity.toResponse()
          }

          public override fun update(id: UUID, request: UserUpdateRequest): UserResponse {
            val entity = collection.findOneById(id) ?: error("PLACEHOLDER")
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            request.preferences?.let {
              entity.preferences.let { preferences ->
                it.stuff?.let {
                  preferences.stuff.let { stuff ->
                    it.info?.let {
                      stuff.info.let { info ->
                        it.isCool?.let {
                          info.isCool = it
                        }
                      }
                    }
                  }
                }
              }
            }
            entity.updatedAt = now
            collection.save(entity)
            return entity.toResponse()
          }

          public override fun delete(id: UUID): Unit {
            collection.deleteOneById(id)
          }

          public override fun countAll(): CountResponse {
            val count = collection.countDocuments()
            return CountResponse(count)
          }

          public override fun getAll(chunk: Int, offset: Int): List<UserResponse> {
            val entities = collection.find().skip(chunk * offset).limit(chunk)
            return entities.toList().map { entity ->
              entity.toResponse()
            }
          }
        }
        """.trimIndent()
      ) { it.replace("PLACEHOLDER", errorMessage) }
    }
  }
}) {
  companion object {
    val simpleSourceFile = SourceFile.kotlin(
      "Spec.kt", """
        package test

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.mongo.Document

        @Domain("User")
        interface User {
          val name: String
        }

        @Document
        interface UserDoc : User
        """.trimIndent()
    )

    val nestedDocumentFile = SourceFile.kotlin(
      "Spec.kt", """
        package test

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.mongo.Document

        @Domain("User")
        interface User {
          val name: String
          val age: Int
          val preferences: UserPreferences
        }

        interface UserPreferences {
          val status: String
          val subscribed: Boolean
        }

        @Document
        interface UserDoc : User
      """.trimIndent()
    )

    val deeplyNestedDocument = SourceFile.kotlin(
      "Spec.kt", """
        package test

        import io.bkbn.lerasium.core.Domain
        import io.bkbn.lerasium.mongo.Document

        @Domain("User")
        interface User {
          val preferences: UserPreferences
        }

        interface UserPreferences {
          val stuff: UserStuff
        }

        interface UserStuff {
          val info: UserInfo
        }


        interface UserInfo {
          val isCool: Boolean
        }

        @Document
        interface UserDoc : User
      """.trimIndent()
    )
  }
}
