package com.ziqni.transformer

/*
 * Copyright (c) 2023. ZIQNI LTD registered in England and Wales, company registration number-09693684
 */
import com.typesafe.scalalogging.LazyLogging
import com.ziqni.transformers.domain.{BasicAwardModel, BasicEntityChangeSubscriptionRequest, BasicEntityChanged, BasicEventModel, BasicRewardModel, HttpResponseEntity}
import com.ziqni.transformers.{EventbusAddress, EventbusArgs, EventbusGroup, EventbusMessage, Json, ZiqniContext, ZiqniMqTransformer, ZiqniTransformerEventbusConfig, ZiqniTransformerInfo}
import org.joda.time.DateTime
import org.json4s.{DefaultFormats, JArray, JValue, JsonAST}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Try

class AdvancedSampleWithCallback extends ZiqniMqTransformer with LazyLogging {
	private implicit val formats: DefaultFormats.type = DefaultFormats

	// Callback example
	private val PostToUrl = "SOME-URL-TO-POST-TO"

	// JSON keys
	val ACTION_KEY = "action"

	// Action types
	val TRANSACTION_SUMMARY = "round_summary"
	val BET_ACTION = "bet"
	val CANCEL_BET_ACTION = "cancel_bet"
	val WIN_ACTION = "win"
	val LOSS_ACTION = "loss"
	val WIN_MULTIPLIER = "win_multiplier"

	/** Convert milliseconds time into human readable time * */
	private def timeStampToDateTime(timestamp: Long) = new DateTime(timestamp)


	/**
	 * Happens when the class is initialized
	 *
	 */
	override def initTransformerEventbus(ziqniTransformerInfo: ZiqniTransformerInfo): ZiqniTransformerEventbusConfig =
		ZiqniTransformerEventbusConfig(ziqniTransformerInfo.connectionId, "sisal-mq-trans", List(handleEventbusMessage))

	private def handleEventbusMessage(from: Option[EventbusAddress], toGroup: Option[EventbusGroup], message: EventbusMessage, args: EventbusArgs): Unit = {
		// Do something with the message from another transformer
	}

	/**
	 * Handle incoming message from RabbitMQ
	 *
	 * @param message
	 * @param args
	 * @param ziqniApi
	 */
	override def apply(message: Array[Byte], ziqniContext: ZiqniContext, args: Map[String, Any]): Unit = {
		handleMessage(message, ziqniContext, args)
	}

	private def handleMessage(message: Array[Byte], ziqniContext: ZiqniContext, args: Map[String, Any]): Unit = {

		val messageAsString = ZiqniContext.convertByteArrayToString(message)

		val jsonObj = ZiqniContext.fromJsonString(messageAsString)
		jsonObj match {
			case jArr: JArray => jArr.arr.foreach(jsonValue => handleIndividualJObject(jsonValue, ziqniContext))
			case _ => handleIndividualJObject(jsonObj, ziqniContext)
		}

	}


	private def handleIndividualJObject(jsonObj: JValue, ziqniContext: com.ziqni.transformers.ZiqniContext): Unit = {
		if (Json.keyExists(jsonObj, ACTION_KEY)) {
			val eventType: String = Json.getFromJValue[String](jsonObj, ACTION_KEY)

			eventType match {
				case TRANSACTION_SUMMARY =>
					handleTransactionSummaryActionEventMapping(eventType, jsonObj, ziqniContext)

				case _ =>
					throw new Exception(s"Message with unknown event type received.")
			}
		} else {
			throw new Exception(s"Message with unknown event type received.")
		}
	}

