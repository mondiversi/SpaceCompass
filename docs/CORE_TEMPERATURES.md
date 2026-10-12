# Core temperature references

These rows are additional **interior model estimates**, appended after existing surface,
atmospheric or photospheric temperatures in object details. They are not live telemetry,
surface conditions, or day/night catalogue sorting references. All unit conversion occurs
before rounding to the source's approximate precision. Kelvin remains a reference alongside
the selected Celsius/Fahrenheit unit; million-degree solar values use compact scientific
notation with the selected numeric separators. Core-mantle-boundary estimates have their
own label and must not be described as temperatures at the centre.

| Object | Adopted reference | Interpretation and primary source |
| --- | --- | --- |
| Sun | ≈15 million °C | Central estimate, [NASA/Marshall Solar Physics](https://solarscience.msfc.nasa.gov/interior.shtml). |
| Jupiter | ≈24,000 °C | Near-centre estimate, [NASA Space Place](https://spaceplace.nasa.gov/jupiter/en/). |
| Saturn | ≈10,000 °C | Core prediction, [NASA Cassini reference, page 5](https://www.nasa.gov/wp-content/uploads/2015/01/59910main_cassini.pdf). |
| Moon | ≈1650 K | Core-mantle boundary thermal model, [Righter et al. (2014), NASA NTRS](https://ntrs.nasa.gov/citations/20140007368). |
| Mercury | ≈1800–2000 K | Representative core-mantle-boundary constraints used for Fe–Si interior profiles, tied to lowermost-mantle solidus estimates, [Edmund et al. (2022), Nature Communications](https://doi.org/10.1038/s41467-022-27991-9), discussion and Fig. 4. Not a measured central temperature. |
| Mars | ≈2000–2400 K | Published core reference estimate, [Hsieh et al. (2024), Science Advances](https://doi.org/10.1126/sciadv.adk1087), page 2. |
| Uranus / Neptune | ≈6000 K | Explicitly an **adiabatic reference model**, not a uniquely determined temperature. [Morf & Helled (2025), A&A](https://doi.org/10.1051/0004-6361/202556911), sections 3–4, contrasts reference adiabatic solutions with much hotter non-adiabatic interiors. Their Neptune models reach 12,000–28,000 K; an Uranus solution exceeds 20,000 K. The UI includes this caveat. |

The absence of a core row means no appropriately curated value is supplied. A star's
effective temperature, corona, a comet nucleus's surface, or a black hole's accretion
region does not determine a core temperature. Venus' model parameter priors are not
presented as a measurement. Fictional bodies and spacecraft have no invented core data.

Identity now uses a generic ringed-planet glyph with the existing section size, stroke,
theme tint and decorative semantics.
