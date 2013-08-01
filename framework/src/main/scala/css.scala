package bhoot
import javax.servlet.http._

import Utils._
import UtilsServlet._

case class Scheme(name:String, colors:Array[String], background:String, statusBackground:String)

object CSS {

/*
      background      quickrt link,     "2c7385", modal overlay,
      "99a9a9",       "e7ffff",     "000",    aaa
      foregrnd bttn,  meta status,  hover ind,
      sel ind,        read ind,     "bfbfea", "900",
      modal bkg,    default_body
*/
  val ClassicScheme = Scheme("Classic",
    Array(
      "c8c8c8", "dbdbff", "707784", "fafafa",
      "999",    "fafafa", "a8b4c8", "444",
      "171717", "461446", "c8c8c8", "00004f",
      "aaa",    "900",    "f00",    "eee",
      "000",    "999",    "000",    "000"),
    "url('/images/noise2.jpg')", "")

  val SeaScheme = Scheme("The Sargasso Sea",
    Array(
      "0b486b", "f3ffff", "2c7385", "c9e8e8",
      "99a9a9", "e7ffff", "346179", "3f5059",
      "f3ffff", "0b486b", "c1d9d9", "0b486b",
      "aaa",    "900",    "f00",    "eee",
      "000",    "99a9a9", "346179", "000"),
    // "#d9f4f4")
    "url('/images/boating.png')", "")

  val SafariScheme = Scheme("African Safari",
    Array(
      "033649", "033649", "99773d", "fffaf0",
      "736a5a", "decdad", "034b65", "596982",
      "f3ffff", "183053", "d3c9b9", "033649",
      "aaa",    "900",    "f00",    "eee",
      "000",    "736a5a", "034b65", "000"),
    "#f0e5d2", "")

  val PapayaScheme = Scheme("Papaya Sundae",
    Array(
      "C4574E", "e5cada", "782e59", "fffaf0",
      "736a5a", "e7dcbe", "d88057", "E0CFA7",
      "f5e4b9", "8C004E", "E1DAC7", "1e1e44",
      "aaa",    "900",    "f00",    "eee",
      "000",    "736a5a", "782e59", "000"),
    "#F3EBD6", "")

  val SubwayScheme = Scheme("Subway",
    Array(
      "34532e", "1e1e44", "a12f1c", "000",
      "182615", "182615", "57994a", "182615",
      "f5e4b9", "366729", "351b1b", "B5E0AB",
      "533",    "f99",    "f00",    "34532e",
      "fff",    "d88057", "B5E0AB", "fff"),
    """url('/images/subway.jpg');
      -moz-background-size: 100% 100%;           /* Gecko 1.9.2 (Firefox 3.6) */
      -o-background-size: 100% 100%;           /* Opera 9.5 */
      -webkit-background-size: 100% 100%;           /* Safari 3.0 */
      -khtml-background-size: 100% 100%;           /* Konqueror 3.5.4 */
      -moz-border-image: url(/images/subway.jpg) 0;    /* Gecko 1.9.1 (Firefox 3.5) */""",
    """background: rgb(0, 0, 0);
       background: rgba(40, 10, 10, 0.8);""")

  val HelvetiScheme = Scheme("Helveti",
    Array(
      "eeeeee", "eeeeee", "888888", "eeeeee",
      "888888", "eeeeee", "888888", "eeeeee",
      "444444", "b70404", "d0d0d0", "444444",
      "eeeeee", "e91010", "e91010", "dddddd",
      "000",    "888888", "e91010", "b00404"),
    "#fbfbfb",
    "")

  val HelvetiBlueScheme = Scheme("Helveti Blue",
    Array(
      "eeeeee", "eeeeee", "888888", "eeeeee",
      "888888", "eeeeee", "888888", "eeeeee",
      "444444", "1533ad", "d0d0d0", "444444",
      "eeeeee", "1533ad", "1533ad", "dddddd",
      "000",    "888888", "1533ad", "1533ad"),
    "#fbfbfb",
    "")

  val impColors=12

  var cssCache = Map[Int,String]()


  val schemeMap = Array (ClassicScheme,SeaScheme, SafariScheme, PapayaScheme, SubwayScheme, HelvetiScheme, HelvetiBlueScheme)

  val defaultScheme = 6

