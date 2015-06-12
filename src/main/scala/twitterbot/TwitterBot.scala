package twitterbot

import com.twitter.finagle.{Httpx, Service}
import com.twitter.util.{Await, Future, Promise}
import io.finch._
import io.finch.response._
import io.finch.route._
import rx.lang.scala.Observable
import twitter4j.Status

import scala.util.Properties
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}

object TwitterBot {

  val db = new DataQueries()
  val str = TwitterStatusStream()

  var last: String = "No tweets"
  val tags = Array("seagames", "fifa16")

  val result = tags.map(hashTagStream(_, str)).toList match {
    case obs1 :: obs2 :: tail => obs1.combineLatest(obs2)
  }

  result.subscribe { item =>
    println(item)
    last = item.toString
  }

  val endpoint =
    (Get / "api" / "status" /> currentStatus) |
    (Get / "api" / "test" /> test) |
    (Get / "api" / "tweets" /> tweets) |
    (Get /> hello)

  def tweets() = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest) = {
      val promise = Promise[List[Tweet]]()

      db.latestTweets(10).toList.subscribe(
        tweets => promise.setValue(tweets),
        ex => promise.setException(ex)
      )

      implicit val formats = Serialization.formats(NoTypeHints)

      promise
        .map(write(_))
        .map { tweets => Ok.withContentType(Some("application/json"))(tweets) }
    }
  }

  def hello() = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest) = {
      Future(Ok("This is it"))
    }
  }

  def currentStatus() = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest) = {
      Future(Ok(last))
    }
  }

  def test() = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest) = {
      Future(Ok("Test"))
    }
  }

  def main(args: Array[String]) {
    str.sample(tags)

    val port = Properties.envOrElse("PORT", "8080")
    val _ = Await.ready(Httpx.serve(s":$port", endpoint.toService))
  }

  def hashTagStream(tag: String, twitterStream: TwitterStatusStream) = {
    twitterStream.stream
      .map(HashTag.fromStatus(tag))
      .flatMap(Observable.from(_))
      .map(db.storeTweet)
      .groupBy(_.tag)
      .flatMap(_._2.zipWithIndex)
      .map { case tuple: (HashTag, Int) => tuple._1.withCount(tuple._2 + 1) }
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
}
