package com.ziqni.transformer

import com.ziqni.transformers.defaults.Event
import com.ziqni.transformers.domain._
import com.ziqni.transformers.webhooks.{CustomWebhookSettings, CustomWebhooks}
import com.ziqni.transformers.{ZiqniContext, ZiqniMqTransformer}
import org.joda.time.DateTime

import scala.language.implicitConversions

class SampleSQSWithCallbacks extends ZiqniMqTransformer with CustomWebhooks with CustomFieldEntryImplicits {

  private val PostToUrl: String = "<<some-url>>"

  implicit def toBooleanCustomFieldEntry(v: Boolean): CustomFieldEntryText = CustomFieldEntryText(v.toString)

  /**
   * Handle incoming message from SQS
   */
  override def apply(message: Array[Byte], ziqniContext: ZiqniContext, args: Map[String, Any]): Unit = {
    implicit val context: ZiqniContext = ziqniContext
    handleEvent(Event.fromByteArray(message))
  }

  private def handleEvent(event: Event)(implicit ziqniContext: ZiqniContext): Unit = {
    ziqniContext.ziqniApiAsync.pushEventTransaction(event.asZiqniEvent)
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
  override def getEntityChangeSubscriptionRequest(ziqniContext: ZiqniContext): Seq[ZiqniEntityChangeSubscriptionRequest] = webHookSettings.customEntityChangeSubscriptionRequest

  override def onEntityChanged(change: ZiqniEntityChanged, ziqniContext: ZiqniContext): Unit = onCustomEntityChanged(webHookSettings, change, ziqniContext)

  override def onCustomEntityChanged(settings: CustomWebhookSettings, change: ZiqniEntityChanged, ziqniContext: ZiqniContext): Unit = super.onCustomEntityChanged(settings, change, ziqniContext)

  override def onEntityStateChanged(change: ZiqniEntityStateChanged, ziqniContext: ZiqniContext): Unit = onCustomEntityStateChanged(webHookSettings, change, ziqniContext)

  override def onCustomEntityStateChanged(settings: CustomWebhookSettings, change: ZiqniEntityStateChanged, ziqniContext: ZiqniContext): Unit = super.onCustomEntityStateChanged(settings, change, ziqniContext)


  override def onNewProduct()(implicit settings: CustomWebhookSettings, basicEntityChanged: ZiqniEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onNewProduct()

  override def onNewMember()(implicit settings: CustomWebhookSettings, basicEntityChanged: ZiqniEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onNewMember()

  override def onCompetitionCreated()(implicit settings: CustomWebhookSettings, basicEntityChanged: ZiqniEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onCompetitionCreated()

  override def onCompetitionStarted()(implicit settings: CustomWebhookSettings, basicEntityStateChanged: ZiqniEntityStateChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onCompetitionStarted()

  override def onCompetitionFinished()(implicit settings: CustomWebhookSettings, basicEntityStateChanged: ZiqniEntityStateChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onCompetitionFinished()

  override def onCompetitionCancelled()(implicit settings: CustomWebhookSettings, basicEntityStateChanged: ZiqniEntityStateChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onCompetitionCancelled()

  override def onCompetitionRewardIssued()(implicit settings: CustomWebhookSettings, basicEntityChanged: ZiqniEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onCompetitionRewardIssued()

  override def onContestCreated()(implicit settings: CustomWebhookSettings, basicEntityChanged: ZiqniEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onContestCreated()

  override def onContestStarted()(implicit settings: CustomWebhookSettings, basicEntityStateChanged: ZiqniEntityStateChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onContestStarted()

  override def onContestFinished()(implicit settings: CustomWebhookSettings, basicEntityStateChanged: ZiqniEntityStateChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onContestFinished()

  override def onContestFinalised()(implicit settings: CustomWebhookSettings, basicEntityStateChanged: ZiqniEntityStateChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onContestFinalised()

  override def onContestCancelled()(implicit settings: CustomWebhookSettings, basicEntityStateChanged: ZiqniEntityStateChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onContestCancelled()

  override def onContestRewardCreated()(implicit settings: CustomWebhookSettings, basicEntityChanged: ZiqniEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onContestRewardCreated()

  override def onContestRewardIssued()(implicit settings: CustomWebhookSettings, basicEntityChanged: ZiqniEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onContestRewardIssued()

  override def onContestRewardClaimed()(implicit settings: CustomWebhookSettings, basicEntityStateChanged: ZiqniEntityStateChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onContestRewardClaimed()

  override def onAchievementCreated()(implicit settings: CustomWebhookSettings, basicEntityChanged: ZiqniEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onAchievementCreated()

  override def onAchievementRewardCreated()(implicit settings: CustomWebhookSettings, basicEntityChanged: ZiqniEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onAchievementRewardCreated()

  override def onAchievementRewardIssued()(implicit settings: CustomWebhookSettings, basicEntityChanged: ZiqniEntityChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onAchievementRewardIssued()

  override def onAchievementRewardClaimed()(implicit settings: CustomWebhookSettings, basicEntityStateChanged: ZiqniEntityStateChanged, timestamp: DateTime, additionalFields: Map[String, Any], ziqniContext: ZiqniContext): Unit = super.onAchievementRewardClaimed()
}