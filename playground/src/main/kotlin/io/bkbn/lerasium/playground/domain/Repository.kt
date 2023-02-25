package io.bkbn.lerasium.playground.domain

import io.bkbn.bouncer.core.AbacPolicy
import io.bkbn.bouncer.core.RbacPolicy
import io.bkbn.bouncer.core.abacPolicy
import io.bkbn.bouncer.core.rbacPolicy
import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.auth.AbacPolicyProvider
import io.bkbn.lerasium.core.auth.CrudAction
import io.bkbn.lerasium.core.auth.RbacPolicyProvider
import io.bkbn.lerasium.core.domain.LerasiumDomain
import io.bkbn.lerasium.core.model.ModelProvider
import io.bkbn.lerasium.rdbms.ForeignKey
import io.bkbn.lerasium.rdbms.Table
import java.util.UUID

@Api
@Domain("Repository")
@Table
interface Repository : LerasiumDomain {
  val name: String
  val isPublic: Boolean

  @ForeignKey
  val organization: ModelProvider<UUID, Organization>

  companion object {
    val anonymousAbac = object : AbacPolicyProvider<Unit, CrudAction, Repository> {
      override val policy: AbacPolicy<Unit, CrudAction, Repository> = abacPolicy {
        can("Anonymous can read public repositories", CrudAction.READ) { _, repo -> repo.isPublic }
      }
    }

    val userOrgRbac =
      object : RbacPolicyProvider<User, CrudAction, OrganizationRole, OrganizationRole.Type, Repository> {
        override val policy: RbacPolicy<User, CrudAction, OrganizationRole.Type, Repository> = rbacPolicy {
          can(
            "Org admin can create a new repository",
            CrudAction.CREATE,
            OrganizationRole.Type.ADMIN
          ) { _, _, _ -> true }
          can("Org admin can delete a repository", CrudAction.DELETE, OrganizationRole.Type.ADMIN) { _, _, _ -> true }
        }
      }
  }
}
