/* Copyright : HRJ
  This is code that I am borrowing from an open-source project of mine.
  License is not an issue. I will make it an Apache kind of license.
*/

package bhoot

import javax.servlet._
import javax.servlet.http._

case class Request(req:HttpServletRequest, headers:Map[String,String], params:Map[String,Seq[String]], appPath:String) {
  def getParamOpt(name:String) = params.get(name).flatMap{p => 
    if (p.first.length > 0)
      Some(p.first)
    else None
  }
  def getParam(name:String) = params(name)(0)
  def getIntParam(name:String) = params(name)(0).toInt
  def getIntParamOpt(name:String) = getParamOpt(name).map(_.toInt)
}

case class UnquotedString(s:String)

private object servletInstance {
  var initParms:Map[String,String] = _
}

object Utils {
  import scala.xml._

  implicit def toReduceLeftOpt[A](s: => Seq[A]) = new {
    def reduceLeftOpt[B >: A](f : (B, A) => B) : Option[B] = {
      if (s.isEmpty) {
        None
      } else {
        Some(s.reduceLeft(f))
      }
    }
  }

  def makeAcronym(title:String,acronym:String) = <acronym title={title}>{acronym}</acronym>
  def makeDefinition(title:String,defn:String) = <acronym title={title}>{defn}</acronym>

  def makeParagraphs(str:String) = {
    str split ("\n") map (x => <p>{x}</p>)
  }

  val emdash = scala.xml.Unparsed("&#8212;")
  val ellipsis = scala.xml.Unparsed("&#8230;")

  def getArrayInteger(arr:java.sql.Array) = {
    assert (arr.getBaseType == java.sql.Types.INTEGER)
    val javaArray = arr.getArray
    javaArray.asInstanceOf[Array[java.lang.Integer]].map(_.intValue)
  }

  def getArrayString(arr:java.sql.Array) = {
    assert (arr.getBaseType == java.sql.Types.VARCHAR)
    val javaArray = arr.getArray
    javaArray.asInstanceOf[Array[java.lang.String]]
  }

  /** Helper for extracting data from a Rowset */
  def extractResults[T](resultSet:java.sql.ResultSet, f:java.sql.ResultSet => T, max:Option[Int]):Tuple2[List[T],Int] = {
    if (resultSet.next) {
      var items:List[T] = Nil
      var keepGoing = true
      var count = 0
      var countOk = true
      while (keepGoing && countOk) {
        items ::= f(resultSet)
        count += 1

        if (max.isDefined && (count >= max.get))
          countOk = false

        keepGoing = resultSet.next
      }
       // continue counting
      while(keepGoing) {
        count += 1
        keepGoing = resultSet.next
      }
      (items, count)
    } else {
      (Nil,0)
    }
  }

  /** Helper for processing data from a Rowset */
  def forEachResult(resultSet:java.sql.ResultSet, f:java.sql.ResultSet => Unit, max:Option[Int]):Unit = {
    if (resultSet.next) {
      var keepGoing = true
      var count = 0
      var countOk = true
      while (keepGoing && countOk) {
        f(resultSet)
        count += 1

        if (max.isDefined && (count >= max.get))
          countOk = false

        keepGoing = resultSet.next
      }
       // continue counting
      while(keepGoing) {
        count += 1
        keepGoing = resultSet.next
      }
    }
  }

  /** extractResults preserving the order */
  def extractResultsOrd[T](resultSet:java.sql.ResultSet, f:java.sql.ResultSet => T, max:Option[Int]):Tuple2[List[T],Int] = {
    val (items, count) = extractResults(resultSet, f, max)
    (items.reverse, count)
  }

  def quote(s:String) = '"' + s.replaceAll("\n", "\\\\n").replaceAll("\"", "\\\\\"") + '"'

  // make a Scala map into a JSON dictionary (object)
  def jsonify [A,B](m:Map[A,B]) = {
    "{" + 
    m.map(e => quote(e._1.toString) + ": " + 
      (
        e._2 match {
          case b:Boolean => b
          case UnquotedString(s) => s
          case i:Int => i.toString
          case a:Array[Any] => "[" + a.map(_.toString).reduceLeftOption(_ + "," + _).getOrElse("") + "]"
          case s:Seq[_] => "[" + s.map(_.toString).reduceLeftOption(_ + "," + _).getOrElse("") + "]"
          case x => quote(x.toString)
        }
      )
    ).reduceLeft(_ + ",\n" + _) +
    "}"
  }

  def jsonify [A](s:Seq[A]) = {
    UnquotedString(
      "[" + 
        s.map(_.toString).reduceLeftOpt(_ + "," + _).getOrElse("") +
      "]"
    )
  }

