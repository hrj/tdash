package bhoot

import javax.servlet.http._
import Utils._
import UtilsServlet._

case class MyClientInfo(id:Int, name:String, nameXML:String, url:String, totalTweetCount:Long, replyPerc:Float)
case class MyHistory(rank:Int, totalPerc:Float, createdAt:java.sql.Timestamp)
case class MyHotClient(clientId:Int, change:Float)

object statCache {
  private var lastStatUpdate = 0L
  private var lastHistUpdate = 0L
  private var lastHotClientUpdate = 0L

  var stats:List[(MyClientInfo,Int)] = _
  var clientMap = scala.collection.mutable.Map[Int, (MyClientInfo,Int)]()
  var allStatsRendered:List[String] = _
  var totalTweets = 0L
  var totalClients = 0L
  var tDashRank = 0

  var hotClientsStr = ""

  def renderStat(infoRnk:(MyClientInfo, Int), updatetDashRank:Boolean, change:Option[Int]) = {
    val (info, rank) = infoRnk

    if (updatetDashRank) {
      if(info.name == "tDash") tDashRank = rank + 1
    }

    val replyType = getReplyType(info.totalTweetCount, info.replyPerc)

    """<tr class="%s"><td>%d</td><td><a href="/stats/client/%d">%s</a></td><td>%.2f %%</td><td>%s</td>%s</tr>""" format (
        replyClass(replyType),
        rank + 1,
        info.id,
        info.nameXML,
        info.totalTweetCount * 100.0 / totalTweets,
        replyTypeStr(replyType),
        change.map(x => "<td>&#8593;&nbsp;%d</td>" format x).getOrElse("")
      )
  }

  def update = {
    this synchronized {
      val now = System.currentTimeMillis

      if ((now - lastStatUpdate) > 20*60*1000L) {           // 20 mins
        val hotClients =
          if ((now - lastHotClientUpdate) > 6*60*60*1000L) {  // 6 hours
            lastHotClientUpdate = now
            println("getting hot clients")
            Some(dbHelper.getHotClients) // get hotclients before stats to ensure that each hot client is mapped into clientMap
          } else {
            None
          }

        println(now + ":Fetching stats")
        stats = dbHelper.getClientInfo.zipWithIndex
        clientMap.clear
        stats foreach {info => clientMap(info._1.id) = info}

        totalTweets = dbHelper.getTotalTweets
        allStatsRendered = stats.map(x => renderStat(x, true, None))

        lastStatUpdate = now
        totalClients = stats.length

        if (hotClients.isDefined) {
          hotClientsStr = hotClients.get.map {hotclient =>
            renderStat(clientMap(hotclient.clientId), false, Some(hotclient.change.toInt))
          }.reduceLeftOpt(_ + _).getOrElse("<td>No matches found! No clients changed their rank significantly.</td>")
        }

        if (lastHistUpdate == 0L) {
          lastHistUpdate = dbHelper.getMaxHistTimestamp.map(_.getTime).getOrElse(0L)
        } 

        if ((now - lastHistUpdate) > 24*60*60*1000L) {
          println("Updating history")
          stats foreach {x =>
            val (info, rank) = x
            dbHelper.insertStatHist(info.id, rank, (info.totalTweetCount*100.0 / totalTweets).toFloat)
          }

          lastHistUpdate = now
        }
      }
    }
  }

  val replyTypeStr = Array("-", "bot", "rarely", "often", "very often")
  val replyClass = Array("repUnk", "repBot", "repRarely", "repOften", "repVeryOften")

  def getReplyType(totalTweets:Long, perc:Float) = {
    if (totalTweets < 100) {
      0
    } else {
      if (perc < 1.0) {
        1
      } else if (perc < 25.0) {
        2
      } else if (perc < 50.0) {
        3
      } else {
        4
      }
    }
  }

}


object Stats {
  val clientsPerPage = 20

  def makePageStr(page:Int) = {
    val prevStr =
      if (page <= 0) {
        ""
      } else {
        """<a href="/stats/clients?page=%d">&lt;</a>""" format (page - 1)
      }

    val nextStr =
      if ((page > 100) || (page >= (statCache.totalClients / clientsPerPage))) {
        ""
      } else {
        """<a href="/stats/clients?page=%d">&gt;</a>""" format (page + 1)
      }

    prevStr + "&nbsp;Page&nbsp;" + (page + 1) + "&nbsp;" + nextStr
  }

