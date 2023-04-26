package com.ziqni.transformer.sample

import com.ziqni.transformer.FastTrackKafkaSample
import com.ziqni.transformer.test.ZiqniTransformerTester
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, GivenWhenThen}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.must.Matchers

class FastTrackKafkaSampleTest extends AnyFunSpec with Matchers with GivenWhenThen with BeforeAndAfterEach with BeforeAndAfterAll {

  describe("Test the message queue receiver implementation") {

    val ziqniTransformerTester  = ZiqniTransformerTester.loadDefaultWithSampleData
    val fastTrackSampleTransformer = new FastTrackKafkaSample

    it("should receive a published user balance message") {
      
      Then("transform it into an event")
      fastTrackSampleTransformer.kafka(fastTrackSampleTransformer.TOPIC_USER_BALANCES_UPDATE, Array.empty, FastTrackKafkaSampleTest.userBalancesUpdateV1,ziqniTransformerTester.ziqniContextExt)
    }

    it("should receive a published payment message") {
      Then("transform it into an event")
      fastTrackSampleTransformer.kafka(fastTrackSampleTransformer.TOPIC_PAYMENT, Array.empty, FastTrackKafkaSampleTest.paymentV1,ziqniTransformerTester.ziqniContextExt)
    }

    it("should receive a published game round message") {
      Then("transform it into an event")
      fastTrackSampleTransformer.kafka(fastTrackSampleTransformer.TOPIC_GAME_ROUND, Array.empty, FastTrackKafkaSampleTest.gameRoundV1,ziqniTransformerTester.ziqniContextExt)
    }

    it("should receive a published user created message") {
      Then("transform it into an event")
      fastTrackSampleTransformer.kafka(fastTrackSampleTransformer.TOPIC_USER_CREATE_V2, Array.empty, FastTrackKafkaSampleTest.userCreateV2,ziqniTransformerTester.ziqniContextExt)
    }

    it("should receive a published user login message") {
      Then("transform it into an event")
      fastTrackSampleTransformer.kafka(fastTrackSampleTransformer.TOPIC_LOGIN_V2, Array.empty, FastTrackKafkaSampleTest.loginV2,ziqniTransformerTester.ziqniContextExt)
    }
  }

}
object FastTrackKafkaSampleTest {
  
  /**
   * https://www.fasttrack-solutions.com/en/resources/integration/real-time-data/login
   */
  def loginV2 = s"""{
                 |  "user_id": "7865312321",
                 |  "is_impersonated": true,
                 |  "ip_address": "192.0.2.1",
                 |  "user_agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.75 Safari/537.36",
                 |  "timestamp": "2015-03-02T8:27:58.721607Z",
                 |  "origin": "sub.example.com"
                 |}""".stripMargin.getBytes

  /**
   * https://www.fasttrack-solutions.com/en/resources/integration/real-time-data/payments
   */
  def paymentV1 = s"""{
                     |  "amount": 32.76,
                     |  "bonus_code": "CHRISTMAS2018",
                     |  "currency": "EUR",
                     |  "exchange_rate": 0.1,
                     |  "fee_amount": 2.34,
                     |  "note": "string",
                     |  "origin": "sub.example.com",
                     |  "payment_id": "23541",
                     |  "status": "Approved",
                     |  "timestamp": "2015-03-02T8:27:58.721607Z",
                     |  "type": "Debit",
                     |  "user_id": "7865312321",
                     |  "vendor_id": "562",
                     |  "vendor_name": "Skrill"
                     |}""".stripMargin.getBytes

  /**
   * https://www.fasttrack-solutions.com/en/resources/integration/real-time-data/casino/game-rounds
   */
  def gameRoundV1 = s"""{
                       |  "user_id": "7865312321",
                       |  "round_id": "12384A",
                       |  "game_id": "1234",
                       |  "game_name": "Dead or Alive",
                       |  "game_type": "Slots",
                       |  "vendor_id": "12",
                       |  "vendor_name": "Netent",
                       |  "real_bet_user": 10.0,
                       |  "real_win_user": 5.0,
                       |  "bonus_bet_user": 0.0,
                       |  "bonus_win_user": 0.0,
                       |  "real_bet_base": 10.0,
                       |  "real_win_base": 5.0,
                       |  "bonus_bet_base": 0,
                       |  "bonus_win_base": 0,
                       |  "user_currency": "EUR",
                       |  "device_type": "desktop",
                       |  "timestamp": "2015-03-02T8:27:58.721607+06:00",
                       |  "origin": "sub.example.com",
                       |	"meta":{
                       |		"key1": 10.00,
                       |		"key2": "some string",
                       |		"key3": false
                       |	}
                       |}""".stripMargin.getBytes

  /***
   * https://www.fasttrack-solutions.com/en/resources/integration/real-time-data/registrations
   */
  def userCreateV2 = s"""{
                        |  "user_id": "7865312321",
                        |  "url_referer": "https://www.example.com",
                        |  "note": "Any note",
                        |  "user_agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36",
                        |  "ip_address": "1.1.1.1",
                        |  "timestamp": "2015-03-02T8:27:58.721607Z",
                        |  "origin": "sub.example.com"
                        |}	""".stripMargin.getBytes

  /***
   * https://www.fasttrack-solutions.com/en/resources/integration/real-time-data/balances
   */
  def userBalancesUpdateV1 = s"""{
                                |  "balances": [
                                |    {
                                |      "amount": 50,
                                |      "currency": "EUR",
                                |      "key": "real_money",
                                |      "exchange_rate": 1
                                |    },
                                |    {
                                |      "amount": 50,
                                |      "currency": "EUR",
                                |      "key": "bonus_money",
                                |      "exchange_rate": 1
                                |    }
                                |  ],
                                |  "origin": "example.com",
                                |  "timestamp": "2019-07-16T12:00:00Z",
                                |  "user_id": "7865312321"
                                |}""".stripMargin.getBytes
}
