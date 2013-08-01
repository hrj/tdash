package bhoot
import javax.servlet.http._
import Utils._
import UtilsServlet._

object Pic {
  private def getIntFromAscii(ascii:Char) = {
    val digit = 
      if ((ascii >= '0') && (ascii <= '9')) {
        (ascii - '0')
      } else if ((ascii >= 'a') && (ascii <= 'z')) {
        10 + (ascii - 'a')
      } else if ((ascii >= 'A') && (ascii <= 'Z')) {
        10 + 26 + (ascii - 'A')
      } else if (ascii == '_') {
        10 + 26 + 26
      } else if (ascii == '-') {
        10 + 26 + 26 + 1
      } else {
        -1
      }
    if (digit < 0) None
    else Some(digit.toInt)
  }

  final private def getIdFromStr(str:String) = {
    val ids = str.take(8).map(getIntFromAscii)
    ids.takeWhile(_.isDefined).foldLeft(0)(64*_ + _.get)
  }

  private def intToBase64(i:Int) = {
    var str = ""
    var counter = i
    do {
      val digit = counter % 64
      val ascii =
        if (digit < 10)
          ('0' + digit).toChar
        else if (digit < 36)
          ('a' + (digit - 10)).toChar
        else if (digit < 62)
          ('A' + (digit - 36)).toChar
        else if (digit == 62)
          '_'
        else
          '-'

      str = ascii + str
      counter /= 64
    } while (counter > 0)
    str
  }

  val htmlStdStr = """
   <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
  <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
    <head>
      <meta http-equiv="Content-Type" content="text/html; charset=utf-8" /> 
  """

  val footerStdStr = """
      <div id="footer">
        <h1>tDash | a Twitter Dashboard with easy image upload!</h1>
        <ul>
          <li class="menuItem"><a href="/"><span class="menuLabel">Home</span></a></li>
          <li class="menuItem"><a href="/about.html"><span class="menuLabel">About</span></a></li>
          <li class="menuItem"><a href="http://tdash.uservoice.com"><span class="menuLabel">Feedback</span></a></li>
          <li class="menuItem"><a href="/images/screenshotLarge3.png"><span class="menuLabel">Screenshot</span></a></li>
          <li class="menuItem"><a href="http://twitter.com/tdash"><span class="menuLabel">Follow-us</span></a></li>
          <li class="menuItem"><a href="/media.html"><span class="menuLabel">tDash in the Media</span></a></li>
        </ul>
      </div>
    </body>
  </html>
  """

  def getShowThumb (request:Request, response:HttpServletResponse):String = {
    val uploadId = getIdFromStr(request.getParam("id"))
    "http://tdash.org/"+computeThumbPath(uploadId)  // the redirect URL
  }

  val perPagePics = 10

  object DateFormatter {
    private lazy val dateFormatter = new java.text.SimpleDateFormat("EEE, d MMM yyyy")

    def format(date:java.util.Date) = {
      this synchronized {
        dateFormatter format date
      }
    }
  }