	private def handleTransactionSummaryActionEventMapping(eventType: String, jsonObj: JsonAST.JValue, ziqniContext: com.ziqni.transformers.ZiqniContext): Unit = {
		implicit val context: ExecutionContextExecutor = ziqniContext.ziqniExecutionContext

		val additionalJackpotWinAmount = Json.getFromJValue[Int](jsonObj, "additionalJackpotWinAmount")
		val jackpotWinAmount = Json.getFromJValue[Double](jsonObj, "jackpotWinAmount")
		val playBonusWinAmount = Json.getFromJValue[Double](jsonObj, "playBonusWinAmount")
		val totalBetAmount = Json.getFromJValueAsOption[Double](jsonObj, "totalBetAmount")
		val totalWinAmount = Json.getFromJValue[Double](jsonObj, "totalWinAmount")
		val timestamp = timeStampToDateTime(
			Json.getFromJValue[Long](jsonObj, "timestamp")
		)
		val gamePhaseId = Json.getFromJValue[String](jsonObj, "gamePhaseId")
		val txId = Json.getFromJValue[String](jsonObj, "txId")
		val accountCode = Json.getFromJValue[String](jsonObj, "accountCode")
		val deviceType = Json.getFromJValue[String](jsonObj, "deviceType")
		val nickname = Json.getFromJValueAsOption[String](jsonObj, "nickname")
		val licenseeId = Json.getFromJValue[Int](jsonObj, "licenseeId")
		val productType = Json.getFromJValue[Int](jsonObj, "productType")
		val gameCode = Json.getFromJValue[Int](jsonObj, "gameCode").toString
		val gameDescription = Json.getFromJValue[String](jsonObj, "gameDescription")
		val gameProviderId = Json.getFromJValue[Int](jsonObj, "gameProviderId")
		val gameProviderDescription = Json.getFromJValue[String](jsonObj, "gameProviderDescription")
		val gameTypeId = Json.getFromJValue[Int](jsonObj, "gameTypeId")
		val gameTypeDescription = Json.getFromJValue[String](jsonObj, "gameTypeDescription")
		val gameSubTypeId = Json.getFromJValue[Int](jsonObj, "gameSubTypeId")
		val extWalletGameType = Json.getFromJValue[Int](jsonObj, "extWalletGameType")
		val extWalletGameSubType = Json.getFromJValue[Int](jsonObj, "extWalletGameSubType")
		val regulatorTicketId = Json.getFromJValue[String](jsonObj, "regulatorTicketId")
		val regulatorTransactionCode = Json.getFromJValue[String](jsonObj, "regulatorTransactionCode")

		val totalWin: Double = totalWinAmount / 100

		val memberRefId = accountCode
		val memberMeta: scala.collection.mutable.Map[String, String] = new scala.collection.mutable.HashMap[String, String]()
		val customFields = new scala.collection.mutable.HashMap[String, Seq[Any]]()

		val groups: scala.collection.mutable.ArrayBuffer[String] = scala.collection.mutable.ArrayBuffer[String]()
		groups.append(s"licensee[$licenseeId]")
		groups.append(s"deviceType[$deviceType]")

		for {
			totalBet <- if (totalBetAmount.isEmpty) getBetValue(gamePhaseId, ziqniContext) else Future(totalBetAmount.get / 100)

			customFieldsSize <- Future(() -> {
				customFields.put("licenseeId", Seq(licenseeId))
				customFields.put("gameCode", Seq(gameCode))
				customFields.put("gameTypeId", Seq(gameTypeId))
				customFields.put("extWalletGameType", Seq(extWalletGameType))
				customFields.put("extWalletGameSubType", Seq(extWalletGameSubType))
				customFields.put("gameSubTypeId", Seq(gameSubTypeId))
				customFields.put("productType", Seq(productType))
				customFields.put("deviceType", Seq(deviceType))
				customFields.put("betValue", Seq(totalBet))
				customFields.put("winValue", Seq(totalWin))
				customFields.put("regulatorTicketId", Seq(regulatorTicketId))
				customFields.put("regulatorTransactionCode", Seq(regulatorTransactionCode))
				customFields.size
			})

			// create or update new member
			memberId <- getOrCreateMember(memberRefId, nickname, groups.toSeq, Option(memberMeta.toMap), { groups =>
				val updatedGroups: scala.collection.mutable.ArrayBuffer[String] = scala.collection.mutable.ArrayBuffer[String]()
				groups.foreach(gr =>
					if (gr != s"licensee[$licenseeId]")
						updatedGroups.append(gr)
				)
				updatedGroups.append(s"licensee[$licenseeId]")

				updatedGroups.toSeq
			}, { metadata => metadata }, ziqniContext)

			// get or create UoM
			uom <- ziqniContext.ziqniApiAsync.getUnitOfMeasure("euro")

			_ <- {

				uom.foreach(u => getOrCreateEventAction(eventType, ziqniContext, Option(u.getUnitOfMeasureKey)))

				getOrCreateProduct(gameCode, gameDescription, Seq(gameProviderId.toString, gameProviderDescription), gameTypeDescription, None, ziqniContext) // creation product

				if (playBonusWinAmount > 0) {
					val playBonusEvent = BasicEventModel(
						memberId = memberId,
						action = "win_play_bonus",
						tags = Seq.empty,
						eventRefId = txId,
						memberRefId = memberRefId,
						entityRefId = gameCode,
						batchId = Option(gamePhaseId),
						sourceValue = (playBonusWinAmount / 100),
						metadata = customFields.toMap,
						transactionTimestamp = timestamp
					)

					ziqniContext.ziqniApiAsync.pushEvent(playBonusEvent)
				}

				if (jackpotWinAmount > 0) {
					val jackpotWinEvent = BasicEventModel(
						memberId = memberId,
						action = "jackpot_win_amount",
						tags = Seq.empty,
						eventRefId = txId,
						memberRefId = memberRefId,
						entityRefId = gameCode,
						batchId = Option(gamePhaseId),
						sourceValue = (jackpotWinAmount / 100),
						metadata = customFields.toMap,
						transactionTimestamp = timestamp
					)

					ziqniContext.ziqniApiAsync.pushEvent(jackpotWinEvent)
				}

				if (additionalJackpotWinAmount > 0) {
					val additionalJackpotWinEvent = BasicEventModel(
						memberId = memberId,
						action = "jackpot_additional_win_amount",
						tags = Seq.empty,
						eventRefId = txId,
						memberRefId = memberRefId,
						entityRefId = gameCode,
						batchId = Option(gamePhaseId),
						sourceValue = (additionalJackpotWinAmount / 100),
						metadata = customFields.toMap,
						transactionTimestamp = timestamp
					)

					ziqniContext.ziqniApiAsync.pushEvent(additionalJackpotWinEvent)
				}

				if (totalWin > 0 && totalBet > 0) {
					val winMultiplier: Double = calculateWinMultiplier(totalBet, totalWin);
					val winMultiplierEvent = BasicEventModel(
						memberId = memberId,
						action = WIN_MULTIPLIER,
						tags = Seq.empty,
						eventRefId = txId,
						memberRefId = memberRefId,
						entityRefId = gameCode,
						batchId = Option(gamePhaseId),
						sourceValue = winMultiplier,
						metadata = customFields.toMap,
						transactionTimestamp = timestamp
					)

					ziqniContext.ziqniApiAsync.pushEvent(winMultiplierEvent)
				}

				val basicBetEvent = BasicEventModel(
					memberId = memberId,
					action = BET_ACTION,
					tags = Seq.empty,
					eventRefId = txId,
					memberRefId = memberRefId,
					entityRefId = gameCode,
					batchId = Option(gamePhaseId),
					sourceValue = totalBet,
					metadata = customFields.toMap,
					transactionTimestamp = timestamp
				)

				ziqniContext.ziqniApiAsync.pushEventTransaction(basicBetEvent)

				val basicEvent = BasicEventModel(
					memberId = memberId,
					action = WIN_ACTION,
					tags = Seq.empty,
					eventRefId = txId,
					memberRefId = memberRefId,
					entityRefId = gameCode,
					batchId = Option(gamePhaseId),
					sourceValue = totalWin,
					metadata = customFields.toMap,
					transactionTimestamp = timestamp
				)

				ziqniContext.ziqniApiAsync.pushEvent(basicEvent)
			}
		} yield memberId
	}

