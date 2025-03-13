## NAACCR XML Version History

**Version 11.3**

- Changed type of pediatricId from mixed to text in NAACCR 25 dictionary.
- Changed type of pediatricIdVersionCurrent from digits to numeric in NAACCR 25 dictionary.
- Changed type of pediatricIdVersionOriginal from digits to numeric in NAACCR 25 dictionary.
- Changed type of pdl1 from mixed to text in NAACCR 25 dictionary.
- Changed type of whiteBloodCellCount from digits to text in NAACCR 25 dictionary.

**Version 11.2**

- The minimum Java version of the library is now Java 11; Java 8 is no longer supported (this doesn't apply to the SAS macros).
- Updated XStream dependency from version 1.4.20 to version 1.4.21.

**Version 11.1**

- Fixed two minor mistakes in the NAACCR 25 base dictionary.

**Version 11.0**

- Added support for NAACCR 25.
- Added support for new specification version 1.8 (added support for new dateTime type).
- Re-compiled SAS library under Java 8; it is no longer compatible with Java 7.

**Version 10.2**

- Changed data level errors from an exception to a validation error reported on the item.
- Fixed behavior of the reading SAS macro when it deals with fields that have a value too long in the XML file.
- Added new 'specs' parameter to allow writing a provided specifications version instead of the default library one.

**Version 10.1**

- Optimized read SAS macro.
- Fixed a major issue with the write SAS macro.

**Version 10.0**

- Added support for NAACCR 24.
- Added support for new specification version 1.7 (removed trim attribute and some padding options); only NAACCR 24 dictionary will use that version for now).
- Removed standalone program (the removal was announced a year ago when support for NAACCR 23 was added).
- Re-wrote the SAS macro to use a temp fixed-column file instead of a temp CSV file to resolve a maxed-out line length issue.

**Version 9.1**
 
- Fixed specification version not correctly used when provided in the options.
- Updated XStream dependency from version 1.4.19 to version 1.4.20.

**Version 9.0**

- Added support for NAACCR 23.
- Added support for new specification version 1.6 (removed allowUnlimitedText and groupedItems); only NAACCR 23 dictionary will use that version for now.
- Items that need to be zero-padded are now only padded if they contain a numeric value.
- Fixed an issue with the SAS macro where path to CSV dictionary would not work properly if it contained spaces. 
- Updated embedded JRE to Java 17.0.4 (this only affects the standalone program).

**Version 8.10**

- Added new optional parameter to the read SAS macro to allow creating the grouped items in the target SAS data set.
- Updated XStream dependency from version 1.4.18 to version 1.4.19.
- Updated embedded JRE to Java 17.0.3 (this only affects the standalone program).

**Version 8.9**

- Added new utility method "xmlToXml" in NaaccrXmlUtils class to allow a file to be re-created while applying some logic to each patient it contains.
- Improve validation of start columns for N18 dictionaries and prior.
- Fixed validation of user-defined dictionaries not detecting grouped items.
- Re-added support for providing a user-defined dictionary when processing a flat file (Standalone program only).
- Updated embedded JRE to Java 17.0.2 (this only affects the standalone program).

**Version 8.8**

- Fixed an error in standalone program preventing it from using multiple user-defined dictionaries.
- Updated embedded JRE to Java 17.0.1 (this only affects the standalone program).
- Added new optional parameter to the write SAS macro to allow disabling tumor grouping. 

**Version 8.7**

- Fixed an error in SAS macros preventing them from using multiple user-defined dictionaries.

**Version 8.6**

- Improved help and feedback of SAS macros.

**Version 8.5**

- Upgraded all base dictionaries to specification v1.5; added new dateLastModified attribute. 

**Version 8.4**

- Removed left-zero-padded attributes for reportingFacility in all base dictionaries.
- Changed length of dcStateFileNumber in NAACCR 22 base dictionary.
- Tweaked code that loads dictionaries to not always report a missing default namespace as an error.

**Version 8.3**

- Fixed default namespace not always written for dictionaries.

**Version 8.2**

- Dictionary validation will now fail if the expected default namespace is not provided.
- Fixed non-standard root attributes not written on their own line.
- Updated XStream dependency from version 1.4.17 to version 1.4.18.
- Updated commons-lang3 dependency from version 3.11 to version 3.12.0.
- Updated commons-io dependency from version 2.7 to version 2.11.0.

