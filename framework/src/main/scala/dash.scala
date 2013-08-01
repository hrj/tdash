package bhoot
import javax.servlet.http._
import Utils._
import UtilsServlet._

object Dash {
  val flattrSrc = """
    <div id="flattrDiv" style="display:none;text-align:center;margin-top:0.5em;">
      <script type="text/javascript"> var flattr_url = 'http://tdash.org'; </script>
      <script src="http://api.flattr.com/button/load.js" type="text/javascript"></script>
    </div>
"""

  val uservoiceSrc = """
    <!-- script type="text/javascript">
    var uservoiceOptions = {
      /* required */
      key: 'tdash', host: 'tdash.uservoice.com', forum: '37308', showTab: false,
      alignment: 'left', background_color:'#f00', text_color: 'white', hover_color: '#06C', lang: 'en'
    };

    function _loadUserVoice() {
      var s = document.createElement('script');
      s.setAttribute('type', 'text/javascript');
      s.setAttribute('src', ("https:" == document.location.protocol ? "https://" : "http://") + "cdn.uservoice.com/javascripts/widgets/tab.js");
      document.getElementsByTagName('head')[0].appendChild(s);
    }
    _loadSuper = window.onload;
    window.onload = (typeof window.onload != 'function') ? _loadUserVoice : function() { _loadSuper(); _loadUserVoice(); };
    </script-->
  """

  val htmlStdStr = """
   <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
  <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
    <head>
      <meta http-equiv="Content-Type" content="text/html; charset=utf-8" /> 
  """

  val htmlBody1 = """
    <title>tDash.org | Twitter Dashboard</title>
    <meta content="Read your Twitter timeline and post updates to your status. All from within your browser." name="description" />
    <meta content="Twitter browser client status update" name="keywords" />"""

  val htmlScripts = """
    <script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.6.1/jquery.min.js" ></script>
    <script type="text/javascript" src="/scripts/compound2.js" ></script>
    <script type="text/javascript" src="/scripts/dash-new4.js" ></script>
"""

  val htmlBody1StartNoLogin = htmlStdStr + htmlBody1
  val htmlBody1Start = htmlBody1StartNoLogin + htmlScripts

