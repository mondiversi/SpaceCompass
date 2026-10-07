package me.mondiversi.spacecompass

import android.content.Context
import android.net.Uri

/** Bundled, integrity-verified Leaflet; the device contacts only OSM for map tiles. */
internal fun spaceCompassPositionMapHtml(context: Context, url: String, dark: Boolean,
    pickable: Boolean, token: String, zoomIn: String, zoomOut: String): String {
    val uri = Uri.parse(url)
    val coordinates = requireNotNull(uri.getQueryParameter("marker")).split(',').map(String::toDouble)
    val latitude = coordinates[0].coerceIn(-85.0, 85.0)
    val longitude = coordinates[1]
    require(longitude.isFinite() && longitude in -180.0..180.0 && latitude.isFinite())
    val css = context.assets.open("maps/leaflet.css").bufferedReader().use { it.readText() }
    val js = context.assets.open("maps/leaflet.js").bufferedReader().use { it.readText() }
    val inLabel = org.json.JSONObject.quote(zoomIn)
    val outLabel = org.json.JSONObject.quote(zoomOut)
    val background = if (dark) "#101418" else "#f5f7f8"
    val surface = if (dark) "#282d33" else "#ffffff"
    val text = if (dark) "#ffffff" else "#101418"
    val tileStyle = if (dark) "filter:invert(1) hue-rotate(180deg) brightness(.95) saturate(.8) contrast(.95);" else ""
    return """<!doctype html><html><head><meta name="color-scheme" content="light"><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
      <style>$css
      html,body,#map{height:100%;width:100%;margin:0;background:$background;}
      .leaflet-tile-pane{$tileStyle}
      .leaflet-control-zoom a,.leaflet-control-attribution{background:$surface!important;color:$text!important;}
      .leaflet-control-zoom a{width:48px!important;height:48px!important;line-height:48px!important;font-size:26px!important;}
      .leaflet-control-attribution a{color:${if (dark) "#55c8db" else "#176b88"}!important;}
      .space-pin{background:transparent;border:0;}
      .space-pin svg{width:32px;height:44px;filter:drop-shadow(0 2px 2px #0008);}
      </style></head><body><div id="map"></div><script>$js</script><script>
      var map=L.map('map',{zoomControl:false,attributionControl:true}).setView([$latitude,$longitude],${if (pickable) 5 else 15});
      L.control.zoom({zoomInTitle:$inLabel,zoomOutTitle:$outLabel}).addTo(map);
      var loaded=0;var tiles=L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,
        attribution:'<a href="https://www.openstreetmap.org/copyright">© OpenStreetMap contributors</a>'}).addTo(map);
      tiles.on('tileload',function(){loaded++;});
      tiles.on('load',function(){if(loaded===0)window.location.href='spacecompass://tiles-error/$token';});
      var pin=L.divIcon({className:'space-pin',iconSize:[32,44],iconAnchor:[16,44],html:
        '<svg viewBox="0 0 32 44"><path fill="#e53935" stroke="#fff" stroke-width="2" d="M16 42C12 35 2 24 2 16a14 14 0 0 1 28 0c0 8-10 19-14 26Z"/><circle cx="16" cy="16" r="5" fill="#fff"/></svg>'});
      var marker=L.marker([$latitude,$longitude],{icon:pin}).addTo(map);
      ${if (pickable) "map.on('click',function(e){var p=e.latlng.wrap();marker.setLatLng(p);window.location.href='spacecompass://pick/$token?latitude='+p.lat+'&longitude='+p.lng;});" else ""}
      </script></body></html>"""
}
