package bhoot

import scala.actors._

object UpdateWorker extends Actor {
  case class UpdateStatus(status:String, oauthToken:String, oauthTokenSecret:String)
  case class UpdateStatusAndroid(status:String, oauthToken:String, oauthTokenSecret:String, verifier:String)

  start

  import Actor._
  def act() = {
    loop {
      react {

        case UpdateStatus(status, oauthToken, oauthTokenSecret) => {
          WebApp.updateStatus(status, oauthToken, oauthTokenSecret)
        } // case

        case UpdateStatusAndroid(status, oauthToken, oauthTokenSecret, verifier) => {
          WebApp.updateStatusAndroid(status, oauthToken, oauthTokenSecret, verifier)
        } // case

      } // react
    } // loop
  } // act
}
