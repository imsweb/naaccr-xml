## NAACCR XML Version History

**Version 2.0**

 - Removed Class-Path attribute from generated JAR.

**Version 2.0**

 - Removed "any-attribute" from the XSD since those are not properly supported; only kept the one for the NaaccrData tag.
 - Added line number on the item object; populated only when reading items.
 - Validation errors for specific items are now reported directly on those items instead of the parent entity.
 - Added included/excluded list of items to the options of the standalone GUI.
 - Fixed a bug where values too long were not correctly reported as errors, or not correctly truncated.
 - Added optional attribute "allowUnlimitedText" to the dictionary item tag (specification 1.1).
 - Made start column optional in user-defined dictionaries (specification 1.1).
 - Made NAACCR version optional in user-defined dictionaries  specification 1.1).
 - Added optional attribute "specificationVersion" to the root dictionary and data XML tags, now set to 1.1 by default.
 - Updated commons-lang3 dependency to version 2.5.
 - Updated commons-io dependency to version 3.4.
 - Updated XStream dependency to version 1.4.9.
 - Library now requires Java 8 at minimum.

**Version 1.3**

 - Added new option to control whether or not the padding rules should be applied, by default they won't be applied (since they actually change data and it's rarely desired).
 - Added new option to specify which items need to be processed (this option is not available in the GUI yet).
 - Fixed some bad data types in all base dictionaries.
 - Fixed a bug in the dictionary validation preventing a standard item from the default user dictionary to be re-defined.

**Version 1.2**

 - Improved the dictionary page in the standalone GUI.
 - Stopped using exception when validating a dictionary.
 - Fixed bug related to caching items on each abstract entities.
 - Fixed two bad item ID (and name) in NAACCR 16 dictionary.

**Version 1.1**

 - Fixed wrong path to the main GUI class in the JAR's manifest.

**Version 1.0**

 - Added support for NAACCR 16.
 - Renamed root packages from "org.naaccr.xml" to "com.imsweb.naaccrxml"
 - Changed padding rules for the 5 physician fields so they are not 0-padded anymore.
 - Added a non-gui batch mode (BatchProcessor).

**Version 0.8 (beta)**

 - Added LICENSE file to the created JAR.
 - Fixed some data types in both dictionaries.

**Version 0.7 (beta)**

 - Fixed window sizing issues and other minor bugs.
 - Added a simple Help page.
 - The 'timeGenerated' attribute is now optional.
 - Library is now writing a namespace in the root attributes, to comply with the XSD.

**Version 0.6 (beta)**

 - Added support for XZ compression.
 - Added the XSD files to the released JAR file.
 - Added proper support for padding values in the library.
 - Now presenting the options before the analysis phase in the GUI.
 - Added an About dialog.
 - Now displaying a summary of the validation errors in the GUI.
 - Switched to a centralized class of validation errors.

**Version 0.5 (beta)**

 - Now using a deterministic way to create the NAACCR IDs from the item names.
 - Added a Validate XML page.
 - Removed Samples page.