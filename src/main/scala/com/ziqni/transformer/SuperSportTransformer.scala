package com.ziqni.transformer

import com.ziqni.transformers.domain._
import com.ziqni.transformers.{Json, ZiqniContext, ZiqniMqTransformer}

import org.joda.time.DateTime
import org.json4s.JsonAST

import scala.language.implicitConversions
import scala.concurrent.{ExecutionContextExecutor, Future}

class SuperSportTransformer extends ZiqniMqTransformer with CustomFieldEntryImplicits {

  private val DefaultCurrencyCode: String = "HRK"
  private val SystemAsProductRefId: String = "system"
  private val TransactionTypeIdMap: Map[Int, String] = Map(
    11 -> "Casino deposit (positive amount value, added to player wallet)",
    12 -> "Casino withdraw (negative amount value, subtracted from player wallet)",
    13 -> "Win (positive amount value, added to player wallet)",
    14 -> "Bet (negative amount value, subtracted from player wallet)",
    15 -> "Player Bonus (Positive amount value when added to player wallet and cancelation of bonus when negative amount value, subtracted from player wallet)",
    101 -> "Player login to casino",
    102 -> "New player registration to casino"
  )

  private val BonusTransactionId = 15
  private val BonusCancellationActionKey = "bonus_cancellation"

  private val SpinTransactionId = 10005
  private val SpinTransactionActionKey = "spin"

  private val WinMultiplierAction = "win-multiplier"

  /**
   * Default method required by the transformer. No code to be written here.
   *
   * @param message
   * @param ziqniContext
   * @param args
   */
  override def apply(message: Array[Byte], ziqniContext: ZiqniContext, args: Map[String, Any]): Unit = {
    println(s"${DateTime.now()}[${ziqniContext.spaceName}] Nothing to do here.")
  }

  /**
   * Handle incoming messages from sqs
   */
  override def sqs(headers: Map[String, String], message: Array[Byte], messageId: String, ziqniContext: ZiqniContext): Unit = {
    implicit val ziqniContext: ZiqniContext = ziqniContext
    implicit val executionContext: ExecutionContextExecutor = ziqniContext.ziqniExecutionContext

    val messageAsString = ZiqniContext.convertByteArrayToString(message)
    val jsonObj: JsonAST.JValue = ZiqniContext.fromJsonString(messageAsString)

    try {
      /** Extract the transaction type and remove all white spaces */
      val transactionTypeName = Json.getFromJValue[String](jsonObj, "TransactionTypeName").replaceAll(" ", "_")

      transactionTypeName match {
        case "NewPlayerRegistration" => handleNewPlayerRegistration(jsonObj)
        case "PlayerLogin" => handlePlayerLogin(jsonObj)
        case _ => handleMessage(jsonObj)
      }
    } catch {
      case e: Exception =>
        println(s"${DateTime.now()}[${ziqniContext.spaceName}] Exception occurred for message: [$messageAsString]")
        e.printStackTrace()
    }
  }

  /**
   * Handle player registration message
   *
   * @param jsonObj
   * @param ziqniContext
   */
  private def handleNewPlayerRegistration(jsonObj: JsonAST.JValue)(implicit ziqniContext: ZiqniContext): Unit = {
    val dateCreated = Json.getFromJValue[String](jsonObj, "DateCreated")
    val transactionTypeName = Json.getFromJValue[String](jsonObj, "TransactionTypeName").replaceAll(" ", "_").toLowerCase
    val transactionTypeID = Json.getFromJValue[Int](jsonObj, "TransactionTypeID")
    val playerID = Json.getFromJValue[Long](jsonObj, "PlayerID")
    val playerNickname = Json.getFromJValue[String](jsonObj, "PlayerNickname").replaceAll(" ", "_")

    /** Check and create action */
    checkAndCreateEventAction(transactionTypeName, transactionTypeID)

    /** Check and create product if missing */
    getOrCreateProduct(SystemAsProductRefId, SystemAsProductRefId, Seq.empty, SystemAsProductRefId, None)

    /** Check and member product if missing */
    getOrCreateMember(playerID.toString, playerNickname)

    val amount = 0.0
    val transactionTime = new DateTime(dateCreated)

    /** Submit event for processing */
    val basicEvent = getBasicEvent(transactionTypeName, dateCreated, playerID.toString, SystemAsProductRefId, Map.empty, transactionTime, amount, None, None)

    /** Send event into CL for processing * */
    ziqniContext.ziqniApiAsync.pushEvent(basicEvent)
  }

