package bhoot

import java.io.{BufferedReader, InputStreamReader}
import java.net.{URL}

case class User(
  id:Int, screenName:String, followerCount:Int, friendCount:Int,
  statusesCount:Int, createdAt:java.util.Date, lastUpdate:java.util.Date)

case class TaskCacher(f: () => List[Int]) {
  var cache : List[Int] = Nil

  def getNextTask = {
    if (cache.isEmpty) {
      // try to fill the cache
      cache = f()
      println("Cache of tasks" + cache)
    }

    val task = cache.firstOption
    if (task.isDefined) {
      cache = cache.tail
    }
    task
  }
}

object Common {
  val minRemainingHits = 120

  var requests = 0
  var remainingHits = 0
  var lastCheck = System.currentTimeMillis
  // val consumer = Consumer("lrhF8SXnl5q3gFOmzku4Gw", "PbB4Mr8pKAChWmd6AocY6gLmAKzPKaszYnXyIDQhzE")
  val consumer = dispatch.oauth.Consumer("br3OrTMzKFaqVro19KqfA", "WWDAGcqdpJeudVTJg6O5vxf9HyEiYOoZmmF2nJlBE")
  val androidConsumer = dispatch.oauth.Consumer("kRkuNSIFpGyyhrAOZUNLXA", "pAVs29CxzvUAAeG3qOgY9yYICAF78mn3LCJfbZePSJk")

  def getRemHits(threshold:Int) = synchronized {
    if ((remainingHits < threshold) && ((System.currentTimeMillis - lastCheck) > (60*1000))) {
      checkBandwidth(true)
      lastCheck = System.currentTimeMillis
    }
    remainingHits >= threshold
  }

  // def checkBandwidth = {requests+=1; if (requests%10 == 0) println("------>  Warning:Enable check bw!\n")}

  def checkBandwidth:Unit = checkBandwidth(false)

  def checkBandwidth(forced:Boolean):Unit = {
    synchronized {
      if (forced || ((requests % 50) == 0)) {
        do {
          // check status
          val url = new URL("http://twitter.com/account/rate_limit_status.json")
          val reader = urlReader(url)
          if (reader != null) {

            val status = JSON.parse(reader).get.asInstanceOf[Map[String,Any]]
            remainingHits = status("remaining_hits").toString.toInt
            println("remaining_hits = " + remainingHits)
            if ((!forced) && (remainingHits < minRemainingHits)) {
              println("Sleeping for 5 mins")
              Thread.sleep(10 * 60 * 1000)
            }
          } else {
              println("Got Error. Seelping for 1 min")
              Thread.sleep(60 * 1000)
          }

        } while ((remainingHits < (minRemainingHits * 0.8).toInt) && !forced)
      }
      requests += 1
    }
  }

  def urlReader(url:URL) = {
    var done = false
    var reader:java.io.BufferedReader = null
    var countTries = 0

    while (!done) {
      try {
        reader = new BufferedReader(new InputStreamReader(url.openConnection.getInputStream))
        done = true
      } catch {
        case e:java.io.FileNotFoundException => println(e);done=true
        case e:java.io.IOException => {
          if (countTries > 2) {
            done = true
          } else {
            println(e);Thread.sleep(100)
          }
        }
      }
      countTries += 1
    }

    reader
  }

  private val dateFormatter = new java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy")

  def lookupUsers(userIds : Seq[Int], oauthToken : dispatch.oauth.Token) = {
    import dispatch.{Http, Request}
    import dispatch.twitter.Twitter
    import dispatch.oauth.OAuth._
    val http = new Http
    val params = Map("user_id" -> userIds.mkString(","))
    val request = (new Request("http://api.twitter.com/1/users/lookup.json")) <<? (params) <@ (Common.consumer, oauthToken)

    var users = List[User]()

    try {
      http(request >> {is => 

        val reader = new BufferedReader(new InputStreamReader(is))

        if (reader != null) {

          val detailsAll = JSON.parse(reader).get.asInstanceOf[List[Map[String,Any]]]

          users = detailsAll.map{details =>
            val createdDate = dateFormatter.parse(details("created_at").toString)
            
            User(
              details("id").toString.toInt,
              details("screen_name").toString,
              details("followers_count").toString.toInt,
              details("friends_count").toString.toInt,
              details("statuses_count").toString.toInt,
              createdDate,
              details.get("status").map{case x:Map[String,Any] => dateFormatter.parse(x("created_at").toString)}.getOrElse(createdDate)
            )
          }
        }
      })
    } catch {
      case e => println(e)
    }

    users
  }

  def getUserDetails(userId:Either[Int,String]) = {
    var details:Option[User] = None
    var retryCount = 0

    while ((!details.isDefined) && (retryCount < 3)) {
      details = try {
        checkBandwidth

        println("fetching details for " + userId)

        val url = new URL("http://twitter.com/users/show.json?%s" format (
          userId.fold(id => "user_id="+id, screenName => "screen_name="+screenName)
        ))

        val reader = urlReader(url)

        if (reader != null) {

          val details = JSON.parse(reader).get.asInstanceOf[Map[String,Any]]

          val createdDate = dateFormatter.parse(details("created_at").toString)
          
          Some(User(
            details("id").toString.toInt,
            details("screen_name").toString,
            details("followers_count").toString.toInt,
            details("friends_count").toString.toInt,
            details("statuses_count").toString.toInt,
            createdDate,
            details.get("status").map{case x:Map[String,Any] => dateFormatter.parse(x("created_at").toString)}.getOrElse(createdDate)
          ))
        } else None
      } catch {
        case e:java.lang.RuntimeException => println("rcvd exception in getUserDetails: " + e);None
      }
      retryCount +=1
    }

    details
  }

}