	/**
	 * handles member creation
	 *
	 * @param memberRef              external member reference
	 * @param displayName            external member display name
	 * @param groups                 member group segmentation
	 * @param memberMeta             additional metadata
	 * @param onMemberGroupUpdate    on member group update callback function
	 * @param onMemberMetadataUpdate on member metadata update callback function
	 * @param ziqniApi
	 */
	private def getOrCreateMember(memberRef: String, displayName: Option[String], groups: Seq[String],
																memberMeta: Option[Map[String, String]], onMemberGroupUpdate: Seq[String] => Seq[String],
																onMemberMetadataUpdate: Option[Map[String, String]] => Option[Map[String, String]], ziqniContext: com.ziqni.transformers.ZiqniContext): Future[Option[String]] = {

		implicit val context: ExecutionContextExecutor = ziqniContext.ziqniExecutionContext

		for {
			memberIdFound <- ziqniContext.ziqniApiAsync.memberIdFromMemberRefId(memberRef)
			out <- memberIdFound match {
				case Some(mid) =>
					val member = ziqniContext.ziqniApi.getMember(mid).get

					// group update
					val mGroups = member.getTags
					val gToSet = (if (mGroups.isEmpty) groups else {
						onMemberGroupUpdate(mGroups.get)
					}).toArray

					// metadata update
					val mMetadata = member.getMetaData
					val updateMetadata = if (mMetadata.isEmpty) memberMeta else {
						onMemberMetadataUpdate(mMetadata)
					}

					// update member object
					ziqniContext.ziqniApiAsync.updateMember(mid, Option(memberRef), Option(displayName.getOrElse(memberRef)), Option(gToSet.distinct), updateMetadata)
				case _ =>
					ziqniContext.ziqniApiAsync.createMember(memberRef, displayName.getOrElse(memberRef), groups.distinct, memberMeta)
			}} yield out
	}


