package it.teamDigitale.kafkaProducers.eventConverters

import java.nio.ByteBuffer
import java.text.SimpleDateFormat

import it.teamDigitale.avro.{ DataPoint, Event, AvroConverter }

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.xml.{ NodeSeq, XML }

/**
 * Created with <3 by Team Digitale.
 */
class TorinoTrafficConverter extends EventConverter {

  import TorinoTrafficConverter._

  def convert(timeMap: Map[String, Long] = Map()): (Map[String, Long], Option[Seq[Array[Byte]]]) = {
    val time = timeMap.getOrElse(url, -1L)
    val xml = XML.load(url)
    val traffic_data: NodeSeq = xml \\ "traffic_data"
    val ftd_data = traffic_data \\ "FDT_data"
    val generationTimeString = (traffic_data \\ "@generation_time").text
    val generationTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(generationTimeString).getTime

    if (generationTimestamp > time) {
      val tags = for {
        tag <- ftd_data
      } yield convertDataPoint(tag, generationTimestamp)

      val avro = tags.map(x => AvroConverter.convertDataPoint(x))
      //avro.foreach(println(_))
      val newTimeMap = Map(url -> generationTimestamp)
      (newTimeMap, Some(avro))
    } else {
      (timeMap, None)
    }

  }
  @Deprecated
  private def convertEvent(ftd_data: NodeSeq, generationTimestamp: Long): Event = {

    val lcd1 = (ftd_data \ "@lcd1").text
    val road_LCD = (ftd_data \ "@Road_LCD").text
    val road_name = (ftd_data \ "@Road_name").text
    val offset = (ftd_data \ "@offset").text
    val lat = (ftd_data \ "@lat").text
    val lon = (ftd_data \ "@lng").text
    val latLon = s"$lat-$lon"
    val direction = (ftd_data \ "@direction").text
    val accuracy = (ftd_data \ "@accuracy").text
    val period = (ftd_data \ "@period").text
    val flow = (ftd_data \\ "speedflow" \ "@flow").text
    val speed = (ftd_data \\ "speedflow" \ "@speed").text

    val attributes: Map[String, String] = Map(
      att_lcd1 -> lcd1,
      att_road_LCD -> road_LCD,
      att_road_name -> road_name,
      att_offset -> offset,
      att_direction -> direction,
      att_accuracy -> accuracy,
      att_period -> period,
      att_flow -> flow,
      att_speed -> speed
    )
    new Event(
      version = 0L,
      id = Some("TorinoFDT"),
      ts = generationTimestamp,
      event_type_id = 1,
      source = Some(url.hashCode.toString),
      location = latLon,
      host = host,
      service = url,
      body = Some(ftd_data.toString().getBytes()),
      attributes = attributes
    )
  }

  private def convertDataPoint(ftd_data: NodeSeq, generationTimestamp: Long): DataPoint = {

    val lcd1 = (ftd_data \ "@lcd1").text
    val road_LCD = (ftd_data \ "@Road_LCD").text
    val road_name = (ftd_data \ "@Road_name").text
    val offset = (ftd_data \ "@offset").text
    val lat = (ftd_data \ "@lat").text
    val lon = (ftd_data \ "@lng").text
    val latLon = s"$lat-$lon"
    val direction = (ftd_data \ "@direction").text
    val accuracy = (ftd_data \ "@accuracy").text
    val period = (ftd_data \ "@period").text
    val flow = (ftd_data \\ "speedflow" \ "@flow").text.toDouble
    val speed = (ftd_data \\ "speedflow" \ "@speed").text.toDouble

    val tags: Map[String, String] = Map(
      att_lcd1 -> lcd1,
      att_road_LCD -> road_LCD,
      att_road_name -> road_name,
      att_offset -> offset,
      att_direction -> direction,
      att_accuracy -> accuracy,
      att_period -> period
    )

    val values = Map(
      att_flow -> flow,
      att_speed -> speed
    )

    new DataPoint(
      version = 0L,
      id = Some(measure), //here we should put something as "traffic"
      ts = generationTimestamp,
      event_type_id = measure.hashCode,
      location = latLon,
      host = host,
      service = url,
      body = Some(ftd_data.toString().getBytes()),
      tags = tags,
      values = values
    )
  }

}

object TorinoTrafficConverter {

  val att_lcd1 = "FDT_data"
  val att_road_LCD = "Road_LCD"
  val att_road_name = "Road_name"
  val att_offset = "offset"
  val att_direction = "direction"
  val att_accuracy = "accuracy"
  val att_period = "period"
  val att_flow = "flow"
  val att_speed = "speed"
  val measure = "opendata.torino.get_fdt"
  val url = "http://opendata.5t.torino.it/get_fdt"
  val host = "http://opendata.5t.torino.it"

}

