var dashOauth = new function () {
  var totalCalls = 0;
  function oauthGetJSONP_Internal(url, params, accessor, callBackData, completionFunc) {
    var callBackName = "jsonp" + totalCalls++;
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
    // var builtURL = url + "?" + $.param(params) + '&' + OAuth.formEncode(message.parameters);

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
    var head = document.getElementsByTagName("head")[0];
    var script = document.createElement("script");
    script.src = builtURL;
    head.appendChild(script);
  }

  this.oauthGetJSONP = function(url, params, accessor, data, completionFunc) {
    oauthGetJSONP_Internal(url,params,accessor,data, completionFunc);
  };
};

var dash = new function() {
  function internalShowCount(n, id, descr) {
    var ucstr = '';
    for (var i = 0; i < n.length; i++) {
      ucstr += '<span class="countDigit">'+n.slice(i, i+1)+'</span>';
    }
    $("#"+id).html(ucstr);
    $("#"+id+"Descr").text(descr);
  }

  this.showCount = internalShowCount;
}

function featureShow(event) {
  var id = $(this).attr("data-id");
  $("#featureDescr td[data-id|="+id+"]").addClass("featureShown");
}

function featureHide(event) {
  var id = $(this).attr("data-id");
  $("#featureDescr td[data-id|="+id+"]").removeClass("featureShown");
}

$(document).ready(function() {
  dash.showCount(imgCount, "imgCount", "images uploaded");
  dash.showCount(userCount, "userCount", "users signed-in");
  $("#featureDescr td").hover(featureShow, featureHide);

  if (oauthAccessor.length > 0) {
    for (acc in oauthAccessor) {
      dashOauth.oauthGetJSONP("http://api.twitter.com/1/account/verify_credentials.json", {}, oauthAccessor[acc], null, function(response,data) {
        if (response.screen_name) {
          if ($("#loggedInInfo").length == 0) {
            // $("#badges").html('<a target="_blank" href="http://oneforty.com/item/tdash?utm_source=badge"><img src="http://cdn.oneforty.com/ext/badge-red.png" width="120" height="120" style="border: 0px" alt="Review tDash on oneforty"></img></a>');
    
            // we are the first
            var helperStr = '<div id="loggedInInfo"><p>Welcome back <b id="names">' + response.screen_name + '</b> !</p>';
            helperStr += '<p><a href="/oauth/viewPower">Proceed to dashboard?</a></p>';
            helperStr += '<p><a href="/pic/uploadStart">Upload a pic?</a></p>';
            helperStr += '<p><a href="/oauth/logout">Sign out?</a></p>';
            helperStr == '</div>';
            $("#loginInfo").html(helperStr);
          } else {
            // just append
            $("#names").append(", " + response.screen_name);
          }
        }
      });
    }
  }
});