  def makeHtml(title:String, description:String, keywords:String, headNodes:Node, body:Node) = {
    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
      <head>
        <meta http-equiv="Content-Type" content="application/xhtml+xml; charset=UTF-8"></meta>
        <meta http-equiv="X-UA-Compatible" content="IE=8" />
        <title>{title}</title>
        <meta name="description" content={description}></meta>
        <meta name="keywords" content={keywords} ></meta>

        {headNodes.child}
      </head>

      {body}

    </html>
  }

  def sequenceToXML(children: Seq[Node], pscope:NamespaceBinding, stripComment: Boolean, writer:java.io.OutputStreamWriter) {
    if (children.isEmpty) {
      return
    } else if (children forall { 
      case y: Atom[_] => !y.isInstanceOf[Text] 
      case _ => false
    }) { // add space
      val it = children.elements
      val f = it.next
      writeXML(f, pscope, stripComment, writer)
      while (it.hasNext) {
        val x = it.next
        writer.write(' ')
        writeXML(x, pscope, stripComment, writer)
      }
    } else {
      for (c <- children) writeXML(c, pscope, stripComment, writer)
    }
  }

  def writeXML(x: Node, pscope:NamespaceBinding, stripComment: Boolean, writer:java.io.OutputStreamWriter) {
    x match {

      case c: Comment if !stripComment =>
        writer.write(c.buildString(stripComment))

      case s: SpecialNode =>
        writer.write(s.buildString(stripComment))

      case g: Group =>
        for (c <- g.nodes) writeXML(c, x.scope, stripComment, writer)

      case _  =>
        // print tag with namespace declarations
        writer.write('<')
        writer.write(x.label)

        if (x.attributes ne null) writer.write(x.attributes.toString)
        writer.write(x.scope.buildString(pscope))
        writer.write('>')
        sequenceToXML(x.child, x.scope, stripComment, writer)
        writer.write("</")
        writer.write(x.label)
        writer.write('>')
    }
  }
}

object UtilsServlet {
  def initParms = servletInstance.initParms

  def renderError(sc:Int)(request:Request, response:HttpServletResponse):Unit = {
    response.sendError(sc,"")
  }

  // Only sets the content type. Actual work is done by 'hndlr'
  def wrapHtmlDynamic(hndlr:Bootup#Handler)(request:Request, response:HttpServletResponse):Unit = {
    response.setContentType("text/html")
    hndlr(request,response)
  }
  def wrapXhtmlDynamic(hndlr:Bootup#Handler)(request:Request, response:HttpServletResponse):Unit = {
    response.setContentType("application/xhtml+xml")
    hndlr(request,response)
  }

  def renderCSS(output:String)(request:Request, response:HttpServletResponse):Unit = {
    response.setContentType("text/css")
    val out = response.getWriter
    out println output
  }

  def renderCSSDynamic(hndlr:Bootup#Handler)(request:Request, response:HttpServletResponse):Unit = {
    val output  = hndlr(request, response)

    renderCSS(output)(request,response)
  }


  def renderHtml(output:String)(request:Request, response:HttpServletResponse):Unit = {
    response.setContentType("text/html")
    val out = response.getWriter
    out println output
  }

  def renderHtmlDynamic(hndlr:Bootup#Handler)(request:Request, response:HttpServletResponse):Unit = {
    val output  = hndlr(request, response)

    // make sure this is not cached
    response.addHeader("Cache-Control", "no-cache")
    response.addHeader("Pragma", "no-cache")

    renderHtml(output)(request,response)
  }

  def renderJson(output:String)(request:Request, response:HttpServletResponse):Unit = {
    response.setContentType("application/json")

    val out = response.getWriter
    out println output
  }

  def renderJsonDynamic(hndlr:Bootup#Handler)(request:Request, response:HttpServletResponse):Unit = {
    try {
      val output  = hndlr(request, response)

      // make sure this is not cached
      response.addHeader("Cache-Control", "no-cache")
      response.addHeader("Pragma", "no-cache")

      renderJson(output)(request,response)
    } catch {
      case e => e.printStackTrace;println(e);renderJson("{'success':false}")(request,response)
    }
  }

  def renderJS(output:String)(request:Request, response:HttpServletResponse):Unit = {
    response.setContentType("text/javascript")
    val out = response.getWriter
    out println output
  }

  def renderJSDynamic(hndlr:Bootup#Handler)(request:Request, response:HttpServletResponse):Unit = {
    val output  = hndlr(request, response)
    renderJson(output)(request,response)
  }

  def redirect(url:String)(request:Request, response:HttpServletResponse):Unit = {
    response.sendRedirect(url)
  }

