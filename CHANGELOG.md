## NAACCR XML Version History

**Version 5.3 (not released yet)**

- Added new methods on Patient and Tumor to remove a given data item.
- Updated embedded JRE to Java 12.0.1 (this only affects the standalone program).

**Version 5.2**

- Fixed wrong expected length for Confidential format.
- Updated embedded JRE to Java 12 (this only affects the standalone program).
- Now setting the line number of the root naaccrData objects.

**Version 5.1**

- Fixed recordNumberRecode item in base 18 dictionary to have Tumor instead of Patient as its ParentXmlTag.
- Added missing validation on record type attribute when loading a dictionary.
- Updated embedded JRE to Java 11.0.2 (this only affects the standalone program).

**Version 5.0**

- Changed the release of the standalone program, it now contains an embedded JRE.
- Invalid TimeGenerated is now reported as a validation error instead of an exception.

**Version 4.15**

- The ampersand is now considered as a word separator when generating an NAACCR ID from a data item name.
- Changed data types of RUCA/URIC from digits to mixed.
- Fixed warnings in the console about unsafe access to private fields.
- Updated XStream dependency from version 1.4.10 to version 1.4.11.1.

**Version 4.14**

- Changed dictionary validation methods to return all errors instead of just the first one.
- Added setTumors on Patient class and setPatients on NaaccrData class.
- Removed dependency to XZ compression since it does not look like anyone uses it.
- Fixed first two new lines characters in data files not honoring the new line character option.

**Version 4.13**

- Removed dependency to JAXB API; this library now uses the standard date/time classes to deal with ISO 8601 dates.
- Added an option to specify the end-of-line characters when writing flat or XML data (does not apply to dictionaries).
- Set NAACCR ID and value on NaaccrValidationError object for CODE_BAD_NAACCR_ID errors

**Version 4.12**