  def getViewUser (request:Request, response:HttpServletResponse):String = {
    val (cookies, loginTokens) = WebApp.processLoginCookies(request.req)
    val userScreenName = loginTokens.flatMap(token=>dbHelper.getScreenName(token._2, token._3)).firstOption

    val appPath = request.appPath
    val nameStr = appPath.drop(5)         /*      user/           */
    val userId = dbHelper.getUserIdFromScreenName(nameStr)
    if (userId.isDefined) {
      val pageNum = request.getIntParamOpt("page").getOrElse(0)
      val uploadDetails = dbHelper.getRecentUploadsDetails(userId.get, 0, pageNum * perPagePics, perPagePics + 1)

      val picStr =
        uploadDetails.take(perPagePics).map(detail =>
          ("""
            <tr class="uploadSummary">
              <td><a class="recPic" href="/x%s"><img alt="%s" src="%s"/></a></td>""" format (intToBase64(detail.id), detail.descr, computeThumbPath(detail.id))) +
              """<td><p><b>""" + detail.descr + """</b></p>
                  <p>Uploaded on > <b>""" + DateFormatter.format(detail.createdAt) + """</b></p>
                  <p>Views > <b>""" + detail.viewCount + """</b></p>
                  <p>Logged-in views > <b>""" + detail.viewLoggedCount + """</b></p>
                  <p>Comments > <b>""" + dbHelper.getCommentCountForUpload(detail.id) + """</b></p>
              </td>
            </tr>"""
        ).reduceLeftOpt(_ + _).getOrElse(
          if (pageNum == 0) {
            """ <p>This user has not uploaded any pics yet.</p> """
          } else {
            """ <p>No more pics to show! Please navigate back.</p> """
          }
        )

      val leftPanel = """
        <h1>Pics by """ + nameStr + """</h1>
        <table cellspacing="0"><tbody>""" + picStr +
        """</tbody></table> """ +
        (if (uploadDetails.length > perPagePics) """<p><a href="/pic/user/%s?page=%d">View older pics</a></p>""" format (nameStr, pageNum+1) else "")

      val rightPanel = """
        <h1>"""+nameStr+"""</h1>
        <p><a target="blank" href='http://twitter.com/"""+nameStr+"""'>Twitter profile</a></p>
        <p id="newPic">[+] <a href="/pic/uploadStart">Upload new pic</a></p>
      """

      htmlStdStr + """
            <title>""" + nameStr + """ | tDash</title>
            <link href="/css/common.css" type="text/css" rel="stylesheet" media="screen,projection" />
            <link href="/css/pic.css" type="text/css" rel="stylesheet" media="screen,projection" />
          </head>
          <body>
            <div id='welcomeBar'>
              <table><tbody><tr><td><a id="homeLink" href="/"><img alt="logo" src='/images/logoColorSmall.png' /></a></td><td id='welcomeMsg'>""" + welcomeMsg(userScreenName) + """</td></tr></tbody></table>
            </div>
            <div id='content'>
              <table cellspacing="0"><tbody><tr>
              <td id="leftPanel">""" + leftPanel + """</td>
              <td id="rightPanel">""" + rightPanel + """</td>
              </tr></tbody></table>
            </div>
      """ + footerStdStr
    } else {
      htmlStdStr + """
            <title>""" + nameStr + """ | tDash</title>
          <link href="/css/common.css" type="text/css" rel="stylesheet" media="screen,projection" />
            <link href="/css/pic.css" type="text/css" rel="stylesheet" media="screen,projection" />
          </head>
          <body>
            <div id='welcomeBar'>
              <table><tbody><tr><td><a id="homeLink" href="/"><img alt="logo" src='/images/logoColorSmall.png' /></a></td><td id='welcomeMsg'>""" + welcomeMsg(userScreenName) + """</td></tr></tbody></table>
            </div>
            <div id='contentPadded'>
              <h1>""" + nameStr + """</h1>
              <p>No pics uploaded yet.</p>
              <p id="newPic">[+] <a href="/pic/uploadStart">Upload new pic</a></p>
            </div>
      """ + footerStdStr
    }
  }

  private def computeThumbPath(id:Int) = getShardedDir(id) + "/thumb"+id

