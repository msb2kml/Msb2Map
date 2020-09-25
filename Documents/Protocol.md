# Protocol between Msb2Map and controlling applications

The information here is of interest only to develop applications
to drive the Msb2Map application.

The control is performed first by extra parameters associated
with the launcher intent and later by the parameters of
broadcast intents.

### Launcher intent

+ String parameter "**CALLER**": this is the sting that is used
 as name of the button to finish the application. The default
 is "Exit".
+ Location parameter "**CENTER**": the map is initially centered
 on this location.
+ Double parameter "**ZOOM**": initial zoom factor for the map.
 The default factor is 15.0.
+ Boolean parameter "**StartGPS**": if true, display the locations
 stored in the file StartGPS.gpx. True by default.
+ Boolean parameter "**Tail**": if true, the locations received 
  initially through the broadcasts are displayed as a vapor trail,
  otherwise the locations are displayed as a permanent track.
  True by default. The mode could be changed at the start of
  a new line.


### Launcher acknowledgment

Once the Msb2Map is launched, the map setup is complete, the
locations in StartGPS.gpx are displayed if requested and the
broadcast receiver has been opened, a broadcast is emitted
to signal the readiness.  
This broadcast has for action "**org.js.ACK**" and there is an extra
parameter that is the name of the application (Msb2Map).  
The driving application could wait for this acknowledgment
to be sure that no data is lost.

### Return to driving application

The driving application should launch Msb2Map with the flag
"FLAG\_RECEIVER\_FOREGROUND". It could detect the end
of Msb2Map in an "onResume" function.

A continuous stream of broadcasts should be broken at interval
to permit the possible detection of the end of Msb2Map.

### Broadcast parameters

Msb2Map listens for broadcasts with the action "**org.js.LOC**".
The data is in the extra parameters with 3 cases:

1. The parameter "**LOC**" is present
    + Location parameter "**LOC**": location of the next point of the track.
     Drawn as vapor trail or continuous track.
    + Boolean parameter "**START**": true if this location is the start of
     a new line.
    + Boolean parameter "**Tail**": if true and if the location is the
     start of a new line, this line is drawn as a vapor tail. The line
     is drawn as a permanent track if this parameter is false. The
     default is to continue a previously.
    + String parameter "**BUBBLE**": string to be written in the information
     field.
    + Integer parameter "**COLOR**": color to draw the segment ending
     at this point. The default is black.

2. The parameter "**WPT**" is present
    + Location parameter "**WPT**": location of a marker to draw on the
     map with the form of a diabolo.
    + String parameter "**WPT\_NAME**": name (bubble) to give to the marker
     drawn for the "**WPT**" parameter.
    + String parameter "**BUBBLE**": string to be written in the information
     field.

3. The parameter "**WPT**" is not present or is null
    + Boolean parameter "**PICKING**": if true start a session of picking
     locations for the user. Stop the session if false.
    + Boolean parameter "**PICKWPT**": if true the picking session concerns
     waypoints. If false, the picking is for route waypoints.

Only one "**LOC**", "**WPT**" or "**PICKING**" could be present in a
broadcast message and they are mutually exclusive.

### Return of picked locations

Each picked location is broadcast immediately with the action
"**org.js.PICKED**". The extra parameters are:

1. Location parameter "**LOC**": the location that has been picked.
  The altitude could be specified if the position concerns a waypoint.
  The location is null if it is deleted by the user.

2. Integer parameter "**INDEX**": the index of order in the picking session.
  The location index is conserved if its position is edited in a
  later operation.

3. String parameter "**NAME**": the name given (or generated) for the location.