  def htmlBody2Gen(title:String, message:String, message2:String) = """
  </head>
  <body>
    <script type="text/javascript">
    (function(){
      var bsa = document.createElement('script');
         bsa.type = 'text/javascript';
         bsa.async = true;
         bsa.src = 'http://s3.buysellads.com/ac/bsa.js';
      (document.getElementsByTagName('head')[0]||document.getElementsByTagName('body')[0]).appendChild(bsa);
    })();
    </script>
    <div id="view">
      <table id="viewTbl" class="topAlignTbl" cellspacing="0">
        <tbody><tr>
          <td class="paddedLeftTd">
            <div class="paddedLeft">
              <div id="headCorner">
                <a href="/" class="linkNoDec"><img class="logo" src="/images/logoColorSmall.png" /></a>
                <p style="margin:.5em 0"><select id="logSelect" onchange="dash.userSelectChange();"></select><br/>
                <span id="logInfo"></span></p>
              </div>
            </div>
          </td>
          <td class="paddedRightTd">
            <div class="paddedRight">
              <div id="toolBar">
                <table cellspacing="0" style="text-align:left;"><tbody><tr style="vertical-align:top">
                  <td>
                    <form accept-charset="UTF-8" id="updateForm" method="post" action="https://api.twitter.com/1/statuses/update.xml" target="upload_target">
                      <textarea name="status" rows="3" cols="60" id="updateInput" ></textarea>
                      <input id="replyToParam" type="text" name="in_reply_to_status_id" value='0' style="display:none;" />
                      <input type="text" name="source" value='tDash' style="display:none;" />
                      <input type="text" id="upd_oauth_consumer_key" name="oauth_consumer_key" value='tDash' style="display:none;" />
                      <input type="text" id="upd_oauth_nonce" name="oauth_nonce" value='tDash' style="display:none;" />
                      <input type="text" id="upd_oauth_signature" name="oauth_signature" value='tDash' style="display:none;" />
                      <input type="text" id="upd_oauth_signature_method" name="oauth_signature_method" value='HMAC-SHA1' style="display:none;" />
                      <input type="text" id="upd_oauth_timestamp" name="oauth_timestamp" value='tDash' style="display:none;" />
                      <input type="text" id="upd_oauth_token" name="oauth_token" value='tDash' style="display:none;" />
                      <input type="text" id="upd_oauth_version" name="oauth_version" value='1.0' style="display:none;" />
                      <input type="submit" id="updateSubmit" value="" style="display:none;"/>
                    </form>
                  </td>
                  <td id="updateInfo">
                    <p id="topInfo">What are you doing?</p>
                    <p style="margin:1px 0;"><span id="updateCharCount" class="charCount">140</span>
                    <!--img alt="Submit update" id="updateButton" onclick="dash.updateClick()" src="/images/updateButt.png" /-->
                    <input type="image" id="updateButton" onclick="dash.updateClick()" alt="Submit Update" src="/images/updateButt.png" /></p>
                    <!--input type="button" id="updateButton" onclick="dash.updateClick()" value="&#9997; Submit Update"/-->
                  </td>
                  <td id="replyInfo"></td>
                </tr></tbody></table>
                <div style="clear:both;">
                  <table cellspacing="0"><tbody><tr style="vertical-align:bottom">
                    <td style="width:100%;">
                      <div class="horizDivider">
                        <div class="horizDividerInner"> </div>
                      </div>
                    </td>

                    <td style="white-space:nowrap;">
                      <input id="fetchNewButton" type="button" value="Fetch now!" onclick="dash.fetchNew();"/>
                      <input id="imgUpButton" type="button" value="Share a Photo" onclick="dash.imgUpload();"/>
                      <!--input id="markAllReadButton" type="button" value="Mark folder as read" onclick="dash.markAllRead();"/-->
                      <span class="toolButt"><input id="showReadButton" type="checkbox" /><label for="showReadButton">Show Read</label></span>
                      <select id="sortOrderSelect" onchange="dash.timeOrderChange();">
                        <option value="oldTop">Older tweets on top</option>
                        <option value="newTop" selected="selected">Newer tweets on top</option>
                      </select>
                      <select id="powerSelect" onchange="dash.powerSelect()">
                        <option style="display:none;color:#eee;" value="more" selected="selected">More&#8230;</option>
                        <option value="enableNotifications">Enable notifications&#8230;</option>
                        <option value="settings">Settings&#8230;</option>
                        <option value="signOut">Sign Out&#8230;</option>
                        <option value="picView">Your pics&#8230;</option>
                        <option value="themeSelect">Themes&#8230;</option>
                        <optgroup label="Power User" class="ogroup">
                          <option value="sync">Sync&#8230;</option>
                          <option value="purge">Purge DB&#8230;</option>
                          <option value="dm">Direct Message&#8230;</option>
                          <option value="feedback">Feedback&#8230;</option>
                        </optgroup>
                        <optgroup label="Help" class="ogroup">
                          <option value="keys">Keys&#8230;</option>
                          <option value="about">About&#8230;</option>
                        </optgroup>
                      </select>
                      <input id="helpButton" type="button" value="? Help" onclick="dash.showHelp();"/>
                    </td>
                  </tr></tbody></table>
                </div>
              </div>
            </div>
          </td>
        </tr>
        <tr>
          <td class="paddedLeftTd">
            <div class="paddedLeft">
              <div class="navTab">
                <p id="navHeadFriends" class="tabHead" onclick="dash.changeMode(0,true)">Friends</p>
                <div id="navFriends"></div>
              </div>
              <div class="navTab">
                <p id="navHeadList" class="tabHead" onclick="dash.changeMode(2, true)">Lists</p>
                <div id="navList" style="display:none;"></div>
              </div>
              <div class="navTab">
                <p id="navHeadFolders" class="tabHead" onclick="dash.changeMode(1)">Folders</p>
                <div id="navFolders" style="display:none;">
                  <p class="navElem" onclick="dash.folderClick('mentions');">Mentions</p>
                  <p class="navElem" onclick="dash.folderClick('inbox');">Inbox</p>
                  <p class="navElem" onclick="dash.folderClick('outbox');">Outbox</p>
                  <p class="navElem" onclick="dash.folderClick('favs');">Favourites</p>
                </div>
              </div>
              <div class="navTab">
                <p id="navHeadToolBox" class="tabHead" onclick="dash.changeMode(4)">Tool Box</p>
                <div id="navToolBox" style="display:none;">
                  <p class="navElem" onclick="dash.toolClick('clicks');">Clicks</p>
                </div>
              </div>
              <div class="navTab">
                <p id="navHeadSearch" class="tabHead" onclick="dash.changeMode(5)">Search</p>
                <div id="navSearch" style="display:none;">
                  <input id="searchInput" type="text" size="8"/>
                  <div id="navSearchInner">
                  </div>
                </div>
              </div>
              <div class="navTab">
                <p id="navHeadTrends" class="tabHead" onclick="dash.changeMode(3)">Trends</p>
                <div id="navTrends" style="display:none;">Fetching&#8230;</div>
              </div>
            </div>""" + flattrSrc + """
            <!-- BuySellAds.com Zone Code -->
            <div id="bsap_1270076" class="bsarocks bsap_dade2eb1bbfc3a41945ef1939328976e"></div>
            <!-- End BuySellAds.com Zone Code -->
          </td>
          <td class="paddedRightTd">
            <div class="paddedRight">
              <div id="readerInfo">"""+title+"""</div>
              <div id="reader">""" + message + """</div>
            </div>
          </td>
        </tr></tbody>
      </table>
      <table id="statusBar"><tbody><tr>
        <td id="fetchStatus"></td>
        <td id="fetchCount"></td>
        <td id="submitStatus"></td>
        <td id="versionStatus"></td>
      </tr></tbody></table>
    </div>""" + message2 + """
  </body>
</html>"""

