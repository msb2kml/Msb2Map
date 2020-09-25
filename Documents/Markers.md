# Waypoints
The waypoints that are specified by the driving application are
drawn as markers with a form of diabolo on its base.

A waypoint positioned by the user while in a picking session
has a form of butterfly. The diabolo and the butterfly forms
are complementary and are two halves of a square if they are superposed.

Transient waypoints are drawn as a reticle.

![Msb2Map.jpg](Gallery/Msb2Map.jpg)

# Lines
The suite of points of tracks and routes have no dedicated
representation: only the segments of lines that join them are drawn.
The color of each segment could be specified by the driving
application. The default color is black.

![RouteAndWpt.jpg](Gallery/RouteAndWpt.jpg)

# Line drawing modes
There are two possible modes:

+ Entire track or route: the whole of the line is displayed at once.

+ Vapor trail: the driving application send the points with a
 delay between each one. The most recent location is noted as
 a reticle marker. Taping on this marker shows a bubble with
 the altitude (if available). The map is kept centered on this
 marker. Points and segments received before the last 20 locations
 are erased. This gives the impression of a vanishing vapor trail.

