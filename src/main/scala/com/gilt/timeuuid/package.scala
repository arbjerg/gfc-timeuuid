package com.gilt

import java.util.{Date, UUID}
import java.nio.ByteBuffer



package object timeuuid extends {

	implicit def asUuidConversions(from: UUID) =
	    new UuidConversions(from)
		
	implicit def asByteArrayConversions(from: Array[Byte]) =
	    new ByteArrayConversions(from)
	
    class UuidConversions(from: UUID) {
	
	  import com.gilt.timeuuid.Clock
		
      assert(from.version() == 1, "Version 1 uuid required")

      def toLong: Long = from.timestamp() / 10000 + Clock.StartEpoch

      def toDate: Date = new Date((from.timestamp() / 10000) + Clock.StartEpoch)

      def toBytes: Array[Byte] = ByteBuffer.wrap(new Array[Byte](2 * 8))
        .putLong(from.getMostSignificantBits)
        .putLong(from.getLeastSignificantBits)
        .array()
    }
	
    class ByteArrayConversions(from: Array[Byte]) {
      assert(from.length == 16, "Expected 16 bytes, got " + from.mkString(","))

      def toUUID: UUID = {
        val buffer = ByteBuffer.wrap(from)
        new UUID(buffer.getLong, buffer.getLong)
      }
    }

}
