package bhoot

import Utils._

case class Upload(id:Int, userId:Int, creator:String, createdAt:java.util.Date, viewCount:Int, viewLoggedCount:Int, descr:String)
case class Comment(screenName:String, comment:String)

/** Evertying related to the database */
object dbHelper {     
  import java.sql.ResultSet

  private lazy val connection = java.sql.DriverManager getConnection("jdbc:postgresql:tdash","postgres","xyz")
  // private lazy val twinkleConnection = java.sql.DriverManager getConnection("jdbc:postgresql:twinkle","postgres","xyz")

  private lazy val connectionManualCommmit = {
    val conn = java.sql.DriverManager getConnection("jdbc:postgresql:tdash","postgres","xyz")
    conn.setAutoCommit(false)
    conn
  }

  private def makeFetchFwdStmt(query:String) = {
    connection.prepareStatement (
      query,
      ResultSet.FETCH_FORWARD,
      ResultSet.TYPE_FORWARD_ONLY)
  }

  private def todayDate = {
    new java.sql.Date((new java.util.Date).getTime)
  }

  private def timeNow = {
    new java.sql.Timestamp((new java.util.Date).getTime)
  }

  //////////////////////////
  private lazy val insertNewUserStmt = {
    val queryStr = """
      INSERT into users
      (user_id, screen_name, followerUpdated)
      SELECT  ?, ?, '1-1-1980'
      WHERE NOT EXISTS (SELECT null FROM users WHERE user_id=?);
    """
    
    connection.prepareStatement(queryStr)
  }
  private lazy val updateUserNameStmt = {
    val queryStr = """
      UPDATE users
      SET screen_name = ?
      WHERE user_id=?;
    """
    
    connection.prepareStatement(queryStr)
  }

  def insertNewUser(user_id:Int, screen_name:String) = {
    val numInsertions =
      insertNewUserStmt synchronized {
        insertNewUserStmt.setInt(1,user_id)
        insertNewUserStmt.setString(2,screen_name)
        insertNewUserStmt.setInt(3,user_id)
        insertNewUserStmt.execute
        insertNewUserStmt.getUpdateCount
      }

    if (numInsertions != 1) {
      // update the username assuming it failed because of existing entry
      updateUserNameStmt synchronized {
        updateUserNameStmt.setString(1, screen_name)
        updateUserNameStmt.setInt(2, user_id)
        updateUserNameStmt.execute
      }
    }
  }

  //////////////////////////
  private lazy val insertNewOAuthStmt = {
    val queryStr = """
      INSERT into oauth_tokens
      (user_id, oauth_token, oauth_token_secret, oauth_verifier, created_at)
      SELECT  ?, ?, ?, ?, 'today'
      WHERE NOT EXISTS (SELECT null FROM oauth_tokens where (user_id=?) AND (oauth_token=?) AND (oauth_token_secret=?));
    """
    
    connection.prepareStatement(queryStr)
  }

  def insertNewOAuth(user_id:Int, screen_name:String, oauth_token:String, oauth_token_secret:String, oauth_verifier:String) = {
    insertNewUser(user_id, screen_name)

    insertNewOAuthStmt synchronized {
      insertNewOAuthStmt.setInt(1,user_id)
      insertNewOAuthStmt.setString(2,oauth_token)
      insertNewOAuthStmt.setString(3,oauth_token_secret)
      insertNewOAuthStmt.setString(4,oauth_verifier)
      insertNewOAuthStmt.setInt(5,user_id)
      insertNewOAuthStmt.setString(6,oauth_token)
      insertNewOAuthStmt.setString(7,oauth_token_secret)
      insertNewOAuthStmt.execute
      insertNewOAuthStmt.getUpdateCount
    }
  }

  //////////////////////////
  private lazy val insertNewAndroidOAuthStmt = {
    val queryStr = """
      INSERT into android_oauth_tokens
      (user_id, oauth_token, oauth_token_secret, oauth_verifier, created_at)
      SELECT  ?, ?, ?, ?, 'today'
      WHERE NOT EXISTS (SELECT null FROM android_oauth_tokens where (user_id=?) AND (oauth_token=?) AND (oauth_token_secret=?));
    """
    
    connection.prepareStatement(queryStr)
  }