  def getViewPic (request:Request, response:HttpServletResponse):String = {
    val (cookies, loginTokens) = WebApp.processLoginCookies(request.req)
    val userScreenName = loginTokens.flatMap(token=>dbHelper.getScreenName(token._2, token._3)).firstOption
    // val cookies = WebApp.processCookies(request.req)
    // val userScreenName = dbHelper.getScreenName(cookies.get("oauth_token").getOrElse(""), cookies.get("oauth_token_secret").getOrElse(""))
    // val userId = dbHelper.getUserIdFromToken(cookies.get("oauth_token").getOrElse(""), cookies.get("oauth_token_secret").getOrElse(""))
    // val userScreenName = userId.flatMap(dbHelper.getScreenNameFromUserId(_))

    val appPath = request.appPath
    val idStr = appPath.drop(5)         /*      view/           */
    val uploadId = getIdFromStr(idStr)

    val uploadDetails = dbHelper.getUpload(uploadId)
    if (uploadDetails.isDefined) {
      // update view count
      dbHelper.updateViewCount(uploadId, if (userScreenName.isDefined) 1 else 0)

      val shardedDir = getShardedDir(uploadId)
      val diskPath = homeDir+shardedDir
      val imgName = "/orig"+uploadId

      val comments = dbHelper.getComments(uploadId)
      val commentStr = """<form id="newCommentFrm" method="post" action="/pic/addComment"><table id="commentTbl" cellspacing="0"><tbody>""" + comments.map(c =>
        """<tr><td class="commentSrc">%s</td><td class="commentTxt">%s</td></tr>""" format (c.screenName, c.comment)
      ).reduceLeftOpt(_ + _).getOrElse("")

      val newCommentForm = 
        if (userScreenName.isDefined) {
          """
            <tr><td><input style="display:none;" id="uploadIdField" type="text" name="upload_id" value="%d" readonly="readonly"></input></td></tr>
            <tr><td class="formDescrTd">Enter your comment</td><td class="formFieldTd"><textarea id="commentTxtField" rows="2" cols="40" type="textarea" name="comment"></textarea></td></tr>
            <tr><td></td><td class="formFieldTd"><label><input id="updateTwitterBtn" checked="checked" type="checkbox" name="update_twitter" value="yes"></input>Post comment to Twitter</label></td></tr>
            <tr><td></td><td class="formFieldTd"><input id="submitCommentBtn" type="submit" value="Add comment"></input></td></tr>
            </tbody></table></form>""" format (uploadId)
        } else {
          """<tr><td></td><td><p class="loginCommentLink"><a href="%s">Login</a> to leave a comment</p></td></tr>
          </tbody></table></form>""" format makeReturnPath(Some("viewPic"+intToBase64(uploadId)))
        }

      val leftPanel = ("""
        <div id="toolbar">
          <input type="image" src="/images/rotateButt.png" alt="rotate clockwise" onclick="$('#mainImg').rotateRight();"/>
        </div>
        <img id="mainImg" style="max-width:2px" alt="%s" src='%s'></img>""" format(uploadDetails.get.descr, shardedDir + imgName)) +
        """<p id="descr">""" + uploadDetails.get.descr + """</p>
      """ + commentStr + newCommentForm

      val recentUploadIds = dbHelper.getRecentUploads(uploadDetails.get.userId, uploadId, 0, 2)

      val recentUploads = 
        if (recentUploadIds.length > 0) {
        """
          <p><em>Recent Pics from """ + uploadDetails.get.creator + """</em></p>
          <table><tbody><tr id="recPicsRow">""" +
          recentUploadIds.map(id => """<td><a class="recPic" href="/x""" + intToBase64(id)  +"""" ><img alt="" src='""" + computeThumbPath(id) + "'/></a></td>").reduceLeft(_ + _) + """
          </tr></tbody></table>
        """
        } else {
          """<p>First pic by this user!</p>"""
        }

      val rightPanel = """
        <p>Posted by <a href="/pic/user/""" + uploadDetails.get.creator + """"><b>""" + uploadDetails.get.creator + """</b></a><br/>on """ + DateFormatter.format(uploadDetails.get.createdAt) + """</p>
        <p>Views &#8250; <b>""" + uploadDetails.get.viewCount + """</b><br/>
        Logged-in views &#8250; <b>""" + uploadDetails.get.viewLoggedCount + """</b><br/>
        Comments &#8250; <b>""" + comments.length + """</b></p>
        """ + recentUploads + """
        <p id="newPic">[+] <a href="/pic/uploadStart">Upload new pic</a></p>
      """

      htmlStdStr + """
            <title>""" + uploadDetails.get.descr.take(50) + """ | tDash</title>
          <link href="/css/common.css" type="text/css" rel="stylesheet" media="screen,projection" />
            <link href="/css/pic.css" type="text/css" rel="stylesheet" media="screen,projection" />
            <script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.4.3/jquery.min.js"></script>
            <script type="text/javascript" src="/scripts/jquery.rotate.1-1-a.min.js"></script>
            <script type="text/javascript" src="/scripts/pic.js"></script>
          </head>
          <body>
            <div id='welcomeBar'>
              <table><tbody><tr><td><a id="homeLink" href="/"><img alt="logo" src='/images/logoColorSmall.png' /></a></td><td id='welcomeMsg'>""" + welcomeMsg(userScreenName) + """</td></tr></tbody></table></div>
            <div id='content'>
              <table cellspacing="0"><tbody><tr>
              <td id="leftPanel">""" + leftPanel + """</td>
              <td id="rightPanel">""" + rightPanel + """</td>
              </tr></tbody></table>
            </div>
      """ + footerStdStr
    } else {
      htmlStdStr + """
          <link href="/css/common.css" type="text/css" rel="stylesheet" media="screen,projection" />
            <link href="/css/pic.css" type="text/css" rel="stylesheet" media="screen,projection" />
          </head>
          <body>
          <p>Welcome """ + userScreenName.getOrElse("guest") + """</p>
           <ul>
            <li><b>appPath</b>:  """ + request.appPath + """</li>
            <li><b>uploadId</b>:  """ + uploadId + """</li>
           </ul>
           <p>You seem to have followed a wrong URL</p>
      """ + footerStdStr
    }
  }

