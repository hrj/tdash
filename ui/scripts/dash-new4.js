$.fn.setCursorPosition = function(pos) {
  this.each(function(index, elem) {
    if (elem.setSelectionRange) {
      elem.setSelectionRange(pos, pos);
    // } else if (elem.selectionStart) {
      // elem.selectionStart = elem.selectionEnd = pos;
    } else if (elem.createTextRange) {
      var range = elem.createTextRange();
      range.collapse(true);
      range.moveEnd('character', pos);
      range.moveStart('character', pos);
      range.select();
    }
  });
  return this;
};

function createMyNotification(title, content) {
  var notifer = null;

  if (window.webkitNotifications) {
    if (window.webkitNotifications.checkPermission() == 0) { // 0 is PERMISSION_ALLOWED
      notifier = window.webkitNotifications.createNotification('/images/logoColor57x57.png', title, content);
      notifier.show();

      /*
        } else if (options.notificationType == 'html') {
           return window.webkitNotifications.createHTMLNotification('http://someurl.com');
        } 
      */
    }
  }
  return notifier;
}

var showMore = false;
function toggleMoreFriends() {
  $('#moreFrndContent').toggle();
  var newLabel = ($("#moreButt").text() == "Less") ? "More" : "Less";
  $('#moreButt').text(newLabel);
  showMore=!showMore;
}

function changeCSS(url) {
  $("#cssLink").attr("href", url);
}

function getCurrTime() {return (new Date()).getTime();}

var currLogin = -1,
    readerObj = null;



var rQ = new function () {
  var pipeline = [], reqInFlight = 0, lastFlight = 0, lastSuccessFlight=0;

  this.nQ = function(loginNum, highPriority, msg, url, params, callbackData, callback) {
    pipeline.push([msg, url,params,callbackData,callback, getCurrTime(), loginNum, highPriority]);
    setFetchCount();
    if (reqInFlight == 0) {
      fly();
    }
  }

  function setFetchCount() {
    if (pipeline.length > 0) {
      $("#fetchCount").text("Pending requests: " + pipeline.length);
    } else {
      $("#fetchCount").text("");
      $("#flattrDiv").show();

      dash.fetchOver();
    }
  }

  function showFetch(msg) {
    $("#fetchStatus").text( (msg.length > 0) ? 'Fetching ' + msg : msg);
  }

  function fly() {
    var elem = pipeline.shift(),
        currTime = getCurrTime();

    var oldLimit = (currTime - lastSuccessFlight) < 120000 ? 15*60000 : 5*60000;

    if (((!elem[7]) && (currLogin >= 0) && (elem[6] != currLogin)) || ((getCurrTime() - elem[5]) > oldLimit)) {
      showFetch("");
      setFetchCount();
      if (pipeline.length > 0) {
        fly();
      }
    } else {
      reqInFlight += 1;
      lastFlight = getCurrTime();
      showFetch(elem[0]);
      dashOauth.oauthGetJSONP(elem[1], elem[2], oauthAccessor[elem[6]], elem[3], function(response, cbdata) {
        lastSuccessFlight = getCurrTime();
        elem[4](response, cbdata);
        reqInFlight -= 1;
        showFetch("");
        setFetchCount();
        if (pipeline.length > 0) {
          fly();
        }
      });
    }
  }

  this.selfCheck = function() {
    var diff = getCurrTime() - lastFlight;
    if ((pipeline.length > 0) && (reqInFlight > 0)) {
      if( diff > 30000) {
        fly();
      } else if (diff > 15000) {
        showFetch("Will retry in " + (30 - Math.round(diff/1000)) + " seconds");
      }
    }

    setTimeout("rQ.selfCheck();", 2000);
  }

  this.selfCheck();
};

