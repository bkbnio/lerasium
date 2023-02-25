package io.bkbn.lerasium.core.model

sealed interface ModelProvider<I, D> {
  val id: I?
  val data: D?

  companion object {
    fun <I> ofId(id: I): ModelProvider<I, Nothing> = ModelId(id)
    fun <D> ofData(data: D): ModelProvider<Nothing, D> = ModelData(data)
  }
}

data class ModelId<I>(override val id: I) : ModelProvider<I, Nothing> {
  override val data: Nothing? = null
}

data class ModelData<D>(override val data: D) : ModelProvider<Nothing, D> {
  override val id: Nothing? = null
}
