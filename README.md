NAACCR XML
==========

This project is an attempt from the NAACCR XML Task Force to map the NAACCR flat-file format to an XML one.

### New to GitHub?

Since the project is public, you can view any files it contains using your browser.

The [/src/main/resources](https://github.com/depryf/naaccr-xml/tree/master/src/main/resources) folder contains the latest 
version of the dictionary used by the library.

Using the library
-----------------

There are three ways to use this library:

1. Using streams
2. Using utility methods
3. Using the Graphical User Interface

### Using streams
This is the recommended way to use the library; 4 streams are provided:
* [PatientXmlReader](https://github.com/depryf/naaccr-xml/blob/master/src/main/java/org/naaccr/xml/PatientXmlReader.java)
* [PatientXmlWriter](https://github.com/depryf/naaccr-xml/blob/master/src/main/java/org/naaccr/xml/PatientXmlWriter.java)
* [PatientFlatReader](https://github.com/depryf/naaccr-xml/blob/master/src/main/java/org/naaccr/xml/PatientFlatReader.java)
* [PatientFlatWriter](https://github.com/depryf/naaccr-xml/blob/master/src/main/java/org/naaccr/xml/PatientFlatWriter.java)

The readers provide a ***readPatient()*** method that returns the next patient available, or null if the end of the stream is reached.
The writers provide a ***writePatient(patient)*** method.

Transforming a flat file into the corresponding XML file and vice-versa becomes very simple with those streams; just create the streams and write every patient you read...

### Using utility methods
A few higher-level utility methods have been defined in the [NaaccrXmlUtils](https://github.com/depryf/naaccr-xml/blob/master/src/main/java/org/naaccr/xml/NaaccrXmlUtils.java) class (only the required parameters are shown for clarity):

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
* [NaaccrXmlOptions](https://github.com/depryf/naaccr-xml/blob/master/src/main/java/org/naaccr/xml/NaaccrXmlOptions.java) - options for customizing the read/write and errors reporting operations
* [NaaccrDictionary](https://github.com/depryf/naaccr-xml/blob/master/src/main/java/org/naaccr/xml/entity/dictionary/NaaccrDictionary.java) - a user-defined dictionary (if none is provided, the default user-defined dictionary will be used)
* [NaaccrObserver](https://github.com/depryf/naaccr-xml/blob/master/src/main/java/org/naaccr/xml/NaaccrObserver.java) - an observer allowing to report progress as the files are being processed.

### Using the Graphical User Interface

The library contains an experimental GUI that wraps some of the utility methods and provides a more user-friendly environment for processing files.

To start the GUI, just double-click the JAR file created from this project; it will invoke the main GUI class 
([Standlone](https://github.com/depryf/naaccr-xml/blob/master/src/main/java/org/naaccr/xml/gui/Standalone.java)).

You can also type the following in a DOS prompt, after navigating to the folder containing the JAR file:
```
java -jar naaccr-xml-XXX.jar
```
Where XXX is the downloaded version.


Convention for the item's ID (naaccrId attribute)
-------------------------------------------------

The IDs are generated from the corresponding item names using the following logic:

1. Spaces, dashes, slashes periods and underscores are considered as word separators and replaced by a single space
2. Anything in parenthesis is removed (along with the parenthesis)
3. Any non-digit and non-letter character is removed
4. The result is split by spaces
5. The first part is un-capitalized, the other parts are capitalized
6. All the parts are concatenated back together

The dictionary utility class contains a method that generates the ID for a given name, see the ***createNaaccrIdFromItemName(String)*** method in
[NaaccrDictionaryUtils](https://github.com/depryf/naaccr-xml/blob/master/src/main/java/org/naaccr/xml/NaaccrXmlDictionaryUtils.java).