  /**
   * Handle player login message
   *
   * @param jsonObj
   * @param ziqniContext
   */
  private def handlePlayerLogin(jsonObj: JsonAST.JValue)(implicit ziqniContext: ZiqniContext): Unit = {
    val dateCreated = Json.getFromJValue[String](jsonObj, "DateCreated")
    val transactionTypeID = Json.getFromJValue[Int](jsonObj, "TransactionTypeID")
    val transactionTypeName = Json.getFromJValue[String](jsonObj, "TransactionTypeName").replaceAll(" ", "_").toLowerCase
    val playerID = Json.getFromJValue[Long](jsonObj, "PlayerID")
    val playerNickname = Json.getFromJValue[String](jsonObj, "PlayerNickname").replaceAll(" ", "_")
    val loginID = Json.getFromJValue[Long](jsonObj, "LoginID")

    checkAndCreateEventAction(transactionTypeName, transactionTypeID)

    getOrCreateProduct(SystemAsProductRefId, SystemAsProductRefId, Seq.empty, SystemAsProductRefId, None)

    getOrCreateMember(playerID.toString, playerNickname)

    val amount = 0.0
    val transactionTime = new DateTime(dateCreated)

    val basicEvent = getBasicEvent(transactionTypeName, loginID.toString, playerID.toString, SystemAsProductRefId, Map.empty, transactionTime, amount, None, None)

    /** Send event into CL for processing * */
    ziqniContext.ziqniApiAsync.pushEvent(basicEvent)
  }

  /**
   * Handle all other messages
   *
   * @param jsonObj
   * @param ziqniContext
   */
  private def handleMessage(jsonObj: JsonAST.JValue)(implicit ziqniContext: ZiqniContext, executionContext: ExecutionContextExecutor): Unit = {
    val transactionID = Json.getFromJValue[Long](jsonObj, "TransactionID")
    val dateCreated = Json.getFromJValue[String](jsonObj, "DateCreated")
    val transactionTypeID = Json.getFromJValue[Int](jsonObj, "TransactionTypeID")
    val transactionTypeName = Json.getFromJValue[String](jsonObj, "TransactionTypeName").replaceAll(" ", "_").toLowerCase
    val playerID = Json.getFromJValue[Long](jsonObj, "PlayerID")
    val playerNickname = Json.getFromJValue[String](jsonObj, "PlayerNickname").replaceAll(" ", "_")
    val gameID = Json.getFromJValueAsOption[Long](jsonObj, "GameID")
    val gameName = Json.getFromJValueAsOption[String](jsonObj, "GameName").map(_.replaceAll(" ", "_"))
    val gameCode = Json.getFromJValueAsOption[String](jsonObj, "GameCode").map(_.replaceAll(" ", "_"))
    val providerName = Json.getFromJValueAsOption[String](jsonObj, "ProviderName").map(_.replaceAll(" ", "_"))
    val amount = Json.getFromJValue[Double](jsonObj, "Amount")
    val transactionSpinID = Json.getFromJValueAsOption[Long](jsonObj, "TransactionSpinID")

    checkAndCreateEventAction(transactionTypeName, transactionTypeID)

    val productRef = gameID.map(_.toString).getOrElse(SystemAsProductRefId)
    val productName = gameName.getOrElse(SystemAsProductRefId)

    getOrCreateProduct(productRef, productName, providerName.map(p => Seq(p)).getOrElse(Seq.empty), "", gameCode.flatMap(g => Option(Map("GameCode" -> g))))

    getOrCreateMember(playerID.toString, playerNickname)

    val transactionTime = new DateTime(dateCreated)

    val customFields: CustomFieldEntryMap =Map("transactionTypeID" -> transactionTypeID)

    /** Convert the amount to positive value since we handle positive values only. */
    val amountAsPositiveValue = if (amount < 0) amount * -1 else amount

    if (transactionTypeID == BonusTransactionId && amount < 0) {
      checkAndCreateEventAction(BonusCancellationActionKey, transactionTypeID)
      val basicEvent = getBasicEvent(BonusCancellationActionKey, transactionID.toString, playerID.toString, productRef, customFields.toMap, transactionTime, amountAsPositiveValue, None, Option(transactionSpinID.getOrElse(transactionID).toString))

      ziqniContext.ziqniApiAsync.pushEvent(basicEvent)
    } else {
      val basicEvent = getBasicEvent(transactionTypeName, transactionID.toString, playerID.toString, productRef, customFields.toMap, transactionTime, amountAsPositiveValue, None, Option(transactionSpinID.getOrElse(transactionID).toString))

      if (transactionTypeID == 14) { // bet transaction
        if (transactionSpinID.isEmpty) throw new Exception("Received bet transaction without spin id.")
        ziqniContext.ziqniApiAsync.pushEventTransaction(basicEvent)

        checkAndCreateEventAction(SpinTransactionActionKey, SpinTransactionId)
        createSpinEvent(basicEvent)
      } else if (transactionTypeID == 13) {
        if (transactionSpinID.isEmpty) throw new Exception("Received win transaction without spin id.")
        ziqniContext.ziqniApiAsync.pushEvent(basicEvent)

        for {
          originalBetValue <- getBetValue(transactionSpinID.get)
          result <- createWinMultiplierEvent(None, originalBetValue, basicEvent)
        } yield result
      } else
        ziqniContext.ziqniApiAsync.pushEvent(basicEvent)

    }

  }