  def insertNewAndroidOAuth(user_id:Int, oauth_token:String, oauth_token_secret:String, oauth_verifier:String) = {

    insertNewAndroidOAuthStmt synchronized {
      insertNewAndroidOAuthStmt.setInt(1,user_id)
      insertNewAndroidOAuthStmt.setString(2,oauth_token)
      insertNewAndroidOAuthStmt.setString(3,oauth_token_secret)
      insertNewAndroidOAuthStmt.setString(4,oauth_verifier)
      insertNewAndroidOAuthStmt.setInt(5,user_id)
      insertNewAndroidOAuthStmt.setString(6,oauth_token)
      insertNewAndroidOAuthStmt.setString(7,oauth_token_secret)
      insertNewAndroidOAuthStmt.execute
      insertNewAndroidOAuthStmt.getUpdateCount
    }
  }

  //////////////////////////
  private lazy val insertNewUploadStmt = {
    val queryStr = """
      INSERT into uploads
              (id, user_id, created_at,       descr, image_name, image_type, view_count, view_logged_count, cloud, safe)
      VALUES  (?,  ?,       CURRENT_TIMESTAMP,?,    ?,          ?,          0,          0,                 false, false);
    """
    
    connection.prepareStatement(queryStr)
  }

  def insertNewUpload(id:Int, user_id:Int, descr:String, image_name:String, image_type:String) = {
    insertNewUploadStmt synchronized {
      insertNewUploadStmt.setInt(1,id)
      insertNewUploadStmt.setInt(2,user_id)
      insertNewUploadStmt.setString(3,descr)
      insertNewUploadStmt.setString(4,image_name)
      insertNewUploadStmt.setString(5,image_type)
      insertNewUploadStmt.execute
      insertNewUploadStmt.getUpdateCount
    }
  }

  //////////////////////////
  private lazy val insertNewCommentStmt = {
    val queryStr = """
      INSERT into up_comments
              (upload_id, user_id, created_at,      comment_txt)
      VALUES  (?,         ?,       timestamp 'now', ?);
    """
    
    connection.prepareStatement(queryStr)
  }

  def insertNewComment(uploadId:Int, userId:Int, comment:String) = {
    insertNewCommentStmt synchronized {
      insertNewCommentStmt.setInt(1,uploadId)
      insertNewCommentStmt.setInt(2,userId)
      insertNewCommentStmt.setString(3,comment)
      insertNewCommentStmt.execute
      insertNewCommentStmt.getUpdateCount
    }
  }

  //////////////////////////
  private lazy val insertNewFollowerStmt = {
    val queryStr = """
      INSERT into followers
      (user_id, follower_id)
      VALUES (?, ?);
    """
    
    connection.prepareStatement(queryStr)
  }

  def insertFollower(userId:Int, followerId:Int) = {
    insertNewFollowerStmt synchronized {
      insertNewFollowerStmt.setInt(1,userId)
      insertNewFollowerStmt.setInt(2,followerId)
      insertNewFollowerStmt.execute
      insertNewFollowerStmt.getUpdateCount
    }
  }

  //////////////////////////
  private lazy val clearFollowersStmt = {
    val queryStr = """
      DELETE FROM followers
      WHERE (user_id = ?);
    """
    
    connection.prepareStatement(queryStr)
  }

  def clearFollowers(userId:Int) = {
    clearFollowersStmt synchronized {
      clearFollowersStmt.setInt(1,userId)
      clearFollowersStmt.execute
      clearFollowersStmt.getUpdateCount
    }
  }


  //////////////////////////
  private lazy val removeKeyStmt = {
    val queryStr = """
      DELETE FROM oauth_tokens
      WHERE (user_id = ?) AND (oauth_token=?) AND (oauth_token_secret=?);
    """
    
    connection.prepareStatement(queryStr)
  }

  def removeKey(userId:Int, tok:String, tokSecret:String) = {
    removeKeyStmt synchronized {
      removeKeyStmt.setInt(1,userId)
      removeKeyStmt.setString(2,tok)
      removeKeyStmt.setString(3,tokSecret)
      removeKeyStmt.execute
      removeKeyStmt.getUpdateCount
    }
  }

