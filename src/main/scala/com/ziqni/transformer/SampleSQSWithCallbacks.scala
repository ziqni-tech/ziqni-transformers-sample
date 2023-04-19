package com.ziqni.transformer

import com.ziqni.transformers.domain.{BasicEntityChangeSubscriptionRequest, BasicEntityChanged, BasicEntityStateChanged, BasicEventModel, CustomFieldEntry}
import com.ziqni.transformers.webhooks.{CustomWebhookSettings, CustomWebhooks}
import com.ziqni.transformers.{ZiqniContext, ZiqniMqTransformer}
import org.joda.time.DateTime
import org.json4s.DefaultFormats
import org.json4s.jackson.parseJson

import java.util.Objects

class SampleSQSWithCallbacks extends ZiqniMqTransformer with CustomWebhooks {

  private implicit val formats: DefaultFormats.type = DefaultFormats
  private val Number: String = "Number"
  private val Text: String = "Text"
  private val PostToUrl: String = "<<some-url>>"

  /**
   * Handle incoming message from SQS
   */
  override def apply(message: Array[Byte], ziqniContext: ZiqniContext, args: Map[String, Any]): Unit = {
    implicit val context: ZiqniContext = ziqniContext
    val messageAsString = ZiqniContext.convertByteArrayToString(message)

    parseJson(messageAsString)
      .extract[List[ReevoEventMessage]]
      .foreach(a1 =>
        handleEvent(a1)
      )
  }

  private def handleEvent(reevoEventMessage: ReevoEventMessage)(implicit ziqniContext: ZiqniContext): Unit = {
    ziqniContext.ziqniApiAsync.pushEventTransaction(reevoEventMessage.asBasicEventModel)
  }

  ////////////////////////////////////////////////////////////
  /// >>       WEBHOOK REPLACEMENT:: OPTIONAL           << ///
  /// >> Replace old webhooks with system notifications << ///
  /// >>        NOT RECOMMENDED, USE WEBHOOKS           << ///
  ////////////////////////////////////////////////////////////

  private final val webHookSettings = CustomWebhookSettings(
    url = PostToUrl,
    headers = Map.empty,
    basicAuth = None,
    onNewProductEnabled = true,
    onNewMemberEnabled = true,
    onCompetitionCreatedEnabled = true,
    onCompetitionStartedEnabled = true,
    onCompetitionFinishedEnabled = true,
    onCompetitionCancelledEnabled = true,
    onCompetitionRewardIssuedEnabled = true,
    onContestCreatedEnabled = true,
    onContestStartedEnabled = true,
    onContestFinishedEnabled = true,
    onContestFinalisedEnabled = true,
    onContestCancelledEnabled = true,
    onContestRewardCreatedEnabled = true,
    onContestRewardIssuedEnabled = true,
    onContestRewardClaimedEnabled = true,
    onAchievementCreatedEnabled = true,
    onAchievementRewardCreatedEnabled = true,
    onAchievementRewardIssuedEnabled = true,
    onAchievementRewardClaimedEnabled = true
  )

  /**
   * The system events we would like to be notified about
   *
   * @param ziqniContext The context for this transformer
   * @return
   */
  override def getEntityChangeSubscriptionRequest(ziqniContext: ZiqniContext): Seq[BasicEntityChangeSubscriptionRequest] = webHookSettings.classicEntityChangeSubscriptionRequest

  override def onEntityChanged(change: BasicEntityChanged, ziqniContext: ZiqniContext): Unit = onCustomEntityChanged(webHookSettings, change, ziqniContext)

  override def onCustomEntityChanged(settings: CustomWebhookSettings, change: BasicEntityChanged, ziqniContext: ZiqniContext): Unit = super.onCustomEntityChanged(settings, change, ziqniContext)

  override def onEntityStateChanged(change: BasicEntityStateChanged, ziqniContext: ZiqniContext): Unit = onCustomEntityStateChanged(webHookSettings, change, ziqniContext)

  override def onCustomEntityStateChanged(settings: CustomWebhookSettings, change: BasicEntityStateChanged, ziqniContext: ZiqniContext): Unit = super.onCustomEntityStateChanged(settings, change, ziqniContext)

