/*
 * Copyright (c) 2023. ZIQNI LTD registered in England and Wales, company registration number-09693684
 */
package com.ziqni.transformer

import com.typesafe.scalalogging.LazyLogging
import com.ziqni.transformers.domain.{BasicEventModel, CustomFieldEntry}
import com.ziqni.transformers.{ZiqniContext, ZiqniMqTransformer}
import org.joda.time.DateTime
import org.json4s.DefaultFormats
import org.json4s.jackson.parseJson

import scala.concurrent.ExecutionContextExecutor

/**
 * Samples taken from https://www.fasttrack-solutions.com/en/resources/integration/real-time-data/registrations
 */
class FastTrackSample extends ZiqniMqTransformer with LazyLogging {

  private implicit val formats: DefaultFormats.type = DefaultFormats

  val TOPIC_USER_BALANCES_UPDATE = "USER_BALANCES_UPDATE"
  val TOPIC_PAYMENT = "PAYMENT"
  val TOPIC_GAME_ROUND = "GAME_ROUND"
  val TOPIC_USER_CREATE_V2 = "USER_CREATE_V2"
  val TOPIC_LOGIN_V2 = "LOGIN_V2"
  override def apply(message: Array[Byte], ziqniContext: ZiqniContext, args: Map[String, Any]): Unit = {
    implicit val z: ZiqniContext = ziqniContext
    implicit val e: ExecutionContextExecutor = ziqniContext.ziqniExecutionContext

    val topic = args.get("topic").map(s => s.toString)
    val messageAsString = ZiqniContext.convertByteArrayToString(message)
    val jsValue = parseJson(messageAsString)

    if (topic.exists(topic => topic.equalsIgnoreCase(TOPIC_USER_BALANCES_UPDATE)))
      handleUserBalancesUpdate(jsValue.extract[UserBalancesUpdate])

    else if (topic.exists(topic => topic.equalsIgnoreCase(TOPIC_PAYMENT)))
      handlePayment(jsValue.extract[Payment])

    else if (topic.exists(topic => topic.equalsIgnoreCase(TOPIC_GAME_ROUND)))
      handleGameRound(jsValue.extract[GameRound])

    else if (topic.exists(topic => topic.equalsIgnoreCase(TOPIC_USER_CREATE_V2)))
      handleUserCreateV2(jsValue.extract[UserCreateV2])

    else if (topic.exists(topic => topic.equalsIgnoreCase(TOPIC_LOGIN_V2)))
      handleLoginV2(jsValue.extract[LoginV2])

    else
      throw new NotImplementedError(s"The topic [$topic] has not been implemented")
  }

  private def handleUserBalancesUpdate(userBalancesUpdate: UserBalancesUpdate)(implicit ziqniContext: ZiqniContext, context: ExecutionContextExecutor): Unit = {}

  private def handlePayment(payment: Payment)(implicit ziqniContext: ZiqniContext, context: ExecutionContextExecutor): Unit = {}

  private def handleGameRound(gameRound: GameRound)(implicit ziqniContext: ZiqniContext, context: ExecutionContextExecutor): Unit = {}

  private def handleUserCreateV2(userCreateV2: UserCreateV2)(implicit ziqniContext: ZiqniContext, context: ExecutionContextExecutor): Unit = {}

  private def handleLoginV2(loginV2: LoginV2)(implicit ziqniContext: ZiqniContext, context: ExecutionContextExecutor): Unit = {}


  /**
   * These models were generated from the json object using https://transform.tools/json-to-scala-case-class
   */
  case class Balances(
                       amount: Double,
                       exchange_rate: Int,
                       currency: String,
                       key: String
                     ) {
    def asBasicEventModel: BasicEventModel = {
      null
    }
  }

  case class UserBalancesUpdate(
                                 user_id: String,
                                 timestamp: DateTime,
                                 origin: String,
                                 balances: Seq[Balances]
                               ) {
    def asBasicEventModel: BasicEventModel = {
      null
    }
  }