  def postAddComment (request:Request, response:HttpServletResponse):String = {
    val (cookies, loginTokens) = WebApp.processLoginCookies(request.req)
    val userId = loginTokens.flatMap(token=>dbHelper.getUserIdFromToken(token._2, token._3)).firstOption
    if (userId.isDefined) {
      // check how many comments from this bugger
      val commentCount = dbHelper.getCommentCount(userId.get)
      if (commentCount > maxCommentsPerDay) {
        htmlStdStr + ("""
            <link href="/css/common.css" type="text/css" rel="stylesheet" media="screen,projection" />
              <link href="/css/pic.css" type="text/css" rel="stylesheet" media="screen,projection" />
            </head>
            <body>
             <p>You were trying to add more than %d comments in the last 24 hours. In the interest of reducing spam, we limit the number of comments per day. Cool down. Please come back after some time.</p>
        """ format maxCommentsPerDay) + footerStdStr
      } else {
        val uploadId = request.getIntParam("upload_id")
        val comment = secureEscape(request.getParam("comment"))
        val updateTwitter = request.getParamOpt("update_twitter")
        dbHelper.insertNewComment (uploadId, userId.get, comment)
        if(updateTwitter.isDefined) {
          UpdateWorker ! UpdateWorker.UpdateStatus(comment.take(110) + " http://"+WebApp.domainName.getOrElse("tdash.org")+"/x"+intToBase64(uploadId), cookies("oauth_token"), cookies("oauth_token_secret"))
        }
        Notifier ! Notifier.NewComment(intToBase64(uploadId), computeThumbPath(uploadId), comment)
        response.sendRedirect("http://tdash.org/x"+intToBase64(uploadId))
        ""
      }
    } else {
      htmlStdStr + """
          <link href="/css/common.css" type="text/css" rel="stylesheet" media="screen,projection" />
            <link href="/css/pic.css" type="text/css" rel="stylesheet" media="screen,projection" />
          </head>
          <body>
           <p>You were either not logged in or your login credentials have expired.</p>
      """ + footerStdStr
    }
  }

  private def makeReturnPath(returnPath:Option[String]) = "/oauth/login" + returnPath.map("?return="+_).getOrElse("")

  private def welcomeMsg(screenName:Option[String]):String = welcomeMsg(screenName, None)

  private def welcomeMsg(screenName:Option[String], returnPath:Option[String]):String = {
    if (screenName.isDefined)
      """<a href="/pic/user/"""+screenName.get+""""<img src="/images/home32x32.png" />&nbsp;<b>"""+screenName.get+"""</b>
        </a>
        <a id="signOutLink" href="/oauth/logout">[Sign out]</a>
      """
    else
      """Welcome guest. <a href="%s"><b id="signInLink">Sign in with OAuth</b></a>""" format (makeReturnPath(returnPath))
  }

  implicit def provideGroup[T] (l:List[T]) = new {
    def group(n:Int) = l.zipWithIndex.foldRight[List[List[T]]](Nil){case ((x,i), a) =>
      if ( (i +1) % n == 0) {
        List(x) :: a
      } else {
        if (a.isEmpty)
          List(List(x))
        else
          (x :: a.head) :: a.tail
      }
    }
  }

  val maxUploadsPerDay = 4
  val maxCommentsPerDay = 20

  val safeUploadKey = 3789
    