- Fixed alignment for items circumferentialResectionMargin (#3823) and hcgPostOrchiectomyLabValue (#3846).

**Version 4.11**

- The 'naaccrName' attribute is now enforced to be up to 50 characters.
- Standalone GUI now shows the number of written Patient and Tumor when creating XML files.
- Added new menu item to standalone GUI to save a given dictionary as CSV file.
- Added support for user-defined dictionary to SAS module.
- Updated commons-lang3 dependency from version 3.6 to version 3.7.
- Updated commons-io dependency from version 2.5 to version 2.6.
- Updated XZ compression library from version 1.6 to version 1.8.

**Version 4.10**

- Added maximum length of 50 characters for NaaccrId attribute in the dictionaries.
- Added new DuplicateItemException to deal with duplicate items, which are never allowed in this standard.
- Writing unknown items is now tied to the 'unknownItemHandling' option.
- Improved handling of CDATA sections when reading/writing XML for SAS.

**Version 4.9**

- Added a missing flush to the PatientFlatWriter.closeAndKeepAlive method.
- Now replacing a single CR (without a LF) by a space when writing to a flat file.
- Fixed an exception happening when trying to write XML with padding turned ON.
- PatientReader and PatientWriter interfaces are now extending Closeable instead of AutoCloseable.
- Added support for creating SAS definition file in the standalone GUI interface.

**Version 4.8**

- Fixed padding, alignment and dataType attributes for items in NAACCR dictionaries

**Version 4.7**

- Fixed typos in NAACCR ID of new NAACCR 18 data item from 'ceaPretreatmentIntrepretation' to 'ceaPretreatmentInterpretation'.
- Fixed wrong NAACCR ID and name for item #1788 in version 15 dictionary.

**Version 4.6**

- Final version of the NAACCR 18 dictionary.

**Version 4.5**
 
- Added support for NAACCR 18; that version is not final yet and it's possible it will change in a future release.
- Extensions now return their line number if the class implements NaaccrXmlExtension.
- Added a new dictionary editor in the standalone GUI.
- Added new jaxb-api (version 2.3.0) dependency required by Java 9.

**Version 4.4**

- Fixed an exception in new validation method added in version 4.3.

**Version 4.3**

- Added a new dictionary utility method that validates a combination of dictionaries instead of a single one.
- The patient readers/writers are now implementing a common reader/writer interface.
- Improved handling of extension to allow several extension objects per data level instead of a single one.
- Improved the algorithm that generate NAACCR XML ID from names so it handles roman numerals better.
- Improved user-defined dictionaries validation; an error will now be reported if two dictionaries define overlapping columns.
- Added a proper security environment to XStream by limiting the classes that it can create when loading XML files.
- Updated XStream dependency from version 1.4.9 to version 1.4.10.
- Updated commons-lang3 dependency from version 3.4 to version 3.6.
- Updated XZ compression library from version 1.5 to version 1.6.

**Version 4.2**

- Fixed a bug in the dictionary validation that would not flag as invalid a dictionary with a missing URI attribute.
- Added support in NaaccrXmlUtils for new lineToPatient and patientToLine methods.

**Version 4.1**

- Addressed a bug crashing the Standalone GUI when minimizing the application.

**Version 4.0**

- Updated default specifications version from 1.2 to 1.3.
- Relaxed the validation of the NAACCR numbers in user-defined dictionaries (specifications 1.3).
- Relaxed the rule requiring all the user dictionaries from data files to be provided to the library.
- Changed type of causeOfDeath in all base dictionaries from digits to text.
- Fixed start column of grouped item extendOfDisease10Dig in all base dictionaries.
- Now display the number of Patient and Tumor tags found in the source XML file in the GUI.
- Added an option to the GUI for extracting the dictionaries as CSV files.

**Version 3.0**

- Updated default specifications version from 1.1 to 1.2.
- Added the ability to cache a runtime dictionary when using the XML reader/writer inside a loop.
- Added 'getDefault()' method to options and configuration objects.
- Now caching the internal dictionaries (base and default user) in the dictionary utility class.
- Added support for multiple user dictionaries (specifications 1.2).
- Added support for grouped items definition in the base dictionaries (specifications 1.2).
- Relaxed the type of many items in standard base dictionaries (specifications 1.2).
- Removed support for "regexValidation" in dictionaries (specifications 1.2).
- Added full support for extensions (user-defined XML blocks) at the NaaccrData, Patient and Tumor levels.
- Added better support for non-printable control characters; those will be ignored by default when writing item values; an error can be thrown instead by setting the 'ignoreControlCharacters' options to false.
- Carriage Returns (CR) are now translated to Line Feed (LF) when writing item values; this library was writing it as '&#xd;' which it technically correct but was causing some confusion.
- Added proper support for namespaces.

**Version 2.3**

- Fixed wrong item length for seerMets in version 16, was defined as length 1 instead of 2.

**Version 2.2**

- Fixed wrong item number in 14, 15 and 16 default user dictionaries for reserved 17 item.
- Fixed wrong item number in 16 base dictionary for items derivedSeerCmbNSrc and derivedSeerCmbMSrc.

**Version 2.1**

- Fixed NAACCR ID of Item Numbers 272 and 282; those should not have been renamed from NAACCR 15.
- Removed Class-Path attribute from generated JAR.

**Version 2.0**

- Updated default specifications version from 1.0 to 1.1.
- Removed "any-attribute" from the XSD since those are not properly supported; only kept the one for the NaaccrData tag.
- Added line number on the item object; populated only when reading items.
- Validation errors for specific items are now reported directly on those items instead of the parent entity.
- Added included/excluded list of items to the options of the standalone GUI.
- Fixed a bug where values too long were not correctly reported as errors, or not correctly truncated.
- Added optional attribute "allowUnlimitedText" to the dictionary item tag (specifications 1.1).
- Made start column optional in user-defined dictionaries (specifications 1.1).
- Made NAACCR version optional in user-defined dictionaries  specifications 1.1).
- Added optional attribute "specificationVersion" to the root dictionary and data XML tags, now set to 1.1 by default.
- Updated commons-lang3 dependency to version 3.4.
- Updated commons-io dependency to version 2.5.
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