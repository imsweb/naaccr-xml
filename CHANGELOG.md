## NAACCR XML Version History

**Version 1.1**

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