	// handle action creation inside in action helpers
	private def getOrCreateEventAction(action: String, ziqniContext: ZiqniContext, basicUnitOfMeasureModelKey: Option[String]): Unit = {
		implicit val context: ExecutionContextExecutor = ziqniContext.ziqniExecutionContext

		ziqniContext.ziqniApiAsync.eventActionExists(action.toLowerCase).map(exists => {
			if (!exists)
				ziqniContext.ziqniApiAsync.createEventAction(action.toLowerCase, None, None, basicUnitOfMeasureModelKey)
		})
	}

	/**
	 * handle product creation
	 *
	 * @param productReferenceId external product reference ID
	 * @param displayName        external display name
	 * @param providers          provider name
	 * @param productType        product type
	 * @param metaData           additional metadata
	 * @param ziqniApi
	 */
	private def getOrCreateProduct(productReferenceId: String, displayName: String, providers: Seq[String], productType: String, metaData: Option[Map[String, String]], ziqniContext: com.ziqni.transformers.ZiqniContext): Unit = {
		implicit val context: ExecutionContextExecutor = ziqniContext.ziqniExecutionContext

		ziqniContext.ziqniApiAsync.productIdFromProductRefId(productReferenceId).map(result => {
			if (result.isEmpty)
				ziqniContext.ziqniApiAsync.createProduct(productReferenceId, displayName, providers, productType, 1, metaData)
		})
	}

	/**
	 * Extract bet action using the game phase ID
	 *
	 * @param gamePhaseId unique round ID
	 * @param ziqniApi
	 * @return
	 */
	private def getBetValue(gamePhaseId: String, ziqniContext: com.ziqni.transformers.ZiqniContext): Future[Double] = {
		implicit val context: ExecutionContextExecutor = ziqniContext.ziqniExecutionContext

		ziqniContext.ziqniApiAsync
			.findByBatchId(gamePhaseId)
			.map(eventModel => eventModel.filter(x => x.action == BET_ACTION))
			.map(betEvents => {
				if (betEvents.isEmpty)
					0.0
				else
					betEvents.map(x => x.sourceValue).sum
			})
	}

	/**
	 * Extract bet action using the game phase ID
	 *
	 * @param gamePhaseId unique round ID
	 * @param ziqniApi
	 * @return
	 */
	private def calculateWinMultiplier(totalBetAmount: Double, totalWinAmount: Double): Double = {
		totalWinAmount / totalBetAmount
	}

	////////////////////////////////////////////////////////
	///>>      WEBHOOK REPLACEMENT:: OPTIONAL          <<///
	///>>Replace old webhooks with system notifications<<///
	////////////////////////////////////////////////////////


	val TYPE_OF_CHANGE_CREATED = 1;
	val TYPE_OF_CHANGE_UPDATED = 2;
	val TYPE_OF_CHANGE_DELETED = 3;