  def getHotClients (request:Request, response:HttpServletResponse):String = {
    statCache.update

    val result = """
  <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
  <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
    <head>
      <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
      <title>Twitter Applications Statistics</title>
      <link rel="alternate" href="http://twitter.com/statuses/user_timeline/95156827.rss" title="tDash News" type="application/rss+xml" />
      <link href="/css/common.css" type="text/css" rel="stylesheet" media="screen,projection" />
      <link href="/css/stats.css" type="text/css" rel="stylesheet" media="screen,projection" />
      <meta content="Twitter application statistics" name="description" />
      <meta content="Twitter browser client status update dashboard AJAX tDash photos upload" name="keywords" />
    </head>
    <body>
    <table id="bodyTbl" cellspacing="0"><tbody>
      <tr>
        <td class="logoTd"><img src="/images/appStatsLogo.png" alt="Twitter app stats"/></td>
        <td class="contentTd">
          <h1>Hot Twitter clients</h1>
          <p>(that have risen up in the rankings in the last four days)</p>
    """ +
    ("<p><b>%.2f</b> million tweets indexed<br/>" format (statCache.totalTweets/1000000.0)) +
    ("<b>%d</b> clients detected</p>" format (statCache.totalClients)) +
    ("""
        </td>
      </tr>
      <tr>
        <td class="logoTd"></td>
        <td class="contentTd">
          <form action="/stats/clients" method="get" id="searchForm">Search<input type="text" name="query"/>
          </form>
        <p><a href="/stats/clients">View all clients</a></p>
        </td>
      </tr>
      <tr>
        <td class="logoTd">
          <p class="tagLine" style="font-size:80%%"><em>brought to you by</em></p>
          <a class="noBorderLink" href="/"><img class="logo" alt="logo" style="margin-top:50px" src="/images/logoColorSmall.png" /></a>
          <p class="tagLine"><em>a browser based twitter client</em></p>
          <p class="tagLine" style="font-size:80%%">currently ranked #%d among %d clients</p>
        </td>
        <td class="contentTd">
    """ format (statCache.tDashRank, statCache.totalClients)) +
    """<table id="statTbl"><thead><tr><th>Present rank</th><th>Name</th><th>% of total tweets</th><th>Used for replies</th><th>Change in rank</th></tr></thead><tbody>""" + 
    statCache.hotClientsStr +
"""
          </tbody></table>""" +
"""
          <div id="footer">
            <ul>
              <li class="menuItem"><a href="/about.html"><span class="menuLabel">About</span></a></li>

              <li class="menuItem"><a target="_blank" href="http://tdash.uservoice.com"><span class="menuLabel">Feedback</span></a></li>
                <li class="menuItem"><a target="_blank" href="/images/screenshotLarge4.png"><span class="menuLabel">Screenshot</span></a></li>
                <li class="menuItem"><a target="_blank" href="http://twitter.com/tdash"><span class="menuLabel">Follow-us</span></a></li>
              <li class="menuItem"><a href="/media.html"><span class="menuLabel">tDash in the Media</span></a></li>
            </ul>
          </div>
          <p id="copyright">&#169; 2009,2010 tDash.org</p>

        </td>
      </tr>
    </tbody></table></body></html>"""

    result
  }