  val noLoggedInTitle = """
      <p style="text-align:center">Welcome to <b><a class="linkNoDec" href="/"><img class="linkNoDec" alt="tDash" style="vertical-align:middle;padding:0 10px;" src="/images/logoColorSmall.png"/></a></b> a browser based client for Twitter.</p>
  """

  val noLoggedInMessage = """
    <div style="text-align:center" id="alert">
      <p><img src="/images/user_warning_48.png" /></p>
      <p>You are not authenticated!<br/><span class="noAuthTip">(Cookies need to be enabled for authentication)</span></p>
      <p>Would you like to <a href="/oauth/login">Sign in</a>?<br/><span class="noAuthTip">oAuth is used for signing in with Twitter.<br/>No registration is required!</span></p>
    </div>
  """

  val loggedInFormsNScripts =     """
    <div style="display:none;">
      <!--iframe id="upload_target" name="upload_target" onload="dash.statusSubmitted()"></iframe-->
      <form id="favForm" method="post" action="" target="upload_target">
        <input type="text" id="fav_oauth_consumer_key" name="oauth_consumer_key" value='tDash'/>
        <input type="text" id="fav_oauth_nonce" name="oauth_nonce" value='tDash'/>
        <input type="text" id="fav_oauth_signature" name="oauth_signature" value='tDash'/>
        <input type="text" id="fav_oauth_signature_method" name="oauth_signature_method" value='HMAC-SHA1'/>
        <input type="text" id="fav_oauth_timestamp" name="oauth_timestamp" value='tDash'/>
        <input type="text" id="fav_oauth_token" name="oauth_token" value='tDash'/>
        <input type="text" id="fav_oauth_version" name="oauth_version" value='1.0'/>
        <input type="submit" id="favSubmit" value=""/>
      </form>
      <form id="rtForm" method="post" action="" target="upload_target">
        <input type="text" id="rt_oauth_consumer_key" name="oauth_consumer_key" value='tDash'/>
        <input type="text" id="rt_oauth_nonce" name="oauth_nonce" value='tDash'/>
        <input type="text" id="rt_oauth_signature" name="oauth_signature" value='tDash'/>
        <input type="text" id="rt_oauth_signature_method" name="oauth_signature_method" value='HMAC-SHA1'/>
        <input type="text" id="rt_oauth_timestamp" name="oauth_timestamp" value='tDash'/>
        <input type="text" id="rt_oauth_token" name="oauth_token" value='tDash'/>
        <input type="text" id="rt_oauth_version" name="oauth_version" value='1.0'/>
        <input type="submit" id="rtSubmit" value=""/>
      </form>
      <div id="settings">
        <span class="toolButt">Auto fetch every <select id="autoFetchSelect">
          <option value="6">6 mins</option>
          <option value="12" selected="selected">12 mins</option>
          <option value="24">24 mins</option>
          <option value="36">36 mins</option>
          </select>
        </span>
        <h4>Settings like the above are <em>Coming soon</em></h4>
      </div>
      <!--iframe id="fav_target" name="fav_target" onload="dash.favSubmitted()"></iframe-->
    </div>
""" + uservoiceSrc

  val htmlBody2 = htmlBody2Gen("", "", loggedInFormsNScripts)
  val htmlBody2NoLoggedIn = htmlBody2Gen(noLoggedInTitle, noLoggedInMessage, "")

  def getViewPower (request:Request, response:HttpServletResponse):String = {
    response.setHeader("Cache-Control","no-cache")

    val (cookies, loginTokens) = WebApp.processLoginCookies(request.req)
    val userId = loginTokens.flatMap(token=>dbHelper.getUserIdFromToken(token._2, token._3)).firstOption
    val userScreenName = userId.map(dbHelper.getScreenNameFromUserId(_))
    val schemeId = cookies.get("scheme_id").map(_.toInt).getOrElse(CSS.defaultScheme)

    // if (userId.isDefined) {
      val out = response.getWriter

      if (loginTokens.length > 0) {
        out println htmlBody1Start
        out println """<script type="text/javascript">""" + WebApp.makeCreds(loginTokens, response) + """</script>"""
      } else {
        out println htmlBody1StartNoLogin
      }

      out println ("""<link rel="stylesheet" href="/css/date_input.css" type="text/css"/>""")
      out println ("""<link id="cssLink" href="/oauth/getCss?scheme_id=%d" type="text/css" rel="stylesheet" media="screen,projection" />""" format (schemeId))

      if (loginTokens.length > 0) {
        htmlBody2
      } else {
        htmlBody2NoLoggedIn
      }
  }

  def postSettingTimeOrder (request:Request, response:HttpServletResponse):String = {
    val (cookies, loginTokens) = WebApp.processLoginCookies(request.req)
    val loginNum = request.getIntParamOpt("loginId")
    val newValue = request.getParamOpt("value").map(_ == "true")

    if (loginNum.isDefined && newValue.isDefined) {
      val userId = loginTokens.find(_._1 == loginNum.get).flatMap(token=>dbHelper.getUserIdFromToken(token._2, token._3))
      if (userId.isDefined) {
        dbHelper.updateOldOnTopSetting(userId.get, newValue.get)
      }
    }

    ""
  }
}