  //////////////////////////
  private lazy val getImgCountStmt = {
    val queryStr = """
      SELECT count(id)
      FROM uploads;
    """
    
    connection.prepareStatement(queryStr)
  }

  def getImgCount = {
    getImgCountStmt synchronized {
      val result = extractResults(getImgCountStmt.executeQuery, {row => row.getInt(1)}, None)

      result._1.first
    }
  }

  //////////////////////////
  private lazy val getUserCountStmt = {
    val queryStr = """
      SELECT count(user_id)
      FROM oauth_tokens;
    """
    
    connection.prepareStatement(queryStr)
  }

  def getUserCount = {
    getUserCountStmt synchronized {
      val result = extractResults(getUserCountStmt.executeQuery, {row => row.getInt(1)}, None)

      result._1.first
    }
  }

  //////////////////////////
  private lazy val getUserIdFromTokenStmt = {
    val queryStr = """
      SELECT user_id
      FROM oauth_tokens
      WHERE (oauth_token = ?) AND (oauth_token_secret = ?);
    """
    
    connection.prepareStatement(queryStr)
  }

  def getUserIdFromToken(oauth_token:String, oauth_token_secret:String) = {
    getUserIdFromTokenStmt synchronized {
      getUserIdFromTokenStmt.setString(1,oauth_token)
      getUserIdFromTokenStmt.setString(2,oauth_token_secret)
      val result = extractResults(getUserIdFromTokenStmt.executeQuery, {row => row.getInt(1)}, None)

      result._1.firstOption
    }
  }

  //////////////////////////
  private lazy val getAndroidUserIdFromTokenStmt = {
    val queryStr = """
      SELECT user_id
      FROM android_oauth_tokens
      WHERE (oauth_token = ?) AND (oauth_token_secret = ?) AND (oauth_verifier = ?);
    """
    
    connection.prepareStatement(queryStr)
  }

  def getAndroidUserIdFromToken(oauth_token:String, oauth_token_secret:String, oauth_verifier:String) = {
    getAndroidUserIdFromTokenStmt synchronized {
      getAndroidUserIdFromTokenStmt.setString(1,oauth_token)
      getAndroidUserIdFromTokenStmt.setString(2,oauth_token_secret)
      getAndroidUserIdFromTokenStmt.setString(3,oauth_verifier)
      val result = extractResults(getAndroidUserIdFromTokenStmt.executeQuery, {row => row.getInt(1)}, None)

      result._1.firstOption
    }
  }

  //////////////////////////
  private lazy val getScreenNameFromTokenStmt = {
    val queryStr = """
      SELECT screen_name
      FROM oauth_tokens, users
      WHERE (oauth_token = ?) AND (oauth_token_secret = ?) AND (oauth_tokens.user_id = users.user_id);
    """
    
    connection.prepareStatement(queryStr)
  }

  def getScreenNameFromToken(oauth_token:String, oauth_token_secret:String) = {
    getScreenNameFromTokenStmt synchronized {
      getScreenNameFromTokenStmt.setString(1,oauth_token)
      getScreenNameFromTokenStmt.setString(2,oauth_token_secret)
      val result = extractResults(getScreenNameFromTokenStmt.executeQuery, {row => row.getString(1)}, None)

      result._1.firstOption
    }
  }

  private lazy val getUserIdFromScreenNameStmt = {
    val queryStr = """
      SELECT user_id
      FROM users
      WHERE (screen_name = ?);
    """
    
    connection.prepareStatement(queryStr)
  }

  def getUserIdFromScreenName(screenName:String) = {
    getUserIdFromScreenNameStmt synchronized {
      getUserIdFromScreenNameStmt.setString(1,screenName)
      val result = extractResults(getUserIdFromScreenNameStmt.executeQuery, {row => row.getInt(1)}, None)

      result._1.firstOption
    }
  }

  //////////////////////////
  private lazy val getSettingsStmt = {
    val queryStr = """
      SELECT old_on_top
      FROM user_settings
      WHERE (user_id = ?);
    """
    
    connection.prepareStatement(queryStr)
  }

  def getSettings(userId:Int) = {
    getSettingsStmt synchronized {
      getSettingsStmt.setInt(1,userId)
      val result = extractResults(getSettingsStmt.executeQuery, {row => UserSetting(row.getBoolean(1))}, None)

      result._1.firstOption
    }
  }


