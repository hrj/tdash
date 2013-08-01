package bhoot

import scala.actors._

object Worker extends Actor {
  case class FindFollowers(userId:Int, oauthToken:dispatch.oauth.Token)

  start

  val LongTimeInMs = (3*24*60*60*1000L)

  import Actor._
  def act() = {
    loop {
      react {
        case FindFollowers(userId, oauthToken) => {
          // check wheter data is too old
          val lastUpdated = dbHelper.getFollowerUpdated(userId).map(_.getTime).getOrElse(0L)
          val currTime = System.currentTimeMillis

          if ((currTime - lastUpdated) > LongTimeInMs) {
            // it's older than 3 days
            println ("Finding followers for " + userId)

            // get the new data
            val followerIds = TwitterHelper.getContactIds(userId, true, oauthToken)
            if (followerIds.isDefined) {

              // delete the old
              dbHelper.clearFollowers(userId)

              var needNames = List[Int]()

              followerIds.get.foreach {id =>
                // insert the new
                dbHelper.insertFollower(userId, id)
                
                // make sure screen name is available
                if ( ! dbHelper.getScreenNameFromUserId(id).isDefined) {
                  needNames ::= id
                }
              }

              import Pic.provideGroup

              var namesResolved = 0

              if (needNames.length > 0) {
                val needNamesGrouped = needNames.group(100)

                needNamesGrouped foreach {ids =>
                  val details = Common.lookupUsers(ids, oauthToken)
                  details foreach { details => dbHelper.insertNewUser(details.id, details.screenName) }
                  namesResolved += details.length
                }
              }


              // update the timestamp
              if(namesResolved == needNames.length) {
                dbHelper.setFollowerUpdated(userId, currTime)
              }

              // notify
              Notifier ! Notifier.FoundFollowers(userId,followerIds.get.length)
            }
          } else {
            Notifier ! Notifier.VerifiedCredentials(userId)
          }
        } // case

      } // react
    } // loop
  } // act
}
