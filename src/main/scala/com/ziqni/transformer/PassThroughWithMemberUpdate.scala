package com.ziqni.transformer

import com.typesafe.scalalogging.LazyLogging
import com.ziqni.transformers.domain._
import com.ziqni.transformers._
import org.joda.time.DateTime
import org.json4s.JsonAST.{JDouble, JInt}
import org.json4s.jackson.parseJson
import org.json4s.{JObject, JString, JValue}

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 *In this example we assume that a custom field value is being sent as [customFields: {operator=operatorId1}]
 * and we want to create/update the member to reflect this.
 */
class PassThroughWithMemberUpdate extends ZiqniMqTransformer with LazyLogging with CustomFieldEntryImplicits {

  override def apply(message: Array[Byte], ziqniContext: ZiqniContext, args: Map[String, Any]): Unit = throw new RuntimeException()

  override def rabbit(message: Array[Byte], routingKey: String, exchangeName: String, ziqniContext: ZiqniContext): Unit = {
    implicit val executionContextExecutor: ExecutionContextExecutor = ziqniContext.ziqniExecutionContext

    val messageAsString = ZiqniContext.convertByteArrayToString(message)
    val jValue: JValue = parseJson(messageAsString)

    val memberId = Json.getFromJValueAsOption[String](jValue, "memberId")
    val memberRefId = Json.getFromJValue[String](jValue, "memberRefId")
    val action = Json.getFromJValue[String](jValue, "action")
    val batchId = Json.getFromJValueAsOption[String](jValue, "batchId")
    val eventRefId = Json.getFromJValue[String](jValue, "eventRefId")
    val entityRefId = Json.getFromJValue[String](jValue, "entityRefId")
    val sourceValue = Json.getFromJValue[Double](jValue, "sourceValue")
    val transactionTimestamp = Json.getFromJValue[DateTime](jValue, "transactionTimestamp")
    val tags = Json.getFromJValueAsOption[Seq[String]](jValue, "tags").getOrElse(Seq.empty)
    val unitOfMeasure = Json.getFromJValueAsOption[String](jValue, "unitOfMeasure")
    val customFields = Json.getFromJValueAsOption[Map[String,JValue]](jValue, "customFields")
    //val entityId = Json.getFromJValueAsOption[String](jValue, "entityId") <<< Not Supported
    //val points = Json.getFromJValue[Double](jValue, "points") <<< Not Supported
    //val metadata = Json.getFromJValueAsOption[Map[String,String]](jValue, "metadata") <<< Not Supported

    val event = ZiqniEvent(
      memberId = memberId,
      memberRefId = memberRefId,
      entityRefId = entityRefId,
      eventRefId = eventRefId,
      batchId = batchId,
      action = action,
      tags = tags,
      sourceValue = sourceValue,
      unitOfMeasure = unitOfMeasure,
      transactionTimestamp = transactionTimestamp,
      customFields = customFields.map(parseCustomFields).getOrElse(Map.empty)
    )


    val out = for {

      member <- ziqniContext.ziqniApiAsync.getOrCreateMember(
        referenceId = memberRefId,
        createAs = () => {
          val operator = event.customFields.get("operator").map(x=>Seq(s"${x.value}")).getOrElse(Seq.empty)
          CreateMemberRequest(memberRefId, memberRefId, operator, customFields = Map.empty, metadata = Map.empty)
        }
      )
      _ <- {
        val operator = event.customFields.get("operator").map(x=>Seq(s"${x.value}")).getOrElse(Seq.empty)

        if (operator.nonEmpty && member.getTags.exists(x => x.contains(operator.head))) {
          val tags = member.getTags.map(x => x ++ operator )
          val newCustomFields: Map[String, CustomFieldEntry[_]] = member.getCustomFields
          val done = ziqniContext.ziqniApiAsync.updateMember(member.getMemberId, Option(memberRefId), member.getDisplayName, tags, customFields = Option(newCustomFields))
          done.onComplete {
              case scala.util.Success(_) =>
              case scala.util.Failure(exception) => ziqniContext.ziqniSystemLogWriter("Failed to update member and push event", exception, LogLevel.ERROR)
            }
          done
        }
        else Future.successful()
      }
    } yield {
      ziqniContext.ziqniApiAsync.pushEvent(event)
    }

    out.onComplete {
      case scala.util.Success(_) =>
      case scala.util.Failure(exception) => ziqniContext.ziqniSystemLogWriter("Failed to push event", exception, LogLevel.ERROR)
    }
  }

  private def parseCustomFields(in:Map[String,JValue]): Map[String, CustomFieldEntry[_<:Any]] = in.map( v => (v._1, asCustomFieldEntry(v._2)))

  private def asCustomFieldEntry(in:JValue): CustomFieldEntry[_<:Any] =
    in match {
      case jString: JString =>
        toText(jString.values)

      case jInt: JInt =>
        toNumInt(jInt.values.toInt)

      case jDouble: JDouble =>
        toNumDouble(jDouble.values)

      case jObject: JObject =>
        toText(jObject.values.toString())

      case _ =>
        toText(in.values.toString)
    }
}
