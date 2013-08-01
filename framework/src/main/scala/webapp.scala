package bhoot
import javax.servlet.http._

import Utils._
import UtilsServlet._

case class UserSetting(oldOnTop:Boolean) {
  def toJSON = {
    "{oldOnTop:"+oldOnTop+"}"
  }
}

object WebApp {
  import dispatch.oauth._
  import dispatch.json.JsHttp._
  import dispatch.twitter._
  // import all the methods, including implicit conversions, defined on dispatch.Http
  // import dispatch._
  import dispatch.Http
  import dispatch.Http._

  val getHandlerMap = List[Tuple2[String,Bootup#UnitHandler]] (
    "login" -> renderHtmlDynamic (getLogin) _,
    "logout" -> renderHtmlDynamic (getLogout) _,
    "return" -> renderHtmlDynamic (getReturn) _,
    "credentials.js" -> renderJSDynamic (getCredentials) _,
    "credentials2.js" -> renderJSDynamic (getCredentials2) _,
    "credsall.js" -> renderJSDynamic (getAllCredentials) _,
    "credsall2.js" -> renderJSDynamic (getAllCredentials2) _,
    "viewPower" -> renderHtmlDynamic (Dash.getViewPower) _,
    "getCss" -> renderCSSDynamic (CSS.getCss) _,
    "chooseTheme" -> renderHtmlDynamic (CSS.getChooseTheme) _,

    // pic related
    "uploadStart" -> renderHtmlDynamic (Pic.getUploadStart) _,
    "showThumb" -> pureRedirect (Pic.getShowThumb) _,
    "view/.*" -> renderHtmlDynamic (Pic.getViewPic) _,
    "user/.*" -> renderHtmlDynamic (Pic.getViewUser) _,

    // stats related
    "clients" -> renderHtmlDynamic (Stats.getClients) _,
    "clients/hot" -> renderHtmlDynamic (Stats.getHotClients) _,
    "client/.*" -> renderHtmlDynamic (Stats.getClient) _
  )

  val postHandlerMap = List[Tuple2[String,Bootup#UnitHandler]] (
    "addComment" -> renderHtmlDynamic (Pic.postAddComment) _,
    "purgeBadCreds" -> renderJsonDynamic (Admin.postPurge) _,
    "settingTimeOrder" -> renderJsonDynamic (Dash.postSettingTimeOrder) _,
    "themeSave" -> renderHtmlDynamic (CSS.postThemeSave) _
  )

  val postRawHandlerMap = List[Tuple2[String,Bootup#UnitHandler]] (
    "upload" -> renderHtmlDynamic (Pic.postUpload) _,
    "uploadAndroid" -> renderHtmlDynamic (Pic.postUploadAndroid) _
  )

  val seconds90days = 90*24*60*60

  private object keyMaker {
    private lazy val rnd = new java.util.Random(System.currentTimeMillis)
    def getNew(length:Int) = {
      this synchronized {
        ((1 to length) map {x =>
          val rndNum = rnd.nextInt(26 + 26 + 10)

          if (rndNum < 26) { ('a' + rndNum).toChar }
          else if (rndNum < 52) { ('A' + (rndNum - 26)).toChar }
          else {('0' + (rndNum - 52)).toChar}

        }) mkString
      }
    }
  }

  def processCookies(request:HttpServletRequest) = {
    val rawCookies = request.getCookies

    val rawCookieList = 
      if (rawCookies == null) Nil
      else rawCookies.toList

    var map = Map[String,String]()

    rawCookieList foreach {cookie =>
      val name = cookie.getName
      name match {
        case "oauth_token" => map += ("oauth_token" -> cookie.getValue)
        case "oauth_token_secret" => map += ("oauth_token_secret" -> cookie.getValue)
        case "return" => map += ("return" -> cookie.getValue)
        case _ => ;
      }
    }

    map
  }

  def processLoginCookies(request:HttpServletRequest) = {
    val rawCookies = request.getCookies

    val rawCookieList = 
      if (rawCookies == null) Nil
      else rawCookies.toList

    var map = Map[String,String]()
    var tokens = List[(Int, String, String)]()

    var oldToken : Option[String] = None
    var oldTokenSecret : Option[String] = None

    rawCookieList foreach {cookie =>
      val name = cookie.getName
      if (name == "return") {
        map += ("return" -> cookie.getValue)
      } else if (name == "multipleLoginOk") {
        map += ("multipleLoginOk" -> cookie.getValue)
      } else if (name == "scheme_id") {
        map += ("scheme_id" -> cookie.getValue)
      } else if (name startsWith "mauth") {
        val cookieContents = cookie.getValue.split(",")
        val id = name.drop(5).toInt
        if (id >= 0) {
          // added as a special case because of a little bug. Can be removed after a few months from May 5th 2010
          tokens ::= (id, cookieContents(0), cookieContents(1))
        } else {
          println("Gotcha: mauth negative id: " + cookie.getValue)
        }
      } else if (name == "oauth_token") {
        oldToken = Some(cookie.getValue)
      } else if (name == "oauth_token_secret") {
        oldTokenSecret = Some(cookie.getValue)
      }
    }

    if (oldToken.isDefined && oldTokenSecret.isDefined) {
      tokens ::= (-1, oldToken.get, oldTokenSecret.get)
    }

    (map, tokens)
  }

  lazy val domainName = initParms.get("domainName")

  def setCookie(name:String,value:String,age:Int, response:HttpServletResponse) = {
    val cookie = new Cookie(name, value)
    cookie.setMaxAge(age)
    if (domainName.isDefined) {
      cookie.setDomain(domainName.get)
    }
    cookie.setPath("/")
    response.addCookie(cookie)
  }

  private def deleteCookie(name:String, response:HttpServletResponse) = {
    setCookie(name, "", 0, response)
  }

  private def setLoginCookie(token:(Int,String,String),response:HttpServletResponse) = {
/*
    val lstCookies = List(
      new Cookie("mauth"+token._1, token._2 + "," + token._3)
    )
    lstCookies foreach { cookie =>
      cookie.setMaxAge(seconds90days)
      if (domainName.isDefined) {
        cookie.setDomain(domainName.get)
      }
      cookie.setPath("/")
    }
    lstCookies foreach {cookie => response.addCookie(cookie)}
*/
    if (token._1 >= 0) {
      setCookie("mauth"+token._1, token._2 + "," + token._3, seconds90days, response)
    } else {
      setCookie("oauth_token", token._2, seconds90days, response)
      setCookie("oauth_token_secret", token._3, seconds90days, response)
    }
      
  }

  private def setLoginCookies(access_token:Map[String,String],response:HttpServletResponse) = {
/*
    val lstCookies = List(
      new Cookie("oauth_token", access_token("oauth_token")),
      new Cookie("oauth_token_secret", access_token("oauth_token_secret"))
    )
    lstCookies foreach { cookie =>
      cookie.setMaxAge(seconds90days)
      if (domainName.isDefined) {
        cookie.setDomain(domainName.get)
      }
      cookie.setPath("/")
    }
    lstCookies foreach {cookie => response.addCookie(cookie)}
*/
    setCookie("oauth_token", access_token("oauth_token"), seconds90days, response)
    setCookie("oauth_token_secret", access_token("oauth_token_secret"), seconds90days, response)
  }

  private def unsetLoginCookie(id:Int, response:HttpServletResponse) = {
    if (id >= 0) {
        setCookie("mauth"+id,"-1", 0, response)
      } else {
        setCookie("oauth_token","", 0, response)
        setCookie("oauth_token_secret", "", 0, response)
      }
/*
    val lstCookies = if (id >= 0) {
        List( new Cookie("mauth"+id,"-1"))
      } else {
        List( new Cookie("oauth_token","-1"), new Cookie("oauth_token_secret", ""))
      }

    lstCookies foreach {cookie =>
      cookie.setMaxAge(0) // age = 0 makes it go away
      if (domainName.isDefined) {
        cookie.setDomain(domainName.get)
      }
      cookie.setPath("/")
    }
    lstCookies foreach {cookie => response.addCookie(cookie)}
*/
  }

  private def unsetLoginCookies(response:HttpServletResponse) = {
    val lstCookies = List(
      new Cookie("oauth_token","-1"),
      new Cookie("oauth_token_secret", "")
    )
    lstCookies foreach {cookie =>
      cookie.setMaxAge(0) // age = 0 makes it go away
      if (domainName.isDefined) {
        cookie.setDomain(domainName.get)
      }
      cookie.setPath("/")
    }
    lstCookies foreach {cookie => response.addCookie(cookie)}
  }


  def updateStatus(status:String, oauth_token:String, oauth_token_secret:String) = {
    val http = new Http
    val result = http(Status.update(
                  status,
                  Common.consumer,
                  Token(oauth_token,
                  oauth_token_secret)) ># {js => js})
    println(result)
  }

  def updateStatusAndroid(status:String, oauth_token:String, oauth_token_secret:String, verifier:String) = {
    import dispatch.oauth.OAuth._
    import dispatch.oauth.OAuth._
    def update(status: String, consumer: Consumer, token: Token, verifier:String) =
      "https://api.twitter.com/1/statuses/update.json" << Map("status" -> status) <@ (consumer, token, verifier)

    val http = new Http
    val result = http(update(
                  status,
                  Common.androidConsumer,
                  Token(oauth_token,
                  oauth_token_secret),
                  verifier) ># {js => js})
    println(result)
  }

  def findEmptySlot (tokens:List[(Int,String,String)]) = {
    if (tokens.length > 0) {
      var sorted = tokens.sort(_._1 < _._1)

      val first = sorted.first._1
      if (first == -1) {
        sorted = sorted.tail
      }
      var i = 0;
      while (sorted.length > 0 && sorted.first._1 == i) {
        sorted = sorted.tail
        i += 1
      }

      i
    } else 0
  }


  private def getReturn (request:Request, response:HttpServletResponse):String = {
    response.setHeader("Cache-Control","no-cache")

    // http://tdash.org/return/?oauth_token=YqwSFNT2c9zxpcIT4hQHrkpZKQYoNH91QeyHDF7snwU
    println(request.params)
    val oauth_token = request.getParamOpt("oauth_token")
    val oauth_verifier = request.getParamOpt("oauth_verifier")

    if (oauth_token.isDefined && oauth_verifier.isDefined) {
      // Get Access token
      val svc = Twitter.host / "/oauth"

      def get_access_token(consumer: Consumer, token: Token, verifier:String) =  {
        import OAuth._
        svc.secure.POST / "access_token" <@ (consumer, token, verifier) >% { m =>
          println(m)
          // (Token(m).get, m("user_id"), m("screen_name"))
          m
        }
      }

      val http = new Http
      val access_token = (http(get_access_token(Common.consumer, Token(oauth_token.get,""), oauth_verifier.get)))
      
      // Generate our own key
      // val ourSecret = keyMaker.getNew(10)
      
      if (access_token.isDefinedAt("oauth_token")) {
        val userId = access_token("user_id").toInt
        val oauthToken = access_token("oauth_token")
        val oauthTokenSecret = access_token("oauth_token_secret")

        dbHelper.insertNewOAuth(userId, access_token("screen_name"), oauthToken, oauthTokenSecret, oauth_verifier.get)
        Worker ! Worker.FindFollowers(userId, new Token(oauthToken, oauthTokenSecret))

        Notifier ! Notifier.LoginSuccess(access_token("screen_name"))

        val (cookies,tokensFromCookies) = processLoginCookies(request.req)

        // Set cookies
        deleteCookie("return", response)

        if (cookies.isDefinedAt("multipleLoginOk")) {
          deleteCookie("multipleLoginOk", response)

          // check for similar tokens in previous cookies
          if (!tokensFromCookies.find(token => (token._2 == access_token("oauth_token")) && (token._3 == access_token("oauth_token_secret"))).isDefined) {
            val tokenStr = access_token("oauth_token")
            val tokenSecretStr = access_token("oauth_token_secret")
            setLoginCookie((findEmptySlot(tokensFromCookies), tokenStr, tokenSecretStr), response)
            Notifier ! Notifier.MultipleLogin(tokenStr, tokenSecretStr, tokensFromCookies)
          }
        } else {
          // TODO check whether previous login cookies are present. If so, show a warning message
          setLoginCookies(access_token, response)
        }

        // Redirect to logged-in html
        response.sendRedirect(computeReturnPath(cookies.get("return")))
        ""
      } else {
        """<html><body><h1>Oauth Error</h1><p>Something went wrong. Please visit our <a href="/">main page</a> and try again.</p></body></html>"""
      }

    } else if (request.getParamOpt("denied").isDefined) {
      """<html><body><h1>Access denied</h1><p>You seem to have changed your mind about logging in to tDash.</p><p>Nevermind! Please visit our <a href="/">main page</a> and you are welcome to try again.</p></body></html>"""
    } else {
      """<html><body><h1>Oauth Error</h1><p>Something went wrong. Please visit our <a href="/">main page</a> and try again.</p></body></html>"""
    }
  }

  private def getLogout (request:Request, response:HttpServletResponse):String = {
    response.setHeader("Cache-Control","no-cache")

    val (cookies, tokensFromCookies) = processLoginCookies(request.req)

    if (request.params.get("loginNum").isDefined) {
      request.params("loginNum").foreach {loginNum =>
        tokensFromCookies.find(_._1.toString == loginNum).foreach{token =>
          unsetLoginCookie(token._1, response)
        }
      }
      response.sendRedirect("/")
      ""
    } else {

      if (tokensFromCookies.length > 0) {
        """
        <html>
        <h1>Signing out...</h1>
        You are currently logged into multiple accounts.<p>Please select the accounts that you want to log off from:
        <form>
          %s
        <p><input type="submit" value="Sign out"/></p>
        </form>
        </p>
        """ format (tokensFromCookies.flatMap(x => dbHelper.getScreenNameFromToken(x._2, x._3).map((x._1, _))).map{y => 
          "<input type=\"checkbox\" name=\"loginNum\" value=\"" + y._1 + "\"/>"+y._2+"<br/>"
        }).mkString
      } else {
        unsetLoginCookies(response)
        response.sendRedirect("/")
        ""
      }
    }

  }

  private def computeReturnPath(returnPath:Option[String]) = {
    if (returnPath.isDefined) {
      returnPath.get match {
        case "uploadPic" => "/pic/uploadStart"
        case x if (x.startsWith("viewPic")) => "/x"+x.drop(7).toString
        case _ => "/oauth/viewPower"
      }
    } else {
      "/oauth/viewPower"
    }
  }

  private def getLogin (request:Request, response:HttpServletResponse):String = {
    response.setHeader("Cache-Control","no-cache")
    val (cookies, tokens) = processLoginCookies(request.req)

    if (request.getParamOpt("addNewConfirm").isDefined && (tokens.length > 0)) {
      """
      <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
      <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
        <head>
          <meta http-equiv="Content-Type" content="text/html; charset=utf-8" /> 
          <link href="/css/common.css" type="text/css" rel="stylesheet" media="screen,projection" />
          <link href="/css/index.css" type="text/css" rel="stylesheet" media="screen,projection" />
        </head>
        <body>
   <table id="bodyTbl" cellspacing="0"><tbody>
      <tr>
        <td class="logoTd"><img src="/images/logoColorSmall.png" alt="tDash"/></td>
        <td class="contentTd">
          <h2>Sign-in with another account</h2>
          <p>You are already logged in as</p>
          <ul style="font-size:120%%;list-style:none;padding:0;">%s</ul>
          <p style="padding:6px;background:#dee6f1;margin:3em 0 1em 0;">After you click the following link make sure you login into the new account on Twitter.</p>
          <p style="font-family:MEgalopolisRegular, sans-serif;font-size:200%%;"><a href="/oauth/login?addNew=true">Add another account</a></p>
        </td>
      </tr>
    </table>
        </body>
      </html>
      """ format (tokens.flatMap(x => dbHelper.getScreenNameFromToken(x._2, x._3).map("<li>"+_+"</li>")).mkString)
    } else if (request.getParamOpt("addNew").isDefined) {
      // Fetch a new Oauth Token
      // one single-threaded http access point, please!
      val http = new Http

      // oauth sesame
      // val tok = http(Auth.request_token(Common.consumer))
      val tok = http(Auth.request_token(Common.consumer, "http://tdash.org/oauth/return"))

      // generate the url the user needs to go to, to grant us access
      // val auth_uri = Auth.authorize_url(tok).to_uri
      val auth_uri = (Auth.svc.secure / "authorize" <<? Map("oauth_token"->tok.value)).to_uri

      setCookie("multipleLoginOk", "true", 24*60*60, response)

      response.sendRedirect(auth_uri.toString)
      ""
    } else {

      var validCredCount = 0

      tokens foreach { token =>
        val userIdOpt = dbHelper.getUserIdFromToken(token._2, token._3)
        if (userIdOpt.isDefined) {

          // reset the login cookies so that they don't expire soon
          setLoginCookie(token, response)
          validCredCount += 1
        } else {
          unsetLoginCookie(token._1, response)
        }
      }

      if ((validCredCount == 0)) {
        // Fetch a new Oauth Token
        // one single-threaded http access point, please!
        val http = new Http

        // oauth sesame
        // val tok = http(Auth.request_token(Common.consumer))
        val tok = http(Auth.request_token(Common.consumer, "http://tdash.org/oauth/return"))

        // generate the url the user needs to go to, to grant us access
        // val auth_uri = Auth.authorize_url(tok).to_uri
        val auth_uri = (Auth.svc.secure / "authorize" <<? Map("oauth_token"->tok.value)).to_uri

        request.getParamOpt("return").foreach(setCookie("return",_, 24*60*60 /*1 day*/, response))

        response.sendRedirect(auth_uri.toString)
      } else {

        deleteCookie("return", response)
        val returnPath = request.getParamOpt("return")
        response.sendRedirect(computeReturnPath(returnPath))
      }

      ""
    }
  }

  // val SHA1 = "HmacSHA1";
  // private def bytes(str: String) = str.getBytes(UTF_8)
  // import javax.crypto

  // import org.apache.http.protocol.HTTP.UTF_8
  // import org.apache.commons.codec.binary.Base64.encodeBase64


  private def getCredentials (request:Request, response:HttpServletResponse):String = {
    response.setHeader("Cache-Control","no-cache")
    val cookies = processCookies(request.req)
    if (cookies.isDefinedAt("oauth_token") && cookies.isDefinedAt("oauth_token_secret")) {
      val oauthToken = cookies("oauth_token")
      val oauthTokenSecret = cookies("oauth_token_secret")

      val userIdOpt = dbHelper.getUserIdFromToken(oauthToken, oauthTokenSecret)

      if (userIdOpt.isDefined) {
        val followers = dbHelper.getFollowers(userIdOpt.get)

        val followerStr =
          "var myFollowers = [" +
          followers.map(quote).reduceLeftOpt(_+","+_).getOrElse("") +
          "];"

        Worker ! Worker.FindFollowers(userIdOpt.get, new Token(oauthToken, oauthTokenSecret))

        val oauthStr =
          """var oauthAccessor = {
              token:'%s',
              tokenSecret:'%s',
              consumerKey:'%s',
              consumerSecret:'%s'};
          """ format (
            oauthToken,
            oauthTokenSecret,
            Common.consumer.key,
            Common.consumer.secret
          )

         oauthStr+ followerStr
      } else {
        unsetLoginCookies(response)
        "var oauthAccessor = null; var myFollowers = null;"
      }
    } else {
      "var oauthAccessor = null; var myFollowers = null;"
    }
  }

  abstract class CachedValue[T] {
    var lastUpdate = 0L
    val forSyncOnly = new java.lang.Integer(0)

    var cachedValue:T = _

    def getValue:T

    def get = {
      forSyncOnly synchronized {
        val now = System.currentTimeMillis
        if ((now - lastUpdate) > 120000) {
          println("updating cache")
          cachedValue = getValue
          lastUpdate = now
        }
      }

      cachedValue
    }
  }
  
  object userCount extends CachedValue[Int] {
    def getValue = dbHelper.getUserCount
  }
  
  object imgCount extends CachedValue[Int] {
    def getValue = dbHelper.getImgCount
  }
  

  private def getCredentials2 (request:Request, response:HttpServletResponse):String = {
    response.setHeader("Cache-Control","no-cache")
    val cookies = processCookies(request.req)

    val statStr = """
     var userCount = '%s';
     var imgCount = '%s';
    """ format (userCount.get, imgCount.get)

    val authResponse = 
      if (cookies.isDefinedAt("oauth_token") && cookies.isDefinedAt("oauth_token_secret")) {
        val oauthToken = cookies("oauth_token")
        val oauthTokenSecret = cookies("oauth_token_secret")

        val userIdOpt = dbHelper.getUserIdFromToken(oauthToken, oauthTokenSecret)

        if (userIdOpt.isDefined) {
          Worker ! Worker.FindFollowers(userIdOpt.get, new Token(oauthToken, oauthTokenSecret))

          val oauthStr =
            """var oauthAccessor = {
                token:'%s',
                tokenSecret:'%s',
                consumerKey:'%s',
                consumerSecret:'%s'};
            """ format (
              oauthToken,
              oauthTokenSecret,
              Common.consumer.key,
              Common.consumer.secret
            )

           oauthStr
        } else {
          unsetLoginCookies(response)
          "var oauthAccessor = null; var myFollowers = null; var userCount='%s';" format (userCount.get)
        }
      } else {
        "var oauthAccessor = null; var myFollowers = null; var userCount='%s';" format (userCount.get)
      }

    authResponse + statStr
  }

  val defaultSetting = UserSetting(false)

  def makeCreds (loginTokens:List[(Int,String,String)], response:HttpServletResponse) = {
    var authStr = List[String]()
    var followerStr = List[String]()
    var loginMapStr = List[String]()
    var settingStr = List[String]()


    loginTokens foreach {token =>
      val userIdOpt = dbHelper.getUserIdFromToken(token._2, token._3)

      if (userIdOpt.isDefined) {
        Worker ! Worker.FindFollowers(userIdOpt.get, new Token(token._2, token._3))

        authStr ::= ("""{
            token:'%s',
            tokenSecret:'%s',
            consumerKey:'%s',
            consumerSecret:'%s'}
        """ format (
          token._2,
          token._3,
          Common.consumer.key,
          Common.consumer.secret
        ))

        val followers = dbHelper.getFollowers(userIdOpt.get)
        followerStr ::= (
          "[" +
          followers.map(_.toLowerCase).map(quote).reduceLeftOpt(_+","+_).getOrElse("") +
          "]"
        )

        loginMapStr ::= (token._1.toString)

        val settings = dbHelper.getSettings(userIdOpt.get)
        
        settingStr ::= settings.getOrElse(defaultSetting).toJSON

      } else {
        unsetLoginCookie(token._1, response)
      }
    }

    "var oauthAccessor=[" + authStr.mkString(",") + "]; var followers=["+ followerStr.mkString(",") +
       "]; var loginMap=[" + loginMapStr.mkString(",") +
       "]; var settings=[" + settingStr.mkString(",") + "];"
  }

  private def getAllCredentials (request:Request, response:HttpServletResponse):String = {
    response.setHeader("Cache-Control","no-cache")
    val (cookies, tokens) = processLoginCookies(request.req)

    makeCreds(tokens, response)
  }

  private def getAllCredentials2 (request:Request, response:HttpServletResponse):String = {
    response.setHeader("Cache-Control","no-cache")

    val (cookies, tokens) = processLoginCookies(request.req)

    val statStr = """
     var userCount = '%s';
     var imgCount = '%s';
    """ format (userCount.get, imgCount.get)

    val authResponse = {
      "var oauthAccessor = [" + (
        tokens map {token =>
          println(token)
          val userIdOpt = dbHelper.getUserIdFromToken(token._2, token._3)

          if (userIdOpt.isDefined) {
            Worker ! Worker.FindFollowers(userIdOpt.get, new Token(token._2, token._3))

            """{
                token:'%s',
                tokenSecret:'%s',
                consumerKey:'%s',
                consumerSecret:'%s'}
            """ format (
              token._2,
              token._3,
              Common.consumer.key,
              Common.consumer.secret
            )

          } else {
            unsetLoginCookie(token._1, response)
            ""
          }
        } mkString (",") 
      ) + "];"
    }

    authResponse + statStr
  }

/*
  private def getFollowers (request:Request, response:HttpServletResponse):String = {
    response.setHeader("Cache-Control","no-cache")
    val cookies = processCookies(request.req)
    if (cookies.isDefinedAt("oauth_token") && cookies.isDefinedAt("oauth_token_secret")) {
      val oauthToken = cookies("oauth_token")
      val oauthTokenSecret = cookies("oauth_token_secret")

      val userIdOpt = dbHelper.getUserIdFromToken(oauthToken, oauthTokenSecret)
      ""
    } else {
      "var myFollowers = null;"
    }
  }
*/

  private object dateHelp {
    lazy val dateFormatter = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)
    lazy val yyyyFormatter = new java.text.SimpleDateFormat("yyyy")
    lazy val MMFormatter = new java.text.SimpleDateFormat("MM")
    lazy val ddFormatter = new java.text.SimpleDateFormat("dd")

    val msPerSec = 1000L
    val msPerMin = msPerSec * 60
    val msPerHour = msPerMin * 60
    val msPerDay = msPerHour * 24
    val zoneOffset = {val now = new java.util.GregorianCalendar; now.get(java.util.Calendar.ZONE_OFFSET) + now.get(java.util.Calendar.DST_OFFSET)}

    def dateDiff(oldDate:java.util.Date, newDate:java.util.Date) = {
      ((newDate.getTime + zoneOffset) / msPerDay) - ((oldDate.getTime + zoneOffset) / msPerDay)
    }

    def dateDiffMs(oldMS:Long, newMS:Long) = {
      ((newMS+zoneOffset) / msPerDay) - ((oldMS+zoneOffset) / msPerDay)
    }

    def difference (oldMs:Long, newMs:Long, suffix:String) = {
      val diff = ((newMs + zoneOffset)/msPerDay) - ((oldMs+zoneOffset) / msPerDay)
      if (diff < 0) {
        ""
      } else if (diff == 0) {
        "today"
      } else if (diff == 1) {
        "1 day " + suffix
      } else {
        diff + " days " + suffix
      }
    }
  }

}
