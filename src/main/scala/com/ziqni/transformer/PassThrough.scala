package com.ziqni.transformer

import com.typesafe.scalalogging.LazyLogging
import com.ziqni.transformers.{ZiqniContext, ZiqniMqTransformer}
import com.ziqni.transformers.domain.{CustomFieldEntryImplicits, ZiqniEvent}
import org.json4s.{JObject, JString, JValue}
import org.json4s.jackson.parseJson
import com.ziqni.transformers.domain._
import com.ziqni.transformers._
import org.joda.time.DateTime
import org.json4s.JsonAST.{JDouble, JInt}

class PassThrough extends ZiqniMqTransformer with LazyLogging with CustomFieldEntryImplicits {

  override def apply(message: Array[Byte], ziqniContext: ZiqniContext, args: Map[String, Any]): Unit = throw new RuntimeException()

  override def rabbit(message: Array[Byte], routingKey: String, exchangeName: String, ziqniContext: ZiqniContext): Unit = {

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

    ziqniContext.ziqniApiAsync.pushEvent(event)
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
