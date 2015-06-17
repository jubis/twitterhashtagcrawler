package twitterbot

import com.twitter.finagle.{Httpx, Service}
import com.twitter.util.{Await, Future, Promise}
import io.finch._
import io.finch.response._
import io.finch.route._
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.write
import rx.exceptions.CompositeException
import rx.lang.scala.Observable
import twitter4j.Status

import scala.collection.JavaConversions._
import scala.util.Properties

object TwitterBot {

  val db = new DataQueries()
  val str = TwitterStatusStream()

  val tags = Array("CelebApps", "labourdebate")

  val fight = tags.map(hashTagStream(_, str)).toList match {
    case obs1 :: obs2 :: _ => obs1.combineLatest(obs2)
  }

  val publishedFight = fight
    .replay(1)
    .refCount


  publishedFight.subscribe(
    println,
    {
      case error: CompositeException => error.getExceptions.map(_.getMessage).foreach(println)
      case error => println(error.toString)
    }
  )

  def nextToFuture[T](obs: Observable[T]): Future[T] = {
    val promise = Promise[T]()
    obs.take(1).subscribe(
      promise.setValue,
      promise.setException
    )
    promise
  }

  val endpoint =
    (Get / "api" / "status" /> currentStatus) |
    (Get / "api" / "test" /> test) |
    (Get / "api" / "tweets" /> tweets) |
    (Get /> hello)

  def tweets() = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest) = {
      implicit val formats = Serialization.formats(NoTypeHints)

      nextToFuture(db.latestTweets(10).toList)
        .map(write(_))
        .map { tweets => Ok.withContentType(Some("application/json"))(tweets) }
        .handle { case error => InternalServerError(s"loading failed, ${error.getMessage}") }
    }
  }

  def hello() = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest) = {
      Future(Ok("This is it"))
    }
  }

  def currentStatus() = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest) = {
      nextToFuture(publishedFight)
        .map(next => Ok(next.toString))
    }
  }

  def test() = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest) = {
      Future(Ok("Test"))
    }
  }

  def main(args: Array[String]) {
    //publishedFight.connect
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
