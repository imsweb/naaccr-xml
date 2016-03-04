# NAACCR XML

[![Build Status](https://travis-ci.org/imsweb/naaccr-xml.svg?branch=master)](https://travis-ci.org/imsweb/naaccr-xml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.imsweb/naaccr-xml/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.imsweb/naaccr-xml)

This library provides support for the NAACCR XML format.

Information about the format and the Task Force that developed it can be found on [http://naaccrxml.org/](http://naaccrxml.org/).

## Download

The library is available on [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.imsweb%22%20AND%20a%3A%22naaccr-xml%22).

To include it to your Maven or Gradle project, use the group ID `com.imsweb` and the artifact ID `naaccr-xml`.

You can check out the [release page](https://github.com/imsweb/naaccr-xml/releases) for a list of the releases and their changes.

This library requires Java7 or later.

## Usage

There are four ways to use this library:

1. Using the stream classes
2. Using the NAACCR XML Utility class (NaaccrXmlUtils)
3. Using the Graphical User Interface (Standalone)
4. Using the no-GUI batch class (BatchProcessor)

### Using the stream classes
This is the recommended way to use the library; 4 streams are provided:
* [PatientXmlReader](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/PatientXmlReader.java)
* [PatientXmlWriter](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/PatientXmlWriter.java)
* [PatientFlatReader](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/PatientFlatReader.java)
* [PatientFlatWriter](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/PatientFlatWriter.java)

The readers provide a ***readPatient()*** method that returns the next patient available, or null if the end of the stream is reached.
The writers provide a ***writePatient(patient)*** method.

Transforming a flat file into the corresponding XML file and vice-versa becomes very simple with those streams; just create the stream and write every patient you read...

### Using the NAACCR XML Utility class (NaaccrXmlUtils)
A few higher-level utility methods have been defined in the [NaaccrXmlUtils](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/NaaccrXmlUtils.java) class (only the required parameters are shown for clarity):

*Reading methods*
* NaaccrData ***readXmlFile*** (File xmlFile, ...)
* NaaccrData ***readFlatFile*** (File flatFile, ...)

*Writing methods*
* void ***writeXmlFile*** (NaaccrData data, File xmlFile, ...)
* void ***writeFlatFile*** (NaaccrData data, File flatFile, ...)

*Translation methods*
* void ***flatToXml*** (File flatFile, File xmlFile, ...)
* void ***xmlToFlat*** (File xmlFile, File flatFile, ...)

There are other utility methods, but those are the main ones.

All those methods accept the following optional parameters (optional in the sense that null can be passed to the method):
* [NaaccrXmlOptions](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/NaaccrXmlOptions.java) - options for customizing the read/write and errors reporting operations
* [NaaccrDictionary](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/entity/dictionary/NaaccrDictionary.java) - a user-defined dictionary (if none is provided, the default user-defined dictionary will be used)
* [NaaccrObserver](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/NaaccrObserver.java) - an observer allowing to report progress as the files are being processed.

### Using the Graphical User Interface (Standalone)

The library contains an experimental GUI that wraps some of the utility methods and provides a more user-friendly environment for processing files.

To start the GUI, just double-click the JAR file created from this project; it will invoke the main GUI class 
([Standalone](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/gui/Standalone.java)).

You can also type the following in a DOS prompt, after navigating to the folder containing the JAR file:
```
java -jar naaccr-xml-X.X.jar
```
where X.X is the downloaded version.

### Using the no-GUI batch class (BatchProcessor)

The library also contains an experimental no-GUI class that can be used to process files in batch
([BatchProcessor](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/BatchProcessor.java)).

Here is an example of how to start it:
```
java -cp naaccr-xml-X.X.jar BatchProcessor options.properties
```
where X.X is the downloaded version.

This assumes the options file is in the same folder as the JAR file (but it can be anywhere and a full path can be provided on the command line).

See the [BatchProcessor](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/BatchProcessor.java) class for a description of each individual option.

Here is how a typical options file would look like:

```properties
input.folder=C:\\input
processing.mode=flat-to-xml
output.folder=C:\\output
```

## Dealing with dictionaries

The project contains two dictionaries for each supported NAACCR versions: the main dictionary and the default user defined dictionary; here are the ones for NAACCR 15:
* [naaccr-dictionary-150.xml](https://github.com/imsweb/naaccr-xml/blob/master/src/main/resources/naaccr-dictionary-150.xml)
* [user-defined-naaccr-dictionary-150.xml](https://github.com/imsweb/naaccr-xml/blob/master/src/main/resources/user-defined-naaccr-dictionary-150.xml)

In addition, the project also contains a utility class ([NaaccrXmlDictionaryUtils](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/NaaccrXmlDictionaryUtils.java))
 to read, write and validate a given dictionary file. Note that there is no syntax differences between a base dictionary and a user-defined one.

That utility class also contains a method to create a NAACCR ID (used for the "naaccrId" attribute) from a given item name using the following rules:

1. Spaces, dashes, slashes periods and underscores are considered as word separators and replaced by a single space
2. Anything in parenthesis is removed (along with the parenthesis)
3. Any non-digit and non-letter character is removed
4. The result is split by spaces
5. The first part is un-capitalized, the other parts are capitalized
6. All the parts are concatenated back together

## XML Schemas

The following schemas are available in the project:
* [naaccr_data.xsd](https://github.com/imsweb/naaccr-xml/blob/master/src/main/resources/naaccr_data.xsd) - W3C Schema for the data files
* [naaccr_dictionary.xsd](https://github.com/imsweb/naaccr-xml/blob/master/src/main/resources/naaccr_dictionary.xsd) - W3C Schema for the dictionary files