	/**
	 * The system events we would like to be notified about
	 *
	 * @param ziqniContext The context for this transformer
	 * @return
	 */
	override def getEntityChangeSubscriptionRequest(ziqniContext: ZiqniContext): Seq[BasicEntityChangeSubscriptionRequest] = {
		Seq(
			BasicEntityChangeSubscriptionRequest("EntityChanged", "Reward"),
			BasicEntityChangeSubscriptionRequest("EntityChanged", "Member"),
			BasicEntityChangeSubscriptionRequest("EntityChanged", "Product"),

			BasicEntityChangeSubscriptionRequest("EntityChanged", "Award"),
			BasicEntityChangeSubscriptionRequest("EntityStateChanged", "Award"),

			BasicEntityChangeSubscriptionRequest("EntityChanged", "Achievement"),
			BasicEntityChangeSubscriptionRequest("EntityStateChanged", "Achievement"),

			BasicEntityChangeSubscriptionRequest("EntityChanged", "Competition"),
			BasicEntityChangeSubscriptionRequest("EntityStateChanged", "Competition"),

			BasicEntityChangeSubscriptionRequest("EntityChanged", "Contest"),
			BasicEntityChangeSubscriptionRequest("EntityStateChanged", "Contest")
		)
	}

	/**
	 * If the transformer is subscribed to entity changes then this method is invoked
	 *
	 * @param change The change events
	 */
	override def onEntityChanged(change: BasicEntityChanged, ziqniContext: ZiqniContext): Unit = {
		logger.error(s"onEntityChanged: typeOffChange[${change.typeOffChange}] - ${change.toString}")
		implicit val e: ExecutionContextExecutor = ziqniContext.ziqniExecutionContext
		implicit val z: ZiqniContext = ziqniContext
		implicit val c: BasicEntityChanged = change

		if ("Member".equalsIgnoreCase(change.entityType))
			onEntityChanged(onCreate = onMemberCreated)

		else if ("Achievement".equalsIgnoreCase(change.entityType))
			onEntityChanged(onCreate = onAchievementCreated)

		else if ("Reward".equalsIgnoreCase(change.entityType))
			onEntityChanged(onCreate = onRewardCreated)

		else if ("Award".equalsIgnoreCase(change.entityType))
			onEntityChanged(onCreate = onAwardIssued, onUpdate = onAwardClaimed)

		else
			logger.error(s"Unsupported entity type, typeOffChange:${change.entityType} - ${change.toString}")
	}

	private def onEntityChanged(onCreate: () => Unit, onUpdate: () => Unit = () => {})(implicit change: BasicEntityChanged): Unit = {
		if (TYPE_OF_CHANGE_CREATED == change.typeOffChange)
			onCreate.apply()
		else if (TYPE_OF_CHANGE_UPDATED == change.typeOffChange)
			onUpdate.apply()
		else
			logger.error(s"Unsupported entity change, typeOffChange:${change.typeOffChange} - ${change.toString}")
	}


	private def onMemberCreated()(implicit change: BasicEntityChanged, ziqniContext: ZiqniContext, e: ExecutionContextExecutor): Unit = {
		ziqniContext.ziqniApiAsync.getMember(change.entityId).map {
			case Some(member) => try {
				val body = Map[String, Any](
					"memberId" -> change.entityId,
					"memberRefId" -> change.entityRefId,
					"resourcePath" -> s"/api/${ziqniContext.spaceName}/members/${change.entityId}",
					"objectType" -> "NewMember"
				)
				defaultPushBody(body, "onNewMember", response => {
					val json = ZiqniContext.fromJsonString(response.content)
					for {
						name <- (json \ "name").extractOpt[String]
						groups <- (json \ "flags").extractOpt[Array[String]]
					} yield {
						ziqniContext.ziqniApiAsync.updateMember(member.getClMemberId, Some(member.getMemberRefId), Some(name), Some(groups), member.getMetaData)
					}
				},
					response => {
						val meta = member.getMetaData.getOrElse(Map.empty) ++ Map("error" -> response.content)
						ziqniContext.ziqniApiAsync.updateMember(member.getClMemberId, Some(member.getMemberRefId), member.getDisplayName, member.getTags, Some(meta))
					})
			} catch {
				case e: Throwable =>
					logger.error(s"Failed to handle member ${change.entityId}", e)
					throw e
			}

			case _ =>
				logger.error(s"Failed to find member ${change.entityId}")
		}
	}