  def getClient (request:Request, response:HttpServletResponse):String = {
    val appPath = request.appPath
    val idStr = appPath.drop(7).takeWhile(_.isDigit).foldLeft("")(_ + _)         /*      client/           */
    val clientId = if(idStr.length > 0) idStr.toInt else -1

    statCache.update

    val clientInfoOpt = statCache.clientMap.get(clientId)
    val clientName = clientInfoOpt.map(_._1.nameXML).getOrElse("Unknown client")
    val history = clientInfoOpt.map(info => dbHelper.getHistory(clientId)).getOrElse(Nil)

/*
      <!-- BuySellAds.com Ad Code -->
      <script type="text/javascript">
      (function(){
        var bsa = document.createElement('script');
           bsa.type = 'text/javascript';
           bsa.async = true;
           bsa.src = '//s3.buysellads.com/ac/bsa.js';
        (document.getElementsByTagName('head')[0]||document.getElementsByTagName('body')[0]).appendChild(bsa);
      })();
      </script>
      <!-- END BuySellAds.com Ad Code -->
*/

/*
          <div id="adFooter">
            <!-- BuySellAds.com Zone Code -->
            <div id="bsap_1246337" class="bsarocks bsap_dade2eb1bbfc3a41945ef1939328976e"></div>
            <!-- END BuySellAds.com Zone Code -->
          </div>
*/
    ("""
  <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
  <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
    <head>
      <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
      <title>%s | Detailed report</title>
      <link rel="alternate" href="http://twitter.com/statuses/user_timeline/95156827.rss" title="tDash News" type="application/rss+xml" />
      <link href="/css/common.css" type="text/css" rel="stylesheet" media="screen,projection" />
      <link href="/css/stats.css" type="text/css" rel="stylesheet" media="screen,projection" />
      <script src="/scripts/raphael-combined.js" type="text/javascript"></script>
      <script type="text/javascript">                                                                                      
        var rH = [%s];
       window.onload = function() {                                                                                       
         var r = Raphael("graphHolder"),                                                                                       
             fin = function () {                                                                                          
               this.flag = r.g.popup(this.bar.x, this.bar.y, this.bar.value || "0").insertBefore(this);                   
             },                                                                                                           
             fout = function () {                                                                                         
               this.flag.animate({opacity: 0}, 200, function () {this.remove();});                                        
             };                                                                                                           
                                                                                                                          
         r.g.barchart(10, 10, 300, 220, [rH]).hover(fin, fout);                        
       }                                                                                                                  
     </script>
      <meta content="Detailed report of Twitter application %s" name="description" />
      <meta content="Twitter application client detailed report %s" name="keywords" />
    </head>
    <body>
    <table id="bodyTbl" cellspacing="0"><tbody>
      <tr>
        <td class="logoTd"><img src="/images/appStatsLogo.png" alt="Twitter app stats"/></td>
        <td class="contentTd">
          <h1>Stats for <a target="_blank" href="%s" rel="nofollow">%s</a></h1>
          <p>%.4f%% of total tweets</p>
          <p>Currently ranked #%d among %d clients</p>
    """ format (
          clientName,
          history.map(_.rank + 1).mkString(","),
          clientName, clientName,
          clientInfoOpt.map(_._1.url).getOrElse("/stats/clients"),
          clientName,
          clientInfoOpt.map(_._1.totalTweetCount * 100.0 / statCache.totalTweets).getOrElse(0.0),
          clientInfoOpt.map(_._2 + 1).getOrElse(0), statCache.totalClients
        )) +
     ("""
          <a href="/stats/clients">View all clients</a>
        </td>
      </tr>
      <tr>
        <td class="logoTd">
          <p class="tagLine" style="font-size:80%%"><em>brought to you by</em></p>
          <a class="noBorderLink" href="/"><img class="logo" alt="logo" style="margin-top:50px" src="/images/logoColorSmall.png" /></a>
          <p class="tagLine"><em>a browser based twitter client</em></p>
          <p class="tagLine" style="font-size:80%%">currently ranked #%d among %d clients</p>
        </td>
        <td class="contentTd">
          <p>Rank history<br/>(Smaller is better)</p>
          <div id="graphHolder" style="width:350px;margin:0 auto;"></div>
     """ format (statCache.tDashRank, statCache.totalClients)) +
    ("<p><b>%.2f</b> million tweets indexed</p>" format (statCache.totalTweets/1000000.0)) +
    """
        </td>
      </tr>
      <tr>
        <td class="logoTd">
        </td>
        <td class="contentTd">
          <div id="footer">
            <ul>
              <li class="menuItem"><a href="/about.html"><span class="menuLabel">About</span></a></li>

              <li class="menuItem"><a target="_blank" href="http://tdash.uservoice.com"><span class="menuLabel">Feedback</span></a></li>
                <li class="menuItem"><a target="_blank" href="/images/screenshotLarge4.png"><span class="menuLabel">Screenshot</span></a></li>
                <li class="menuItem"><a target="_blank" href="http://twitter.com/tdash"><span class="menuLabel">Follow-us</span></a></li>
              <li class="menuItem"><a href="/media.html"><span class="menuLabel">tDash in the Media</span></a></li>
            </ul>
          </div>
          <p id="copyright">&#169; 2009,2010 tDash.org</p>

        </td>
      </tr>
    </tbody></table></body></html>"""
  }

