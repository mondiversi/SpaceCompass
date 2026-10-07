# Inline catalog search

The 48 dp search field sits after the master checkbox and before the right-aligned
current value heading. It filters as text changes, without network requests or
restarting observations/distance calculations. Clearing the field restores the
existing type, horizon and sort choices. Reset Filters clears the search too.
The total in the page title remains the available catalog total, while filter
counts and the master-checkbox state use the complete displayed intersection.

Search matches localized display names and stable scientific catalog IDs.
Case, accents, whitespace and punctuation do not prevent a match. Searching
cannot reveal the hidden object; it must already belong to the available catalog.
Result ordering is still controlled by the existing sort. Search never changes
checked objects; the master checkbox still affects only the visible subset.

Text survives configuration changes via saved composition state and is not
written into persistent filter preferences. The Search keyboard action dismisses
the keyboard without clearing results. The clear action retains a 48 dp touch
target, and both search labels are translated in all twenty resource catalogs.

## Wider search field

The visible input is 30% wider than its former equal-column allocation, excluding
the unchanged 12 dp start and 8 dp end spacing. Width is calculated from the
available header width so phone/tablet and orientation changes keep the same
proportion. The right-aligned sort-value heading takes the remaining space and
wraps naturally; input height, checkbox placement and result rows are unchanged.
