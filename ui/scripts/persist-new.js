if(typeof deconcept=="undefined"){var deconcept=new Object()}if(typeof deconcept.util=="undefined"){deconcept.util=new Object()}if(typeof deconcept.SWFObjectUtil=="undefined"){deconcept.SWFObjectUtil=new Object()}deconcept.SWFObject=function(f,d,m,g,j,l,n,i,a,e){if(!document.getElementById){return}this.DETECT_KEY=e?e:"detectflash";this.skipDetect=deconcept.util.getRequestParameter(this.DETECT_KEY);this.params=new Object();this.variables=new Object();this.attributes=new Array();if(f){this.setAttribute("swf",f)}if(d){this.setAttribute("id",d)}if(m){this.setAttribute("width",m)}if(g){this.setAttribute("height",g)}if(j){this.setAttribute("version",new deconcept.PlayerVersion(j.toString().split(".")))}this.installedVer=deconcept.SWFObjectUtil.getPlayerVersion();if(!window.opera&&document.all&&this.installedVer.major>7){deconcept.SWFObject.doPrepUnload=true}if(l){this.addParam("bgcolor",l)}var b=n?n:"high";this.addParam("quality",b);this.setAttribute("useExpressInstall",false);this.setAttribute("doExpressInstall",false);var k=(i)?i:window.location;this.setAttribute("xiRedirectUrl",k);this.setAttribute("redirectUrl","");if(a){this.setAttribute("redirectUrl",a)}};deconcept.SWFObject.prototype={useExpressInstall:function(a){this.xiSWFPath=!a?"expressinstall.swf":a;this.setAttribute("useExpressInstall",true)},setAttribute:function(a,b){this.attributes[a]=b},getAttribute:function(a){return this.attributes[a]},addParam:function(a,b){this.params[a]=b},getParams:function(){return this.params},addVariable:function(a,b){this.variables[a]=b},getVariable:function(a){return this.variables[a]},getVariables:function(){return this.variables},getVariablePairs:function(){var a=new Array();var b;var c=this.getVariables();for(b in c){a.push(b+"="+c[b])}return a},getSWFHTML:function(){var d="";if(navigator.plugins&&navigator.mimeTypes&&navigator.mimeTypes.length){if(this.getAttribute("doExpressInstall")){this.addVariable("MMplayerType","PlugIn");this.setAttribute("swf",this.xiSWFPath)}d='<embed type="application/x-shockwave-flash" src="'+this.getAttribute("swf")+'" width="'+this.getAttribute("width")+'" height="'+this.getAttribute("height")+'"';d+=' id="'+this.getAttribute("id")+'" name="'+this.getAttribute("id")+'" ';var c=this.getParams();for(var a in c){d+=[a]+'="'+c[a]+'" '}var b=this.getVariablePairs().join("&");if(b.length>0){d+='flashvars="'+b+'"'}d+="/>"}else{if(this.getAttribute("doExpressInstall")){this.addVariable("MMplayerType","ActiveX");this.setAttribute("swf",this.xiSWFPath)}d='<object id="'+this.getAttribute("id")+'" classid="clsid:D27CDB6E-AE6D-11cf-96B8-444553540000" width="'+this.getAttribute("width")+'" height="'+this.getAttribute("height")+'">';d+='<param name="movie" value="'+this.getAttribute("swf")+'" />';var c=this.getParams();for(var a in c){d+='<param name="'+a+'" value="'+c[a]+'" />'}var b=this.getVariablePairs().join("&");if(b.length>0){d+='<param name="flashvars" value="'+b+'" />'}d+="</object>"}return d},write:function(a){if(this.getAttribute("useExpressInstall")){var b=new deconcept.PlayerVersion([6,0,65]);if(this.installedVer.versionIsValid(b)&&!this.installedVer.versionIsValid(this.getAttribute("version"))){this.setAttribute("doExpressInstall",true);this.addVariable("MMredirectURL",escape(this.getAttribute("xiRedirectUrl")));document.title=document.title.slice(0,47)+" - Flash Player Installation";this.addVariable("MMdoctitle",document.title)}}if(this.skipDetect||this.getAttribute("doExpressInstall")||this.installedVer.versionIsValid(this.getAttribute("version"))){var c=(typeof a=="string")?document.getElementById(a):a;c.innerHTML=this.getSWFHTML();return true}else{if(this.getAttribute("redirectUrl")!=""){document.location.replace(this.getAttribute("redirectUrl"))}}return false}};deconcept.SWFObjectUtil.getPlayerVersion=function(){var b=new deconcept.PlayerVersion([0,0,0]);if(navigator.plugins&&navigator.mimeTypes.length){var a=navigator.plugins["Shockwave Flash"];if(a&&a.description){b=new deconcept.PlayerVersion(a.description.replace(/([a-zA-Z]|\s)+/,"").replace(/(\s+r|\s+b[0-9]+)/,".").split("."))}}else{try{var c=new ActiveXObject("ShockwaveFlash.ShockwaveFlash.7")}catch(d){try{var c=new ActiveXObject("ShockwaveFlash.ShockwaveFlash.6");b=new deconcept.PlayerVersion([6,0,21]);c.AllowScriptAccess="always"}catch(d){if(b.major==6){return b}}try{c=new ActiveXObject("ShockwaveFlash.ShockwaveFlash")}catch(d){}}if(c!=null){b=new deconcept.PlayerVersion(c.GetVariable("$version").split(" ")[1].split(","))}}return b};deconcept.PlayerVersion=function(a){this.major=a[0]!=null?parseInt(a[0]):0;this.minor=a[1]!=null?parseInt(a[1]):0;this.rev=a[2]!=null?parseInt(a[2]):0};deconcept.PlayerVersion.prototype.versionIsValid=function(a){if(this.major<a.major){return false}if(this.major>a.major){return true}if(this.minor<a.minor){return false}if(this.minor>a.minor){return true}if(this.rev<a.rev){return false}return true};deconcept.util={getRequestParameter:function(d){var c=document.location.search||document.location.hash;if(c){var b=c.substring(1).split("&");for(var a=0;a<b.length;a++){if(b[a].substring(0,b[a].indexOf("="))==d){return b[a].substring((b[a].indexOf("=")+1))}}}return""}};deconcept.SWFObjectUtil.cleanupSWFs=function(){var c=document.getElementsByTagName("OBJECT");for(var b=0;b<c.length;b++){c[b].style.display="none";for(var a in c[b]){if(typeof c[b][a]=="function"){c[b][a]=function(){}}}}};if(deconcept.SWFObject.doPrepUnload){deconcept.SWFObjectUtil.prepUnload=function(){__flash_unloadHandler=function(){};__flash_savedUnloadHandler=function(){};window.attachEvent("onunload",deconcept.SWFObjectUtil.cleanupSWFs)};window.attachEvent("onbeforeunload",deconcept.SWFObjectUtil.prepUnload)}if(Array.prototype.push==null){Array.prototype.push=function(a){this[this.length]=a;return this.length}};(function(){if(window.google&&google.gears){return}var a=null;if(typeof GearsFactory!="undefined"){a=new GearsFactory()}else{try{a=new ActiveXObject("Gears.Factory");if(a.getBuildInfo().indexOf("ie_mobile")!=-1){a.privateSetGlobalObject(this)}}catch(b){if((typeof navigator.mimeTypes!="undefined")&&navigator.mimeTypes["application/x-googlegears"]){a=document.createElement("object");a.style.display="none";a.width=0;a.height=0;a.type="application/x-googlegears";document.documentElement.appendChild(a)}}}if(!a){return}if(!window.google){google={}}if(!google.gears){google.gears={factory:a}}})();Persist=(function(){var e="0.2.0",d,g,a,f,c;var b=(function(){if(Array.prototype.indexOf){return function(h,i){return Array.prototype.indexOf.call(h,i)}}else{return function(k,m){var j,h;for(j=0,h=k.length;j<h;j++){if(k[j]==m){return j}}return -1}}})();c=function(){};a=function(h){return"PS"+h.replace(/_/g,"__").replace(/ /g,"_s")};C={search_order:["localstorage","globalstorage","gears","ie","flash"],name_re:/^[a-z][a-z0-9_ -]+$/i,methods:["init","get","set","remove","load","save","iterate"],sql:{version:"1",create:"CREATE TABLE IF NOT EXISTS persist_data (k TEXT UNIQUE NOT NULL PRIMARY KEY, v TEXT NOT NULL)",get:"SELECT v FROM persist_data WHERE k = ?",set:"INSERT INTO persist_data(k, v) VALUES (?, ?)",remove:"DELETE FROM persist_data WHERE k = ?",keys:"SELECT * FROM persist_data"},flash:{div_id:"_persist_flash_wrap",id:"_persist_flash",path:"persist.swf",size:{w:1,h:1},args:{autostart:true}}};g={gears:{size:-1,test:function(){return(window.google&&window.google.gears)?true:false},methods:{init:function(){var h;h=this.db=google.gears.factory.create("beta.database");h.open(a(this.name));h.execute(C.sql.create).close()},get:function(j){var k,l=C.sql.get;var h=this.db;var i;h.execute("BEGIN").close();k=h.execute(l,[j]);i=k.isValidRow()?k.field(0):null;k.close();h.execute("COMMIT").close();return i},set:function(k,n){var i=C.sql.remove,m=C.sql.set,l;var h=this.db;var j;h.execute("BEGIN").close();h.execute(i,[k]).close();h.execute(m,[k,n]).close();h.execute("COMMIT").close();return n},remove:function(i){var j=C.sql.get;sql=C.sql.remove,r,val=null,is_valid=false;var h=this.db;h.execute("BEGIN").close();h.execute(sql,[i]).close();h.execute("COMMIT").close();return true},iterate:function(k,j){var i=C.sql.keys;var l;var h=this.db;l=h.execute(i);while(l.isValidRow()){k.call(j||this,l.field(0),l.field(1));l.next()}l.close()}}},globalstorage:{size:5*1024*1024,test:function(){return window.globalStorage?true:false},methods:{key:function(h){return a(this.name)+a(h)},init:function(){this.store=globalStorage[this.o.domain]},get:function(h){h=this.key(h);return this.store.getItem(h)},set:function(h,i){h=this.key(h);this.store.setItem(h,i);return i},remove:function(h){var i;h=this.key(h);i=this.store[h];this.store.removeItem(h);return i},}},localstorage:{size:-1,test:function(){return window.localStorage?true:false},methods:{key:function(h){return this.name+">"+h},init:function(){this.store=localStorage},get:function(h){h=this.key(h);return this.store.getItem(h)},set:function(h,i){h=this.key(h);this.store.setItem(h,i);return i},remove:function(h){var i;h=this.key(h);i=this.store.getItem(h);this.store.removeItem(h);return i},iterate:function(n,m){var h=this.store;for(var k=h.length;k--;){var j=h.key(k);var o=j.split(">");if((o.length==2)&&(o[0]==this.name)){n.call(m||this,o[1],h.getItem(j))}}}}},ie:{prefix:"_persist_data-",size:64*1024,test:function(){return window.ActiveXObject?true:false},make_userdata:function(i){var h=document.createElement("div");h.id=i;h.style.display="none";h.addBehavior("#default#userdata");document.body.appendChild(h);return h},methods:{init:function(){var h=g.ie.prefix+a(this.name);this.el=g.ie.make_userdata(h);if(this.o.defer){this.load()}},get:function(h){var i;h=a(h);if(!this.o.defer){this.load()}i=this.el.getAttribute(h);return i},set:function(h,i){h=a(h);this.el.setAttribute(h,i);if(!this.o.defer){this.save()}return i},remove:function(h){var i;h=a(h);if(!this.o.defer){this.load()}i=this.el.getAttribute(h);this.el.removeAttribute(h);if(!this.o.defer){this.save()}return i},load:function(){this.el.load(a(this.name))},save:function(){this.el.save(a(this.name))}}},flash:{test:function(){if(!deconcept||!deconcept.SWFObjectUtil){return false}var h=deconcept.SWFObjectUtil.getPlayerVersion().major;return(h>=8)?true:false},methods:{init:function(){if(!g.flash.el){var k,i,j,h=C.flash;j=document.createElement("div");j.id=h.div_id;document.body.appendChild(j);k=new deconcept.SWFObject(this.o.swf_path||h.path,h.id,h.size.w,h.size.h,"8");for(i in h.args){k.addVariable(i,h.args[i])}k.write(j);g.flash.el=document.getElementById(h.id)}this.el=g.flash.el},get:function(h){var i;h=a(h);i=this.el.get(this.name,h);return i},set:function(i,j){var h;i=a(i);h=this.el.set(this.name,i,j);return h},remove:function(h){var i;h=a(h);i=this.el.remove(this.name,h);return i}}}};var f=function(){var n,j,h,m,k=C.methods,o=C.search_order;for(n=0,j=k.length;n<j;n++){d.Store.prototype[k[n]]=c}d.type=null;d.size=-1;for(n=0,j=o.length;!d.type&&n<j;n++){h=g[o[n]];if(h.test()){d.type=o[n];d.size=h.size;for(m in h.methods){d.Store.prototype[m]=h.methods[m]}}}d._init=true};d={VERSION:e,type:null,size:0,add:function(h){g[h.id]=h;C.search_order=[h.id].concat(C.search_order);f()},remove:function(i){var h=b(C.search_order,i);if(h<0){return}C.search_order.splice(h,1);delete g[i];f()},Store:function(h,i){if(!C.name_re.exec(h)){throw new Error("Invalid name")}if(!d.type){throw new Error("No suitable storage found")}i=i||{};this.name=h;i.domain=i.domain||location.hostname||"localhost";i.domain=i.domain.replace(/:\d+$/,"");i.domain=(i.domain=="localhost")?"":i.domain;this.o=i;i.expires=i.expires||365*2;i.path=i.path||"/";this.init()}};f();return d})();