  override def onNewProduct()(implicit settings: CustomWebhookSettings, basicEntityChanged: BasicEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onNewProduct()

  override def onNewMember()(implicit settings: CustomWebhookSettings, basicEntityChanged: BasicEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onNewMember()

  override def onCompetitionCreated()(implicit settings: CustomWebhookSettings, basicEntityChanged: BasicEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onCompetitionCreated()

  override def onCompetitionStarted()(implicit settings: CustomWebhookSettings, basicEntityStateChanged: BasicEntityStateChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onCompetitionStarted()

  override def onCompetitionFinished()(implicit settings: CustomWebhookSettings, basicEntityStateChanged: BasicEntityStateChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onCompetitionFinished()

  override def onCompetitionCancelled()(implicit settings: CustomWebhookSettings, basicEntityStateChanged: BasicEntityStateChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onCompetitionCancelled()

  override def onCompetitionRewardIssued()(implicit settings: CustomWebhookSettings, basicEntityChanged: BasicEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onCompetitionRewardIssued()

  override def onContestCreated()(implicit settings: CustomWebhookSettings, basicEntityChanged: BasicEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onContestCreated()

  override def onContestStarted()(implicit settings: CustomWebhookSettings, basicEntityStateChanged: BasicEntityStateChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onContestStarted()

  override def onContestFinished()(implicit settings: CustomWebhookSettings, basicEntityStateChanged: BasicEntityStateChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onContestFinished()

  override def onContestFinalised()(implicit settings: CustomWebhookSettings, basicEntityStateChanged: BasicEntityStateChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onContestFinalised()

  override def onContestCancelled()(implicit settings: CustomWebhookSettings, basicEntityStateChanged: BasicEntityStateChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onContestCancelled()

  override def onContestRewardCreated()(implicit settings: CustomWebhookSettings, basicEntityChanged: BasicEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onContestRewardCreated()

  override def onContestRewardIssued()(implicit settings: CustomWebhookSettings, basicEntityChanged: BasicEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onContestRewardIssued()

  override def onContestRewardClaimed()(implicit settings: CustomWebhookSettings, basicEntityStateChanged: BasicEntityStateChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onContestRewardClaimed()

  override def onAchievementRewardCreated()(implicit settings: CustomWebhookSettings, basicEntityChanged: BasicEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onAchievementRewardCreated()

  override def onAchievementRewardIssued()(implicit settings: CustomWebhookSettings, basicEntityChanged: BasicEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onAchievementRewardIssued()

  override def onAchievementRewardClaimed()(implicit settings: CustomWebhookSettings, basicEntityStateChanged: BasicEntityStateChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onAchievementRewardClaimed()

  override def onAchievementCreated()(implicit settings: CustomWebhookSettings, basicEntityChanged: BasicEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onAchievementCreated()



  /**
   *
   * @param gameId              Id of the game - required
   * @param date                timestamp of transaction - required
   * @param bet_type            bet type (bet/win) - required
   * @param bonus               bonus win ( not used currently always 0 can be used in future) - required
   * @param freeRoundWin        Freeround win - required
   * @param bonusBet            Not currently used , can be used in future
   * @param jackpotContribution Jackpot contribution - required
   * @param operatorId          OperatorId - required
   * @param agentId             AgentID - required
   * @param playerSessionId     PlayersessionId - required
   * @param currency            Currency - required
   * @param bonusWin            bonusWin ( not used currently always 0 can be used in future) - required
   * @param amount              amount (bet / win amount) - required
   * @param ticket              ticket (roundId) - required
   * @param transId             transId - required
   * @param gameTitle           gameTitle - required
   * @param system_out          system_out - can be skipped
   * @param date_end            date_end - can be skipped
   * @param sessionId           sessionId - required
   * @param date_start          date_start - can be skipped
   * @param gameSessionId       gamesessionId - required
   * @param provider            game provider code - required
   * @param requestId           requestId - required
   * @param playerId            playerId - required
   * @param status              Transaction status - can be skipped
   */
  case class ReevoEventMessage(
                                gameId: Int, // Product Ref ID
                                date: Long, // Transaction Time
                                bet_type: String, // Action
                                bonus: Int,
                                freeRoundWin: Int,
                                bonusBet: Int,
                                jackpotContribution: Int,
                                operatorId: Int,
                                agentId: Int,
                                playerSessionId: String,
                                currency: String,
                                bonusWin: Int,
                                amount: Double, // Source value
                                ticket: String,
                                transId: String,
                                gameTitle: String,
                                system_out: Int,
                                date_end: String,
                                sessionId: String,
                                date_start: String,
                                gameSessionId: String,
                                provider: String,
                                requestId: String,
                                playerId: Int, // Member Ref ID
                                status: String
                              ) {
    def asBasicEventModel: BasicEventModel = {
      val customFields = Map[String, CustomFieldEntry[Any]](
        "bonus" -> CustomFieldEntry(Number, bonus),
        "freeroundwin" -> CustomFieldEntry(Number, freeRoundWin),
        "bonusbet" -> CustomFieldEntry(Number, bonusBet),
        "jackpotcontribution" -> CustomFieldEntry(Number, jackpotContribution),
        "operatorid" -> CustomFieldEntry(Number, operatorId),
        "agentid" -> CustomFieldEntry(Number, agentId),
        "playersessionid" -> CustomFieldEntry(Text, playerSessionId),
        "ticket" -> CustomFieldEntry(Text, ticket),
        "transid" -> CustomFieldEntry(Text, transId),
        "sessionid" -> CustomFieldEntry(Text, sessionId),
        "gamesessionid" -> CustomFieldEntry(Text, gameSessionId),
        "provider" -> CustomFieldEntry(Text, provider),
        "requestid" -> CustomFieldEntry(Text, requestId),
        "status" -> CustomFieldEntry(Text, status)
      )

      val nonNegativeAmount = if (Objects.nonNull(amount) && amount >= 0) amount else amount * (-1)
      BasicEventModel(None, playerId.toString, gameId.toString, transId, None, bet_type, nonNegativeAmount, new DateTime(date), Seq.empty, Map.empty, customFields)
    }
  }
}