  def getUploadStart (request:Request, response:HttpServletResponse):String = {
    val (cookies, loginTokens) = WebApp.processLoginCookies(request.req)
    val embedded = request.getParamOpt("embed").isDefined

    val loginIdOpt = 
      if (embedded) {
        request.getIntParamOpt("loginId").orElse(if (loginTokens.length == 1) Some(loginTokens.first._1) else None)
      } else {
        request.getIntParamOpt("loginId").orElse(loginTokens.firstOption.map(_._1))
      }

    if (embedded && (loginTokens.length > 1) && (!loginIdOpt.isDefined)) {
      htmlStdStr + """
            <title>Upload new pic | tDash</title>
            <link href="/css/common.css" type="text/css" rel="stylesheet" media="screen,projection" />
            <link href="/css/pic.css" type="text/css" rel="stylesheet" media="screen,projection" />
          </head>
          <body><p>You are probably running an older version of the client.</p><p>Please hit refresh/reload in your browser now!</p><p>The tDash version should be <b>3.3</b> or higher</p>""" +
      footerStdStr
      
    } else {
      val loginId = loginIdOpt.getOrElse(-1)
      val loginToken = loginTokens.find(_._1 == loginId)
      val token = loginToken.map(_._2).getOrElse("")
      val tokenSecret = loginToken.map(_._3).getOrElse("")

      val screenName = dbHelper.getScreenName(token, tokenSecret)

      val loginFormField =
        if (embedded || loginTokens.length == 1) {
          """<input type="hidden" name="loginId" value="%d">""" format (loginId)
        } else {
          var optStr = ""

          loginTokens.foreach {token =>
            val screenName = dbHelper.getScreenName(token._2, token._3)
            if (screenName.isDefined) {
              optStr += ("""<option value="%d">%s</option>""" format (token._1, screenName.get))
            }
          }
          val result = """
          <tr>
            <td class="formLabel"><p class="labelContent">Select account to post from</p></td>
            <td class="formInput"><select name="loginId">""" + optStr + """</select></td>
          </tr> """
          result
        }

      val loginClarification =
        if (embedded && (loginTokens.length > 1)) {
          "<h3>(posing as "+screenName.get + ")</h3>"
        } else ""


      val welcomeBar =
        if (embedded) {
          ""
        } else {
          """
          <div id='welcomeBar'>
            <table><tbody><tr><td><a id="homeLink" href="/"><img src='/images/logoColorSmall.png' /></a></td><td id='welcomeMsg'>""" + welcomeMsg(screenName) + """</td></tr></tbody></table>
          </div>
          """
        }

      val embedInput =
        if (embedded) {
          """<input type=hidden name="embed" value="true" />"""
        } else {
          ""
        }


      val recentUploads =
        if (embedded || screenName.isDefined) {
          ""
        } else {
          val recentUploadIds = dbHelper.getGlobalRecentUploads(0, if (screenName.isDefined) 6 else 18)
          val recentUploadIdsByRow = recentUploadIds.group(6)

          val result = """
            <div id="recGlobalPics">
              <h2>Recently uploaded pics</h2>
              <table cellspacing="0"><tbody>""" +
              recentUploadIdsByRow.map(row =>
                """<tr class="recGlobalPicsRow">""" +
                  row.map(id => """<td><a class="recPic" href="/x""" + intToBase64(id)  +"""" ><img alt='thumbnail' src='""" + computeThumbPath(id) + "'/></a></td>").reduceLeftOpt(_ + _).getOrElse("") + """
                  </tr>"""
                ).reduceLeftOpt(_ + _).getOrElse("") + """
              </tbody></table>
            </div>
          """
          result
        }


      if (screenName.isDefined) {
        val userId = dbHelper.getUserIdFromToken(token, tokenSecret)
        val picCount = dbHelper.getUploadCount(userId.get)

        val uploadForm = 
          if (picCount > maxUploadsPerDay) {
            """
              <div id="uploadForm">
                <h1>You have uploaded %d pics in the last 24 hours!</h1>
                <p>The limit of pic uploads per day has been reached. Please upload again after some time.</p>
              </div>
            """ format picCount
          } else {
            """
            <div id="uploadForm" %s>
              <h1>Share a picture on Twitter</h1>""" + loginClarification + """
              <form method="post" action="/pic/upload" enctype="multipart/form-data">
                <table cellspacing="0"><tbody>""" + loginFormField + """
                  <tr>
                    <td class="formLabel"><p class="labelContent">Select an image from disk<br/><span class="formLabelHint">Accepted formats JPG, PNG</span></p></td>
                    <td class="formInput"><input name="imgFile" type="file"></input></td>
                  </tr>
                  <tr>
                    <td class="formLabel"><p class="labelContent">Enter a description<br/><span class="formLabelHint">Keep it short. It will be tweeted</span></p></td>
                    <td class="formInput"><textarea rows="3" cols="40" name="descr"></textarea></td>
                  </tr>
                </tbody></table> """ + embedInput + """
                <p><input type="submit" value="Upload"></input></p>
              </form>
            </div>
          """ format (if (embedded) "class=\"embeddedForm\"" else "class=\"freeForm\"")
        }

        htmlStdStr + """
              <title>Upload new pic | tDash</title>
              <link href="/css/common.css" type="text/css" rel="stylesheet" media="screen,projection" />
              <link href="/css/pic.css" type="text/css" rel="stylesheet" media="screen,projection" />
            </head>
            <body> """ + welcomeBar +  uploadForm + recentUploads + """
          """ +
        (if (embedded) "</body></html>" else footerStdStr)

      } else {
      htmlStdStr + """
            <title>Upload new pic | tDash</title>
            <link href="/css/common.css" type="text/css" rel="stylesheet" media="screen,projection" />
            <link href="/css/pic.css" type="text/css" rel="stylesheet" media="screen,projection" />
          </head>
          <body>
            <div id='welcomeBar'>
              <table><tbody><tr><td><a id="homeLink" href="/"><img alt="logo" src='/images/logoColorSmall.png' /></a></td><td id='welcomeMsg'>""" + welcomeMsg(screenName,Some("uploadPic")) + """</td></tr></tbody></table>
            </div>
            <div id='content'>
            <p id="signInMsg">Please <a href="%s"><b>sign in</b></a> to upload a picture</p>
            </div>
            """.format(makeReturnPath(Some("uploadPic"))) + recentUploads +
        footerStdStr
      }
    }
  }

