import com.twitter.finagle.{Httpx, Service}
import com.twitter.util.{Await, Future}
import io.finch._
import io.finch.response._
import io.finch.route._
import rx.lang.scala.Observable
import twitter4j.Status
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.Properties

object TwitterBot {

  var last: String = ""

  val str = TwitterStatusStream()

  val tags = Array("nationalicedteaday", "InThe90sIThought")

  import scala.concurrent.duration._
  val result = tags.map(hashTagStream(_, str)).toList match {
    case obs1 :: obs2 :: tail => obs1.combineLatest(obs2)
  }
  result.subscribe { item =>
    println(item)
    last = item.toString
  }

  val endpoint = Get /> hello

  def hello() = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest) = {
      println("start apply")
      Future(Ok(last))
    }
  }

  case class HashTag(tag: String, tweet: String, count: Int) {
    def withCount(c: Int) = {
      this.copy(count = c)
    }

    override def toString = s"$tag : $count"
  }

  object HashTag {
    def fromStatus(tag: String)(status: Status) = {
      status
        .getHashtagEntities
        .map(_.getText.toLowerCase)
        .filter(_ == tag.toLowerCase)
        .map(HashTag(_, status.getText, 0))
    }
  }

  def main(args: Array[String]) {
    str.sample(tags)

    val port = Properties.envOrElse("PORT", "8080")
    val _ = Await.ready(Httpx.serve(s":$port", endpoint.toService))
  }

  def hashTagStream(tag: String, stream: TwitterStatusStream) = {
    stream.stream
      .map(HashTag.fromStatus(tag))
      .flatMap(Observable.from(_))
      .groupBy(_.tag)
      .flatMap(_._2.zipWithIndex)
      .map { case tuple: (HashTag, Int) => tuple._1.withCount(tuple._2 + 1) }
  }
}