  def getCss (request:Request, response:HttpServletResponse):String = {
    val schemeId = request.getIntParamOpt("scheme_id").getOrElse(defaultScheme)

    cssCache synchronized {
      if (!cssCache.isDefinedAt(schemeId)) {
        val cssStr = makeCSS(schemeId)
        cssCache += (schemeId -> cssStr)
        cssStr
      } else {
        cssCache(schemeId)
      }
    }

  }

  def renderSchemePreview (scheme:Scheme) = {
    ("""<div style="padding:4px;background:%s;">""" format (scheme.background)) +
    scheme.colors.take(impColors).map(c => """<span style="font-size:60px;background:#%s;">&nbsp;</span>""" format c).mkString +
    """</div>"""
  }

  val htmlStdStr = """
   <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
  <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
    <head>
      <meta http-equiv="Content-Type" content="text/html; charset=utf-8" /> 
  """
  private val htmlBody1 = """
    <title>tDash.org | Twitter Dashboard</title>
    <meta content="Change the theme of the tDash Twitter interface" name="description" />
    <meta content="Twitter browser client theme color configure" name="keywords" />
    <link href="/css/common.css" type="text/css" rel="stylesheet" media="screen,projection" />
    <script type="text/javascript">
      function clickScheme(id) {
        if (top) {
          if (top.changeCSS) {
            top.changeCSS('/oauth/getCss?scheme_id='+id);
          }
        }
      }
    </script>
    
  </head>
  <body>
"""
  def getChooseTheme (request:Request, response:HttpServletResponse):String = {
    val (cookies,loginTokens) = WebApp.processLoginCookies(request.req)

    val prevSchemeId = cookies.get("scheme_id").map(_.toInt).getOrElse(defaultScheme)
    // val prevSchemeId = request.getIntParamOpt("scheme_id").getOrElse(0)

    val isEmbedded = request.getParamOpt("embed").isDefined

    val embeddedStr = 
      if (isEmbedded) 
        """<input type="hidden" name="embed" value="true"/>"""
      else ""

    htmlStdStr + htmlBody1 + """<form method="post" action="/oauth/themeSave">""" + embeddedStr + """
  <div style="float:right"><input style="margin:1em 2em;" type="submit" value="Save Theme"/></div>
  <div style="overflow-y:scroll;height:450px;"><table style="width:100%;"><tbody>""" +
    schemeMap.zipWithIndex.map(scheme =>
      """<tr><td style="margin:2px;"><input type="radio" onclick="clickScheme(%d);" name="scheme_id" id="choice_%d" value="%d" %s/><label for="choice_%d">%s</label></td><td style="margin:2px 6px;">%s</td></tr>""" format (
        scheme._2, scheme._2, scheme._2, if (scheme._2 == prevSchemeId) "checked" else "",
        scheme._2, scheme._1.name, renderSchemePreview(scheme._1)
      )
    ).mkString +
  """</tbody></table></div></form>
  </body>
</html>"""
  }

  def postThemeSave (request:Request, response:HttpServletResponse):String = {
    val newSchemeId = request.getIntParamOpt("scheme_id").getOrElse(defaultScheme)
    val isEmbedded = request.getParamOpt("embed").isDefined

    val embeddedStr = 
      if (isEmbedded) 
        """<p>You can now close this window."""
      else ""

    WebApp.setCookie("scheme_id", newSchemeId.toString, WebApp.seconds90days, response)

    htmlStdStr + """
      <link href="/css/common.css" type="text/css" rel="stylesheet" media="screen,projection" />
    </head>
    <body>
      <h1>Saved</h1>
      <p>Your chosen theme (%s) has been associated with this computer.</p>%s
    </body>
  </html>""" format (schemeMap(newSchemeId).name, embeddedStr)
  }