	private def onAchievementCreated()(implicit change: BasicEntityChanged, ziqniContext: ZiqniContext, e: ExecutionContextExecutor): Unit = try {

		ziqniContext.ziqniApiAsync.getAchievement(change.entityId).map {
			case Some(achievement) =>
				try {
					val body = Map[String, Any](
						"achievementId" -> change.entityId,
						"achievementName" -> achievement.getName,
						"achievementDescription" -> achievement.getDescription,
						"status" -> achievement.getStatus,
						"startDate" -> achievement.getStartDate.getMillis,
						"endDate" -> achievement.getEndDate.map(t => t.getMillis),
						"memberGroups" -> achievement.getGroups,
						"resourcePath" -> s"/api/${ziqniContext.spaceName}/achievement/${change.entityId}",
						"objectType" -> "AchievementCreated"
					)
					defaultPushBody(body, "onAchievementCreated")
				} catch {
					case e: Throwable =>
						logger.error(s"Failed to handle achievement ${change.entityId}", e)
						throw e
				}

			case _ =>
				logger.error(s"Failed to find achievement ${change.entityId}")
		}

	} catch {
		case e: Exception =>
			logger.error("Failed to push achievement created", e)
			throw e
	}

	private def onRewardCreated()(implicit change: BasicEntityChanged, ziqniContext: ZiqniContext, e: ExecutionContextExecutor): Unit = {
		val reward = ziqniContext.ziqniApi.getReward(change.entityId)
		val contest = reward.flatMap(r => ziqniContext.ziqniApi.getContest(r.getEntityId))

		val rewardMetaDataProduct = reward.flatMap(_.getMetaData.flatMap(_.get("productRef")))

		val body = Map[String, Any](
			"reward" -> reward.map(buildReward),
			"rewardProductRef" -> rewardMetaDataProduct.orElse(contest.flatMap(_.getProductRefIds.flatMap(_.toSeq.filterNot(_ == "system").headOption))),
			"resourcePath" -> s"/api/${ziqniContext.spaceName}/reward/${change.entityId}",
			"objectType" -> s"${change.metadata.get("parentType")}RewardCreated"
		)
		defaultPushBody(body, s"on${change.metadata.get("parentType")}RewardCreated",
			handleRewardCreated(reward, _, ziqniContext),
			onRewardCreationError(reward, _)
		)
	}

	private def onAwardIssued()(implicit change: BasicEntityChanged, ziqniContext: ZiqniContext, e: ExecutionContextExecutor): Unit = {

		val award = ziqniContext.ziqniApi.getAward(change.entityId)

		val body = Map[String, Any](
			s"${award.get.getEntityType.toLowerCase()}Id" -> change.entityParentId,
			"memberId" -> award.get.getMemberId,
			"memberRefId" -> ziqniContext.ziqniApi.memberRefIdFromMemberId(award.get.getMemberId),
			"award" -> award.map(buildAward),
			"resourcePath" -> s"/api/${ziqniContext.spaceName}/awards/${change.entityParentId}",
			"objectType" -> s"${award.get.getEntityType}RewardIssued"
		)
		defaultPushBody(body, s"on${award.get.getEntityType}RewardIssued")
		onAwardClaimed()
	}

	private def onAwardClaimed()(implicit change: BasicEntityChanged, ziqniContext: ZiqniContext, e: ExecutionContextExecutor): Unit = {
		val award = ziqniContext.ziqniApi.getAward(change.entityId)

		if (award.get.claimed) {

			val body = Map[String, Any](
				s"${award.get.getEntityType.toLowerCase()}Id" -> change.entityId,
				"memberId" -> award.get.getMemberId,
				"memberRefId" -> ziqniContext.ziqniApi.memberRefIdFromMemberId(award.get.getMemberId),
				"award" -> award.map(buildAward),
				"rewardTypeKey" -> award.get.getRewardTypeKey,
				"resourcePath" -> s"/api/${ziqniContext.spaceName}/awards/?id=${change.entityParentId}",
				"objectType" -> s"${award.get.getEntityType}RewardClaimed"
			)
			defaultPushBody(body, s"on${award.get.getEntityType}RewardClaimed")
		}
	}

