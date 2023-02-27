package io.bkbn.lerasium.core.model

sealed interface DomainProvider<I, D> {
  val id: I
  val data: D?

  companion object {
    fun <I, D> from(id: I, data: D? = null): DomainProvider<I, D> =
      if (data == null) OnlyId(id) else WithData(id, data)
  }

  data class OnlyId<I, D>(override val id: I) : DomainProvider<I, D> {
    override val data: D? = null
  }

  data class WithData<I, D>(override val id: I, override val data: D) : DomainProvider<I, D>
}