var dash = new function () {
  // static data
  var currVersion = "5.49",
      releaseDate = "2-Aug-2011",

      aboutStr = '<h2>tDash.org</h2><p>A dashboard for Twitter.<br/></p><p><em>Ver ' + currVersion+' | '+releaseDate+ '</em></p><p>tDash is undergoing rapid development. Please give us your <a target="_blank" href="http://tdash.uservoice.com">feedback here</a>.</p><hr/><p>Copyright 2009-2010 tDash.org</p><p>Follow <a target="_blank" href="http://twitter.com/tdash">@tdash</a> for updates</p><hr/><p id="APILimit"></p>',
      keyStr = '<h2>Keyboard shortcuts</h2><p>All shortcuts are case insensitive</p> <table cellspacing="0" id="helpKeyShorts"><tbody><tr><td class="helpKey">Space</td><td>Go to next tweet.<br/>Jumps to next folder if no more unread tweets in current folder</td></tr><tr><td><span class="helpKey">o</span></td><td>Open URL(s) in the selected tweet</td></tr>  <tr><td><span class="helpKey">n</span> or <span class="helpKey">j</span></td><td>Go to next tweet</td></tr><tr><td><span class="helpKey">p</span> or <span class="helpKey">k</span></td><td>Go to previous tweet</td></tr> <tr><td class="helpKey">u</td><td>Fetch new tweets (get Updates)</td></tr> <tr><td class="helpKey">s</td><td>Enter your status</td></tr> <tr><td class="helpKey">r</td><td>Reply to currently selected tweet</td></tr> <tr><td class="helpKey">f</td><td>Favourite the currently selected tweet</td></tr> <tr><td class="helpKey">t</td><td>ReTweet the currently selected tweet</td></tr> <tr><td class="helpKey">q</td><td>Quick RT the currently selected tweet</td></tr><tr><td class="helpKey">?</td><td>Show this help screen</td></tr> <tr><td class="helpKey">Esc</td><td>Close modal screens like this screen</td></tr></tbody></table>',
      millisPerHour = 3600000,

      folderInfoStartStr = '<table><tbody><tr>',
      folderInfoEndStr = '<td id="folderButtTd"><input id="markAllReadButton" type="button" value="Mark folder as read" onclick="dash.markAllRead();"/></td></tr></tbody></table>',
      ignoreEvents = false,
      formSubmitted = false,

      mode = {none:100,friend:0,folder:1,list:2,trends:3, tool:4, search:5},
      modeToElem = {100:"",0:"#navFriends",1:"#navFolders",2:"#navList",3:"#navTrends", 4:"#navToolBox", 5:"#navSearch"},
      modeToChecker = {100:checkNoneUnread, 0:checkFriendUnread, 1:checkFolderUnread, 2:checkListUnread,3:checkTrendUnread,4:checkNoneUnread, 5:checkNoneUnread},

      namePattern = /@([a-zA-Z0-9_]+)/gi,
      urlSelectBox = null,
      urlHostPattern = /http:\/\/([^\/]*)\/.*/,
      urlPattern = /(ftp|http|https|file):\/\/[\S]+/gi;
      hashTagPattern = /( |^)#([\w]+)/gi;
      // var urlPattern = /(?i)\b((?:[a-z][\w-]+:(?:\/{1,3}|[a-z0-9%])|www\d?[.])(?:[^\s()<>]+|\ ([^\s()<>]+\))+(?:\([^\s()<>]+\)|[^`!()\[\]{};:\'".,<>?«»“”‘’\s]))/gi;
  var navMode = mode.none;

  // view related data
  var folderData = {
        'mentions': {url:"https://api.twitter.com/1.1/statuses/mentions_timeline.json",    name:"Mentions",    converter:null},
        /*
        'inbox':    {url:"https://api.twitter.com/1.1/direct_messages.json",      name:"Inbox",       converter:DMInToTweet},
        'outbox':   {url:"https://api.twitter.com/1.1/direct_messages/sent.json", name:"Outbox",      converter:DMOutToTweet},
        */
        'favs':     {url:"https://api.twitter.com/1.1/favorites/list.json",            name:"Favourites",  converter:null}
      },
      displayedTweetIds = [],
      displayedSearchTweets = [],

      charCountElem = null,
      currUser = null,
      navSelection = 0,
      currTweetSelection = 0,
      totalFriendUnread = 0, totalListUnread = 0,
      totalFolderUnread = 0, totalTrendUnread = 0,
      friends = {}, sortedFriends = [],
      countOfValidUsers = 0,
      prevNotifier = null, windowHasFocus = true;

  // shared operational data
  var replyToPipeline = [],
      appendFriendParams = {count:200},
      lastUpdate = getCurrTime();


  // common model data (shared)
  var tweetCache = {};

  // user specific model data
  var userData = [];

  function checkNoneUnread() {return false;}

  function closePrevNotifier() {
    if (prevNotifier != null) {
      prevNotifier.cancel();
    }
  }

  function switchToWindow() {
    window.focus();
    closePrevNotifier();
  }

  this.fetchOver = function() {
    if (!windowHasFocus) {
      var totalUnread = totalFriendUnread + totalFolderUnread;
      if (totalUnread > 0) {
        closePrevNotifier();

        var message = [];
        if (totalFriendUnread > 0) {
          message.push(totalFriendUnread + " from friends");
        }
        if (totalFolderUnread > 0) {
          message.push(totalFolderUnread + " in folders");
        }
        prevNotifier = createMyNotification(totalUnread + " new tweets", message.join(", "));
        if (prevNotifier) {
          prevNotifier.onclick = switchToWindow;
        }
      }
    }
  }

  $(window).focus(
      function() {
        windowHasFocus = true;
        closePrevNotifier();
      })
  .blur(
      function() {
        windowHasFocus = false;
        closePrevNotifier();
      }
  );


  // TODO: dontPersist is not set at any call site
  function addTweetCache(tweet, dontPersist) {
    if (!tweetCache[tweet.id_str]) {
      tweet.tdashRead = false;
      tweetCache[tweet.id_str] = tweet;
      if (tweetReadStore.get && (!dontPersist)) {
        if (tweetReadStore.get(tweet.id_str)) {
           markTweetRead(tweetCache[tweet.id_str]);
        }
        /*
        tweetReadStore.get(tweet.id_str, function(ok,val) {
          if (ok && val) {
             markTweetRead(tweetCache[this.key]);
             updateNav();
          }
        }, {"key":tweet.id_str});
        */
      }
    }
  }

  function tweetExistsInList(id, list) {
    if (list) {
      for (var i=list.length; i--; ) {
        if (list[i] == id) {
          return tweetCache[list[i]];
        }
      }
    }
    return null;
  }


  function checkFriendUnread() {
    // try to find the next unread friend
    var unReadFriend = null;

    for (var i in friends) {
      var friend = friends[i];
      if (friend.tdashUnread > 0) {
        unReadFriend = friend;
        break;
      }
    }
    
    if (unReadFriend) {
      dash.navClick(unReadFriend.id);
      return true;
    }

    return false;
  }
  function checkListUnread() {
    // try to find the next unread List
    var unReadList = null;

    for (var i in currUser.userLists) {
      var list = currUser.userLists[i];
      if (list.tdashUnread > 0) {
        unReadList = list;
        break;
      }
    }
    
    if (unReadList) {
      dash.listClick(unReadList.id);
      return true;
    }
    return false;
  }

  function checkFolderUnread() {
    // try to find the next unread List
    var unReadFolder = null;

    var myFolderData = currUser.folderData;

    for (var i in myFolderData) {
      if (myFolderData[i].unread > 0) {
        unReadFolder = i;
        break;
      }
    }
    
    if (unReadFolder) {
      dash.folderClick(unReadFolder);
      return true;
    }
    return false;
  }

  function checkTrendUnread() {
    var unReadTrend = null;

    for (var i in trendCache) {
      var trend = trendCache[i];
      if (!trend.tdashRead) {
        unReadTrend = i;
        break;
      }
    }
    
    if (unReadTrend) {
      dash.trendClick(unReadTrend);
      return true;
    }
    return false;
  }


  function showListHelp() {
    if (currUser) {
      if (currUser.userLists.length > 0) {
        $("#readerInfo").html('<p><input type="button" onClick="dash.markAllListRead();" value="Mark all lists as read" /></p>' );
        $(readerObj).html('');
      } else {
        $("#readerInfo").html('<p>Apparently, you haven\'t subscribed to any lists. Lists are a great way to organise, prioritise your contacts.</p><p>You can find out more <a href="http://help.twitter.com/forums/10711-getting-started/entries/76460-twitter-lists" target="_blank">here</a></p>' );
        $(readerObj).html('');
      }
    }
    resizeIframe();
  }

  this.markAllListRead = function() {
    if (totalListUnread > 0) {
      if (confirm("Are you sure you want to mark " + totalListUnread + " tweets from your Lists as read?")) {
        var userLists = currUser.userLists;
        for(var i = userLists.length; i--;) {
          var tweets = currUser.twtsFrmLists[userLists[i].id];
          for (var j=tweets.length; j--;) {
            markTweetRead(tweetCache[tweets[j]]);
          }
        }
        updateNav();
      }
    }
  };

  this.changeMode = function(newMode, actualClick) {
    var modeChanged = (navMode != newMode);
    if (modeChanged) {
      $(modeToElem[navMode]).hide();
      navMode = newMode;
      $(modeToElem[newMode]).slideDown(200);
    }
    if (actualClick) {
      if (newMode == mode.friend) {
        // Click on the friend tab shows all unread friends
        dash.navClick(0);
      } else if (newMode == mode.list) {
        // Click on the List tab shows help and other utility functions
        showListHelp();
      }
    }
    /*
    if (newMode == mode.trends) {
      $(modeToElem[mode.trends]).addClass('navTabLast');
    } else {
      $(modeToElem[mode.trends]).removeClass('navTabLast');
    }
    */
    updateNav();
  }

  function formatCount(num) {return ((num > 0) ? ' ('+num+')' : '');};

  function friendCompare(f1, f2) {
    var s1 = friends[f1].screen_name,
        s2 = friends[f2].screen_name;
    return (s1 < s2) ? -1 : ((s1 == s2) ? 0 : 1);
  }

  function updateNav() {
    var navUnreadStr = '',
        navReadStr = '';

    $(".navSel").removeClass("navSel");

    if ((navMode == mode.none) || (navMode == mode.friend)) {
      for (var i in sortedFriends) {
        var friend = friends[sortedFriends[i]];
        if (friend.tdashUnread > 0) {
          navUnreadStr += '<p onclick="dash.navClick('+friend.id+')" class="navElem '+(navSelection==friend.id?'navSel':'')+'">'+friend.screen_name+' ('+friend.tdashUnread+')</p>';
        } else {
          navReadStr += '<p onclick="dash.navClick('+friend.id+')" class="navElem navRead '+(navSelection==friend.id?'navSel':'')+'">'+friend.screen_name+' </p>';
        }
      }
      if (navReadStr.length > 0) {
        navReadStr = '<div id="moreFriends"><div id="moreButt" onclick="toggleMoreFriends()">'+(showMore?'Less':'More')+'</div><div'+(showMore?'':' style="display:none"')+' id="moreFrndContent">'+navReadStr+'</div></div>';
      }
      $('#navFriends').html(navUnreadStr + navReadStr);
      $('#navList').empty();
      $('#navFolders').empty();
    } else if (navMode == mode.list) {
      if (currUser) {
        for (var i in currUser.userLists) {
          var list = currUser.userLists[i];
          if (list.tdashUnread > 0) {
            navUnreadStr += '<p onclick="dash.listClick('+list.id+')" class="navElem '+(navSelection==list.id?'navSel':'')+'">'+list.slug+' ('+list.tdashUnread+')</p>';
          } else {
            navReadStr += '<p onclick="dash.listClick('+list.id+')" class="navElem navRead '+(navSelection==list.id?'navSel':'')+'">'+list.slug+' </p>';
          }
        }
        $('#navList').html(navUnreadStr + navReadStr);
      }
    } else if (navMode == mode.folder) {
      if (currUser) {
        var navUnreadStr = '';
        for (var folderName in folderData) {
          var folder = folderData[folderName];
          var myFolder = currUser.folderData[folderName];
          if (myFolder.unread > 0) {
            navUnreadStr += '<p onclick="dash.folderClick(\''+folderName+'\');" class="navElem '+(navSelection==folderName?'navSel':'')+'">'+folder.name+' ('+myFolder.unread+')</p>';
          } else {
            // Intentionally navUnreadStr so that the order doesn't change
            navUnreadStr += '<p onclick="dash.folderClick(\''+folderName+'\');" class="navElem navRead '+(navSelection==folderName?'navSel':'')+'">'+folder.name+'</p>';
          }
        }
        $('#navFolders').html(navUnreadStr);
      }
    } else if (navMode == mode.trends) {
      for (var trendName in trendCache) {
        var trend = trendCache[trendName];
        var classStr = trend.tdashRead ? " navRead" : "";
        navUnreadStr += '<p onclick="dash.trendClick(\''+trendName+'\');" class="navElem'+classStr+(navSelection==trendName?' navSel':'')+'">'+trendName+'</p>';
      }
      $('#navTrends').html(navUnreadStr);
    } else if (navMode == mode.tool) {
      var clickCount = currUser ? formatCount(currUser.cCache.length) : '';
      navUnreadStr += '<p onclick="dash.toolClick(\'clicks\');" class="navElem'+(navSelection=="clicks"?' navSel':'')+'">Clicks '+clickCount+'</p>';
      $('#navToolBox').html(navUnreadStr);
    } else if (navMode == mode.search) {
      var searches = currUser.savedSearches;
      if (searches) {
        if (searches.length > 0) {
          for (var i = searches.length; i--;) {
            navUnreadStr += '<p onclick="dash.searchClick('+i+');" class="navElem'+(navSelection==(i+1)?' navSel':'')+'">'+searches[i].name+'</p>';
          }
        } else {
          navUnreadStr += '<p class="navElem">No saved searches</p>';
        }
      } else {
        navUnreadStr += '<p class="navElem">Loading your saved searches</p>';
      }
      $('#navSearchInner').html(navUnreadStr);
    }

    $('#navHeadFriends').html('Friends '+formatCount(totalFriendUnread));
    $('#navHeadList').html('Lists '+formatCount(totalListUnread));
    $('#navHeadFolders').html('Folders '+formatCount(totalFolderUnread));
    $('#navHeadTrends').html('Trends '+formatCount(totalTrendUnread));

    document.title = formatCount(totalFriendUnread + totalFolderUnread) + " tDash"

    resizeIframe();
  }

  function updateReader() {
    totalFriendUnread = 0;
    friends = [];
    sortedFriends = [];
    var tweetsFromFriends = userData[currLogin].twtsFrmFrnds;

    for (var i=tweetsFromFriends.length; i--; ) {
      var tweet = tweetCache[tweetsFromFriends[i]];

      if(!friends[tweet.user.id]) {
        // Hello to a new friend
        friends[tweet.user.id] = tweet.user;
        friends[tweet.user.id].tdashUnread = 0;
        sortedFriends.push(tweet.user.id);
      }

      if (!tweet.tdashRead) {
        totalFriendUnread++;
        friends[tweet.user.id].tdashUnread++;
      }
    }
    sortedFriends.sort(friendCompare);

    redrawCurrentView();
  }

  function formatText(text, tweetId) {
    var onclickStr = ' onclick="dash.regClick('+tweetId+');"';
    text = text.replace(urlPattern,'<a data-tweetId="'+tweetId+'" href="$&" class="st_link" target="_blank"'+onclickStr+'>$&</a>').replace(namePattern, '<a href="http://twitter.com/$1" target="_blank"'+onclickStr+'>$&</a>');
    return text.replace(hashTagPattern, '$1<a target="_blank" href="http://twitter.com/search?q=%23$2">#$2</a>').replace(/\n/g,"<br/>");
  }

  function formatTime(millis, currMillis) {
    var mins = Math.round((currMillis - millis) / 60000);
    if (mins > 60) {
      var hours = Math.round(mins / 60);
      if (hours > 24) {
        var days = Math.round(hours/24);
        return (days > 1 ? days + " days ago" : "one day ago");
      }

      return (hours > 1 ? hours + " hours ago" : "one hour ago");
    }
    return ((mins > 0) ? mins + " mins ago" : "just now");
  }

  function makeNewWindow(link) {
    return link.replace(/[\s]href=/i, ' target="_blank" href=');
  }

  function binSearchSingleComparison(key, store) {
    var N = store.length;
    var min = 0, mid = 0, max = N;
    while (min < max) {
      mid = Math.floor((min + max) / 2);
      if (store[mid] < key) {
        min = mid + 1;
      } else {
        max = mid;
      }
    }
    if ((min < N) && (store[min] == key))
      return min // found
    else
      return -1 // not found
  }

  var shortnerServices = [
    "307.to", "adjix.com", "b23.ru", "bacn.me",
    "bit.ly", "bloat.me", "budurl.com", "cli.gs",
    "clipurl.us", "cort.as", "digg.com", "dwarfurl.com",
    "fb.me", "ff.im", "fff.to", "goo.gl",
    "href.in", "idek.net", "is.gd", "j.mp",
    "kl.am", "korta.nu", "lin.cr", "livesi.de",
    "ln-s.net", "loopt.us", "lost.in", "memurl.com",
    "merky.de", "migre.me", "moourl.com", "nanourl.se",
    "om.ly", "ow.ly", "peaurl.com", "ping.fm",
    "piurl.com", "plurl.me", "pnt.me", "poprl.com",
    "post.ly", "rde.me", "reallytinyurl.com", "redir.ec",
    "retwt.me", "rubyurl.com", "short.ie", "short.to",
    "smallr.com", "sn.im", "sn.vc", "snipr.com",
    "snipurl.com", "snurl.com", "su.pr", "tiny.cc",
    "tinysong.com", "tinyurl.com", "togoto.us", "tr.im",
    "tra.kz", "trg.li", "twurl.cc", "twurl.nl",
    "u.mavrev.com", "u.nu", "ur1.ca", "url.az",
    "url.ie", "urlx.ie", "w34.us", "xrl.us",
    "yep.it", "zi.ma", "zurl.ws", "chilp.it",
    "notlong.com", "qlnk.net", "trim.li", "url4.eu"
    ];
            
    
  /*
  function binSearchNonRecurse(key, store) {
    var min = 0, max = store.length - 1, mid;
    while (true) {
      // print ("min,max = " + min +", " + max);
      mid = Math.round((min + max) / 2);
      if (store[mid] == key) {
        return mid;
      } else {
        if (mid == min) {
          return -1;
        } else {
          if (store[mid] < key) {
            min = mid+1;
          } else {
            max = mid-1;
          }
        }
      }
    }
  }
  */

  function isFollower(name) {
    return (binSearchSingleComparison(name.toLowerCase(), myFollowers) >= 0);
  }

  this.dmChosen = function() {
    var name = $("#dmFollowerChoice").val();
    $.modal.close();
    this.dm(name);
  }

  this.dm = function(name) {
    var divStr = '<div><h2><img style="vertical-align:middle;" src="/images/ink.png"/>Direct Message to ' + name + '</h2>';
    divStr += '<form id="dmForm" method="post" action="https://api.twitter.com/1.1/direct_messages/new.xml" target="upload_target"><textarea name="text" rows="4" cols="40" id="dmInput" ></textarea>';
    divStr += '<input id="dmScreenName" type="text" name="screen_name" value="'+name+'" style="display:none;" />';
    divStr += '<input type="text" id="dm_oauth_consumer_key" name="oauth_consumer_key" value="tDash" style="display:none;" />';
    divStr += '<input type="text" id="dm_oauth_nonce" name="oauth_nonce" value="tDash" style="display:none;" />';
    divStr += '<input type="text" id="dm_oauth_signature" name="oauth_signature" value="tDash" style="display:none;" />';
    divStr += '<input type="text" id="dm_oauth_signature_method" name="oauth_signature_method" value="HMAC-SHA1" style="display:none;" />';
    divStr += '<input type="text" id="dm_oauth_timestamp" name="oauth_timestamp" value="tDash" style="display:none;" />';
    divStr += '<input type="text" id="dm_oauth_token" name="oauth_token" value="tDash" style="display:none;" />';
    divStr += '<input type="text" id="dm_oauth_version" name="oauth_version" value="1.0" style="display:none;" /></form>';
    divStr += '<p id="dmCharCount" class="charCount" >140</p><input type="button" id="dmButton" onclick="dash.dmClick()" value="&#9997; Send message"/>';
    $.modal(divStr,{onShow:function(dlg){$(dlg.container).css('width','auto');$(dlg.container).css('height','auto');ignoreEvents=true}, onClose:function(dlg){ignoreEvents=false;$.modal.close()}});
    $("#dmInput").keyup(dmKeyHandler);
  }

  function statusHoverIn() {
    $(this).find(".indTd").addClass("indHover");
  }
  function statusHoverOut() {
    $(this).find(".indTd").removeClass("indHover");
  }

  function markupStatuses(tweetIds) {
    $('.status').hover(statusHoverIn, statusHoverOut);
    if (myFollowers) {
      for (i in tweetIds) {
        var id = tweetIds[i];

        var name = tweetCache[id].user.screen_name;
        if (isFollower(name)) {
          $('#stat'+id+' .dmPlcHld').html('&nbsp;<span class="st_button" onclick="dash.dm(\''+name+'\')">&#8618;&nbsp;DM</span>');
        }
      }
    }
  }

  function mkThumbImg(tweetId, href, imgUrl, thmbTxt) {
    $('#stat'+tweetId+' .imgPreview').append('<li><a title="'+thmbTxt+' preview" target="_blank" class="linkNoDec" href="'+href+'"><img class="linkNoDec" src="'+imgUrl+'"></img></a></li>');
  }

  var thumbMatchers = [
    [ /http:\/\/tweetphoto\.com\/([\S]+)/i, function(href, tweetId, imageIdIgnored) {
      dashOauth.getJSONP("http://tweetphotoapi.com/api/TPAPI.svc/jsonp/metadatafromurl",
        {url:href, callback:"?"}, tweetId, function(response, tweetId) {
            if (response.ThumbnailUrl) {
              mkThumbImg(tweetId, href, response.ThumbnailUrl, 'TweetPhoto');
            }
          }
      );
    }],
    [ /http:\/\/vimeo\.com\/([0-9]+)/i, function(href, tweetId, videoId) {
      dashOauth.getJSONP("http://vimeo.com/api/v2/video/"+videoId+".json",
        {callback:"?"}, tweetId, function(response, tweetId) {
            if (response.length > 0) {
              if (response[0].thumbnail_small) {
                mkThumbImg(tweetId, href, response[0].thumbnail_small, 'Vimeo');
              }
            }
          }
      );
    }],
    [ /http:\/\/twitpic\.com\/([\S]+)/i, function(href, tweetId, imageId) {
      mkThumbImg(tweetId, href, 'http://twitpic.com/show/mini/'+imageId, 'Twitpic');
    }],
    [ /http:\/\/tdash\.org\/x([\S]+)/i, function(href, tweetId, imageId) {
      mkThumbImg(tweetId, href, 'http://tdash.org/pic/showThumb?id='+imageId, 'tDash');
    }],
    [ /http:\/\/[w.]*flic\.kr\/p\/([\S]+)/i, function(href, tweetId, imageId) {
      mkThumbImg(tweetId, href, 'http://flickr.com/p/img/'+imageId+'_s.jpg', 'Flickr');
    }],
    [ /http:\/\/[w.]*flickr.com\/photos\/[\S]+\/([0-9]+)/i, function(href, tweetId, imageId) {
      // encode to base58 according to http://www.flickr.com/groups/api/discuss/72157616713786392/
      var alphabet = "123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
      var base_count = alphabet.length;
      var encoded = '';
      while (imageId >= base_count) {
        var div = imageId/base_count;
        var mod = (imageId-(base_count*Math.floor(div)));
        encoded = alphabet[mod] + encoded;
        imageId = Math.floor(div);
      }
      if (imageId > 0)
        encoded = alphabet[imageId] + encoded;
      mkThumbImg(tweetId, href, 'http://flickr.com/p/img/'+encoded+'_s.jpg', 'Flickr');
    }],
    [ /http:\/\/.*youtube.com\/.*[?&]v=([^&#]*)/i, function(href, tweetId, videoId) {
      mkThumbImg(tweetId, href, 'http://img.youtube.com/vi/'+videoId+'/2.jpg', 'YouTube');
    }],
    [ /http:\/\/youtu.be\/([^&#?]*)/i, function(href, tweetId, videoId) {
      mkThumbImg(tweetId, href, 'http://img.youtube.com/vi/'+videoId+'/2.jpg', 'YouTube');
    }],
    [ /http:\/\/yfrog\.com\/([\S]+)/i, function(href, tweetId, imageId) {
      mkThumbImg(tweetId, href, 'http://yfrog.com/'+imageId+'.th.jpg', 'yFrog');
    }],
    [ /http:\/\/metacafe\.com\/w[atch]*\/([0-9]+)\/.*/i, function(href, tweetId, videoId) {
      mkThumbImg(tweetId, href, 'http://metacafe.com/thumb/'+videoId+'.jpg', 'MetaCafe');
    }],
    [ /http:\/\/[w.]*dailymotion\.com\/video\/([^_]+).*/i, function(href, tweetId, videoId) {
      mkThumbImg(tweetId, href, 'http://dailymotion.com/thumbnail/160x120/video/'+videoId, 'DailyMotion');
    }],
    [ /http:\/\/pk\.gd\/(.*)/i, function(href, tweetId, imgId) {
      mkThumbImg(tweetId, href, 'http://img.pikchur.com/pic_'+imgId+'_t.jpg', 'Pikchur');
    }],
    [ /http:\/\/mp\.gd\/(.*)/i, function(href, tweetId, videoId) {
      mkThumbImg(tweetId, href, 'http://vid.pikchur.com/vid_'+videoId+'_t.jpg', 'Pikchur Video');
    }],
    [ /http:\/\/pikchur\.com\/(.*)/i, function(href, tweetId, mediaId) {
      if (mediaId.substring(0,2) === 'v/') {
        // video
        mkThumbImg(tweetId, href, 'http://vid.pikchur.com/vid_'+mediaId.substring(2)+'_t.jpg', 'Pikchur Video');
      } else {
        // image
        mkThumbImg(tweetId, href, 'http://img.pikchur.com/pic_'+mediaId+'_t.jpg', 'Pikchur');
      }
    }],
    // 8tracks API http://docs.google.com/Doc?docid=0AQstf4NcmkGwZGdia2c5ZjNfNDNjbW01Y2dmZw&hl=en
    [ /http:\/\/8tracks\.com\/[^\/]+\/(.*)/i, function(href, tweetId, trackId) {
      dashOauth.getJSONP("http://8tracks.com/mixes.jsonp",
        {q:trackId,callback:"?"}, tweetId, function(response, tweetId) {
            if (response.mixes) {
              if (response.mixes.length > 0) {
                var url = response.mixes[0].cover_urls['max133w'];
                mkThumbImg(tweetId, href, url, '8Tracks Cover');
              }
            }
          }
      );
    }],
    // Twitsnaps 
    [ /http:\/\/twitsnaps\.com\/([0-9]+)/i, function(href, tweetId, imageId) {
      mkThumbImg(tweetId, href, 'http://twitsnaps.com/mini/'+imageId, 'Twitsnaps Image');
    }]


    // dailymotion:
    // http://www.dailymotion.com/thumbnail/160x120/video/VIDEOID
  ];

  function getThumbnails(indexIgnored, elem) {
    var myElem = $(elem);
    var href = myElem.attr("href");

    for (var i=thumbMatchers.length; i--;) {
      var match = thumbMatchers[i][0].exec(href);
      if(match) {
        thumbMatchers[i][1](href, myElem.attr('data-tweetId'), match[1]);
      }
    }
  };

  function isShortURL(indexIgnored) {
    var url = $(this).attr("href");
    var match = urlHostPattern.exec(url);
    if (match) {
      if (binSearchSingleComparison(match[1],shortnerServices) >= 0) {
        return true;
      }
    }
    return false;
  }

  function buildReaderInternal(shownTweetIds) {
    var tweetStr = '<table class="stTbl centerAlignTbl">',
        replyTask = [],
        currTime =  getCurrTime(),
        foundCurrSelection = false;

    for (var i in shownTweetIds) {
      var tweet = tweetCache[shownTweetIds[i]];
      if (tweet) {
        var classStr = 'status';
        var indStr = '';
        var tweetId = tweet.id_str ? tweet.id_str : tweet.id;

        if (currTweetSelection == tweetId) {
         classStr += ' selected'; 
         indStr += ' indSel'; 
         foundCurrSelection = true;
        } else if (tweet.tdashRead) {
         indStr += ' indRead'; 
        }
        var createdMillis = Date.parse(tweet.created_at);
        var tweetText = tweet.retweeted_status ? '<span class="quickRTLabel">Quick RT @'+tweet.retweeted_status.user.screen_name + "</span>&nbsp;" + tweet.retweeted_status.text : tweet.text;

        classStr += (tweet.tdashRead ? ' read' : ' unread')
        tweetStr += '<tr data-tweet-id="'+tweetId+'" id="stat'+tweetId+'" onclick="dash.clickStat('+"'"+tweetId+"'"+')" class="' + classStr +'">';
        tweetStr += '<td class="indTd'+indStr+'"></td><td class="imgTd"><img class="prof_img" src="'+tweet.user.profile_image_url+'" /></td>';
        tweetStr += '<td><p class="text">'+formatText(tweetText, tweetId)+'</p>';

        // to avoid event propagation we do http://stackoverflow.com/questions/387736/how-to-stop-event-propagation-with-inline-onclick-attribute
        tweetStr += '<p class="metaStatus"><span class="author" onclick="var event = arguments[0]||window.event;dash.showUserDtl(event,\''+tweetId+'\');">'+tweet.user.screen_name+'</span><span class="dmPlcHld" ></span>';

        if (tweet.tDashOrigId) {
          tweetStr += '&nbsp;<span class="createdAt" data-created-at="'+createdMillis+'"> '+formatTime(createdMillis, currTime)+'</span>';
        } else {
          tweetStr += '&nbsp;<a target="_blank" href="http://twitter.com/'+tweet.user.screen_name+'/status/'+tweetId+'" class="createdAt" data-created-at="'+createdMillis+'"> '+formatTime(createdMillis, currTime)+'</a>';
        }
        var replyToId = tweet.in_reply_to_status_id_str;
        tweetStr += 'from ' + makeNewWindow(tweet.source);
        if (replyToId) {
          tweetStr += ' <a target="_blank" href="http://twitter.com/'+tweet.in_reply_to_screen_name+'/status/'+replyToId+'">in reply to ' + tweet.in_reply_to_screen_name+'</a>';
        } else if (tweet.in_reply_to_screen_name) {
          tweetStr += ' <a target="_blank" href="http://twitter.com/'+tweet.in_reply_to_screen_name+'">in reply to ' + tweet.in_reply_to_screen_name+'</a>';
        }
        tweetStr += '<ul class="imgPreview"></ul>';
        if (replyToId) {
          tweetStr += '<div class="replyTo replyTo'+replyToId+'">Fetching parent tweet</div>';
          replyTask.push(replyToId);
        }
        tweetStr += '</td><td class="buttTd plcHld"></td>';
        tweetStr += '</tr>';
      }
    }

    tweetStr += '</table>';

    $(readerObj).html(tweetStr);

    // this _has_ to be done after the above html change.
    for (i = replyTask.length; i--;) {
      if (i > 10) {
        $('.replyTo'+replyTask[i]).html('<span class="toolButt pointCursor" onclick="dash.fetchReplyTo('+replyTask[i]+',4)">Get parent tweets</span></div></div>');
      } else {
        fetchReplyToInternal(replyTask[i], 1);
      }
    }

    if (shownTweetIds.length > 0) {
      scrollTo($('#stat'+(foundCurrSelection ? currTweetSelection : shownTweetIds[0])).first(), 0);

      setTimeout(function() {
          $("a.st_link").each(getThumbnails).filter(isShortURL).slice(0, 100).addClass("xpanding").longUrl({
              complete: function(result){
                this.removeClass("xpanding").addClass("xpanded");
              },
              callback: function(href,longurl) {
                          if(longurl) {
                            this.attr("href",longurl);
                            if(longurl.length < 40) {
                              this.text(longurl)
                            } else {
                              var t = longurl.slice(0,40)+"...";
                              this.text(t).attr("title",longurl)
                            }
                            getThumbnails(0, this.get(0));
                          }
                        }
              });

          markupStatuses(shownTweetIds);
        }, 0);
      }
    }


  function updateReplyToData(tweet, n) {
    var replyToStr = '<div class="replyToData">In reply to <em>' + tweet.user.screen_name + "</em> &#8227; " + tweet.text + "</div>";
    var replyToId = tweet.in_reply_to_status_id_str;
    if (replyToId) {
      replyToStr += '<div class="replyTo replyTo'+replyToId+'"><div class="replyToData">';
      if (n > 0) {
        replyToStr += 'Fetching parent tweet</div></div>';
      } else {
        replyToStr += '<span class="toolButt pointCursor" onclick="dash.fetchReplyTo(\''+replyToId+'\',4)">Get more tweets</span></div></div>';
      }
    }
    $('.replyTo'+tweet.id_str).html(replyToStr);

    if (replyToId && (n > 0)) {
      if (tweetCache[replyToId]) {
        updateReplyToData(tweetCache[replyToId], n);
      } else {
        fetchReplyToInternal(replyToId, n - 1);
      }
    }
  }

  this.fetchReplyTo = function(id, n) {
    $('.replyTo'+id).html('<div class="replyToData">Fetching parent tweet</div>');
    fetchReplyToInternal(id,n);
  }

  function handleReplyResponse(tweet, n) {
    if (tweet.id_str) {
      addTweetCache(tweet);
      updateReplyToData(tweet, n);
      
      // remove from pipeline
      for (var i = replyToPipeline.length; i--;) {
        if (replyToPipeline[i] == tweet.id_str) {
          replyToPipeline.splice(i,1);
          break;
        }
      }
    }
  }

  function fetchReplyToInternal(id, n) {
    if(tweetCache[id]) {
      updateReplyToData(tweetCache[id], n);
    } else {
      for(var i = replyToPipeline.length; i--;) {
        if (replyToPipeline[i] == id) {
          return;
        }
      }

      // We come here only if request not already pipelined
      replyToPipeline.push(id);

      rQ.nQ(currLogin, false, "Reply to " + id, "https://api.twitter.com/1.1/statuses/show.json", {"id":id}, n, handleReplyResponse);
    }
  }

  this.markAllRead = function() {
    if (navMode == mode.trends) {
      for (id in trendCache) {
        markTrendRead(trendCache[id]);
      }
    } else {
      for (var i in displayedTweetIds) {
        var tweet = tweetCache[displayedTweetIds[i]];
        markTweetRead(tweet);
      }
      buildReaderInternal(displayedTweetIds);
    }
    updateNav();
  }

  function markReadInView() {
    $(".status", readerObj).each(function(index, elem) {
    //for (i in tweetsFromFriends) {
      var tweetId = elem.getAttribute("data-tweet-id");
      var tweet = tweetCache[tweetId];
      if (tweet) {
        // tweets from search will not endup in the cache unless already there
        if (tweet.tdashRead == true) {
          if (tweet.id_str != currTweetSelection) {
            $('#stat'+tweet.id_str).addClass('read');
          }
        }
      }
    });
  }

  this.navClick = function(id) {
    var navChanged = (navSelection != id);

    if (navChanged) {
      currTweetSelection = 0;
    }
    navSelection = id;
    dash.changeMode(mode.friend);

    var tweetList = [];
    var tweetsFromFriends = currUser.twtsFrmFrnds;
    for (var i=tweetsFromFriends.length; i--;) {
      var tweet = tweetCache[tweetsFromFriends[i]];
      if ((navSelection == 0) || (navSelection == tweet.user.id)) {
        tweetList.push(tweet.id_str);   // push + reverse is faster than unshifting everytime
      }
    }
    tweetList.reverse();    // this is to reverse the effect of the countdown

    // Make The readerInfo tab
    var infoStr = folderInfoStartStr;
    if (navSelection != 0) {
      var friend = friends[navSelection];
      var fName = friend.name ? friend.name : "";
      var fLocation = friend["location"] ? friend["location"] : "";
      var fDescr = friend.description ? friend.description : "";
      infoStr += '<td id="friendInfoTd"><b>'+fName+'</b><br/>'+fLocation+'</td>';
      infoStr += '<td id="friendDescrTd">'+fDescr+'</td>';
    } else {
      infoStr += '<td id="friendInfoTd">Showing tweets from all friends</td>';
    }
    infoStr += folderInfoEndStr;
    $("#readerInfo").html(infoStr);

    showTweetsFromList(tweetList);
  }

  function changeSelection(id) {
    currTweetSelection = id;
    var selectedObj = $('.selected');
    selectedObj.find('.plcHld').html('');
    selectedObj.find('.indTd').removeClass('indSel').addClass('indRead');
    selectedObj.removeClass('selected unread');

    var elem = $('#stat'+id).addClass('selected');
    elem.find(".indTd").addClass("indSel");
    var tweet = tweetCache[id];
    if (tweet) {
      markTweetRead(tweet);
    }

    var buttStr = '<table><tbody><tr><td class="buttWrap"><span class="st_button" onclick="dash.replyTo(\''+id+'\')">&#9993;&nbsp;reply</span></td>';
    buttStr += '<td class="buttWrap"><span class="st_button" onclick="dash.retweet(\''+id+'\')">&#8634;&nbsp;retweet</span></td>';
    buttStr += '<tr><td class="buttWrap"><span class="st_button" onclick="dash.fav(\''+id+'\')">&#10084;&nbsp;fav</td>';
    buttStr += '<td class="buttWrap"><span class="st_button" onclick="dash.newRT(\''+id+'\')">Quick RT</span></td>';
    // var replyButt = '<img src="reply.png" />';

    $('#stat'+id+' .plcHld').html(buttStr);

    markReadInView();
    updateNav();

    scrollTo(elem, 400);
  }

  function scrollTo(elem, duration) {
    var prevElem = elem.prev();
    var scrollElem = prevElem.length > 0 ? prevElem : elem;
    if (scrollElem.length > 0) {
      $(readerObj).scrollTo(scrollElem, {'duration':duration});
    }
  }

  this.replyTo = function(id) {
    var tweet = tweetCache[id];
    if (tweet) {
      var replyText = "@"+tweet.user.screen_name+" ";
      $("#replyToParam").val(id);
      $('#replyInfo').html('<p style="font-size:80%;margin:0 1em;text-align:center">Replying to <b id="replyToUserName">'+tweet.user.screen_name+'</b><br/><img title="'+tweet.user.screen_name+'" class="prof_img" src="'+tweet.user.profile_image_url+'"/></p>');
      $("#updateInput").val(replyText).focus().setCursorPosition(replyText.length);
      // $("#updateButton").removeAttr("disabled");
      updateCharCount(replyText.length);
    }
  }

  function clearReply() {
    $("#replyToParam").val(0);
    $('#replyInfo').empty();
  }

  this.retweet = function(id) {
    var tweet = tweetCache[id];
    if (tweet) {
      var retweetText = "RT @"+tweet.user.screen_name+" "+tweet.text;
      clearReply();
      $("#updateInput").val(retweetText).focus().setCursorPosition(0);
      // $("#updateButton").removeAttr("disabled");
      updateCharCount(retweetText.length);
    }
  }

  this.fav = function(id) {
    var message = {
      action:"https://api.twitter.com/1.1/favorites/create/"+id+".xml",
      method:"POST",
      parameters:{}
    }
    makeOAuthForm(message, "fav_");
    $("#favForm").attr("action",message.action);
    setSubmitStatus("Marking as favourite");

    formSubmitted = "fav";
    $("#favForm").submit();
  }

  this.newRTAndDismiss = function(id) {
    $.modal.close();
    dash.newRT(id);
  }
  this.newRT = function(id) {
    var message = {
      action:"https://api.twitter.com/1.1/statuses/retweet/"+id+".xml",
      method:"POST",
      parameters:{}
    }
    makeOAuthForm(message, "rt_");
    $("#rtForm").attr("action",message.action);
    setSubmitStatus("Retweeting");

    formSubmitted = "rt";
    $("#rtForm").submit();
  }

  this.favSubmitted = function() {
    setSubmitStatus("");
  }

  this.clickStat = changeSelection;

  function markTweetRead(tweet) {
    if (!tweet.tdashRead) {
      tweet.tdashRead = true;
      if (tweetReadStore.set) {
        tweetReadStore.set(tweet.id_str, Math.round(getCurrTime()/millisPerHour));
      }
      if (tweetExistsInList(tweet.id_str, currUser.twtsFrmFrnds)) {
        friends[tweet.user.id].tdashUnread--;
        totalFriendUnread--;
      }
      for (i in currUser.userLists) {
        var list = currUser.userLists[i];
        if (tweetExistsInList(tweet.id_str, currUser.twtsFrmLists[list.id])) {
          list.tdashUnread--;
          totalListUnread--;
        }
      }
      for (folderName in folderData) {
        var folder = currUser.folderData[folderName];
        if (tweetExistsInList(tweet.id_str, folder.tweets)) {
          folder.unread--;
          totalFolderUnread--;
        }
      }
    }
  }

  function addTweetsToArray(tweets, list, append) {
    var newTweets = [];
    if (append) {
      var startAppending = false;

      // The for loop is intentionally incremented (not decremented) as order is important
      var tweetsLen = tweets.length;
      for (var i=0; i<tweetsLen; i++) {
        var tweet = tweets[i];
        if (tweet != null) {// This is a workaround for a bug in Twitter API, which returns null sometimes
          // find the first tweet that is not already present in history
          if ((!startAppending) && (!tweetExistsInList(tweet.id_str,list))) {
            // append remaining tweets (inclusive) to history
            startAppending = true;
          }
          if (startAppending) {
            newTweets.push(tweet.id_str);
            addTweetCache(tweet);
          }
        }
      }
      list.push.apply(list,newTweets);

    } else {
      var tweetsLen = tweets.length;

      // The for loop is intentionally incremented (not decremented) as order is important
      for (var i=0; i<tweetsLen; i++) {
        var tweet = tweets[i];
        if (tweet != null) {// This is a workaround for a bug in Twitter API, which returns null sometimes
          if (!tweetExistsInList(tweet.id_str,list)) {
            newTweets.push(tweet.id_str);
            addTweetCache(tweet);
          } else {
            break;
          }
        }
      }
      // apparently the best way to expand an array 
      // see http://stackoverflow.com/questions/1374126/how-to-expand-javascript-array-with-another-array
      list.unshift.apply(list,newTweets);
    }

    return newTweets;
  }


  function getTweets(pageNum, append, getMoreCount) {
    var params = append ? appendFriendParams : currUser.insrtFrndParams;
    params.page = pageNum + 1;

    rQ.nQ(currLogin, false, "Friends' timeline (depth "+getMoreCount+")", "https://api.twitter.com/1.1/statuses/home_timeline.json", params, currLogin, function(tweets, login) {
      lastUpdate = getCurrTime();
      // setStatus("");

      var newTweets = addTweetsToArray(tweets, userData[login].twtsFrmFrnds, append);

      if (newTweets.length > 0) {
        if(!append) {
          params.since_id = tweets[0].id
        }
        updateReader();
      }

      if (getMoreCount > 0) {
        getTweets(pageNum+1, true, getMoreCount-1);
      }
    });
  }

  function isTweetRead(id) {return tweetCache[id].tdashRead}
  function isTweetUnRead(id) {return !(tweetCache[id].tdashRead)}

  function selectNext(jumpToUnread) {
    var foundMatch = -1;
    var foundUnread = false;

    if (currTweetSelection != 0) {
      for (var i in displayedTweetIds) {
        if (!isTweetRead(displayedTweetIds[i])) {
          foundUnread = true;
        }

        if (displayedTweetIds[i] == currTweetSelection) {
          foundMatch = parseInt(i);
          break;
        } 
      }
    }

    if (jumpToUnread
        && (foundMatch == -1)
        && ((navMode == mode.none) || (navMode == mode.trends) || (currTweetSelection != 0))
        && (!foundUnread)) {
      for (var i in mode) {
        var modeId = mode[i]
        var checkerFunc = modeToChecker[modeId];
        var foundFolder = checkerFunc();
        if (foundFolder) {
          dash.changeMode(modeId);
          break;
        }
      }
      /*
      var foundFolder = checkFriendUnread();
      if (!foundFolder) {
        foundFolder = checkListUnread();
      }
      if (!foundFolder) {
        foundFolder = checkFolderUnread();
      }
      if (!foundFolder) {
        foundFolder = checkFriendUnread();
      }
      */
    } else {
      // Trick alert: when foundMatch is -1 we select the first tweet
      changeSelection(displayedTweetIds[foundMatch + 1]);
    }
  }

  function selectPrev() {
    var foundMatch = displayedTweetIds.length;

    for (var i in displayedTweetIds) {
      if (displayedTweetIds[i] == currTweetSelection) {
        foundMatch = parseInt(i);
        break;
      } 
    }

    // Trick alert: when foundMatch is equal to displayedTweetIds.length we select the last tweet
    changeSelection(displayedTweetIds[foundMatch - 1]);
  }


  function markCharCount(elem, len) {

    var remChars = 140 - len;
    elem.text(remChars).removeClass("alert warning error");
    if (remChars < 20) {
      if (remChars < 10) {
        if (remChars < 0) {
          elem.addClass("error");
        } else {
          elem.addClass("warning");
        }
      } else {
        elem.addClass("alert");
      }
    }
  }

  function checkIsNoReply() {
    if ($('#replyToParam').val() != 0) {
      var inputText = $('#updateInput').val()
      var requiredText = '@'+$('#replyToUserName').text();
      if ((inputText == requiredText) || (inputText.indexOf(requiredText+' ') == 0)) {
        return false;
      }
    } else {
      // optimisation, no need to clear
      return false;
    }

    return true;
  }

  function updateCharCount(len) {
    if (len == 0 || checkIsNoReply()) {
      clearReply();
    }
    if (charCountElem == null) {
      charCountElem = $("#updateCharCount");
    }
    markCharCount(charCountElem, len);
  }

  function updateKeyHandler(e) {
    // $("#updateButton").removeAttr("disabled");

    updateCharCount(e.target.value.length);

    CxBrowserStopProp(e);
  }

  function dmKeyHandler(e) {
    // updateCharCount(e.target.value.length);

    markCharCount($("#dmCharCount"), e.target.value.length);
    // CxBrowserStopProp(e);
  }

  function CxBrowserStopProp (e) {
    if (e.cancelBubble) {
      e.cancelBubble = true;
    }
    if (e.stopPropagation) {
      e.stopPropagation();
    }
  }

  function keyHandler(e) {
    if (e.altKey || e.ctrlKey) {
      return;
    }

    var c = String.fromCharCode(e.which);
    if ((e.target.id != 'updateInput') && (e.target.id != 'searchInput')) {
      if (!ignoreEvents) {
        switch(c) {
          case ' ': // SPACE -> Next and also jump to unread folder
            CxBrowserStopProp(e);
            selectNext(true);
            return false; // space bars are treated specially. No propogation
            break;
          case 'j': // j -> Next
          case 'J': // J -> Next
          case 'n': // n -> Next
          case 'N': // n -> Next
          case ' ': // SPACE -> Next
            selectNext();
            CxBrowserStopProp(e);
            break;
          case 'k': // p -> Previous
          case 'K': // p -> Previous
          case 'p': // p -> Previous
          case 'P': // p -> Previous
            selectPrev();
            CxBrowserStopProp(e);
            break;
          case 's': // s -> Status
          case 'S': // s -> Status
            CxBrowserStopProp(e);
            $("#updateInput").focus();
            break;
          case 'r': // r -> reply
          case 'R': // r -> reply
            var t_id = $('.selected').attr('data-tweet-id');
            if (t_id) {
              CxBrowserStopProp(e);
              dash.replyTo(t_id);
            }
            break;
          case 'f': // f -> favourite
          case 'F': // f -> favourite
            var t_id = $('.selected').attr('data-tweet-id');
            if (t_id) {
              dash.fav(t_id);
              CxBrowserStopProp(e);
            }
            break;
          case 't': // t -> retweet
          case 'T': // t -> retweet
            var t_id = $('.selected').attr('data-tweet-id');
            if (t_id) {
              dash.retweet(t_id);
              CxBrowserStopProp(e);
            }
            break;
          case 'q': // t -> quick retweet
          case 'Q': // t -> quick retweet
            var t_id = $('.selected').attr('data-tweet-id');
            if (t_id) {
              $.modal('<p>Are you sure you want to retweet the selected tweet?</p><p><input type="button" value="Yes" onClick="dash.newRTAndDismiss(\''+t_id+'\')"/></p>');
              // dash.newRT(t_id);
              CxBrowserStopProp(e);
            }
            break;
          case 'u': // f -> Update (Fetch new)
          case 'U': // F -> Update (Fetch new)
            CxBrowserStopProp(e);
            dash.fetchNew();
            break;

          case 'o':
          case 'O':  
            CxBrowserStopProp(e);
            // open URL
            var links = $(".selected .text a");
            if (links.length > 1) {

              var urlStr = "<table><tbody>";

              urlSelectBox = links.map(function(i) {
                return $(this).attr("href");
              }).get();

              for (i in urlSelectBox) {
                urlStr += '<tr><td class="urlNumTd">'+i+'</td><td><a target="_blank" href="'+urlSelectBox[i]+'">'+urlSelectBox[i]+'</a></td></tr>';
              }
              urlStr += '</tbody></table><p>Press [Esc] to close this message</p>';

              ignoreEvents = true;
              $.modal("<p>Multiple links found. Press a number to open the URL.</p>"+urlStr, {onClose:function(dlg){ignoreEvents=false;urlSelectBox=null;$.modal.close()}});
            } else if (links.length == 1) {
              dash.regClick($(".selected").attr("data-tweet-id"));
              window.open(links.attr("href"));
            } else {
              if (currTweetSelection != 0) {
                $.modal("<p>No links found!</p><p>Press [Esc] to close this message</p>");
              } else {
                $.modal("<p>To open a URL please select the Tweet first!</p><p>Press [Esc] to close this message</p>");
              }
            }
            break;
          case '?': // ? -> show help screen
            showSelection("keys");
            CxBrowserStopProp(e);
            break;
        }
      } else {
        if (urlSelectBox) {
          if (c >= '0' && c <= '9') {
            dash.regClick($(".selected").attr("data-tweet-id"));
            window.open(urlSelectBox[c]);
          }
        }
      }
    }
    return true;
  }

  function countUnread(tweetIds) {
    var totalUnread = 0;
    for (var j=tweetIds.length; j--;) {
      if (!isTweetRead(tweetIds[j])) {
        totalUnread += 1;
      }
    }
    return totalUnread;
  }

  function getListStatus() {
    for (var i in currUser.userLists) {
      var list = currUser.userLists[i];
      var params =  {per_page:200, "list_id":list.id};

      if (currUser.latestFromList[i]) {
        params.since_id = currUser.latestFromList[i];
      }

      rQ.nQ(currLogin, false, "List " + list.name, "https://api.twitter.com/1.1/lists/statuses.json", params, {"index":i, "login":currLogin}, function(response,data) {
        var user = userData[data.login];
        var list = user.userLists[data.index];
        if (! user.twtsFrmLists[list.id]) {
          user.twtsFrmLists[list.id] = [];
        }
        var newTweets = addTweetsToArray(response, user.twtsFrmLists[list.id], false);
        if (newTweets.length > 0) {
          user.latestFromList[data.index] = newTweets[0];

          var countActualNew = countUnread(newTweets);
          list.tdashUnread += countActualNew;
          if (data.login == currLogin) {
            totalListUnread += countActualNew;
            updateNav();
            if ((navMode == mode.list) && (navSelection == list.id)) {
              redrawCurrentView();
            }
          }
        }
      });
    }
  }

  function DMOutToTweet(response) {
    var retVal = [];
    for (i in response) {
      var elem = response[i];
      var tweet = {user:elem.recipient, favorited:false, in_reply_to_user_id:null, in_reply_to_screen_name:null, in_reply_to_status_id:null,
        source:"web", id_str:"dm"+elem.id_str, text:elem.text, created_at:elem.created_at, tDashOrigId:elem.id_str};
      retVal.push(tweet);
    }
    return retVal;
  }

  function DMInToTweet(response) {
    var retVal = [];
    for (i in response) {
      var elem = response[i];
      var tweet = {user:elem.sender, favorited:false, in_reply_to_user_id:null, in_reply_to_screen_name:null, in_reply_to_status_id:null,
        source:"web", id_str:"dm"+elem.id_str, text:elem.text, created_at:elem.created_at, tDashOrigId:elem.id_str};
      retVal.push(tweet);
    }
    return retVal;
  }

  function getFolderStatus() {
    for (var folderName in folderData) {
      var folder = folderData[folderName];
      var userFolder = userData[currLogin].folderData[folderName];

      rQ.nQ(currLogin, false, folder.name, folder.url, userFolder.options, {"login":currLogin,"name":folderName}, function(response,data) {
        var myFolder = userData[data.login].folderData[data.name];
        var gFolder = folderData[data.name];
        var convertedResponse;
        if (gFolder.converter) {
          convertedResponse = gFolder.converter(response);
        } else {
          convertedResponse = response;
        }

        var newTweets = addTweetsToArray(convertedResponse, myFolder.tweets, false);
        if (newTweets.length > 0) {
          myFolder.options.since_id = convertedResponse[0].tDashOrigId ? convertedResponse[0].tDashOrigId:convertedResponse[0].id_str;

          var countActualNew = $.grep(newTweets,isTweetUnRead).length;
          myFolder.unread += countActualNew;
          if (data.login == currLogin) {
            totalFolderUnread += countActualNew;
            updateNav();
            if ((navMode == mode.folder) && (navSelection == data.name)) {
              redrawCurrentView();
            }
          }
        }
      });
    }
  }

  this.fetchNew = function(depth) {
    depth = depth | 0;
    getTweets(0, false, depth);
    getListStatus();
    getFolderStatus();
    startTrendRoll();
  }

  function setSubmitStatus(txt) {
    $("#statusBar #submitStatus").text(txt);
    resizeIframe();
  }

  function addNewUserLists(response, loginNum) {
    var user = userData[loginNum],
        lists=response;
    // var lists=response.lists;
    for (i in lists) {
      var list = lists[i];
      list.tdashUnread = 0;
      if (! user.twtsFrmLists[list.id]) {
        user.twtsFrmLists[list.id] = [];
      }
    }
    user.userLists.push.apply(user.userLists,lists);
    if (currLogin == loginNum) {
      updateNav();
      redrawCurrentView();
    }
  }

  function getLists(loginNum) {
    rQ.nQ(loginNum, true, "Lists followed", "https://api.twitter.com/1.1/lists/list.json", {}, loginNum,
      function(response, loginNum1) {
        userData[loginNum1].listFetchEnded = true;
        addNewUserLists(response, loginNum1);
        if (loginNum1 == currLogin) dash.fetchNew(2);
      });

    /*
    rQ.nQ(loginNum, true, "Lists created", "https://api.twitter.com/1.1/"+userData[loginNum].credResp.screen_name+"/lists.json", {}, loginNum, function(response, loginNum1) {
      addNewUserLists(response, loginNum1);
      rQ.nQ(loginNum1, true, "Lists followed", "https://api.twitter.com/1.1/"+userData[loginNum].credResp.screen_name+"/lists/subscriptions.json", {}, loginNum1,
        function(response, loginNum2) {
          userData[loginNum2].listFetchEnded = true;
          addNewUserLists(response, loginNum2);
          if (loginNum2 == currLogin) dash.fetchNew(2);
        });
    });
    */
  }

  function showTweetsFromList(tweetList, alwaysShowRead) {
    var shownTweets = [];
    var showRead = alwaysShowRead | $("#showReadButton").get(0).checked;

    for (var i=tweetList.length; i--;) {
      var tweet = tweetCache[tweetList[i]];
      if (showRead || (tweet.tdashRead == false) || (currTweetSelection == tweet.id_str)) {
          shownTweets.push(tweet.id_str);
      }
    }
    if($("#sortOrderSelect").val() == "newTop") {
      shownTweets.reverse();    // to put them back into chronological order
    }
    displayedTweetIds = shownTweets.slice(); // this clones the list

    if (shownTweets.length > 0) {
      buildReaderInternal(shownTweets);
    } else {
      if (tweetList.length > 0) {
        $(readerObj).html('<p class="quickRTLabel" style="padding:2em;margin:2em;font-size:100%;text-align:center">There are <b>'+tweetList.length +'</b> tweets in this folder. To see them tick the "Show Read" checkbox above.</p>');
      } else {
        $(readerObj).html('<p class="quickRTLabel" style="padding:2em;margin:2em;font-size:100%;text-align:center">Either this folder is empty or the tweets are still being fetched.</p>');
      }
    }
    updateNav();
  }

  function redrawCurrentView() {
    if (navMode == mode.friend) {
      dash.navClick(navSelection);
    } else if (navMode == mode.list) {
      dash.listClick(navSelection);
    } else if (navMode == mode.folder) {
      dash.folderClick(navSelection);
    } else if (navMode == mode.trends) {
      showTweetsFromList(displayedSearchTweets);
      // dash.trendClick(navSelection);
    } else if (navMode == mode.tool) {
      dash.toolClick(navSelection);
    } else if (navMode == mode.search) {
      showTweetsFromList(displayedSearchTweets);
    } else {
      // helpful start message
      var totalUnread = (totalFolderUnread + totalFriendUnread + totalListUnread + totalTrendUnread);
      var syncTip =  totalUnread > 500 ? '<hr/><p style="padding:2em;">You have <b>'+totalUnread+'</b> unread tweets.<br/><span class="quickRTLabel pointCursor" onclick="dash.powerSelect(\'sync\')">Click here</span> to mark all tweets as read until a recent date</p>' : '';
      $("#readerInfo").html('<div class="initScreen"><p>Welcome to your dashboard!</p>Click on a menu on the left to get started!</p><p>You can also press the SPACEBAR to jump to the next unread tweet or folder!</p>'+syncTip);
      resizeIframe();
    }
  }

  function findList(listId) {
    for (i in currUser.userLists) {
      if (currUser.userLists[i].id == listId) return currUser.userLists[i];
    }
    return null;
  }

  this.listClick = function (listId) {
    navSelection = listId;
    dash.changeMode(mode.list);

    // Make The readerInfo tab
    var list = findList(listId);
    var infoStr = folderInfoStartStr;
    if (list) {
      infoStr += '<td id="listInfoId"><a target="_blank" href="http://twitter.com' + list.uri + '"><b>'+list.full_name+'</b></a></td>';
      infoStr += '<td id="listDescr">'+list.description+'</td>';
      infoStr += folderInfoEndStr;
      $("#readerInfo").html(infoStr);

      showTweetsFromList(currUser.twtsFrmLists[listId]);
    } else {
      showListHelp();
    }
  }

  this.folderClick = function(folderType) {
    navSelection = folderType;
    dash.changeMode(mode.folder);

    var folder = folderData[folderType];

    if (folder) {

      // Make The readerInfo tab
      var infoStr = folderInfoStartStr;
      infoStr += '<td id="listInfoId"><b>'+folder.name+'</b></td>';
      infoStr += folderInfoEndStr;
      $("#readerInfo").html(infoStr);

      showTweetsFromList(currUser.folderData[folderType].tweets);
    }
  }

  function initUserData(loginNum, credRespA) {
    if (! userData[loginNum]) {
      userData[loginNum] = {
        credResp : credRespA,
        twtsFrmFrnds : [],
        insrtFrndParams : {count:200},
        listFetchEnded : false,
        userLists : [],
        latestFromList : [],
        twtsFrmLists : {},
        cCache : [],
        folderData : {
          'mentions': {unread:0, options:{count:200}, tweets:[]},
          'inbox':    {unread:0, options:{count:200}, tweets:[]},
          'outbox':   {unread:0, options:{count:200}, tweets:[]},
          'favs':     {unread:0, options:{},          tweets:[]},
        },
        savedSearches : null
      }
      return true;
    } else return false;

  }

  function recomputeView() {
    totalListUnread = 0;
    var userLists = currUser.userLists;
    for(var i = userLists.length; i--;) {
      totalListUnread += countUnread(currUser.twtsFrmLists[userLists[i].id]);
    }
    totalFolderUnread = 0;
    for (i in currUser.folderData) {
      totalFolderUnread += currUser.folderData[i].unread;
    }

    
    $("#sortOrderSelect").val(settings[currLogin].oldOnTop?"oldTop":"newTop");
    updateReader();

  }

  this.userSelectChange = function() {
    var selection = $("#logSelect").val();
    if (selection >= 0) {
      currLogin = selection;
      this.showUser(currLogin);
    } else {
      window.location = "/oauth/login?addNewConfirm=true";
    }
  };

  /* NOTE: The currTimeForUpdate is used tricily in order to avoid an anonymous function and closure */
  var currTimeForUpdate = getCurrTime();
  function updateTimes(index,elem, currTime){
    elem.firstChild.nodeValue = formatTime(parseInt(elem.getAttribute('data-created-at')),currTimeForUpdate);
  }

  this.selfUpdater = function() {
    currTimeForUpdate = getCurrTime();

    // update time info of displayed tweets
    $(".createdAt").each(updateTimes);

    if ((currTimeForUpdate - lastUpdate) > ($("#autoFetchSelect").val() * 60 * 1000)) {
      // getTweets(0, false, 0);
      dash.fetchNew();
    }

    setTimeout("dash.selfUpdater();", 60000);
  }

  var url = '';
  var urlStart = 0;
  var urlLength = 0;
  var updateVal = '';

  function makeOAuthForm(message, prefix) {
    OAuth.completeRequest(message, oauthAccessor[currLogin]);        
    OAuth.SignatureMethod.sign(message, oauthAccessor[currLogin]);

    // set the form params
    $("#" + prefix + "oauth_consumer_key").val(message.parameters.oauth_consumer_key);
    $("#" + prefix + "oauth_nonce").val(message.parameters.oauth_nonce);
    $("#" + prefix + "oauth_signature").val(message.parameters.oauth_signature);
    $("#" + prefix + "oauth_timestamp").val(message.parameters.oauth_timestamp);
    $("#" + prefix + "oauth_token").val(message.parameters.oauth_token);
  }

  function updateSubmit() {
    // Replace newlines with CR LF according to this: http://www.w3.org/MarkUp/html-spec/html-spec_toc.html#SEC8.2.1
    var updtText = $("#updateInput").val().replace(/\n/g,"\r\n");

    var message = {
      action:"https://api.twitter.com/1.1/statuses/update.xml",
      method:"POST",
      parameters:{'status':updtText, in_reply_to_status_id:$("#replyToParam").val(),source:"tDash"}
    }
    makeOAuthForm(message, "upd_");
    setSubmitStatus("Submitting status");
    formSubmitted = "update";
    $("#updateForm").submit();
  }

  this.statusSubmitted = function() {
    var upTarget = document.getElementById('upload_target');
    if (formSubmitted) {
      if (formSubmitted == "update") {
        $("#updateInput").val("");
        updateCharCount(0);
        getTweets(0,false,0);
      } else if ((formSubmitted == "dm") || (formSubmitted == "fav")) {
        getFolderStatus();
      } else if (formSubmitted == "rt") {
        getTweets(0,false,0);
      }
      formSubmitted = false;
      upTarget.src="/empty.html";
      setSubmitStatus('Submitted successfully');
      $(readerObj).focus();
    }
  }

  this.dmClick = function() {
    dmVal = $("#dmInput").val();
    var remChars = 140 - dmVal.length;
    if (remChars < 0) {
      alert("Too many chars in DM. Max is 140. Auto URL shortening for DMs is not yet implemented.");
    } else {
      var message = {
        action:"https://api.twitter.com/1.1/direct_messages/new.xml",
        method:"POST",
        parameters:{'text':dmVal,'screen_name':$("#dmScreenName").val()}
      }
      makeOAuthForm(message, "dm_");

      setSubmitStatus("Sending direct message");
      formSubmitted = "dm";
      $("#dmForm").submit();
      $.modal.close();
    }
  }

  this.updateClick = function() {
    updateVal = $("#updateInput").val();
    var remChars = 140 - updateVal.length;
    if (remChars < 0) {
      // find link
      urlStart = updateVal.search(urlPattern);
      if (urlStart >= 0) {
        var headLess = updateVal.slice(urlStart,updateVal.length);
        urlLength = headLess.search(/[\s]/);
        urlLength = urlLength < 0 ? headLess.length : urlLength;
        url = headLess.slice(0, urlLength);
        setSubmitStatus("Shortening URL; url");
        BitlyClient.shorten(url, 'dash.shortenResponse');
      } else {
        alert("Too many chars in update. Max is 140");
        return;
      }
    } else {
      updateSubmit();
    }
  }

  this.shortenResponse = function(response) {
    setSubmitStatus("recvd response");
    if (response.errorCode == 0) {
      var resp = response.results[url];
      if (resp) {
        var shortUrl = resp.shortUrl;
        var shortTweetLength = (updateVal.length - urlLength + shortUrl.length)
        if (shortTweetLength <= 140) {
          // we have a case
          $("#updateInput").val(updateVal.slice(0, urlStart) + shortUrl + updateVal.slice(urlStart + urlLength, updateVal.length));
          updateSubmit();
          return;
        } else {
          alert("After URL shortening, length of tweet is " + shortTweetLength + " which is greater than 140 chars");
          return;
        }
      }
    }
    alert("Couldn't shorten URL successfully");
  }

  function showDM() {
    var divStr = '<div><h2><img style="vertical-align:middle;" src="/images/ink.png"/>Direct Message</h2>';
    if (myFollowers) {
      divStr += '<p>In Twitter, you can send a DM only to someone who follows you.</p>';
      divStr += '<p>Please select one from your list of followers:</p>';
      divStr += '<select id="dmFollowerChoice">';
      for (i in myFollowers) {
        divStr += '<option value="'+myFollowers[i]+'">'+myFollowers[i]+'</option>';
      }
      divStr += '</select>';
    }
    divStr += '<input type="button" onclick="dash.dmChosen();" value="&#9997; DM selected user"/>';
    $.modal(divStr);
  }

  function dlgAutoHeight(dlg){$(dlg.container).css('height','auto')}

  this.imgUpload = function() {
    var loginId = ""
    if (currLogin >= 0) {
      loginId = '&loginId=' +loginMap[currLogin];
    }
    $.modal('<iframe src="/pic/uploadStart?embed=true'+loginId+'" style="width:650px;height:450px;border:none;"/>',{onShow:dlgAutoHeight});
  }

  function showSelection(selection) {
    switch (selection) {
      case "settings":
        $("#settings").modal();
        break;
      case "signOut":
        $.modal('<div><h2>Well, so long...</h2><a href="/oauth/logout">Sign Out?</a></div>');
        break;
      case "themeSelect":
        $.modal('<iframe src="/oauth/chooseTheme?embed=true" style="width:650px;height:450px;border:none;"/>',{opacity:0,onShow:dlgAutoHeight});
        break;
      case "dm":
        showDM();
        break;
      case "keys":
        $.modal(keyStr);
        break;
      case "about":
        $.modal(aboutStr, {onShow:dlgAutoHeight});
        dashOauth.oauthGetJSONP("https://api.twitter.com/1.1/account/rate_limit_status.json", {}, oauthAccessor[currLogin], null, function(response,data) {
          $("#APILimit").html("Twitter API call hourly limit: " + response.hourly_limit +"<br/>Remaining quota for this hour:"+response.remaining_hits);
        });
        break;
      case "feedback":
        UserVoice.Popin.show(uservoiceOptions);
        // $.modal('<h2>We appreciate your feedback</h2><p>Please use <a href="http://tdash.uservoice.com" target="_blank">this forum</a> for ideas, suggestions and bug reports.</p><p>Thanks for your time!</p>');
        break;
      case "picView":
        if (currUser && currUser.credResp) {
          window.open('http://tdash.org/pic/user/'+currUser.credResp.screen_name);
        } else {
          $.modal('<p>You need to be <a href="/oauth/login">logged in</a>!</p>');
        }
        break;
      case "purge":
        var unreadTweets = deleteOld(tweetReadStore, true);
        var unreadTrends = deleteOld(trendReadStore, true);
        $.modal("<h1>Purging old DB content</h1><h2>Just a placeholder right now</h2><p>Backend type:"+Persist.type+"</p><p>Old tweet info: "+unreadTweets[0]+"/"+unreadTweets[1]+"</p><p>Old trend info:"+unreadTrends[0]+"</p>");
        break;
      case "sync":
        var dateScript = $('<script/>').attr('src', '/scripts/jquery.date_input.js').appendTo('head');

        var dateScriptTimer = setInterval(function(){ 
          if (window.DateInput !== undefined) {
            clearInterval(dateScriptTimer);
            dateScript.remove();
            showSyncDlg();
          }
        }, 200);

        // '<script type="text/javascript" src="/scripts/jquery.date_input.js"></script>'
        /*
        $.getScript('/scripts/jquery.date_input.js', function(xhr) {
          try {
            showSyncDlg();
          } catch (err) {
            eval(xhr);
            showSyncDlg();
          }
        });
        */
        break;
      case "enableNotifications":
        if (window.webkitNotifications) {
          if (window.webkitNotifications.checkPermission() == 0) {
            $.modal('<p>Notifications are already enabled!</p>');
          } else {
            window.webkitNotifications.requestPermission();
          }
        } else {
          $.modal('<p>Sorry, your browser doesn\'t support notifications! Try using a recent version of Google-Chrome!</p>');
        }
        break;
      default:
        $.modal('<p>A glitch in the matrix! You probably need to reload the website. Try clearing the caches if needed.</p>');
        break;
    }
  }

  function showSyncDlg() {
    var syncInputStr = '<div id="syncDlg"><h2>Sync (beta)</h2><p>Please select a date to which the display should be synced to.</p><p>All tweets on or before this date will be marked as read</p><input id="max_date_input" type="text" name="date" class="date_input"/><input id="syncButton" type="button" value="Sync"/></div>';
    $.modal(syncInputStr,
      {onShow:function(dlg){
        var dateInput = $("#max_date_input").date_input({maxDate:new Date()})[0];
        $(dlg.container).css('width','auto');
        $(dlg.container).css('height','auto');
        $('#syncButton').click(function() {
          var maxDate = dateInput.selectedDate;
          var countMarked = 0;
          var syncAllTweets = function(id,tweet) {
            if(!tweet.tdashRead) {
              if (new Date(tweet.created_at) <= maxDate) {
                markTweetRead(tweet);
                countMarked += 1;
              }
            }
          }
          
          $.each(tweetCache, syncAllTweets);
          $("#syncDlg").html('<h2>Done!</h2><p>'+countMarked+' tweets were marked as read.</p><p>Press [Esc] to close</p>');
          updateNav();
          redrawCurrentView();
        });
        $(window).resize();
      }});
  }

  this.powerSelect = function(sel) {
    var selection = sel ? sel : $("#powerSelect").val();
    $("#powerSelect").val('more');
    showSelection(selection);
  }

  this.showHelp = function () {
    $.modal('<iframe src="/help.html" style="width:700px;height:500px;"/>');
  }

  this.finishUpload = function(success, resultURL) {
    if (success) {
      $.modal.close();
      $.modal('<h1>Success</h1><p>Your new pic has been posted to Twitter.</p><p>The direct link is <a target="_blank" href="'+resultURL+'">here</a></p>');
    } else {
      $.modal.close();
      $.modal('<h1>Fail</h1>');
    }
  }

  this.showUserDtl = function(evnt, tweetId) {
    CxBrowserStopProp(evnt);
    var thisUser = tweetCache[tweetId].user;
    var infoStr = '<img alt="profile" src="'+ thisUser.profile_image_url + '" /><h1>'+thisUser.screen_name+'</h1>';
    infoStr += '<p><a target="_blank" href="http://twitter.com/' + thisUser.screen_name + '">Twitter profile page</a></p>';
    infoStr += "<p><b>" + thisUser.name+"</b></p>";
    infoStr += "<p><em>" + thisUser.description+"</em></p>";
    infoStr += "<p>Location: <b>" + thisUser["location"]+"</b></p>";
    infoStr += "<p>Friends: <b>" + thisUser.friends_count+"</b></p>";
    infoStr += "<p>Followers: <b>" + thisUser.followers_count+"</b></p>";
    $.modal(infoStr);
  }

  function getSavedSearches(loginNum) {
    rQ.nQ(loginNum, true, "Saved searches", "https://api.twitter.com/1.1/saved_searches/list.json", {}, loginNum, function(response, loginNum1) {
        userData[loginNum1].savedSearches = response;
    });
  }

  this.showUser = function (loginNum, credRespA) {
    var isNewUser = initUserData(loginNum, credRespA);

    if (loginNum == currLogin) {
      // We should be showing this user
      currUser = userData[currLogin];
      myFollowers = followers[currLogin];
    }
    var credResp = credRespA ? credRespA : currUser.credResp;

    countOfValidUsers += 1;

    if (isNewUser) {
      $("#logSelect").append('<option value="'+loginNum+'">'+credResp.screen_name+'</option>');
      if (countOfValidUsers == 1) {
        $("#logSelect").prepend('<option value="-1">Add an account</option>').val(loginNum);
      }
      getLists(loginNum);
      getSavedSearches(loginNum);
    }

    if (loginNum == currLogin) {
      setSubmitStatus("");
      navSelection = 0;
      currTweetSelection = 0;
      dash.changeMode(mode.none);
      displayedTweetIds = [];

      $(readerObj).empty();
      $("#view").show();
      $("#logInfo").html('<img style="padding:.2em .4em .2em 0;vertical-align:middle" class="prof_img" src="'+credResp.profile_image_url+'"/><span class="name">'+credResp.name+'</span>');
      recomputeView();
      updateNav();

      if (currUser.listFetchEnded) {
        dash.fetchNew();
      }

      // updateReader();
      // this.selfUpdater();
    }
  };

  this.startRoll = function () {
    $("#versionStatus").text('tDash v' + currVersion);

    $("#readerInfo").html('<div class="initScreen"><p>Authenticating. If nothing happens for a long time <span id="showAuthTip">click here</span></p></div><div style="display:none;" id="authFailTip">If you are not able to authenticate, <ul><li><a href="http://twitter.com">Twitter</a> could be overloaded or inaccessible. Try browsing to it.</li><li>You might not have signed in yet! <a href="/oauth/login">Click here</a> to sign in.</li><li>You might be using an old version of tDash. Follow <a href="http://twitter.com/tdash">@tdash</a> to know about important updates.</li><li>You might have revoked permission to tDash.org from your Twitter control panel. <a href="http://twitter.com/settings/connections" target="_blank">Click here</a> to check it.<br/>If so, you will have to sign in again.</li></ul>');
    $("#showAuthTip").click(function() {$("#authFailTip").show();});

    $("#showReadButton").change(redrawCurrentView);

    resizeIframe();


    var failAuth = false;

    if (oauthAccessor && oauthAccessor.length > 0) {
      $("#updateInput").keyup(updateKeyHandler);
      $(document).keypress(keyHandler);

      for (i in oauthAccessor) {
        rQ.nQ(i, true, "Authentication", "https://api.twitter.com/1.1/account/verify_credentials.json", {}, i, function(response,index) {
          if (response.screen_name) {
            if (currLogin < 0) {
              $("#readerInfo").html("Authentication successful. Please wait...");
              currLogin = index;
            }
            dash.showUser(index, response);
          }
        });
      }
    } else {
      failAuth = true;
    }

    if(failAuth) {
      $.modal('<div style="text-align:center" id="alert"><p>Welcome to <b><a class="linkNoDec" href="/"><img class="linkNoDec" alt="tDash" style="vertical-align:middle;padding:0 10px;" src="/images/logoColorSmall.png"/></a></b> a browser based client for Twitter.</p><hr/><p><img src="/images/user_warning_48.png" /></p><p>You are not authenticated!<br/><span class="noAuthTip">(Cookies need to be enabled for authentication)</span></p><p>Would you like to <a href="/oauth/login">Sign in</a>?<br/><span class="noAuthTip">oAuth is used for signing in with Twitter.<br/>No registration is required!</span></p></div>',
          {onShow:function(dlg){$(dlg.container).css('width','auto');$(dlg.container).css('height','auto')}});
    } else {
      this.selfUpdater();
    }
  }

  var trendCache = {};
  var trendHistoryCache = {}; // stores state of trends which have been read in the current session
  var trendReadCache = {};

  this.trendRead = function (name) {
    trendReadCache[name] = true;
    if (trendReadStore.set) {
      trendReadStore.set(name, Math.round(getCurrTime()/millisPerHour));
    }
    $("#trendReadBtn").hide();
  }

  function markTrendRead(trend) {
    if (!trend.tdashRead) {
      trendHistoryCache[trend.name] = true;
      trend.tdashRead = true;
      totalTrendUnread--;
      return true;
    }
    return false;
  }

  this.regClick = function (tweetId) {
    if (!tweetExistsInList(tweetId, currUser.cCache)) {
      currUser.cCache.unshift(tweetId);
      updateNav();
    }
  }

  this.toolClick = function(toolType) {
    switch (toolType) {
      case "clicks":
        navSelection = toolType;
        dash.changeMode(mode.tool);
        displayedTweetIds = currUser.cCache;
        $("#readerInfo").html('Your recent URL clicks, showing most recent on top! How is this useful? Click <a href="http://blog.tdash.org/our-new-feature-click-history" target="_blank">here</a> to know more.');
        buildReaderInternal(currUser.cCache);
        break;
    }
  };

  this.trendClick = function(trendId) {
    navSelection = trendId;
    dash.changeMode(mode.trends);

    if (trendId != 0) {
      var trend = trendCache[trendId];
      if (trend) {
        displayedTweetIds = [];
        var metaStr = '';
        var wasUnread = markTrendRead(trend);
        updateNav();
        metaStr += '<div id="trendData">';
        metaStr += '<h2>'+trend.name+'</h2>';
        metaStr += '<p id="trendDescr">'+trend.description.text+'</p>';
        metaStr += '<p>[<a href="http://whatthetrend.com/trend/'+escape(trend.query)+'" target="_blank">Edit description</a>]';
        if (trendReadStore.set && wasUnread) {
          metaStr += '&nbsp;<b id="trendReadBtn" class="st_button" onclick="dash.trendRead(\''+trend.name+'\');">Permanently mark Trend as Read</b>';
        }
        metaStr += '&nbsp;<a id="wttAttrib" target="_blank" href="http://api.whatthetrend.com/">Data by What the Trend?</a></p>';
        metaStr += '</div>';
        $("#readerInfo").html(metaStr);
        $(readerObj).html('<div id="searchResults">Searching tweets for this trend...</div>');
        $.getJSON("https://search.twitter.com/search.json?q="+escape(trend.query)+"&show_user=true&result_type=recent&rpp=100&callback=?",{},function(response){
          if ((navMode == mode.trends) && response.results) {
          buildSearchResults(response.results);
          /*
            var resultStr = '';
            for (i in response.results) {
              var tweet = response.results[i];
              resultStr += '<div class="status"><table class="stTbl centerAlignTbl" cellspacing="0"><tr>';
              resultStr += '<td class="imgTd"><a class="linkNoDec" href="https://twitter.com/'+tweet.from_user+'" target="_blank"><img class="prof_img" src="'+tweet.profile_image_url+'" /></a></td>';
              resultStr += '<td><p class="text">'+formatText(tweet.text, tweet.id_str)+'</p>';
              resultStr += '</td></tr></table></div>';
            }
            $("#reader").html(resultStr);
            resizeIframe();
            */
          }
        });
      }
    }
  }

  function buildSearchResults(tweets) {
    /*
    var tweetStr = '';
    */

    displayedTweetIds = [];
    var shownTweetIds = [];

    for (var i = tweets.length; i--;) {
      var tweet = tweets[i];
      tweet.user = {
        id:tweet.from_user_id,
        profile_image_url:tweet.profile_image_url,
        screen_name:tweet.from_user
      };
      tweet.source = tweet.source.replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g,'"');
      tweet.in_reply_to_screen_name = tweet.to_user;
      addTweetCache(tweet);
      shownTweetIds.unshift(tweet.id_str);
    }

    /*
    $('#reader').html(tweetStr);
    */

    displayedSearchTweets = shownTweetIds;
    showTweetsFromList(shownTweetIds);
  }

  function performSearch(name, query) {
    displayedSearchTweets = [];
    var metaStr = '';
    updateNav();
    metaStr += '<div id="trendData">';
    metaStr += '<h2>'+name+'</h2>';
    metaStr += '<p id="trendDescr">Please wait</p>';
    metaStr += '</div>';
    $("#readerInfo").html(metaStr);
    $(readerObj).html('<div id="searchResults">Searching tweets for this query...</div>');
    $.getJSON("https://search.twitter.com/search.json?q="+escape(query)+"&show_user=true&result_type=recent&rpp=100&callback=?",{},function(response){
      if ((navMode == mode.search) && response.results) {
        $("#trendDescr").html('Found ' + response.results.length + ' results.');
        buildSearchResults(response.results);
        /*
        var resultStr = '';

        for (i in response.results) {
          var tweet = response.results[i];
          resultStr += '<div class="status"><table class="stTbl centerAlignTbl" cellspacing="0"><tr>';
          resultStr += '<td class="imgTd"><a class="linkNoDec" href="https://twitter.com/'+tweet.from_user+'" target="_blank"><img class="prof_img" src="'+tweet.profile_image_url+'" /></a></td>';
          resultStr += '<td><p class="text">'+formatText(tweet.text, tweet.id_str)+'</p>';
          resultStr += '</td></tr></table></div>';
        }
        $("#reader").html(resultStr);
        resizeIframe();
        */
      }
    });
  }

  this.searchClick = function(searchIndex) {
    navSelection = searchIndex + 1;
    dash.changeMode(mode.search);

    var search = currUser.savedSearches[searchIndex];
    performSearch(search.name, search.query);

  }

  this.timeOrderChange = function() {
    var oldOnTopSetting = $("#sortOrderSelect").val() == "oldTop";
    if (currLogin >= 0) {
      if (settings[currLogin].oldOnTop != oldOnTopSetting) {
        // post update
        settings[currLogin].oldOnTop = oldOnTopSetting;
        $.post('/oauth/settingTimeOrder?value='+oldOnTopSetting+'&loginId='+loginMap[currLogin]);
      }
      redrawCurrentView();
    }
  }

  function startTrendRoll() {
    $.getJSON("http://api.whatthetrend.com/api/v2/trends.json?callback=?", {}, function(response) {
      totalTrendUnread = 0;
      trendCache = {};

      for (i in response.trends) {
        var trend = response.trends[i];
        // if (trendReadCache[trend.name] || (prevTrendCache[trend.name] && prevTrendCache[trend.name].tDashRead)) 
        if (trendReadCache[trend.name] || trendHistoryCache[trend.name]) {
          trend.tdashRead = true;
          trendHistoryCache[trend.name] = true;
        }
        trendCache[trend.name] = trend;
        if (!trend.tdashRead) {
          totalTrendUnread++;
          if (trendReadStore.get) {
            if(trendReadStore.get(trend.name)) {
              trendReadCache[trend.name] = true;
              trendCache[trend.name].tdashRead = true;
              totalTrendUnread--;
            }
          }
        }
      }
      updateNav();
    });
  }

  this.searchKeyPress = function(e) {
    if (e.altKey || e.ctrlKey) {
      return;
    }

    if (e.which == 13) {
      $(readerObj).focus();
      var input = $('#searchInput').val();
      performSearch(input, input);
    }
  }
};

/*
function pageY(elem) {
  return elem.offsetParent ? (elem.offsetTop + pageY(elem.offsetParent)) : elem.offsetTop;
}
*/

// TODO completely remove bufferHeight
var bufferHeight = 0;
function resizeIframe() {
  readerObj.style.height = '0px';
  var height = $(window).height();
  var leftPadHeight = 10;
  $('.paddedLeftTd').each(function() {leftPadHeight += $(this).outerHeight(true)});
  if (height < leftPadHeight) {
    height = leftPadHeight;
  }

  var offset = $(readerObj).offset();
  height -= (offset.top + $('#statusBar').outerHeight(true) + bufferHeight);
  height = (height < 0) ? 0 : height;
  readerObj.style.height = height + 'px';
}

var dashOauth = new function () {
  var totalCalls = 0;
  function oauthGetJSONP_Internal(url, inParams, accessor, callBackData, completionFunc) {
    var callBackName = "ojsonp" + totalCalls++;
    var params = $.extend({}, inParams);
    params.callback = callBackName;

    // oauth sign
    var message = {
      action: url,
      method: "GET",
      parameters: params
    };
    OAuth.completeRequest(message, accessor);        
    OAuth.SignatureMethod.sign(message, accessor);
    var builtURL = url + "?" + OAuth.formEncode(message.parameters);

    executeJSONP(callBackName, builtURL, callBackData, completionFunc);
  }

  function executeJSONP(callBackName, url, callBackData, completionFunc) {
    var head = document.getElementsByTagName("head")[0];
    var script = document.createElement("script");

    // build the callback function
    window[callBackName] = function(tmp) {
      completionFunc(tmp, callBackData);
      // garbage collect
      window[callBackName] = undefined;
      try {delete window[callBackName];} catch (e) {}
      if (head)
        head.removeChild(script);
    };

    // insert the script tag
    script.src = url;
    head.appendChild(script);
  }

  this.getJSONP = function(url, inParams, data, completionFunc) {
    var callBackName = "njsonp" + totalCalls++;
    var params = $.extend({}, inParams);
    var paramsFound = false;

    $.each(params, function(index, value) {
        paramsFound = true;
        if (value == '?') {
          params[index] = callBackName;
          return false; // to stop further iteration
        }
    });
    var paramURL = (paramsFound) ? "?" + $.param(params) : "";

    executeJSONP(callBackName, url+paramURL, data, completionFunc);
  };

  this.oauthGetJSONP = function(url, params, accessor, data, completionFunc) {
    oauthGetJSONP_Internal(url,params,accessor,data, completionFunc);
  };

};

var tweetReadStore = null;
var trendReadStore = null;

function deleteOld (store, onlyCount) {
  if (store.iterate) {
    var currTime = (getCurrTime()/ 3600000.0);
    var oldTime = currTime - 15*24;
    var oldKeys = [];
    var total = 0;
    store.iterate(function(key,value) {

      total++;

      if (parseFloat(value) < oldTime) {
        oldKeys.push(key);
      }

    });

    if (!onlyCount) {
      for (var i = oldKeys.length; i--;) {
        store.remove(oldKeys[i]);
      }
    }

    return [oldKeys.length, total];
  }

}

$(document).ready(function() {
  readerObj = document.getElementById('reader');
  window.onresize = resizeIframe;

  $("body").append('<iframe style="display:none" id="upload_target" name="upload_target" onload="dash.statusSubmitted()"></iframe>');

/* Doesn't seem to be needed
  if ($("#paddedLeftTd").width() < 100) {
    $("#paddedLeftTd").width(100);
  }
*/

  Persist.remove('cookie');
  tweetReadStore = new Persist.Store('tDash', {domain:'tdash.org'});
  trendReadStore = new Persist.Store('tDashTrend', {domain:'tdash.org'});

  // deleteOld(tweetReadStore);
  // deleteOld(trendReadStore);

  $("#searchInput").keyup(dash.searchKeyPress);
  dash.startRoll();
});
