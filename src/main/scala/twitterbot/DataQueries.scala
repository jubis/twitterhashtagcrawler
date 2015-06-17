package twitterbot

import java.util.Date

import rx.lang.scala.{Observable, Subscription}
import slick.backend.DatabasePublisher
import slick.driver.H2Driver.api._
import slick.lifted.ProvenShape
import twitterbot.TwitterBot.HashTag

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class DataQuery[A](publisher : DatabasePublisher[A]) {

  def toObservable() : Observable[A] = {
    Observable.create[A] { observer =>
      val future = publisher.foreach(item => observer.onNext(item))

      future.onSuccess({ case _ => observer.onCompleted()})
      future.onFailure({ case ex => observer.onError(ex)})

      Subscription()
    }
  }
}


class DataQueries {

  implicit def toDataQuery[A](publisher : DatabasePublisher[A]) : DataQuery[A] = {
    new DataQuery[A](publisher)
  }

  val db = Database.forURL("jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1", driver="org.h2.Driver")
  val tweets: TableQuery[Tweets] = TableQuery[Tweets]

  val setupAction: DBIO[Unit] = DBIO.seq(
    tweets.schema.create
  )

  val setupFuture: Future[Unit] = db.run(setupAction)

  def latestTweets(n : Int) : Observable[Tweet] = {

    db.stream(tweets.sortBy(_.timestamp.desc).take(n).result)
      .mapResult(Tweet.tupled)
      .toObservable()
  }

  def storeTweet(tag : HashTag) : HashTag = {
    db.run(tweets += (tag.tweet, new Date().getTime))
    tag
  }
}

case class Tweet(text : String, timestamp : Long)

class Tweets(tag: Tag)
  extends Table[(String, Long)](tag, "TWEETS") {

  def text: Rep[String] = column[String]("TEXT", O.PrimaryKey)
  def timestamp: Rep[Long] = column[Long]("TIMESTAMP")

  def * : ProvenShape[(String, Long)] =
    (text, timestamp)
}