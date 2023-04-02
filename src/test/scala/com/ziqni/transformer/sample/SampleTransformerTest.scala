/*
 * Copyright (c) 2022. ZIQNI LTD registered in England and Wales, company registration number-09693684
 */

package com.ziqni.transformer.sample

import com.ziqni.transformer.SampleTransformerWithCallback
import com.ziqni.transformer.test.ZiqniTransformerTester
import org.joda.time.DateTime
import org.scalatest._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.must.Matchers

class SampleTransformerTest extends AnyFunSpec with Matchers with GivenWhenThen with BeforeAndAfterEach with BeforeAndAfterAll {

	val TEST_Exchange = "some-exchange"
	val TEST_RoutingKey = "transaction"

	describe("Test the message queue receiver implementation") {

		it("should receive a published message and transform it into an event") {

			val messageTypesList = Seq(
				SampleTransformerTest.Transaction_Msq(10000, 20000),
				SampleTransformerTest.CANCEL_BET_Msg(txtIdStr = SampleTransformerTest.txId+"-1"),
				SampleTransformerTest.BET_Msg(11000, SampleTransformerTest.gamePhaseId+"-2", SampleTransformerTest.txId+"-2"),
				SampleTransformerTest.BET_Msg(20100, SampleTransformerTest.gamePhaseId+"-2", SampleTransformerTest.txId+"-3"),
				SampleTransformerTest.WIN_Msg((11000 + 20100), 21000, SampleTransformerTest.gamePhaseId+"-2", SampleTransformerTest.txId+"-4"),
				SampleTransformerTest.BET_Msg(20000, SampleTransformerTest.gamePhaseId+"-3", SampleTransformerTest.txId+"-5"),
				SampleTransformerTest.WIN_Msg(20000, 0, SampleTransformerTest.gamePhaseId+"-3", SampleTransformerTest.txId+"-6") // loss event
			)

			val ziqniMqTransformer = ZiqniTransformerTester.loadDefault()
			val args: Map[String, Any] = Map.empty

			When("the message is forwarded")

			/** Call our transformer script **/
			val transformer = new SampleTransformerWithCallback

			messageTypesList.foreach { eventJsonString =>
				/** Receive raw json data **/
				val jsonBytes = eventJsonString.toCharArray.map(_.toByte)

				/** Send the raw data to our transformer script for transformation * */
				transformer.rabbit(jsonBytes, TEST_RoutingKey, TEST_RoutingKey, ziqniMqTransformer.ziqniContextExt)
			}
			Then("the event should be received")


			info ("Verify if data was received successfully by our system")
//			assert(ziqniMqTransformer.ziqniStores.eventsReceivedForTest.nonEmpty)
//			assert(api.eventsReceivedForTest.size == 5)

		}
	}
}

object SampleTransformerTest {

	val gamePhaseId = "test" + DateTime.now().getMillis
	val txId = "testTID" + DateTime.now().getMillis

	/**
	  * new fields added - v1.1"
	  * - nickname
	  * - productType
	  * - deviceType
	  * - gameSessionId
	  * - regulatorTicketId
	  * - regulatorTransactionCode
	  *
	  * removed - v1.1:
	  * - playerId
	  *
	  * fields changed - v1.2
	  * - deviceType: number > string
	  */
	def BET_Msg(amount: Double, gPhaseId: String = gamePhaseId, txtIdStr: String = txId): String = {
		s"""
		  {
			"action": "bet",
			"betAmount": ${amount},
			"betAmountFromPlayBonus":0,
			"jackpotShareAmount":0,
			"timestamp":${DateTime.now().getMillis},
			"gamePhaseId":"${gPhaseId}",
			"txId":"${txtIdStr}",
			"accountCode":"3589125162250000",
			"nickname":"Andrea R.",
			"licenseeId":1,
			"productType":1,
			"deviceType":"Desktop",
			"gameCode": 123,
			"gameDescription":"Mad 4 Halloween",
			"gameProviderId":1,
			"gameProviderDescription":"Espresso Games",
			"gameTypeId":1,
			"gameTypeDescription":"SLOT",
			"gameSubTypeId":1,
			"gameSubTypeDescription":"SLOT",
			"extWalletGameType":9438,
			"extWalletGameSubType":21,
			"gameSessionId" : 72747,
			"regulatorTicketId" : "N522920107CE0CWO",
			"regulatorTransactionCode" : "E9GiuH3pKJAgPeXy"
		}
	""".stripMargin
	}

	/**
	  * new fields added - v1.1"
	  * - nickname
	  * - productType
	  * - deviceType
	  * - gameSessionId
	  * - regulatorTicketId
	  * - regulatorTransactionCode
	  *
	  * removed - v1.1:
	  * - playerId
	  *
	  * fields changed - v1.2
	  * - deviceType: number > string
	  */
	def CANCEL_BET_Msg(gPhaseId: String = gamePhaseId, txtIdStr: String = txId) = {
		s"""
		  {
			"action": "cancel_bet",
			"timestamp":${DateTime.now().getMillis},
			"gamePhaseId":"${gPhaseId}",
			"txId":"${txtIdStr}",
			"accountCode":"3589125162250000",
            "nickname":"Andrea R.",
			"licenseeId":1,
			"productType":1,
			"deviceType":"Desktop",
			"gameCode": 123,
			"gameDescription":"Mad 4 Halloween",
			"gameProviderId":1,
			"gameProviderDescription":"Espresso Games",
			"gameTypeId":1,
			"gameTypeDescription":"SLOT",
			"gameSubTypeId":1,
			"gameSubTypeDescription":"SLOT",
			"extWalletGameType":9438,
			"extWalletGameSubType":21,
			"gameSessionId" : 72747,
			"regulatorTicketId" : "N522920107CE0CWO",
			"regulatorTransactionCode" : "E9GiuH3pKJAgPeXy"
		}
	""".stripMargin
	}

	/**
	  * new fields added - v1.1"
	  * - nickname
	  * - productType
	  * - deviceType
	  * - gameSessionId
	  * - regulatorTicketId
	  * - regulatorTransactionCode
	  *
	  * removed - v1.1:
	  * - playerId
	  *
	  * fields changed - v1.2
	  * - deviceType: number > string
	  */
	def WIN_Msg(betAmount: Double, amount: Double, gPhaseId: String = gamePhaseId, txtIdStr: String = txId): String = {
		s"""
			  {
				"action": "win",
				"additionalJackpotWinAmount": 200,
				"finalPhaseWager": true,
				"finalWager": true,
				"jackpotWinAmount": 200,
				"playBonusWinAmount": 200,
				"totalBetAmount": ${betAmount},
				"totalWinAmount": ${amount},
				"timestamp":${DateTime.now().getMillis + 3},
				"gamePhaseId":"${gPhaseId}",
				"txId":"${txtIdStr}",
				"accountCode":"3589125162250000",
	            "nickname":"Andrea R.",
				"licenseeId":1,
				"productType":1,
				"deviceType":1,
				"gameCode": 123,
				"gameDescription":"Mad 4 Halloween",
				"gameProviderId":1,
				"gameProviderDescription":"Espresso Games",
				"gameTypeId":1,
				"gameTypeDescription":"SLOT",
				"gameSubTypeId":1,
				"gameSubTypeDescription":"SLOT",
				"extWalletGameType":9438,
				"extWalletGameSubType":21,
				"gameSessionId" : 72747,
				"regulatorTicketId" : "N522920107CE0CWO",
				"regulatorTransactionCode" : "E9GiuH3pKJAgPeXy"
			}
		""".stripMargin
	}

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