	private def buildReward(reward: BasicRewardModel) = {
		Map[String, Any](
			"rewardId" -> reward.getClRewardId,
			"name" -> reward.getName,
			"entityId" -> reward.getEntityId,
			"rewardTypeName" -> reward.getRewardTypeName,
			"rewardTypeKey" -> reward.getRewardTypeKey,
			"rewardTypeId" -> reward.getRewardTypeId,
			"value" -> reward.getValue,
			"description" -> reward.getDescription,
			"metadata" -> reward.getMetaData
		)
	}

	private def buildAward(award: BasicAwardModel) = {
		val metadata = award.getRewardMetaData
		val bonusId = metadata.flatMap(_.get("bonusId").flatMap(b => Try(b.toInt).toOption))
		Map[String, Any](
			"rewardId" -> award.getRewardId,
			"awardId" -> award.getClAwardId,
			"name" -> award.getRewardName,
			"entityId" -> award.getEntityId,
			"rewardTypeKey" -> award.getRewardTypeKey,
			"rewardTypeId" -> award.getRewardTypeId,
			"value" -> award.getRewardValue,
			"metadata" -> metadata,
			"bonusId" -> bonusId
		)
	}

	////////////////////////////////////////////////////////
	///>>         WEB PUBLISHING UTILITIES             <<///
	////////////////////////////////////////////////////////

	private def defaultPushBody(body: Map[String, Any], onEventHeader: String, onSuccess: HttpResponseEntity => Unit = _ => (), onFailure: HttpResponseEntity => Unit = _ => ())(implicit ziqniContext: ZiqniContext): Unit = {
		val timestamp = DateTime.now().getMillis
		val withDefaults = body ++ Map(
			"timestamp" -> timestamp,
			"spaceName" -> ziqniContext.spaceName
		)

		val json = ZiqniContext.toJsonFromMap(withDefaults)
		val headers = ziqniContext.ziqniApiHttp.HTTPDefaultHeader(ziqniContext.accountId, onEventHeader)
		val response = retry(() => ziqniContext.ziqniApiHttp.httpPost(PostToUrl, json, headers))

		if (response.statusCode != 200) {
			logger.error(s"${DateTime.now()}[${ziqniContext.spaceName}] Webhook request with body [$json] to url [$PostToUrl] failed with status code [${response.statusCode}] and message [${response.content}]")
			onFailure.apply(response)
		}
		else {
			onSuccess(response)
			logger.info(s"${DateTime.now()}[${ziqniContext.spaceName}] Webhook request to url [$PostToUrl] succeeded with status code [${response.statusCode}] and message [${response.content}]")
		}

	}

	private def retry(call: () => HttpResponseEntity, delay: Int = 5000): HttpResponseEntity = {
		var response: Option[HttpResponseEntity] = None
		for (i <- 1 to 3) {
			response = Try(call.apply()).toOption
			if (response.exists(_.statusCode == 200)) {
				return response.get
			}
			Thread.sleep(delay)
		}
		response.get
	}

	private def onRewardCreationError(reward: Option[BasicRewardModel], response: HttpResponseEntity) = {
		reward.foreach(_.setMetaData(Map("error" -> response.content)))
	}

	private def handleRewardCreated(reward: Option[BasicRewardModel], response: HttpResponseEntity, ziqniContext: ZiqniContext) = {
		try {
			val bonusIdOpt = (ZiqniContext.fromJsonString(response.content) \ "bonusId").extractOpt[Int]
			bonusIdOpt.foreach(bonusId => {
				reward.foreach(_.setMetaData(Map("bonusId" -> bonusId.toString)))
			})

			val errorOpt = (ZiqniContext.fromJsonString(response.content) \ "error").extractOpt[String]
			errorOpt.foreach(error => {
				reward.foreach(_.setMetaData(Map("error" -> error)))
			})
		} catch {
			case e: Throwable =>
				logger.error(s"failed to extract bonusId from response ${response.content}", e)
				reward.foreach(_.setMetaData(Map("error" -> e.getMessage)))
				throw e
		}
	}
}