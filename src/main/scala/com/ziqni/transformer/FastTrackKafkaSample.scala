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
import scala.language.implicitConversions

/**
 * Samples taken from https://www.fasttrack-solutions.com/en/resources/integration/real-time-data/registrations
 */
class FastTrackKafkaSample extends ZiqniMqTransformer with LazyLogging {

  private implicit val formats: DefaultFormats.type = DefaultFormats

  val TOPIC_PAYMENT = "PAYMENT"
  val TOPIC_LOGIN_V2 = "LOGIN_V2"
  val TOPIC_GAME_ROUND = "GAME_ROUND"
  val TOPIC_USER_CREATE_V2 = "USER_CREATE_V2"
  val TOPIC_USER_BALANCES_UPDATE = "USER_BALANCES_UPDATE"

  override def apply(message: Array[Byte], ziqniContext: ZiqniContext, args: Map[String, Any]): Unit = {
    implicit val z: ZiqniContext = ziqniContext
    implicit val e: ExecutionContextExecutor = ziqniContext.ziqniExecutionContext

    val topic = args.get("topic").map(s => s.toString)
    val messageAsString = ZiqniContext.convertByteArrayToString(message)
    val jsValue = parseJson(messageAsString)

    args.get("topic").map(s => s.toString) match {
      case Some(topic) =>

        if (topic.equalsIgnoreCase(TOPIC_USER_BALANCES_UPDATE))
          handleUserBalancesUpdate(jsValue.extract[UserBalancesUpdate])

        if (topic.equalsIgnoreCase(TOPIC_USER_BALANCES_UPDATE))
          handleUserBalancesUpdate(jsValue.extract[UserBalancesUpdate])

        else if (topic.equalsIgnoreCase(TOPIC_PAYMENT))
          handlePayment(jsValue.extract[Payment])

        else if (topic.equalsIgnoreCase(TOPIC_GAME_ROUND))
          handleGameRound(jsValue.extract[GameRound])

        else if (topic.equalsIgnoreCase(TOPIC_USER_CREATE_V2))
          handleUserCreateV2(jsValue.extract[UserCreateV2])

        else if (topic.equalsIgnoreCase(TOPIC_LOGIN_V2))
          handleLoginV2(jsValue.extract[LoginV2])
          
      case _ =>
        throw new NotImplementedError(s"The topic [$topic] has not been implemented")
    }
  }

  private def handleUserBalancesUpdate(userBalancesUpdate: UserBalancesUpdate)(implicit ziqniContext: ZiqniContext, context: ExecutionContextExecutor): Unit = {
    ziqniContext.ziqniApiAsync.pushEvents(userBalancesUpdate.asBasicEventModel)
  }

  private def handlePayment(payment: Payment)(implicit ziqniContext: ZiqniContext, context: ExecutionContextExecutor): Unit = {
    ziqniContext.ziqniApiAsync.pushEvent(payment.asBasicEventModel)
  }

  private def handleGameRound(gameRound: GameRound)(implicit ziqniContext: ZiqniContext, context: ExecutionContextExecutor): Unit = {
    ziqniContext.ziqniApiAsync.pushEvent(gameRound.asBasicEventModel)
  }

  private def handleUserCreateV2(userCreateV2: UserCreateV2)(implicit ziqniContext: ZiqniContext, context: ExecutionContextExecutor): Unit = {
    ziqniContext.ziqniApiAsync.pushEvent(userCreateV2.asBasicEventModel)
  }

  private def handleLoginV2(loginV2: LoginV2)(implicit ziqniContext: ZiqniContext, context: ExecutionContextExecutor): Unit = {
    ziqniContext.ziqniApiAsync.pushEvent(loginV2.asBasicEventModel)
  }

  /// These models were generated from the json object using https://transform.tools/json-to-scala-case-class ///

  private case class UserBalancesUpdate(
                                 user_id: String,
                                 timestamp: DateTime,
                                 origin: String,
                                 balances: Seq[Balance]
                               ) {
    def asBasicEventModel: Seq[BasicEventModel] = {
      balances.map(balance =>
        BasicEventModel(
          memberId = None, // CAN BE NONE - IF NONE THEN LOOKUP OR CREATE, IF NOT NONE THEN CONFIRM
          memberRefId = user_id, // CANNOT BE NULL
          action = "user-balances-update",
          tags = Seq.empty,
          eventRefId = "user-balances-update" + timestamp.getMillis.toString + balance.key,
          entityRefId = "system",
          batchId = None,
          sourceValue = balance.amount,
          transactionTimestamp = timestamp,
          metadata = Map.empty,
          customFields = Map[String, CustomFieldEntry[Any]](
            "exchange_rate" -> balance.exchange_rate,
            "currency" -> balance.currency,
            "key" -> balance.key
          )
        )
      )
    }
  }

  private case class Balance(
                       amount: Double,
                       exchange_rate: Double,
                       currency: String,
                       key: String
                     )
  private case class Payment(
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
    def asBasicEventModel: BasicEventModel = BasicEventModel(
      memberId = None,
      action = "payment",
      tags = Seq.empty,
      eventRefId = payment_id,
      memberRefId = user_id,
      entityRefId = vendor_id,
      batchId = None,
      sourceValue = amount,
      transactionTimestamp = timestamp,
      metadata = Map.empty,
      customFields = Map[String, CustomFieldEntry[Any]](
        "bonus_code" -> bonus_code,
        "currency" -> currency,
        "exchange_rate" -> exchange_rate,
        "note" -> note,
        "origin" -> origin,
        "payment_id" -> payment_id,
        "status" -> status,
        "type" -> `type`,
        "vendor_id" -> vendor_id,
        "vendor_name" -> vendor_name
      )
    )
  }

  private case class GameRound(
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
      eventRefId = round_id,
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

  private case class Meta(
                   key1: Int,
                   key2: String,
                   key3: Boolean
                 )

  private case class UserCreateV2(
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
      eventRefId = "user-create" + timestamp.getMillis.toString + user_id,
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

  private case class LoginV2(
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
      eventRefId = "login" + timestamp.getMillis.toString + user_id,
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

  private implicit def toCustomFieldEntry(s: Double): CustomFieldEntry[Any] = new CustomFieldEntry[Any]("Number", s)
}
