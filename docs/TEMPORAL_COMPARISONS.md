# Temporal comparisons

Key heights and Key events can add independent, fixed-time instances of an object to the sky. Check a row to add its comparison; uncheck it or remove the corresponding island entry to remove it. The Observatory buttons count available rows for their respective pages. Dated paths appear in chronological order after their parent object. Unchecking the parent hides its paths and rechecking it restores the saved selections.

Each comparison retains its UTC instant and observation time zone. Geometry is recalculated for the effective Location position and altitude; it does not retain a separate observer position or advance with the live clock. Solar monthly rows use the actual upper transit on the 21st of each local-calendar month, including daylight-saving transitions. Annual extrema and intermediate-height searches remain independent of these monthly references.

Comparisons have stable provenance IDs, distinct base-derived colours, ordinary above/below-horizon styles, independently selectable points and antipodes. Their definitions and pins persist locally. Captures freeze each instance separately; image legends and CSV/TXT columns retain duplicate catalogue bodies as separate comparisons. Hidden-position exports display times in UTC without changing the calculation or selection identity.

Location files include definitions, selected points, the inspected comparison and antipode visibility when celestial objects are included. Optional version-1 extensions preserve existing live-object and point fields. Older readers ignore comparisons; importing older files does not invent comparison entries. Runtime ephemeris/path caches are rebuilt, not embedded in Location files. Antipodes are restored only when at least one point remains selected.

Comparisons use the same validated astronomical models and ephemerides as live objects. Missing or out-of-range data remain unavailable; no long-term satellite propagation or fabricated remote ephemerides are introduced. Comparisons at different instants are excluded from simultaneous approach detection.

The frozen instance marker uses the originating event/height name (for example Culmination for monthly solar references); Current is reserved for the Location clock. Coincident anchor/culmination captions within one comparison coalesce, while scientific points and selection records remain distinct. An Observatory opened from a comparison can retain its frozen viewing instant, while its Daily path always uses the effective Location date, time, zone and day/next-24-hours mode. This also applies when the live object is unchecked in the catalogue. Comparison pins stay available in the sky/captures but are excluded from the Location daily table and its exports.

Tapping the Key heights or Key events title toggles all enabled rows for the displayed object, page and year. Any existing selection in that scope clears the scope; otherwise all available rows are added in one state update. Other comparisons and their pins remain intact. Bulk addition respects the existing 512-comparison limit without partially adding a page. The optional `comparisonAnnual.<index>` property preserves an annual extreme's semantic title while retaining the original six-field definition, identity and version-1 reader compatibility.

## Personal Moments

An object can have named personal moments with a date and an optional time.
Checking one adds its fixed-time path. The dedicated Personal Moments transfer
includes the object's full list, including unchecked entries. Import adds missing
entries and retains the saved checked state of moments that already exist.
Location exports with celestial objects include active personal moments.