  private def makeCSS(schemeIdArg:Int) = {
    val schemeId = if (schemeIdArg < schemeMap.length) schemeIdArg else 0
    val scheme = schemeMap(schemeId)
    val colors = scheme.colors

val result = """

body {
font-family:sans-serif;
margin:0;
background:""" + scheme.background + """;
color:#""" + colors(16) + """;
}

a {
color:#"""+colors(11)+""";
}

.status td p.text {
padding:0;
margin:0 0 8px 5px;
font-family:serif;
}

.status .metaStatus {
padding:0;
margin:2px 0 2px 5px;
font-size:90%;
font-style:italic;
color:#"""+colors(6)+""";
}

.metaStatus>a{
text-decoration:none;
color:#"""+colors(6)+""";
}

.read {
color:#"""+colors(2)+""";
}

.selected {
color:#"""+colors(19)+""";
}

.text {
font-size:110%;
}

.author {
cursor:pointer;
border-bottom:1px dotted #"""+colors(12)+""";
color:#"""+colors(2)+""";
}

#view {
margin:0;
padding:0;
}

#viewTbl {
width:100%;
}

.logo {
padding:8px 0 0 0;
text-decoration:none;
border:none;
}

.paddedLeftTd {
max-width:20%;
padding:0;
border-right:1px solid #"""+colors(4)+""";
}
.paddedLeft{
padding:0px 8px 0 8px;
/*margin:0 8px;*/
}

.paddedRightTd {
padding:0;
border-left:1px solid #"""+colors(3)+""";
}

.paddedRight{
/*margin:0 8px;*/
padding:0 8px 0px 8px;
-moz-border-radius:6px;
-webkit-border-radius:6px;
}

#topInfo {
font-style:italic;
font-family:serif;
margin:0;
}

.navElem {
padding:2px 5px;
margin:0;
font-size:80%;
color:#"""+colors(16)+""";
cursor:pointer;
}

.navElem:hover {
background:#"""+colors(5)+""";
color:#"""+colors(16)+""";
}

.navRead {
color:#"""+colors(2)+""";
padding:0px 5px;
font-size:75%;
}

.navSel {
background:#"""+colors(5)+""";
color:#"""+colors(18)+""";
font-weight:bold;
padding:3px 0;
}

#readerInfo {
background:#"""+colors(5)+""";
border:1px solid #"""+colors(0)+""";
-moz-border-radius:4px;
-webkit-border-radius:4px;
margin-bottom:16px;
padding:8px 4px;
}

#readerInfo>table{
width:100%;
}
#readerInfo tr {
vertical-align:middle;
}

#friendInfoTd {
padding:0 6px 0 16px;
}

#folderButtTd {
text-align:right;
}

#friendDescrTd {
font-family:serif;
font-style:italic;
padding:0 6px;
text-align:center;
}

#reader{
overflow-y:scroll;
}

.stTbl {
width:100%;
border-spacing:0 18px;
margin:-18px 0;
}

.topAlignTbl>tbody>tr {
vertical-align:top;
}

.centerAlignTbl>tbody>tr {
vertical-align:top;
}

.algnRight {
text-align:right;
}

#toolBar {
padding:10px 10px 10px 0;
text-align:right;
}

.toolButt {
padding:3px 6px 2px 6px;
background:#"""+colors(0)+""";
font-size:90%;
vertical-align:middle;
-moz-border-radius:5px;
-webkit-border-radius:5px;
color:#"""+colors(8)+""";
}

.quickRTLabel {
margin:0 0 2px 0;
padding:3px 6px 2px 6px;
background:#"""+colors(2)+""";
font-size:80%;
vertical-align:middle;
-moz-border-radius:5px;
-webkit-border-radius:5px;
color:#"""+colors(3)+""";
}

.quickRTLabel a {
color:#"""+colors(1)+""";
}
.quickRTLabel a:visited {
color:#"""+colors(1)+""";
}

#statusBar {
background:#"""+colors(0)+""";
color:#"""+colors(8)+""";
padding:0 5px;
text-align:center;
font-size:80%;
width:100%;
}

#fetchStatus, #fetchCount, #submitStatus, #versionStatus {
padding: 0 10px;
}
#submitStatus {
color:#"""+colors(8)+""";
}
#versionStatus {
text-align:right;
}

.createdAt {
padding:0 8px;
}

#updateInfo {
text-align:center;
}

#updateInput {
font-size:110%;
}
.charCount {
padding:0 5px;
margin:0;
font-size:110%;
color:#"""+colors(17)+""";
}
.charCount.alert{
color:#"""+colors(16)+""";
}
.charCount.warning{
color:#"""+colors(13)+""";
}
.charCount.error{
color:#"""+colors(14)+""";
}

#headCorner {
text-align:center;
}

#logInfo {
padding:6px 5px;
margin:0;
}
#logInfo .name {
font-size:80%;
}

.st_button {
padding:1px 5px;
background:#"""+colors(5)+""";
cursor:pointer;
margin:0 6px;
-moz-border-radius:2px;
-webkit-border-radius:2px;
color:#"""+colors(16)+""";
white-space:nowrap;
font-size:80%;
}

.buttWrap {
padding:0 0 6px 0;
}

.prof_img {
max-width:48px;
max-height:48px;
}

.indTd {
width:16px;
text-align:center;
padding:6px 0;
-moz-border-radius:3px;
-webkit-border-radius:3px;
}

.indRead {
background:#"""+colors(10)+""";
}

.indHover {
background:#"""+colors(7)+""";;
}

.indSel {
background:#"""+colors(9)+""";;
}

.imgTd {
width:64px;
text-align:center;
padding:0;
}

.buttTd {
width:15%;
padding:0;
}

.buttTd p {
margin:0;
}

.navTab {
margin:2px 0;
text-align:center;
border:2px solid #"""+colors(0)+""";
-moz-border-radius:3px;
-webkit-border-radius:3px;
}


.tabHead {
padding:2px 0;
background:#"""+colors(0)+""";;
color:#"""+colors(8)+""";
font-weight:bold;
margin:0;
text-align:center;
cursor:pointer;
/*text-shadow:1px 1px 0 #000;*/
}

.tabHead:hover {
background:#"""+colors(6)+""";;;
}

.linkNoDec {
text-decoration:none;
border:none;
}

#powerSelect.ogroup {
margin-top:10px;
padding-top:2px;
border-top:1px solid #"""+colors(2)+""";
}

#simplemodal-overlay {
background:#"""+colors(3)+""";
}

#simplemodal-container {
background:#"""+colors(15)+""";
border:6px solid #"""+colors(9)+""";
}

.simplemodal-data {
padding:8px;
}

#simplemodal-container a.modalCloseImg {
background:url(/images/x.png) no-repeat; /* adjust url as required */
width:25px;
height:29px;
display:inline;
z-index:3200;
position:absolute;
top:-15px;
right:-18px;
cursor:pointer;
}

#helpKeyShorts {
margin:10px 0;
}

#helpKeyShorts tr {
vertical-align:middle;
}

#helpKeyShorts tr>td {
border-bottom:1px solid #"""+colors(12)+""";
padding:5px 15px;
}

.helpKey {
font-weight:bold;
}

.replyTo {
margin:0 0 0 12px;
}

.replyToData {
font-style:italic;
font-family:serif;
padding:4px;
border-bottom:1px dotted #"""+colors(4)+""";
}

#trendData {
padding:8px;
}

#trendData>h2 {
margin:.1em 0;
font-size:120%;
}
#trendData>p{
margin:2px;
}
#trendDescr {
font-style:italic;
font-family:serif;
padding:.5em 0;
}
#trendReadBtn{
font-size:80%;
}
#wttAttrib{
font-size:90%;
font-style:italic;
float:right;
}

#updateButton {
vertical-align:middle;
}

#moreFriends {
border:1px solid #"""+colors(12)+""";
}

#moreButt {
cursor:pointer;
font-size:80%;
}

.xpanding {
background:#"""+colors(12)+""";
}
.xpanded {
background:#"""+colors(3)+""";
padding:0 3px;
font-size:90%;
}

.imgPreview {
padding:0;
margin:0;
}
.imgPreview li {
list-style-type:none;
display:inline;
padding:0 5px;
}
.imgPreview img {
padding:5px 0;
}

.noAuthTip {
font-size:80%;font-style:italic;
color:#"""+colors(4)+""";
}

.urlNumTd {
padding:0 20px;
font-weight:bold;
}

.initScreen {
text-align:center;
}

#showAuthTip {
background:#"""+colors(15)+""";
border-radius:4px;
-moz-border-radius:4px;
-webkit-border-radius:4px;
padding:5px;
}

.pointCursor {
cursor:pointer;
}

.horizDivider {
border-bottom:1px solid #"""+colors(3)+""";
margin-right:10px;
}

.horizDividerInner {
border-bottom:1px solid #"""+colors(4)+""";
}

.syncButton {
font-size:1.2em;
}

#logSelect {
font-size:120%;
text-align:center;
}

.bsap_1270076 {
margin:0.33em auto;
}
"""
  result
  }
}

