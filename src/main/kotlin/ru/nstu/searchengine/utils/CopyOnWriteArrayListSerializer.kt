package ru.nstu.searchengine.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.nstu.searchengine.routes.dto.StatisticResponse
import java.util.concurrent.CopyOnWriteArrayList

object CopyOnWriteArrayListSerializer : KSerializer<CopyOnWriteArrayList<StatisticResponse>> {
	private val delegateSerializer = ListSerializer(StatisticResponse.serializer())
	override val descriptor: SerialDescriptor = delegateSerializer.descriptor

	override fun serialize(encoder: Encoder, value: CopyOnWriteArrayList<StatisticResponse>) {
		delegateSerializer.serialize(encoder, value)
	}

	override fun deserialize(decoder: Decoder): CopyOnWriteArrayList<StatisticResponse> {
		val list = delegateSerializer.deserialize(decoder)
		return CopyOnWriteArrayList(list)
	}
}