  private def getBasicEvent(action: String, transactionId: String, memberRefId: String, productRefId: String,
                            customFields: CustomFieldEntryMap, transactionTime: DateTime, amount: Double, currencyCode: Option[String], batchId: Option[String])(implicit ziqniContext: ZiqniContext) = {

    /** Convert the source value to base currency */
    val sourceValueConverted: Int = if (amount == 0) amount.toInt else convertToBaseCurrency(currencyCode, amount)

    /** Prepare basic event here * */
    val basicEvent = BasicEventModel(
      memberId = None,
      action = action,
      tags = Seq.empty,
      eventRefId = transactionId,
      memberRefId = memberRefId,
      entityRefId = productRefId,
      batchId = batchId,
      sourceValue = sourceValueConverted,
      customFields = customFields,
      transactionTimestamp = transactionTime

      /** Set this to time from the event * */
    )

    basicEvent
  }

  private def createSpinEvent(eventModel: BasicEventModel)(implicit ziqniContext: ZiqniContext): Unit = {
    val spinEvent = eventModel.copy(
      action = SpinTransactionActionKey,
      sourceValue = 1
    )

    ziqniContext.ziqniApiAsync.pushEvent(spinEvent)
  }

  private def convertToBaseCurrency(currency: Option[String], sourceValue: Double)(implicit ziqniContext: ZiqniContext): Int = {
    val sourceValueInLP = sourceValue * 100
    //		val currencyCodeToUse = currency.getOrElse(DefaultCurrencyCode)
    //		val currencyMultiplier = ziqniContext.getUoMMultiplierFromKey(currencyCodeToUse).getOrElse(1.0)
    //		val convertedValue = sourceValueInLP / currencyMultiplier
    sourceValueInLP.toInt
  }

  private def getOrCreateProduct(productReferenceId: String, displayName: String, providers: Seq[String], productType: String, metaData: Option[Map[String, String]])(implicit ziqniContext: ZiqniContext): String = {
    ziqniContext.ziqniApi.productIdFromProductRefId(productReferenceId) match {
      case Some(pId) => pId
      case _ =>
        val productId = ziqniContext.ziqniApi.createProduct(productReferenceId, displayName, providers, productType, 1, metaData)
        if (productId.isEmpty)
          println(s"${DateTime.now()}[${ziqniContext.spaceName}] Product creation failed for product reference [$productReferenceId]")

        productId.get
    }
  }

  private def getOrCreateMember(playerId: String, playerNickName: String)(implicit ziqniContext: ZiqniContext): String = {
    ziqniContext.ziqniApi.memberIdFromMemberRefId(playerId) match {
      case Some(mId) => mId
      case _ =>
        ziqniContext.ziqniApi.createMember(playerId, playerNickName, Seq.empty, None).get
    }
  }

  private def checkAndCreateEventAction(transactionTypeName: String, transactionTypeID: Int)(implicit ziqniContext: ZiqniContext): Unit = {
    if (!ziqniContext.ziqniApi.eventActionExists(transactionTypeName)) {
      val actionMetadata = new scala.collection.mutable.HashMap[String, String]()
      actionMetadata.put("transactionTypeID", transactionTypeID.toString)
      TransactionTypeIdMap.get(transactionTypeID).foreach(t => actionMetadata.put("transactionTypeInfo", t))
      ziqniContext.ziqniApi.createEventAction(transactionTypeName, Option(transactionTypeName), Option(actionMetadata.toMap),None)
    }
  }

  private def createWinMultiplierEvent(currency: Option[String], betAmount: Double, basicEventModel: BasicEventModel)(implicit ziqniContext: ZiqniContext): Future[Boolean] = {
    val betAmountConverted = convertToBaseCurrency(currency, betAmount)
    val winMultiplier = if (betAmountConverted == 0) 0 else basicEventModel.sourceValue / betAmountConverted
    val winMultiplierEvent =
      basicEventModel.copy(
        action = WinMultiplierAction,
        sourceValue = winMultiplier
      )

    ziqniContext.ziqniApiAsync.pushEvent(winMultiplierEvent)

  }

  /**
   * Extract bet action using the transaction spin ID
   */
  private def getBetValue(transactionSpinID: Long)(implicit ziqniContext: ZiqniContext, e: ExecutionContextExecutor): Future[Double] = {
      for {
        a <- ziqniContext.ziqniApiAsync.findByBatchId(transactionSpinID.toString)
      } yield a.map(_.sourceValue).sum
  }
}