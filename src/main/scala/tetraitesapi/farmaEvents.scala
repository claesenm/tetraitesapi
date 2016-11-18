package tetraitesapi

import com.typesafe.config.Config
import org.apache.spark.SparkContext
import spark.jobserver._
import tetraitesapi.Model._

import scala.util.Try


/**
  * Query the timeline for one of more lidano's for a certain window of time.
  *
  * Input:
  *
  * - __lidano__: A regular expression for the lidano key (default: `.*`)
  * - __start__: the start date of a window of interest (default: 1/1/1900)
  * - __end__: The end date of a window of interest (default: 1/1/2500)
  * - __window__: Specify a window using regular expressions on the string dates (default: `.*`)
  */
object farmaEvents extends SparkJob with NamedObjectSupport {

  implicit def rddPersister[T] : NamedObjectPersister[NamedRDD[T]] = new RDDPersister[T]
  implicit def broadcastPersister[U] : NamedObjectPersister[NamedBroadcast[U]] = new BroadcastPersister[U]

  override def validate(sc: SparkContext, config: Config): SparkJobValidation = SparkJobValid

  override def runJob(sc: SparkContext, config: Config): Any = {

    val lidanoQuery:String = Try(config.getString("lidano")).getOrElse("no lidano specified")
    val dayQuery:String = Try(config.getString("day")).getOrElse("no day specified")

    // Fetch raw events
    val NamedRDD(gezoDb, _ ,_) = namedObjects.get[NamedRDD[Gezo]]("gezoDb").get
    val NamedRDD(farmaDb, _ ,_) = namedObjects.get[NamedRDD[Farma]]("farmaDb").get

    // Fetch timeline
    val NamedRDD(gezoTimeline, _ ,_) = namedObjects.get[NamedRDD[TimelineGezo]]("gezoTimeline").get
    val NamedRDD(farmaTimeline, _ ,_) = namedObjects.get[NamedRDD[TimelineFarma]]("farmaTimeline").get

    // Convert to proper format for _output_
    val resultAsMap = farmaDb
      .filter(_.lidano == lidanoQuery)
      .filter(_.baDat == dayQuery)
      .collect

    // TODO: Add a feature which translates the CNK code to ATC.

    Map("meta" -> "Timeline for farma") ++
    Map("data" -> resultAsMap)

  }

}