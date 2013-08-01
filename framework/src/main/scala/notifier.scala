package bhoot

import Utils._

import javax.mail._
import javax.mail.internet._

import java.util.Properties

import scala.actors._

object Delayer {
  import java.util.concurrent._
  lazy val scheduler = Executors.newScheduledThreadPool(1);

  def execute (f : => Unit, delaySeconds:Int) = {
    val runnable = new Runnable { def run = f }
    scheduler.schedule (runnable, delaySeconds, TimeUnit.SECONDS)
  }

}

object Notifier extends Actor {
  case class LoginSuccess (screenName:String)
  case class UploadSuccess (uploadId:String, thumbStr:String)
  case class FoundFollowers (userId:Int, count:Int)
  case class VerifiedCredentials(userId:Int)
  case class Purged(userId:List[Int])
  case class NewComment(uploadId:String, thumbStr:String, comment:String)
  case class MultipleLogin(newLoginToken:String, newLoginSecret:String, tokens:List[(Int,String,String)])

  private val SMTP_HOST_NAME = "localhost"
  private val SMTP_HOST_PORT = 25
  private val SMTP_AUTH_USER = "tdash@hrj.xen.prgmr.com"
  private val SMTP_AUTH_PWD  = "xyz"
  private val SMTP_FROM_ADDR = "tdash notifier"

  private val maxRetries = 5

  private lazy val fromAddress = Array[Address](new InternetAddress(SMTP_AUTH_USER, SMTP_FROM_ADDR))

  val props = new Properties();

  props.put("mail.transport.protocol", "smtp")
  props.put("mail.host", SMTP_HOST_NAME)
  props.put("mail.smtps.auth", "true")
  // props.put("mail.smtps.quitwait", "false")

  val mailSession = Session.getDefaultInstance(props)
  // mailSession.setDebug(true)

  val transport = mailSession.getTransport()

  start

  private def sendMessage(emails:List[String], subject:String, content:String, mimeType:Option[String]):Unit = sendMessage(emails,subject, content, mimeType, 0)

  private def sendMessage(emails:List[String], subject:String, content:String, mimeType:Option[String], retryCount:Int):Unit = {
    try {
      val message = new MimeMessage(mailSession)
      message.addFrom (fromAddress)
      message.setSubject(subject)
      message.setContent(content, mimeType.getOrElse("text/plain"))

      emails foreach {email =>
        message.addRecipient(Message.RecipientType.TO,
             new InternetAddress(email));
      }

      println("Sending mail: «" + subject + "» to " + emails)

      transport.connect (SMTP_HOST_NAME, SMTP_HOST_PORT, SMTP_AUTH_USER, SMTP_AUTH_PWD)

      transport.sendMessage(message, message.getAllRecipients)

      transport.close
    } catch {
      case x:javax.mail.SendFailedException => {
        transport.close
        println
        println("sending failed exception (%d trial):" format (retryCount))
        println(x) 
        val invalidEmails = x.getInvalidAddresses.toList.map(_.toString)
        println("Invalid email addresses:" + invalidEmails)
        val validUnsentEmails = x.getValidUnsentAddresses.toList.map(_.toString)

        if ((retryCount < maxRetries) && (!validUnsentEmails.isEmpty)) {
          val delay = (retryCount * 5) + (System.currentTimeMillis % 7).toInt
          println("retrying for " + validUnsentEmails + "after %d seconds" format (delay))
          Delayer.execute( { sendMessage (validUnsentEmails, subject, content, mimeType, retryCount + 1) }, delay)
        } else {
          println("giving up");
        }
      }
      case x:javax.mail.MessagingException => {
        transport.close
        println
        println("messaging exception (%d trial):" format (retryCount))
        println(x) 
        if (retryCount < maxRetries) {
          val delay = (retryCount * 5) + (System.currentTimeMillis % 7).toInt
          println("retrying after %d seconds" format (delay))
          Delayer.execute( { sendMessage (emails, subject, content, mimeType, retryCount + 1) }, delay)
        } else {
          println("giving up");
        }
      }
      case x => {
        transport.close
        println
        println("unknown exception: " + x)
      }
    }
  }

  private val adminList = List("harshad.rj@gmail.com")

  private lazy val domainName = "[" + UtilsServlet.initParms.get("domainName").getOrElse("local domain") + "]"

  import Actor._
  def act() = {
    loop {
      react {
        case LoginSuccess(screenName:String) => {
          sendMessage(adminList, screenName, (new java.util.Date()).toString + "\n\n" + "http://twitter.com/"+screenName, None)
        }
        case MultipleLogin(newLoginStr, newLoginSecret, tokens) => {
          val screenNames = tokens.map(token=>dbHelper.getScreenNameFromToken(token._2, token._3).getOrElse("____UNKNOWN____"))
          val newName = dbHelper.getScreenNameFromToken(newLoginStr, newLoginSecret).getOrElse("____UNKNOWN____")

          val urls = (newName :: screenNames).map("http://twitter.com/"+_).mkString("\n")
          
          sendMessage(adminList, "multi: " + newName, (new java.util.Date()).toString + "\n\n" + urls, None)
        }
        case Purged(userIds) => {
          val screenNames = userIds.map(dbHelper.getScreenNameFromUserId(_).getOrElse("____UNKNOWN____"))
          val screenNamesStr = screenNames.reduceLeftOpt(_+"\n"+_).getOrElse("none purged")

          sendMessage(adminList,
            "purged " + screenNames.length,
            (new java.util.Date()).toString + "\n\n" + screenNamesStr, None)
        }
        case VerifiedCredentials(userId) => {
          val screenName = dbHelper.getScreenNameFromUserId(userId)
          sendMessage(adminList,
            "credentials for " + screenName.getOrElse(userId),
            (new java.util.Date()).toString + "\n\n" + "http://twitter.com/"+screenName.getOrElse("Not_Found"), None)
        }
        case FoundFollowers(userId, count) => {
          val screenName = dbHelper.getScreenNameFromUserId(userId)
          sendMessage(adminList,
            "followers for " + screenName.getOrElse(userId) + " ("+count+")",
            (new java.util.Date()).toString + "\n\n" + "http://twitter.com/" + screenName.getOrElse("Not_Found"), None)
        }
        case UploadSuccess(uploadId:String, thumbStr:String) => {
          sendMessage(adminList, "image %s uploaded" format (uploadId),
            """
              <p><img src="http://tdash.org/%s"/></p>
              <p><a href="http://tdash.org/x%s">URL View</a></p>
            """ format (thumbStr, uploadId),
            Some("text/html"))
        }
        case NewComment(uploadId, thumbStr, comment) => {
          sendMessage(adminList, "comment on image %s" format (uploadId),
            """
              <p>%s</p>
              <p><img src="http://tdash.org/%s"/></p>
              <p><a href="http://tdash.org/x%s">URL View</a></p>
            """ format (comment, thumbStr, uploadId),
            Some("text/html"))
        }
      }
    }
  }
}