  //////////////////////////
  private lazy val getScreenNameFromUserIdStmt = {
    val queryStr = """
      SELECT screen_name
      FROM users
      WHERE (user_id = ?);
    """
    
    connection.prepareStatement(queryStr)
  }

  def getScreenNameFromUserId(userId:Int) = {
    getScreenNameFromUserIdStmt synchronized {
      getScreenNameFromUserIdStmt.setInt(1,userId)
      val result = extractResults(getScreenNameFromUserIdStmt.executeQuery, {row => row.getString(1)}, None)

      result._1.firstOption
    }
  }

  //////////////////////////
  private lazy val getFollowersStmt = {
    val queryStr = """
      SELECT screen_name
      FROM users, followers
      WHERE (followers.user_id = ?) AND (users.user_id = followers.follower_id)
      ORDER BY screen_name DESC
      LIMIT 4000;
    """
    //                    ^^^^^^         Descending because the scala code then reverses the order & that is more efficient
    
    connection.prepareStatement(queryStr)
  }

  def getFollowers(userId:Int) = {
    getFollowersStmt synchronized {
      getFollowersStmt.setInt(1,userId)
      val result = extractResults(getFollowersStmt.executeQuery, {row => row.getString(1)}, None)

      result._1
    }
  }

  //////////////////////////
/*
  private lazy val getScreenNameFromTwinklerStmt = {
    val queryStr = """
      SELECT screen_name
      FROM users
      WHERE (id = ?);
    """
    
    twinkleConnection.prepareStatement(queryStr)
  }

  def getScreenNameFromTwinkler(userId:Int) = {
    getScreenNameFromTwinklerStmt synchronized {
      getScreenNameFromTwinklerStmt.setInt(1,userId)
      val result = extractResults(getScreenNameFromTwinklerStmt.executeQuery, {row => row.getString(1)}, None)

      result._1.firstOption
    }
  }
*/

  //////////////////////////
  private lazy val getFollowerUpdatedStmt = {
    val queryStr = """
      SELECT followerUpdated
      FROM users
      WHERE (user_id = ?);
    """
    
    connection.prepareStatement(queryStr)
  }

  def getFollowerUpdated(userId:Int) = {
    getFollowerUpdatedStmt synchronized {
      getFollowerUpdatedStmt.setInt(1,userId)
      val result = extractResults(getFollowerUpdatedStmt.executeQuery, {row => row.getDate(1)}, None)

      result._1.firstOption
    }
  }


  //////////////////////////
  private lazy val getUploadStmt = {
    val queryStr = """
      SELECT id, uploads.user_id, screen_name, created_at, view_count, view_logged_count, descr
      FROM uploads, users
      WHERE (id = ?) AND (users.user_id = uploads.user_id);
    """
    
    connection.prepareStatement(queryStr)
  }

  def getUpload(uploadId:Int) = {
    getUploadStmt synchronized {
      getUploadStmt.setInt(1,uploadId)
      val result = extractResults(getUploadStmt.executeQuery, {row =>
        Upload(row.getInt(1), row.getInt(2), row.getString(3), row.getDate(4), row.getInt(5), row.getInt(6), row.getString(7))
      }, None)

      result._1.firstOption
    }
  }

  //////////////////////////
  private lazy val getScreenNameStmt = {
    val queryStr = """
      SELECT users.screen_name
      FROM oauth_tokens, users
      WHERE (oauth_token = ?) AND (oauth_token_secret = ?) AND (users.user_id = oauth_tokens.user_id);
    """
    
    connection.prepareStatement(queryStr)
  }

  def getScreenName(oauth_token:String, oauth_token_secret:String) = {
    getScreenNameStmt synchronized {
      getScreenNameStmt.setString(1,oauth_token)
      getScreenNameStmt.setString(2,oauth_token_secret)
      val result = extractResults(getScreenNameStmt.executeQuery, {row => row.getString(1)}, None)

      result._1.firstOption
    }
  }

  //////////////////////////
  private lazy val updateOldOnTopSettingStmt = {
    val queryStr = """
      SELECT set_old_on_top(?, ?);
      """
    
    connection.prepareStatement(queryStr)
  }