  def pureRedirect(hndlr:Bootup#Handler)(request:Request, response:HttpServletResponse):Unit = {
    val output  = hndlr(request, response)
    response.sendRedirect(output)
  }
}

object postDisable {
  var lock = false;

  def setPostDisable = {
    this synchronized {
      lock = true;
    }
  }
}


class Bootup extends HttpServlet {
  type Handler = ((Request,HttpServletResponse) => String)
  type XMLHandler = ((Request,HttpServletResponse) => String)
  type UnitHandler = ((Request,HttpServletResponse) => Unit)

  def defaultHandler (request:Request, response:HttpServletResponse):String = {
    response.sendError(HttpServletResponse.SC_NOT_FOUND)
    val output = <html>
      <head>
      <title>Error</title>
      </head>
      <body bgcolor="white">
      <h2>Error:This is the default handler</h2>
      {request.headers}
      <ul>
        <li><b>method</b>: {request.req.getMethod}</li>
        <li><b>fullPath</b>: {request.req.getPathInfo}</li>
        <li><b>appPath</b>: {request.appPath}</li>
        <li><b>queryString</b>: {request.req.getQueryString}</li>
      </ul>
      </body>
    </html>

    output.toString
  }

  override def doPost (request:HttpServletRequest, response:HttpServletResponse):Unit = {
    postDisable synchronized {
      if (!postDisable.lock) {
        val headers = getHeaders(request)

        /*
        val pathInfo = request.getPathInfo
        // Application path is got by stripping off the servlet path
        val numDropPath = 0
        val appPath = (pathInfo split("/") filter (_.length > 0) drop (numDropPath)).foldLeft("")((l:String,r:String) => l + "/" + r).drop(1)
        */
        val appPath = request.getPathInfo drop (1)

        // now find out which function will deal with this
        val (handler,paramProcess) = findPostHandler (appPath)
        handler(
          Request(
            request,
            headers,
            if (paramProcess) getParameters(request) else Map[String,List[String]](),
            appPath),
          response)
      }
    }
  }

  override def doGet(request:HttpServletRequest, response:HttpServletResponse):Unit = {
    val headers = getHeaders(request)

    val pathInfo = request.getPathInfo
    /*
    // Application path is got by stripping off the servlet path
    val numDropPath = 0
    val appPath = (pathInfo split("/") filter (_.length > 0) drop (numDropPath)).foldLeft("")((l:String,r:String) => l + "/" + r).drop(1)
    */
    val appPath = if (pathInfo != null) (pathInfo drop (1)).toString else ""

    // now find out which function will deal with this
    val handler = findGetHandler (appPath)
    handler(Request(request,headers,getParameters(request), appPath), response)
  }

  override def init():Unit = {
    servletInstance.initParms = getInitParams(this)
  }

  private def findPostHandler(appPath:String):(UnitHandler,Boolean) = {
    val rawHandler = WebApp.postRawHandlerMap find (appPath matches _._1)
    if (rawHandler.isDefined) {
      (rawHandler map(_._2) getOrElse (UtilsServlet.renderError(HttpServletResponse.SC_NOT_FOUND)), false)
    } else {
      val handler = WebApp.postHandlerMap find (appPath matches _._1)
      (handler map(_._2) getOrElse (UtilsServlet.renderError(HttpServletResponse.SC_NOT_FOUND)), true)
    }
  }

  private def findGetHandler(appPath:String):UnitHandler = {
    val handler = WebApp.getHandlerMap find (appPath matches _._1)
    handler map(_._2) getOrElse (UtilsServlet.renderError(HttpServletResponse.SC_NOT_FOUND))
  }

  private def getHeaders(request:HttpServletRequest) = {/*{{{*/
    // Make the header data available to our handlers
    val headerNames = request.getHeaderNames
    var headers = Map[String,String]()
    while(headerNames.hasMoreElements) {
      val name = headerNames.nextElement.toString
      headers += (name.toLowerCase -> request.getHeader(name))
    }

    headers
  }/*}}}*/

  private def getParameters(request:HttpServletRequest) = {/*{{{*/
    val paramNames = request.getParameterNames
    var params = Map[String,List[String]]()
    while (paramNames.hasMoreElements) {
      val name = paramNames.nextElement.toString
      params += (name -> request.getParameterValues(name).toList)
    }

    params
  }/*}}}*/

  private def getInitParams(servlet:HttpServlet) = {/*{{{*/
    val paramNames = servlet.getInitParameterNames
    var params = Map[String,String]()
    while (paramNames.hasMoreElements) {
      val name = paramNames.nextElement.toString
      params += (name -> servlet.getInitParameter(name))
    }

    params
  }/*}}}*/

  private def wildCardToRe(wcStr : String) = wcStr replace ("*",".*") replace ("?",".")

}
