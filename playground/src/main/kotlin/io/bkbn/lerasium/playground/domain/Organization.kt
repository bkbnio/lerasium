package io.bkbn.lerasium.playground.domain

import io.bkbn.bouncer.core.rbacPolicy
import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.auth.CrudAction
import io.bkbn.lerasium.core.auth.RbacPolicyProvider
import io.bkbn.lerasium.rdbms.Table

@Api
@Domain("Organization")
@Table
interface Organization {
  val name: String

  companion object {
    val userRbac =
      object : RbacPolicyProvider<User, CrudAction, OrganizationRole, OrganizationRole.Type, Organization> {
        override val policy = rbacPolicy<User, CrudAction, OrganizationRole.Type, Organization> {
          can(
            "Admin can always delete an organization",
            CrudAction.DELETE,
            OrganizationRole.Type.ADMIN
          ) { _, _, _ -> true }
        }
      }
  }
}
