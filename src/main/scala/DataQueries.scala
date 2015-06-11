import rx.lang.scala.{Subscription, Observable}
import slick.lifted.ProvenShape

import scala.concurrent.{Future, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import slick.backend.DatabasePublisher
import slick.driver.H2Driver.api._

import scala.concurrent.Future
import scala.util.Success


class DataQueries {

  val db = Database.forConfig("h2mem1")
  val tweets: TableQuery[Tweets] = TableQuery[Tweets]

  val setupAction: DBIO[Unit] = DBIO.seq()
  val setupFuture: Future[Unit] = db.run(setupAction)

  def latestTweets(n : Int) : Observable[Tweet] = {

    Observable.create[Tweet] { observer =>
      val future = db.stream(tweets.sortBy(_.timestamp).take(n).result)
        .mapResult(Tweet.tupled).foreach(tweet => observer.onNext(tweet))

      future.onSuccess({ case _ => observer.onCompleted()})
      future.onFailure({ case ex => observer.onError(ex)})

      Subscription()
    }
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