  import org.apache.commons.fileupload._
  import org.apache.commons.fileupload.disk._
  import org.apache.commons.fileupload.servlet._

  // Create a factory for disk-based file items
  val factory = new DiskFileItemFactory()

  // Create a new file upload handler
  val upload = new ServletFileUpload(factory)

  val homeDir = System.getProperty("user.home")
  val maxSizeBytes = (512 * 1024)

  
  import java.awt._
  import java.awt.image._
  // This is the simple method. Very good results but slower
  // def highQualityScaling(origBufImg:BufferedImage) = awtScaleImage(origBufImg, 100, java.awt.Image.SCALE_SMOOTH)

  // This is a clever hack. Decent quality and fast
  // http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
  def highQualityScaling(img:BufferedImage, maxImageSize:Int) = {
    var w = img.getWidth
    var h = img.getHeight

    val scaleFactor =
      if (w > h)
          (maxImageSize.toFloat / w.toFloat)
      else
          (maxImageSize.toFloat / h.toFloat)

    val targetHeight = (h * scaleFactor).toInt
    val targetWidth = (w * scaleFactor).toInt

    val imgType = 
      // if(img.getTransparency() == Transparency.OPAQUE)
        BufferedImage.TYPE_INT_RGB
      // else
        // BufferedImage.TYPE_INT_ARGB

    var ret = img

    // Use multi-step technique: start with original size, then
    // scale down in multiple passes with drawImage()
    // until the target size is reached
    
    do {
      if (w > targetWidth) {
        w /= 2
        if (w < targetWidth) {
            w = targetWidth
        }
      }

      if (h > targetHeight) {
          h /= 2
          if (h < targetHeight) {
              h = targetHeight
          }
      }

      if ((w == targetWidth) || (h == targetHeight)) {
        w = targetWidth
        h = targetHeight
        ret = awtScaleImage(ret, maxImageSize, java.awt.Image.SCALE_SMOOTH)
      } else {

        val tmp = new BufferedImage(w, h, imgType)
        val g2 = tmp.createGraphics
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        // g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2.drawImage(ret, 0, 0, w, h, null)
        g2.dispose

        ret = tmp;
      }
    } while (w > targetWidth || h > targetHeight);

    ret;
  }

  private def getShardedDir(uploadId:Int) = "/imgStatic/"+(uploadId/1000)

  val errorPage =
    """<html><body>
        <p>There was some error. Please upload your image again.</p><p>Supported image formats are JPEG, PNG, GIF.</p>
        <p>File needs to be less than max size (""" +(maxSizeBytes/1024) + """kb)</p>
    </body></html>"""

  val errorPageForEmbedded =
    """
      <html><head><script type="text/javascript">top.dash.finishUpload(false);</script></head></html>
    """

  case class UploadImageDetails(id:Int, file:java.io.File, thumbFile:java.io.File)
  val idealImgDimension = 1024

  def processFileItem(fileItem:FileItem) = {
    val uploadId = dbState.getNewUploadId
    val shardedDir = getShardedDir(uploadId)
    val diskPath = homeDir+shardedDir

    println("fileName: " + fileItem.getName)
    println("type: " + fileItem.getContentType)
    println("upload id: " + uploadId)

    val resultOpt = {
      val size = fileItem.getSize
      println("size : " + size)
      // first check for size
      if (size > maxSizeBytes) {
        // check for image dimensions
        val tmpImgFile = java.io.File.createTempFile("tmpScaling", "img_"+uploadId.toString)
        fileItem.write(tmpImgFile)
        val origBufImg = javax.imageio.ImageIO.read(tmpImgFile)
        val outResult = if (origBufImg.getWidth > idealImgDimension || origBufImg.getHeight > idealImgDimension) {
          val scaledImg = highQualityScaling(origBufImg, idealImgDimension)
          val imgName = "/orig"+uploadId
          val imgFile = new java.io.File(diskPath+imgName)
          javax.imageio.ImageIO.write(scaledImg, "jpeg", imgFile)
          Some(UploadImageDetails(uploadId, imgFile, null))
        } else {
          None
        }
        tmpImgFile.delete
        outResult
      } else {
        // just copy it to the final destination
        (new java.io.File(diskPath)).mkdir
        val imgName = "/orig"+uploadId
        val imgFile = new java.io.File(diskPath+imgName)
        fileItem.write(imgFile)
        Some(UploadImageDetails(uploadId, imgFile, null))
      }
    }

    fileItem.delete

    val resultWithThumbOpt =
      resultOpt.flatMap {result =>
        // create a thumbnail as well
        val thumbImgName = "/thumb"+uploadId
        val origBufImg = javax.imageio.ImageIO.read(result.file)
        if (origBufImg != null) {
          val scaledImg = highQualityScaling(origBufImg, 100)
          val thumbFile = new java.io.File(diskPath+thumbImgName)
          javax.imageio.ImageIO.write(scaledImg, "jpeg", thumbFile)
          Some(UploadImageDetails(result.id, result.file, thumbFile))
        } else {
          result.file.delete
          None
        }

      }

    resultWithThumbOpt
  }

