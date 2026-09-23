# Planning notes

A running list of follow-up work that's been discussed but not yet built - separate from
the V1 implementation plan (which described what to build first) and the README (which
describes what's actually shipped). Add to this as ideas come up; move an item out once
it's done (a PR, or a line in the README, is enough - this file doesn't need to track
completion).

## In-app documentation: FAQ / "how this app works" section in Settings

The user had to ask how Stressor/Recovery weighting and the daily-load calculation work,
despite it being in the V1 plan - the plan isn't something they have open while using the
app day to day. Two related pieces of work:

1. A short in-app explanation, reachable from Settings, of how the load score is
   calculated - enough that "why is my recovery weight positive" or "why didn't a great
   recovery day erase yesterday's alcohol" are self-service questions.
2. A couple of short tutorials for the less obvious flows (the missed-day/historical
   logging entry point in Logbook; what the notification's quick-toggle buttons can and
   can't do vs. opening the full check-in).

Content the FAQ/explanation should cover (already written out once in conversation -
reuse this rather than re-deriving it):

- **Weight is always positive, for both Stressors and Recovery.** There's no sign to
  flip yourself; the app subtracts recovery in the formula, not in the stored weight.
- **The daily formula** (`ScoringEngine.calculateDailyLoad`): gross load = sum of
  `weight x intensity` across selected Stressor factors; raw recovery = same sum across
  selected Recovery factors; applied recovery is capped at 30% of that day's gross load;
  net load = `max(0, gross - appliedRecovery)`.
  Recovery can meaningfully offset a day, but can never fully erase it.
- **The normalizing denominator (`maxPossibleDailyLoad`) only sums active Stressor
  weights.** Recovery factors never enter it - tracking more recovery options doesn't
  shrink your load percentage.
- **The CSV export shows recovery as a negative number** (`CsvExportRepository`) even
  though the configured weight in Settings is positive - that's a readability choice for
  the export, matching the original plan's own worked example, not a different value.

Not yet decided: where exactly this lives in Settings (a dedicated screen vs. an
expandable section on the existing one), and whether the tutorials are static text or
something more guided (e.g. shown once on first use of a feature).
