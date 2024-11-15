var _____WB$wombat$assign$function_____ = function(name) {return (self._wb_wombat && self._wb_wombat.local_init && self._wb_wombat.local_init(name)) || self[name]; };
if (!self.__WB_pmw) { self.__WB_pmw = function(obj) { this.__WB_source = obj; return this; } }
{
  let window = _____WB$wombat$assign$function_____("window");
  let self = _____WB$wombat$assign$function_____("self");
  let document = _____WB$wombat$assign$function_____("document");
  let location = _____WB$wombat$assign$function_____("location");
  //let top = _____WB$wombat$assign$function_____("top");
  let parent = _____WB$wombat$assign$function_____("parent");
  let frames = _____WB$wombat$assign$function_____("frames");
  let opener = _____WB$wombat$assign$function_____("opener");

if (document.addEventListener) {
    document.addEventListener("DOMContentLoaded", function() {
        loaded();
    });
} else if (document.attachEvent) {
    document.attachEvent("onreadystatechange", function() {
        loaded();
    });
}

function loaded() {

    setInterval(loop, 400);

}

var x = 0;

var titleText = [ "✩", "✩𝓬", "✩𝓬𝓪", "✩𝓬𝓪𝓭", "✩𝓬𝓪𝓭𝓮", "✩𝓬𝓪𝓭𝓮𝓷", "✩𝓬𝓪𝓭𝓮𝓷✩" ];

function loop() {

    document.getElementsByTagName("title")[0].innerHTML = titleText[x++%titleText.length];

}


}
/*
     FILE ARCHIVED ON 01:17:39 Feb 06, 2021 AND RETRIEVED FROM THE
     INTERNET ARCHIVE ON 16:01:42 Nov 15, 2024.
     JAVASCRIPT APPENDED BY WAYBACK MACHINE, COPYRIGHT INTERNET ARCHIVE.

     ALL OTHER CONTENT MAY ALSO BE PROTECTED BY COPYRIGHT (17 U.S.C.
     SECTION 108(a)(3)).
*/
/*
playback timings (ms):
  captures_list: 0.896
  exclusion.robots: 0.024
  exclusion.robots.policy: 0.011
  esindex: 0.02
  cdx.remote: 5.162
  LoadShardBlock: 115.412 (3)
  PetaboxLoader3.datanode: 91.111 (4)
  PetaboxLoader3.resolve: 97.824 (2)
  load_resource: 109.189
*/