  def deleteFiles(uploadImgDetails:UploadImageDetails) = {
    uploadImgDetails.file.delete
    uploadImgDetails.thumbFile.delete
  }

  final private def secureEscape(text: String) = {
    val s = new StringBuilder()
    for (c <- text.elements) c match {
      case '<' => s.append("&lt;")
      case '>' => s.append("&gt;")
      // case '&' => s.append("&amp;")
      // case '"' => s.append("&quot;")
      //case '\'' => s.append("&apos;") // is valid xhtml but not html, and IE doesn't know it, says jweb
      case _   => s.append(c)
    }
    s.toString
  }

  def postUpload (request:Request, response:HttpServletResponse):String = {
    val (cookies, loginTokens) = WebApp.processLoginCookies(request.req)

    var userId:Option[Int] = None
    val isMultipart = ServletFileUpload.isMultipartContent(request.req)

    if (isMultipart ) {
      // Parse the request
      val items = upload.parseRequest(request.req).toArray

      var descr:String = null
      var embedded = false
      var fileItem:FileItem = null
      var loginId = -1

      items foreach {x => x match {
        case f:FileItem =>
          if (f.isFormField) {
            if (f.getFieldName == "descr") {
              descr = secureEscape(new String(f.get))
            } else if (f.getFieldName == "embed") {
              println ("Embedded")
              embedded = true
            } else if (f.getFieldName == "loginId") {
              loginId = (new String(f.get)).toInt
            }
          } else {
            if (f.getFieldName == "imgFile") {
              fileItem = f
            }
          }
        case y => println("Unhandled param:" + y)
      }}

      val loginToken = loginTokens.find(_._1 == loginId)
      val token = loginToken.map(_._2).getOrElse("")
      val tokenSecret = loginToken.map(_._3).getOrElse("")

      userId = dbHelper.getUserIdFromToken(token, tokenSecret)
      println(userId)

      if (userId.isDefined) {
        // Check that we have a file upload request
        val picCount = dbHelper.getUploadCount(userId.get)
        if (picCount <= maxUploadsPerDay) {
          println("An upload is in progress")

          if (descr != null && fileItem != null) {
            val uploadImageDetailsOpt = processFileItem(fileItem)
            println(uploadImageDetailsOpt)

            val outStr =
              if (uploadImageDetailsOpt.isDefined) {
                val uploadId = uploadImageDetailsOpt.get.id
                dbHelper.insertNewUpload (uploadId, userId.get, descr, fileItem.getName, fileItem.getContentType)

                try {
                  UpdateWorker ! UpdateWorker.UpdateStatus(descr.take(110) + " http://"+WebApp.domainName.getOrElse("tdash.org")+"/x"+intToBase64(uploadId), token, tokenSecret)
                  Notifier ! Notifier.UploadSuccess(intToBase64(uploadId), computeThumbPath(uploadId))

                  val resultURL = "http://tdash.org/x"+intToBase64(uploadId)

                  if (embedded) {
                    """
                      <html><head><script type="text/javascript">top.dash.finishUpload(true,"%s");</script></head></html>
                    """ format (resultURL)
                  } else {
                    response.sendRedirect(resultURL)
                    ""
                  }
                } catch {
                  case ex =>
                    println(ex)
                    uploadImageDetailsOpt.foreach(deleteFiles)
                    if (embedded) {
                      errorPageForEmbedded
                    } else {
                      errorPage
                    }
                }
              } else {
                errorPage
              }

            outStr
          } else errorPage

        } else {

          ""
        }

      } else {
        // user is not authenticated
        "<html><body>Upload failed, because we coudn't verify your credentials. Please make sure you are logged in</body></html>"
      }

    } else ""

  }

