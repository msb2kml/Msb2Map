# Protocol between Msb2Map and controlling applications

The information here is of interest only to develop applications
that drive the Msb2Map application.

The control is performed first by extra parameters associated
with the launcher intent and later by the parameters of
broadcast intents.

### Launcher intent

+ String parameter "CALLER": this is the sting that is used
 as name of the button to finish the application. The default
 is "Exit".
+ Location parameter "CENTER": the map is initially centered
 on this location.
+ Double parameter "ZOOM": initial zoom factor for the map.
 The default factor is 15.0.
+ Boolean parameter "StartGPS": if true, display the locations
 stored in the file StartGPS.gpx. True by default.
+ Boolean parameter "Tail": if true, the locations received by
 the broadcasts are displayed as a vapor trail, otherwise the
 locations are displayed as a permanent track. True by default.


### Launcher acknowledgment

Once the Msb2Map is launched, the map setup is complete, the
locations in StartGPS.gpx are displayed if requested and the
broadcast receiver has been opened, a broadcast is emitted
to signal the readiness.  
This broadcast has for action "org.js.ACK" and there is an extra
parameter that is the name of the application (Msb2Map).  
The driving application could wait for this acknowledgment
to be sure that no data is lost.

### Broadcast parameters

Msb2Map listens for broadcasts with the action "org.js.LOC".
The data is in the extra parameters:

+ Location parameter "LOC": location of the next point of the track.
 Drawn as vapor trail or continuous track.
+ String parameter "BUBBLE": string to be written in the information
 field. Only if the "LOC" parameter is present.
+ Integer parameter "COLOR": color to draw the segment ending
 at this point. The default is black. Only if the "LOC" parameter
 is present.
+ Location parameter "WPT": location of a marker to draw on the
 map with the form of a diabolo.
+ String parameter "WPT\_NAME": name (bubble) to give to the marker
 drawn for the "WPT" parameter.

Only one "LOC" or "WPT" could be present in a broadcast message
and they are mutually exclusive.


