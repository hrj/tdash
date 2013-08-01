package bhoot

import java.io.{BufferedReader, InputStreamReader}
import java.net.{URL}

case class Contact(id:Int, name:String)
case class ContactList(friends:List[Contact], secondLevel:Map[Int, Contact])

case class FriendError(msg:String)
case class UserDetails(id:Int, gotDetails:Boolean, gotFriends:Boolean, screenName:String, friendCount:Int, hasError:Boolean)

object TwitterHelper {
  def getContactIds(userId:Int, followers:Boolean, oauthToken:dispatch.oauth.Token) = {
    try {
      println((new java.util.Date) + " finding Contacts of " + userId + " (followers:" + followers + ")")

      val contactType = if (followers) "followers" else "friends"

      Common.checkBandwidth

      // val url = new URL("http://twitter.com/%s/ids.json?user_id=%d" format (contactType, userId))

      import dispatch.{Http, Request}
      import dispatch.twitter.Twitter
      import dispatch.oauth.OAuth._
      val http = new Http
      val request = (new Request(Twitter.host / "1/followers/ids.json")) <@ (Common.consumer, oauthToken)
      http(request >> {is => 

        // val reader = Common.urlReader(url)
        val reader = new BufferedReader(new InputStreamReader(is))

        if (reader != null) {
          val input = reader.readLine
          if ((input(0) == '[') && (input(input.length - 1) == ']')) {
            var index = 1                   // ignore the header [
            val length = input.length - 1   // ignore the trailing ]
            var currentIntValue = 0;
            var errorFound = false
            var contacts:List[Int] = Nil

            while ((index < length) && (!errorFound)) {
              val c = input(index)

              if (Character.isDigit(c)) {
                currentIntValue = (currentIntValue*10 + Character.getNumericValue(c))
              } else if (c == ',') {
                contacts ::= currentIntValue
                currentIntValue = 0
              } else {
                println ("error on " + c)
                errorFound = true
              }

              index += 1
            }

            if (errorFound) {
              None
            } else {
              if (currentIntValue != 0) {
                Some(currentIntValue::contacts)
              } else {
                Some(contacts)
              }
            }

          } else {
            None
          }
        } else {
          None
        }
      })
    } catch {
      case e => println(e); None
    }
  }
}
