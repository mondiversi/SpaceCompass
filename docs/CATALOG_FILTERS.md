# Catalogue filters and category tints

The filter panel uses two equal-width fields per row: Type / Visibility, then
Origin / Pointing. Each label sits inside the outline above its value, without an opaque label patch. Origin distinguishes real objects and science-fiction
references independently of physical type. Pointing means a finite azimuth and
elevation can be calculated for the scenario observer and moment; a below-horizon
direction is still available, and a measured distance is not required.

The existing minute-cadence observation batch supplies pointing availability.
Each completed result is tagged with its calculation key. Pending results are
neither available nor unavailable; an old scenario's results are never reused for
the new key. A completed null/invalid direction is unavailable. This filter adds
no provider requests and never guesses missing ephemerides or GPS coordinates.

Sort, type, visibility, origin and pointing are persisted by stable enum names at
the selection event. Older installations default both new fields to All. Reset
clears all four filters and inline search, retaining sort and checked objects.
Checked objects keep their existing precedence: they remain visible and first,
even when excluded by a filter/search. Counts describe displayed results, except
the existing type counts, which describe the complete available catalogue.

Rows use recognisable theme-specific planetary, stellar, deep-sky, compact-object,
small-body and artificial-object tints. Fictional references use their physical
category. Checked rows retain their category hue with a stronger fill, alongside their existing checkbox, bold name and thumbnail rim. Small values use a 72% text tint to preserve contrast on the stronger backgrounds. Independent checkbox/name/value touch regions and individual orbit colours are unchanged.