  def updateOldOnTopSetting(userId:Int, oldOnTop:Boolean) = {
    updateOldOnTopSettingStmt synchronized {
      updateOldOnTopSettingStmt.setInt(1, userId)
      updateOldOnTopSettingStmt.setBoolean(2, oldOnTop)
      val result = extractResults(updateOldOnTopSettingStmt.executeQuery, {row => 0}, None)

      result._1.firstOption
    }
  }

  //////////////////////////
  private lazy val updateViewCountStmt = {
    val queryStr = """
      UPDATE uploads
      SET view_count = view_count + 1, view_logged_count = view_logged_count + ?
      WHERE id=?;
      """
    
    connection.prepareStatement(queryStr)
  }

  def updateViewCount(uploadId:Int, view_logged_count_change:Int) = {
    updateViewCountStmt synchronized {
      updateViewCountStmt.setInt(1, view_logged_count_change)
      updateViewCountStmt.setInt(2, uploadId)
      updateViewCountStmt.executeUpdate
    }
  }

  //////////////////////////
  private lazy val setFollowerUpdatedStmt = {
    val queryStr = """
      UPDATE users
      SET followerUpdated = ?
      WHERE user_id=?;
      """
    
    connection.prepareStatement(queryStr)
  }

  def setFollowerUpdated(userId:Int,time:Long) = {
    setFollowerUpdatedStmt synchronized {
      setFollowerUpdatedStmt.setDate(1, new java.sql.Date(time))
      setFollowerUpdatedStmt.setInt(2, userId)
      setFollowerUpdatedStmt.executeUpdate
    }
  }

  //////////////////////////
  private lazy val getRecentUploadsStmt = {
    val queryStr = """
      SELECT id
      FROM uploads
      WHERE (user_id = ?) AND (id != ?)
      ORDER BY created_at DESC
      OFFSET ?
      LIMIT ?;
    """
    
    connection.prepareStatement(queryStr)
  }

  def getRecentUploads(userId:Int, uploadId:Int, offset:Int, limit:Int) = {
    getRecentUploadsStmt synchronized {
      getRecentUploadsStmt.setInt(1,userId)
      getRecentUploadsStmt.setInt(2,uploadId)
      getRecentUploadsStmt.setInt(3,offset)
      getRecentUploadsStmt.setInt(4,limit)
      val result = extractResults(getRecentUploadsStmt.executeQuery, {row => row.getInt(1)}, None)

      result._1
    }
  }

  //////////////////////////
  private lazy val getRecentUploadsDetailsStmt = {
    val queryStr = """
      SELECT id, user_id, '', created_at, view_count, view_logged_count, descr
      FROM uploads
      WHERE (user_id = ?) AND (id != ?)
      ORDER BY created_at DESC
      OFFSET ?
      LIMIT ?;
    """
    
    connection.prepareStatement(queryStr)
  }

  def getRecentUploadsDetails(userId:Int, uploadId:Int, offset:Int, limit:Int) = {
    getRecentUploadsDetailsStmt synchronized {
      getRecentUploadsDetailsStmt.setInt(1,userId)
      getRecentUploadsDetailsStmt.setInt(2,uploadId)
      getRecentUploadsDetailsStmt.setInt(3,offset)
      getRecentUploadsDetailsStmt.setInt(4,limit)
      val result = extractResultsOrd(
        getRecentUploadsDetailsStmt.executeQuery, {row =>
          Upload(row.getInt(1), row.getInt(2), row.getString(3), row.getDate(4), row.getInt(5), row.getInt(6), row.getString(7))
        }, None)

      result._1
    }
  }

  //////////////////////////
  private lazy val getGlobalRecentUploadsStmt = {
    val queryStr = """
      SELECT id
      FROM uploads
      ORDER BY created_at DESC
      OFFSET ?
      LIMIT ?;
    """
    
    connection.prepareStatement(queryStr)
  }

  def getGlobalRecentUploads(offset:Int, limit:Int) = {
    getGlobalRecentUploadsStmt synchronized {
      getGlobalRecentUploadsStmt.setInt(1,offset)
      getGlobalRecentUploadsStmt.setInt(2,limit)
      val result = extractResults(getGlobalRecentUploadsStmt.executeQuery, {row => row.getInt(1)}, None)

      result._1
    }
  }

