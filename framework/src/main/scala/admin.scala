package bhoot

import javax.servlet.http._

import Utils._
import UtilsServlet._

object Admin {

  val errorStr = "{success:false}"
  
  def postPurge (request:Request, response:HttpServletResponse):String = {
    val keyIn = request.getParamOpt("key").getOrElse("")
    println(keyIn)
    if (keyIn == "pluckAll") {
      AdminWorker ! AdminWorker.PurgeBadCreds
      "{success:true}"
    } else {
      errorStr
    }
  }
}

import scala.actors._
object AdminWorker extends Actor {
  case object PurgeBadCreds

  start

  import dispatch.{Http, StatusCode}
  import dispatch.twitter.Twitter
  import dispatch.oauth.OAuth._
  import dispatch.json.JsHttp._

  val http = new Http

  import Actor._
  def act() = {
    loop {
      react {
        case PurgeBadCreds => {
          import dispatch.{Request}
          var allPurged:List[Int]=Nil

          val dbKeys = dbHelper.getAllKeys
          dbKeys foreach {key =>
            // check if valid
            val request = (new Request(Twitter.host / "1/account/verify_credentials.json")) <@
              (Common.consumer, dispatch.oauth.Token(key._2,key._3)) 

            var done = false
            var retryCount = 0

            while (!done) {
              if (retryCount > 0) {
                println("--------- Retrying ------- [%d]" format retryCount)
              }
              retryCount += 1
              try {
                http(request ># (obj)) // >> {response => println("Got back " + response)})
                done = true
              } catch {
                case StatusCode(401,contents) =>
                  if (retryCount > 4) {
                    dbHelper.removeKey(key._1,key._2, key._3)
                    allPurged ::= key._1
                    done = true
                  }
                case _ =>
                  // ignore other errors for now
                  // Note that verify-credentials is now rate limited and can cause 400 errors.
                  if (retryCount > 4) {
                    done = true
                  }
              }
            }
          }
          Notifier ! Notifier.Purged(allPurged)

        }

      } // react
    } // loop
  } // act
}