  private def verifyAndroidId(token:String, tokenSecret:String, verifier:String):Option[Int] = {
    import dispatch.{Http, Request}
    import dispatch.oauth.Token
    import dispatch.oauth.OAuth._
    import dispatch.json.JsHttp._

    try {
      val request = (new Request("https://api.twitter.com/1/account/verify_credentials.json")) <@ (Common.androidConsumer, Token(token, tokenSecret), verifier)

      
      val http = new Http 
      val response = http(request ># obj)

      val screenName = ('screen_name ! str)(response)
      val userId = ('id ! num)(response).intValue
      dbHelper.insertNewUser(userId, screenName)
      dbHelper.insertNewAndroidOAuth(userId, token, tokenSecret, verifier)
      Some(userId)

    } catch {
      case e:Exception => 
        e.printStackTrace
      None
    }

  }

  private val errorAndroidMsg = "error,Something went wrong"

  def postUploadAndroid (request:Request, response:HttpServletResponse):String = {
    val (cookies, loginTokens) = WebApp.processLoginCookies(request.req)

    val isMultipart = ServletFileUpload.isMultipartContent(request.req)

    if (isMultipart ) {
      // Parse the request
      val items = upload.parseRequest(request.req).toArray

      var descr:String = null
      var fileItem:FileItem = null
      var verifier:String = null
      var token:String = null
      var tokenSecret:String = null

      items foreach {x => x match {
        case f:FileItem =>
          if (f.isFormField) {
            if (f.getFieldName == "descr") {
              descr = secureEscape(new String(f.get))
            } else if (f.getFieldName == "verifier") {
              verifier = (new String(f.get))
            } else if (f.getFieldName == "token") {
              token = (new String(f.get))
            } else if (f.getFieldName == "token_secret") {
              tokenSecret = (new String(f.get))
            }
          } else {
            if (f.getFieldName == "imgFile") {
              fileItem = f
            }
          }
        case y => println("Unhandled param:" + y)
      }}

      assert(token != null && tokenSecret != null)

      val userId = dbHelper.getAndroidUserIdFromToken(token, tokenSecret, verifier)
      println(userId)

      val verifiedUserId = userId.orElse(verifyAndroidId(token, tokenSecret, verifier))
      println(verifiedUserId)

      if (verifiedUserId.isDefined) {
        // Check that we have a file upload request
        val picCount = dbHelper.getUploadCount(verifiedUserId.get)
        if (picCount <= maxUploadsPerDay) {
          println("An upload is in progress")

          if (descr != null && fileItem != null) {
            val uploadImageDetailsOpt = processFileItem(fileItem)
            println(uploadImageDetailsOpt)

            val outStr =
              if (uploadImageDetailsOpt.isDefined) {
                val uploadId = uploadImageDetailsOpt.get.id
                dbHelper.insertNewUpload (uploadId, verifiedUserId.get, descr, fileItem.getName, fileItem.getContentType)

                try {
                  UpdateWorker ! UpdateWorker.UpdateStatusAndroid(
                    descr.take(110) + " http://"+WebApp.domainName.getOrElse("tdash.org")+"/x"+intToBase64(uploadId),
                    token, tokenSecret, verifier)

                  Notifier ! Notifier.UploadSuccess(intToBase64(uploadId), computeThumbPath(uploadId))

                  val resultURL = "http://tdash.org/x"+intToBase64(uploadId)

                  val result = """
                    success,%s
                  """.format (resultURL)

                  result
                } catch {
                  case ex =>
                    println(ex)
                    uploadImageDetailsOpt.foreach(deleteFiles)
                    errorAndroidMsg
                }
              } else {
                errorAndroidMsg
              }

            outStr
          } else errorAndroidMsg

        } else {
          response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE)

          ""
        }

      } else {
        // user is not authenticated
        errorAndroidMsg
      }

    } else ""

  }

  def awtScaleImage(image:BufferedImage, maxSize:Int, hint:Int) = {
      // We use AWT Image scaling because it has far superior quality
      // compared to JAI scaling.  It also performs better (speed)!
      println("AWT Scaling image to: " + maxSize)
      var w = image.getWidth
      var h = image.getHeight
      val scaleFactor =
      if (w > h)
          (maxSize.toFloat / w.toFloat)
      else
          (maxSize.toFloat / h.toFloat)
      w = (w * scaleFactor).toInt
      h = (h * scaleFactor).toInt
      // since this code can run both headless and in a graphics context
      // we will just create a standard rgb image here and take the
      // performance hit in a non-compatible image format if any
      val i = image.getScaledInstance(w, h, hint)
      val simage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
      val g = simage.createGraphics
      g.drawImage(i, null, null)
      g.dispose
      i.flush
      simage
  }
}