  //////////////////////////
  private lazy val getAllKeysStmt = {
    val queryStr = """
      SELECT user_id, oauth_token, oauth_token_secret
      FROM oauth_tokens;
    """
    
    connection.prepareStatement(queryStr)
  }

  def getAllKeys = {
    getAllKeysStmt synchronized {
      val result = extractResults(
        getAllKeysStmt.executeQuery, {row => (row.getInt(1),row.getString(2), row.getString(3))}, None)

      result._1
    }
  }

  //////////////////////////
  private lazy val getUploadCountStmt = {
    val queryStr = """
      SELECT count(id)
      FROM uploads
      WHERE ((? - created_at) < interval '1d') AND (user_id = ?);
    """
    
    connection.prepareStatement(queryStr)
  }

  def getUploadCount(userId:Int) = {
    getUploadCountStmt synchronized {
      getUploadCountStmt.setTimestamp(1,timeNow)
      getUploadCountStmt.setInt(2,userId)
      val result = extractResults(getUploadCountStmt.executeQuery, {row => row.getInt(1)}, None)

      result._1.first
    }
  }

  //////////////////////////
  private lazy val getCommentCountStmt = {
    val queryStr = """
      SELECT count(*)
      FROM up_comments
      WHERE ((? - created_at) < interval '1d') AND (user_id = ?);
    """
    
    connection.prepareStatement(queryStr)
  }

  def getCommentCount(userId:Int) = {
    getCommentCountStmt synchronized {
      getCommentCountStmt.setTimestamp(1,timeNow)
      getCommentCountStmt.setInt(2,userId)
      val result = extractResults(getCommentCountStmt.executeQuery, {row => row.getInt(1)}, None)

      result._1.first
    }
  }

  //////////////////////////
  private lazy val getCommentCountForUploadStmt = {
    val queryStr = """
      SELECT count(*)
      FROM up_comments
      WHERE (upload_id=?);
    """
    
    connection.prepareStatement(queryStr)
  }

  def getCommentCountForUpload(uploadId:Int) = {
    getCommentCountForUploadStmt synchronized {
      getCommentCountForUploadStmt.setInt(1,uploadId)
      val result = extractResults(getCommentCountForUploadStmt.executeQuery, {row => row.getInt(1)}, None)

      result._1.first
    }
  }

  //////////////////////////
  private lazy val getCommentsStmt = {
    val queryStr = """
      SELECT screen_name, comment_txt
      FROM up_comments, users
      WHERE  (up_comments.upload_id=?) AND (users.user_id = up_comments.user_id)
      ORDER BY created_at DESC
      LIMIT 1000;
    """
    
    connection.prepareStatement(queryStr)
  }

  def getComments(uploadId:Int) = {
    getCommentsStmt synchronized {
      getCommentsStmt.setInt(1,uploadId)
      val result = extractResults(getCommentsStmt.executeQuery, {row => Comment(row.getString(1),row.getString(2))}, None)

      result._1
    }
  }

  //////////////////////////
  private lazy val getTotalTweetsStmt = {
    val queryStr = """
      SELECT sum(total_tweet_count)
      FROM stat_clients;
    """
    
    connection.prepareStatement(queryStr)
  }

  def getTotalTweets = {
    getTotalTweetsStmt synchronized {
      val result = extractResults(getTotalTweetsStmt.executeQuery, {row => row.getInt(1)}, None)

      result._1.first
    }
  }

  //////////////////////////
  private lazy val getClientInfoStmt = {
    val queryStr = """
      SELECT client_id, name, url, total_tweet_count, (total_reply_count*100.0)/(total_tweet_count+1) as perc
      FROM stat_clients
      ORDER BY total_tweet_count DESC, name
    """
    /*
      LIMIT 100
      OFFSET ?;
    */
    
    connection.prepareStatement(queryStr)
  }

  def getClientInfo = {
    getClientInfoStmt synchronized {

      // getClientInfoStmt.setInt(1, page * 100)

      val result = extractResultsOrd(getClientInfoStmt.executeQuery, { row => 
        val name = row.getString(2)
        val nameXML = name.replaceAll("\\\\u(....)", "&#x$1;")

        MyClientInfo(row.getInt(1), name, nameXML, row.getString(3), row.getLong(4), row.getFloat(5))}, None)

      result._1
    }
  }

