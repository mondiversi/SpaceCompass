# Celestial star background

The bundled 4096 x 2048 lossless WebP atlas derives from NASA SVS Deep Star Maps
2020 in ICRF/J2000 celestial coordinates. Right ascension 0h is at the map centre,
RA increases left, and declination decreases from top to bottom. Display shaping
suppresses unresolved faint glow and keeps source-star colours at the original
4k pixel resolution. No downscaling or sharpening filter alters stellar positions.
The atlas contains no names, hit targets or catalogue objects.

Astronomy Engine's HOR -> EQJ transform accounts for the observation UTC instant,
latitude/longitude, precession, nutation and sidereal rotation. ENU east is minus
HOR west. The virtual pinhole and screen basis are shared with the object/ground
projection, including tablet layouts, pitch, roll and actual pointing-panel
offsets. The live TextureView draws a GLES2 quad on its own thread; orientation
updates uniforms instead of allocating/rebuilding a sky bitmap. It is disposed
when the main view is not active or camera imagery replaces the virtual sky.
The atlas is decoded at full resolution only when the app heap is at least
192 MiB and Android does not classify the device as low RAM. A power-of-two
decode also respects both image axes and the actual GPU texture-size limit.
Smaller-memory devices use at most 2048 pixels per axis.
The export software renderer samples the same map for the frozen panoramic
observer/date and chosen central direction. It never paints synthetic stars
over a real camera photograph.

Visibility fades smoothly from astronomical night to twilight, using solar
elevation rather than app theme or timezone. Ground rays have zero alpha;
low-altitude stars fade toward the geometric horizon. Weather attenuation and
cloud/fog layers cover stars. Individual decorative cloud positions are not
weather measurements. This is an illustrative background, not a photometric
prediction or limiting-magnitude model. Individual proper motion, stellar
parallax and extinction are not simulated by the static atlas. Texture sampling
limits angular detail to roughly 0.09 degrees per pixel at full resolution at the celestial equator;
phone compass calibration remains a separate limitation.

Credits: NASA/Goddard SVS; Ernie Wright (USRA); Gaia DR2: ESA/Gaia/DPAC.
Source and reproduction guidance: https://svs.gsfc.nasa.gov/4851/ and
https://svs.gsfc.nasa.gov/help/. Full notice is bundled in assets/licenses/star-map.txt.
