# Continuous solar lighting

The main virtual scene and its frozen panorama use the same immutable lighting
sample derived from the observer's geometric solar elevation. GPS, scenario
position/date and existing sunrise/set calculations supply that elevation;
the selected UI theme and local wall-clock hour do not control scene colors.

Smoothstep blends connect darkness at -18 degrees to subdued twilight at -6,
then strengthen the horizon glow toward the conventional -0.833-degree solar
rise/set height. Warm light remains near the horizon up to +2 degrees and fades
into daylight by +8. At +6 the scene is already mostly daylight, rather than
switching to a full sunset palette. These are illustrative color transitions,
not measured atmospheric brightness or a prediction of actual sunset colors.
[USNO twilight definitions](https://aa.usno.navy.mil/faq/RST_defs) explain the
astronomical/civil limits and the conventional rise/set height.

Both sides of solar noon share their day/horizon palette anchors, so a direction
or discrete phase change cannot cause a sudden hue jump, even at high latitudes.
Missing/non-finite solar samples use night. Existing star visibility continues
to use its independent smooth solar-height curve.

Sky, ground, clouds, fog, rain and snow interpolate through the same lighting
sample. WMO conditions still control coverage and precipitation independently.
Live weather geometry is cached; only changed colors rebuild its shaders.
The two-second UI tween smooths sample/weather changes, while the underlying
solar progression is continuous over the actual twilight interval.

Capture freezes the lighting sample alongside the observer/time/weather.
Changing panorama orientation, labels, position disclosure or export formatting
retains that sample. Real camera photographs retain their captured imagery.

Legacy phase-only palette overloads remain available for isolated rendering
fixtures. All production virtual main/capture paths provide continuous lighting.
