/*
 * Copyright (c) 2023. ZIQNI LTD registered in England and Wales, company registration number-09693684
 */

package com.ziqni.transformer.sample

import com.ziqni.transformer.AdvancedSampleWithCallback
import com.ziqni.transformer.test.ZiqniTransformerTester
import org.joda.time.DateTime
import org.scalatest._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.must.Matchers

class AdvancedSampleWithCallbackTest extends AnyFunSpec with Matchers with GivenWhenThen with BeforeAndAfterEach with BeforeAndAfterAll {

	val TEST_Exchange = "some-exchange"
	val TEST_RoutingKey = "transaction"

	describe("Test the message queue receiver implementation") {

		val ziqniMqTransformer = ZiqniTransformerTester.loadDefaultWithSampleData()

		it("should receive a published message and transform it into an event") {

			val messageTypesList = Seq(
				AdvancedSampleTransformerTest.Transaction_Msq(10000, 20000)
			)
			val args: Map[String, Any] = Map.empty

			When("the message is forwarded")

			/** Call our transformer script **/
			val transformer = new AdvancedSampleWithCallback

			messageTypesList.foreach { eventJsonString =>
				/** Receive raw json data **/
				val jsonBytes = eventJsonString.toCharArray.map(_.toByte)

				/** Send the raw data to our transformer script for transformation * */
				transformer.rabbit(jsonBytes, TEST_RoutingKey, TEST_RoutingKey, ziqniMqTransformer.ziqniContextExt)
			}
			Then("the event should be received")


			info ("Verify if data was received successfully by our system")
//			assert(ziqniMqTransformer.ziqniStores.eventsStore.cache..nonEmpty)
//			assert(api.eventsReceivedForTest.size == 5)

		}

//		it("should create a member") {
//			When("a new member")
//			ziqniMqTransformer.ziqniContextExt.ziqniApiAsync.createMember()
//		}
	}
}

object AdvancedSampleTransformerTest {

	val gamePhaseId = "test" + DateTime.now().getMillis
	val txId = "testTID" + DateTime.now().getMillis

	/**
	  * Transaction model
	  */
	def Transaction_Msq(betAmount: Double, amount: Double, gPhaseId: String = gamePhaseId, txtIdStr: String = txId): String =
		s"""
		   {
		       "action": "round_summary",
		       "additionalJackpotWinAmount": 10,
		       "finalPhaseWager": true,
		       "finalWager": true,
		       "betAmountFromPlayBonus": 0,
		       "jackpotShareAmount": 0,
		       "jackpotWinAmount": 10,
		       "playBonusWinAmount": 10,
		       "totalBetAmount": ${betAmount},
		       "totalWinAmount": ${amount},
		       "timestamp": ${DateTime.now().getMillis + 3},
		       "gamePhaseId": "${gPhaseId}",
		       "txId": "${txtIdStr}",
		       "accountCode": "3589125162250000",
		       "nickname": "Andrea R.",
		       "licenseeId": 1,
		       "productType": 1,
		       "deviceType": "Desktop",
		       "gameCode": 41,
		       "gameDescription": "Mad 4 Halloween",
		       "gameProviderId": 1,
		       "gameProviderDescription": "Espresso Games",
		       "gameTypeId": 1,
		       "gameTypeDescription": "SLOT",
		       "gameSubTypeId": 1,
		       "gameSubTypeDescription": "SLOT",
		       "extWalletGameType": 9438,
		       "extWalletGameSubType": 21,
		       "gameSessionId" : 72747,
		       "regulatorTicketId" : "N522920107CE0CWO",
		       "regulatorTransactionCode" : "E9GiuH3pKJAgPeXy"
		   }
	    """.stripMargin

	/**
	  * Transaction model
	  */
	def Transaction_Msq_09082021(betAmount: Double, amount: Double, gPhaseId: String = gamePhaseId, txtIdStr: String = txId): String =
		s"""
		   {
				"action":"round_summary",
				"additionalJackpotWinAmount":0,
				"finalWager":true,
				"finalPhaseWager":true,
				"betAmountFromPlayBonus":0,
				"jackpotShareAmount":0,
				"jackpotWinAmount":0,
				"playBonusWinAmount":0,
				"totalWinAmount": ${amount},
				"totalBetAmount": ${betAmount},
				"timestamp":1623667368000,
				"gamePhaseId": "${gPhaseId}",
				"txId": "${txtIdStr}",
				"accountCode":"0002388597030000",
				"licenseeId":1,
				"gameCode":1,
				"gameDescription":"Bullets For Money",
				"gameProviderId":1,
				"gameProviderDescription":"Espresso Games test",
				"gameTypeId":1,
				"gameTypeDescription":"SLOT",
				"gameSubTypeId":0,
				"gameSubTypeDescription":"aggiornatta",
				"extWalletGameType":9421,
				"extWalletGameSubType":21,
				"regulatorTicketId":"N529F2010A2F3FBK",
				"regulatorTransactionCode":"9cqTmjhfq57cujBv",
				"gameSessionId":31844,
				"nickname":"Simona F.",
				"productType":1,
				"deviceType":"Desktop"
			}
	    """.stripMargin

	val RANDOM_Msg =
		s"""
		  {
			"timestamp":${DateTime.now().getMillis}

		}
	""".stripMargin

}


