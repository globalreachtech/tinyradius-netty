# Feature Ingest Freeradius dictionary

## Justification for the usefulness of the feature

The default dictionary embedded in tinyradius-netty is useful for testing, but a real implementation requires a more complete dictionary. Instead of creating a dictionary from scratch, most developers will want to get an existing one and use "as-is". The reference for this is the v3 FreeRadius dictionary in `https://github.com/FreeRADIUS/freeradius-server/tree/master/share/dictionary/radius/v3`, but unfortunately it contains some types that are not yet supported in tinyradius-netty, throwing errors and the application not starting.

Until those features are developped, the strategy suggested is not to fail completely when using the standard complete Freeradius v3 dictionary, but be more tolerant and show a warning for the attributes or values not included.

In fact, this strategy is already implemented for some attribute types. It only needs to be implemented in a couple of additional cases.

## Implementation

The additional cases to ignore are the following

* Dictionary lines of type `STRUCT`
* Attribute types that are not integers, but contain a subtype (a dot), for instance `ATTRIBUTE	FreeRADIUS-EAP-TEAP-Intermediate-Result	190.10	short`
* Values for attribute types that are non existing or, more usually, that have been ignored because of the above rule.