  def getClients (request:Request, response:HttpServletResponse):String = {
    val page = request.getIntParamOpt("page").getOrElse(0)
    val query = request.getParamOpt("query")


    statCache.update

    var queryMsg = ""

    val matchData =
      if (query.isDefined) {

        val queryTxt = query.get

        val encodedQuery = scala.xml.parsing.ConstructingParser.fromSource(scala.io.Source.fromString("<dummy>"+queryTxt+"</dummy>"), false).document.apply(0).text

        val decodedQuery = encodedQuery.toLowerCase.map(x => if (x > 256) "\\u%x" format x.toInt else "%c" format x).reduceLeft(_ + _)
        
        val results =
          if(queryTxt.length > 2) {
            val matches = statCache.stats.filter(_._1.name.toLowerCase contains decodedQuery).map(x => statCache.renderStat(x, false, None))
            queryMsg = "<p>%d client(s) matched your search</p>" format (matches.length)
            matches
          } else {
            queryMsg = "<p>Please specify atleast three characters for search</p>"
            Nil
          }

        queryMsg += """<p><a href="/stats/clients">View all clients</a></p>"""

        results
      } else {
        queryMsg += """<p><a href="/stats/clients/hot">View clients that are rising in popularity</a></p>"""
        statCache.allStatsRendered.drop(page*clientsPerPage).take(clientsPerPage)
      }

    val result =
    """
  <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
  <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
    <head>
      <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
      <title>Twitter Applications Statistics</title>
      <link rel="alternate" href="http://twitter.com/statuses/user_timeline/95156827.rss" title="tDash News" type="application/rss+xml" />
      <link href="/css/common.css" type="text/css" rel="stylesheet" media="screen,projection" />
      <link href="/css/stats.css" type="text/css" rel="stylesheet" media="screen,projection" />
      <meta content="Twitter application statistics" name="description" />
      <meta content="top list Twitter browser client status update dashboard AJAX tDash photos upload" name="keywords" />
    </head>
    <body>
    <table id="bodyTbl" cellspacing="0"><tbody>
      <tr>
        <td class="logoTd"><img src="/images/appStatsLogo.png" alt="Twitter app stats"/></td>
        <td class="contentTd">
          <h1>Top Twitter clients</h1>
    """ +
    ("<p><b>%.2f</b> million tweets indexed<br/>" format (statCache.totalTweets/1000000.0)) +
    ("<b>%d</b> clients detected</p>" format (statCache.totalClients)) +
    ("""
        </td>
      </tr>
      <tr>
        <td class="logoTd"></td>
        <td class="contentTd">
          <form action="/stats/clients" method="get" id="searchForm">Search<input type="text" name="query"/>
          </form>""" + queryMsg +
"""
        </td>
      </tr>
      <tr>
        <td class="logoTd">
          <p class="tagLine" style="font-size:80%%"><em>brought to you by</em></p>
          <a class="noBorderLink" href="/"><img class="logo" alt="logo" style="margin-top:50px" src="/images/logoColorSmall.png" /></a>
          <p class="tagLine"><em>a browser based twitter client</em></p>
          <p class="tagLine" style="font-size:80%%">currently ranked #%d among %d clients</p>
        </td>
        <td class="contentTd">
    """ format (statCache.tDashRank, statCache.totalClients)) +
    """<table id="statTbl"><thead><tr><th>Rank</th><th>Name</th><th>% of total tweets</th><th>Used for replies</th></tr></thead><tbody>""" + 
    matchData.reduceLeftOpt(_ + _).getOrElse("<td>No matches found!</td>") +
"""
          </tbody></table>""" +
          query.map(x => "").getOrElse("""<p id="pageNum">""" + makePageStr(page) + """</p>""") +
"""
          <div id="footer">
            <ul>
              <li class="menuItem"><a href="/about.html"><span class="menuLabel">About</span></a></li>

              <li class="menuItem"><a target="_blank" href="http://tdash.uservoice.com"><span class="menuLabel">Feedback</span></a></li>
                <li class="menuItem"><a target="_blank" href="/images/screenshotLarge4.png"><span class="menuLabel">Screenshot</span></a></li>
                <li class="menuItem"><a target="_blank" href="http://twitter.com/tdash"><span class="menuLabel">Follow-us</span></a></li>
              <li class="menuItem"><a href="/media.html"><span class="menuLabel">tDash in the Media</span></a></li>
            </ul>
          </div>
          <p id="copyright">&#169; 2009,2010 tDash.org</p>

        </td>
      </tr>
    </tbody></table></body></html>"""

    result
  }
}