**Version 8.1**

- Fixed error that prevented the standalone program from loading. That error also affected other parts of the library.

**Version 8.0**

- Added support for NAACCR 22.
- Added support for new specification version 1.5 (base dictionaries were NOT upgraded to that version yet).
- Fixed dictionary validation not reporting unknown/invalid XML attributes.
- Updated embedded JRE to Java 11.0.12 (this only affects the standalone program).

**Version 7.13**

- Updated embedded JRE to Java 11.0.11 (this only affects the standalone program).
- Updated XStream dependency from version 1.4.16 to version 1.4.17.
- Improved output of the SAS library in SAS logs. 

**Version 7.12**

- Fixed validation of text values containing new lines.
- Improved SAS macros, added ability to provide items to include as a CSV file.
- Improved read SAS macro, added ability to process compressed files inside a source ZIP file.

**Version 7.11**

- Added new method utility method to stream a given dictionary to a given reader.

**Version 7.10**

- Updated XStream dependency from version 1.4.15 to version 1.4.16.

**Version 7.9**

- Added new parameter to write SAS macro to write NAACCR numbers; optional, defaults to not writing them.
- Changed padding of ca199PretxLabValue to be leftBlank in base N21 dictionary.
- Changed padding of sentinelLymphNodesExamined, sentinelLymphNodesPositive and phase1DosePerFraction to be leftZero in base N18 and N21 dictionaries.

**Version 7.8**

- Improved error message in SAS macros when referencing an invalid CSV dictionary.
- Fixed default filename when exporting a dictionary to a CSV file in standalone program.
- Updated embedded JRE to Java 11.0.10 (this only affects the standalone program).

**Version 7.7**

- Changed "latest" format (which represents the default format for the library) to 210.
- Updated XStream dependency from version 1.4.14 to version 1.4.15.

**Version 7.6**

- Changed data type for item physicianPrimarySurg from digits to text in all base dictionaries.
- Added new option for the reader to throw an exception for missing user-defined dictionaries; default is to not throw the exception.
- Updated embedded JRE to Java 11.0.9 (this only affects the standalone program).
- Updated XStream dependency from version 1.4.13 to version 1.4.14.

**Version 7.5**

- Added logic to automatically move dateOfLastCancerStatus and dateOfLastCancerStatusFlag to each tumor when reading them at the Patient level.

**Version 7.4**

- Changed data level of dateOfLastCancerStatus and dateOfLastCancerStatusFlag from Patient to Tumor in N18 and N21 dictionaries.
- Added support for unlimited text fields in dictionary editor (this only affects the standalone program).
- Improved dictionary validation: unlimited text attribute can only be used with text items.  

**Version 7.3**

- Fixed an error in the write SAS macro that prevented it from correctly running.
- Now writing each root attribute on its own line when creating NAACCR XML data files.
- Updated XStream dependency from version 1.4.12 to version 1.4.13.
- Updated commons-lang3 dependency from version 3.10 to version 3.11.

**Version 7.2**

- Added changelog in comment block on top if N18 and N21 dictionaries.
- Changed padding of ki67 to be leftBlank instead of rightBlank in N21 and N18 base dictionary; it was changed by mistake.

**Version 7.1**

- Changed type of ki67 to be text in N21 and N18 base dictionary.
- Changed type of tnmEditionNumber to be text in N21 and N18 base dictionary.
- Changed type of morphCodingSysCurrent/Original to be mixed in N21 and N18 base dictionary.
- Changed type of figoStage to be text in N21 base dictionary.
- Changed data level from Tumor to Patient for 3 COVID data items in N21 base dictionary.
- Now handling blank values instead of missing attribute for optional attributes.
- Updated embedded JRE to Java 11.0.8 (this only affects the standalone program).

**Version 7.0**

- Added support for NAACCR 21; that version is not final yet and it's possible it will change in a future release.
- Fixed the name of a few data items in NAACCR N18 dictionary.
- Added a NaaccrFormat.NAACCR_VERSION_LATEST constant, it is set to version 18 in this release.
- Added a disclaimer to base dictionaries mentioning those should not be modified.
- Fixed wrong padding for ruralurbanContinuum2013 and censusOccCode2010 in N18 dictionary.
- Updated XStream dependency from version 1.4.11.1 to version 1.4.12.
- Updated commons-io dependency from version 2.6 to version 2.7.
- Updated commons-lang3 dependency from version 3.9 to version 3.10.

