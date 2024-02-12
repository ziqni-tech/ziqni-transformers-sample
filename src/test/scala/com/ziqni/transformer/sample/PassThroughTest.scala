/*
 * Copyright (c) 2023. ZIQNI LTD registered in England and Wales, company registration number-09693684
 */

package com.ziqni.transformer.sample

import com.ziqni.transformer.PassThrough
import com.ziqni.transformer.test.ZiqniTransformerTester
import org.scalatest._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.must.Matchers
import org.json4s.jackson.JsonMethods

import scala.collection.JavaConverters._
import scala.collection.convert.ImplicitConversions.`map AsScala`

class PassThroughTest extends AnyFunSpec with Matchers with GivenWhenThen with BeforeAndAfterEach with BeforeAndAfterAll {

	val TEST_Exchange = "some-exchange"
	val TEST_RoutingKey = "transaction"

	describe("Test the message queue receiver implementation") {

		val passThrough = new PassThrough()
		val ziqniMqTransformer = ZiqniTransformerTester.loadDefaultWithSampleData()

		it("should receive a published message and transform it into an event") {

			val mmmm = new java.util.HashMap[String, Object]()
			mmmm.put("Foo", java.lang.Boolean.TRUE)
			mmmm.put("Bar", java.lang.Double.valueOf(1.1))
			mmmm.put("Mop", java.lang.Integer.valueOf(1))
			mmmm.put("Head", "isText")
			mmmm.put("Heads", Seq("isText","moreText"))

			val event = new com.ziqni.admin.sdk.model.Event()
				.action("buy")
				.entityRefId("apples")
				.sourceValue(1.99)
				.memberRefId("Player-1")
				.customFields(mmmm)

			val eventString = JsonMethods.mapper.writeValueAsString(event)

			When("the message is forwarded")
			passThrough.apply(eventString.getBytes(),ziqniMqTransformer.ziqniContextExt,Map.empty)
			Then("the event should be received")
		}
	}
}