  //////////////////////////
  private lazy val insertStatHistStmt = {
    val queryStr = """
      INSERT into stat_history
      (client_id, rank, tweet_perc, created_at)
      SELECT  ?, ?, ?, CURRENT_TIMESTAMP
    """
    
    connection.prepareStatement(queryStr)
  }

  def insertStatHist(clientId:Int, rank:Int, tweetPerc:Float) = {
    insertStatHistStmt synchronized {
      insertStatHistStmt.setInt(1,clientId)
      insertStatHistStmt.setInt(2,rank)
      insertStatHistStmt.setFloat(3,tweetPerc)
      insertStatHistStmt.execute
      insertStatHistStmt.getUpdateCount
    }
  }

  //////////////////////////
  private lazy val getMaxHistTimestampStmt = {
    val queryStr = """
      SELECT max(created_at)
      FROM stat_history;
    """
    
    connection.prepareStatement(queryStr)
  }

  def getMaxHistTimestamp = {
    getMaxHistTimestampStmt synchronized {

      val result = extractResults(getMaxHistTimestampStmt.executeQuery, {row => row.getTimestamp(1)}, None)

      result._1.firstOption.flatMap(t => if (t == null) None else Some(t))
    }
  }

  //////////////////////////
  private lazy val getHistoryStmt = {
    val queryStr = """
      SELECT rank, tweet_perc, created_at
      FROM stat_history
      WHERE client_id = ?
      ORDER BY created_at DESC
      LIMIT 15;
    """
    
    connection.prepareStatement(queryStr)
  }

  def getHistory(clientId:Int) = {
    getHistoryStmt synchronized {

      getHistoryStmt.setInt(1, clientId)

      val result = extractResults(getHistoryStmt.executeQuery, {row => MyHistory(row.getInt(1), row.getFloat(2), row.getTimestamp(3))}, None)

      result._1
    }
  }

  lazy val isTempHistViewCreated = {
    connection.createStatement.executeUpdate(
      """
      CREATE TEMP VIEW rankOldTbl AS (
        SELECT client_id, avg(rank) AS rankAvg
        FROM stat_history
        WHERE (created_at >= CURRENT_TIMESTAMP - interval '8 day') AND (CREATED_AT < CURRENT_TIMESTAMP - interval '4 day')
        GROUP BY client_id);
      CREATE TEMP VIEW rankNewTbl AS (
        SELECT client_id, avg(rank) AS rankAvg
        FROM stat_history
        WHERE (CREATED_AT >= CURRENT_TIMESTAMP - interval '4 day')
        GROUP BY client_id);
      """
    )
    true
  }

  //////////////////////////
  private lazy val getHotClientsStmt = {
    val queryStr = """
      SELECT r1.client_id, (r2.rankAvg - r1.rankAvg) AS change
      FROm rankNewTbl AS r1, rankOldTbl AS r2
      WHERE (r2.client_id = r1.client_id) AND ((r2.rankAvg - r1.rankAvg) > 100) AND (r1.rankAvg < 7000)
      ORDER BY change DESC
      LIMIT 40;
    """
    
    connection.prepareStatement(queryStr)
  }

  def getHotClients = {
    if (isTempHistViewCreated) {
      getHotClientsStmt synchronized {

        val result = extractResultsOrd(getHotClientsStmt.executeQuery, {row => MyHotClient(row.getInt(1), row.getFloat(2))}, None)

        result._1
      }
    } else {
      Nil
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

  def getMaxUploadId = {
    val stmt = connection.createStatement(
      ResultSet.FETCH_FORWARD,
      ResultSet.TYPE_FORWARD_ONLY)

    val rs = stmt.executeQuery ("select max(id) from uploads;")
    val (values, count) = extractResults(rs, {rs => rs.getInt(1)}, None)
    values(0)
  }

}

object dbState {
  private var maxUploadId = dbHelper.getMaxUploadId
  def getNewUploadId = {
    synchronized {
      maxUploadId += 1
      maxUploadId
    }
  }
}