  case class Payment(
                      amount: Double,
                      bonus_code: String,
                      currency: String,
                      exchange_rate: Double,
                      fee_amount: Double,
                      note: String,
                      origin: String,
                      payment_id: String,
                      status: String,
                      timestamp: DateTime,
                      `type`: String,
                      user_id: String,
                      vendor_id: String,
                      vendor_name: String
                    ) {
    def asBasicEventModel: BasicEventModel = {
      null
    }
  }

  case class GameRound(
                        user_id: String,
                        round_id: String,
                        game_id: String,
                        game_name: String,
                        game_type: String,
                        vendor_id: String,
                        vendor_name: String,
                        real_bet_user: Int,
                        real_win_user: Int,
                        bonus_bet_user: Int,
                        bonus_win_user: Int,
                        real_bet_base: Int,
                        real_win_base: Int,
                        bonus_bet_base: Int,
                        bonus_win_base: Int,
                        user_currency: String,
                        device_type: String,
                        timestamp: DateTime,
                        origin: String,
                        meta: Meta
                      ) {
    def asBasicEventModel: BasicEventModel = BasicEventModel(
      memberId = None,
      action = "game-round",
      tags = Seq.empty,
      eventRefId = null,
      memberRefId = user_id,
      entityRefId = game_id,
      batchId = None,
      sourceValue = 1,
      transactionTimestamp = timestamp,
      customFields = Map[String, CustomFieldEntry[Any]](
        "round_id" -> round_id,
        "real_bet_user" -> real_bet_user,
        "real_win_user" -> real_win_user,
        "bonus_bet_user" -> bonus_bet_user,
        "bonus_win_user" -> bonus_win_user,
        "real_bet_base" -> real_bet_base,
        "real_win_base" -> real_win_base,
        "bonus_bet_base" -> bonus_bet_base,
        "bonus_win_base" -> bonus_win_base,
        "user_currency" -> user_currency,
        "device_type" -> device_type,
        "origin" -> origin,
      )
    )
  }

  case class Meta(
                   key1: Int,
                   key2: String,
                   key3: Boolean
                 )

  case class UserCreateV2(
                           user_id: String,
                           url_referer: String,
                           note: String,
                           user_agent: String,
                           ip_address: String,
                           timestamp: DateTime,
                           origin: String
                         ) {
    def asBasicEventModel: BasicEventModel = BasicEventModel(
      memberId = None,
      action = "user-create",
      tags = Seq.empty,
      eventRefId = null,
      memberRefId = user_id,
      entityRefId = "system",
      batchId = None,
      sourceValue = 1,
      transactionTimestamp = timestamp,
      customFields = Map[String, CustomFieldEntry[Any]](
        "url_referer" -> url_referer,
        "note" -> note,
        "user_agent" -> user_agent,
        "ip_address" -> ip_address,
        "origin" -> origin
      )
    )
  }

  case class LoginV2(
                      user_id: String,
                      is_impersonated: Boolean,
                      ip_address: String,
                      user_agent: String,
                      timestamp: DateTime,
                      origin: String
                    ) {
    def asBasicEventModel: BasicEventModel = BasicEventModel(
      memberId = None,
      action = "login",
      tags = Seq.empty,
      eventRefId = null,
      memberRefId = user_id,
      entityRefId = "system",
      batchId = None,
      sourceValue = 1,
      transactionTimestamp = timestamp,
      customFields = Map[String, CustomFieldEntry[Any]](
        "is_impersonated" -> is_impersonated,
        "ip_address" -> ip_address,
        "user_agent" -> user_agent,
        "origin" -> origin
      )
    )
  }

  private implicit def toCustomFieldEntry(s: String): CustomFieldEntry[Any] = new CustomFieldEntry[Any]("Text", s)

  private implicit def toCustomFieldEntry(s: Array[String]): CustomFieldEntry[Any] = new CustomFieldEntry[Any]("TextArray", s)

  private implicit def toCustomFieldEntry(s: Boolean): CustomFieldEntry[Any] = new CustomFieldEntry[Any]("Text", s)

  private implicit def toCustomFieldEntry(s: Int): CustomFieldEntry[Any] = new CustomFieldEntry[Any]("Number", s)
}