**Version 6.8**

- Added a new option to allow a user-defined dictionary URI to be translated when read from an XML data file.

**Version 6.7**

- Fixed an infinite loop in Java code called by the SAS macros; tweaked the macros a bit.
- Updated conflict resolution for duplicate items in multiple user-defined dictionaries.
- Updated embedded JRE to Java 11.0.7 (this only affects the standalone program).

**Version 6.6**

- Added missing specification 1.4 XSD files, they are identical to the specification 1.3 XSD files.
- It is now possible to generate XML ID from item name in the dictionary editor.
- Updated embedded JRE to Java 11.0.6 (this only affects the standalone program).

**Version 6.5**

- Added support for reporting end lines for Patient and Tumor entities.
- Added support for selecting multiple user-defined dictionaries in the standalone program.
- Dictionary selection dialog will now default to same folder as selected data file (standalone program).
- Updated commons-lang3 dependency from version 3.7 to version 3.9.

**Version 6.4**

- Improved the feedback when processing an XML file referencing a missing user-defined dictionary.
- Updated embedded JRE to Java 11.0.5 (this only affects the standalone program).

**Version 6.3**

- Now always applying space padding rules when writing fixed-columns; the padding options only applies to zero-padding.
- Fixed cell editing issue in the items table of the standalone program.

**Version 6.2**

- Improved support for NAACCR XML extensions; added ability to report line number on embedded extensions.
- Updated embedded JRE to Java 12.0.2 (this only affects the standalone program).

**Version 6.1**

- Added a new option to allow N18 renamed item IDs to be automatically translated by the library when reading XML data.
- Added a new option to allow provided item IDs to be automatically translated by the library when reading XML data.
- Updated LICENSE file that is included with the standalone program.

**Version 6.0**

- Updated default specification version from 1.3 to 1.4.
- Changed maximum length of NAACCR IDs from 50 to 32 characters (specification 1.4).
- Renamed a bunch of IDs in N18 dictionary to comply with new maximum length.

**Version 5.4**

- Fixed a few items being included by mistake in the confidential format in the version 18 dictionary.

**Version 5.3**

- Fixed parent XML element to be Tumor instead of Patient for seerCaseSpecificCod (1914) and seerOtherCod (1915) in N18 dictionary.
- Now applying default value for dataType, padding and trim attributes when reading a dictionary from XML. 
- Added new methods on Patient and Tumor to remove a given data item.
- Added option to standalone program to extract user-defined dictionaries into CSV files.
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

- Updated default specification version from 1.2 to 1.3.
- Relaxed the validation of the NAACCR numbers in user-defined dictionaries (specification 1.3).
- Relaxed the rule requiring all the user dictionaries from data files to be provided to the library.
- Changed type of causeOfDeath in all base dictionaries from digits to text.
- Fixed start column of grouped item extendOfDisease10Dig in all base dictionaries.
- Now display the number of Patient and Tumor tags found in the source XML file in the GUI.
- Added an option to the GUI for extracting the dictionaries as CSV files.

**Version 3.0**

- Updated default specification version from 1.1 to 1.2.
- Added the ability to cache a runtime dictionary when using the XML reader/writer inside a loop.
- Added 'getDefault()' method to options and configuration objects.
- Now caching the internal dictionaries (base and default user) in the dictionary utility class.
- Added support for multiple user dictionaries (specification 1.2).
- Added support for grouped items definition in the base dictionaries (specification 1.2).
- Relaxed the type of many items in standard base dictionaries (specification 1.2).
- Removed support for "regexValidation" in dictionaries (specification 1.2).
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

- Updated default specification version from 1.0 to 1.1.
- Removed "any-attribute" from the XSD since those are not properly supported; only kept the one for the NaaccrData tag.
- Added line number on the item object; populated only when reading items.
- Validation errors for specific items are now reported directly on those items instead of the parent entity.
- Added included/excluded list of items to the options of the standalone GUI.
- Fixed a bug where values too long were not correctly reported as errors, or not correctly truncated.
- Added optional attribute "allowUnlimitedText" to the dictionary item tag (specification 1.1).
- Made start column optional in user-defined dictionaries (specification 1.1).
- Made NAACCR version optional in user-defined dictionaries  specification 1.1).
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