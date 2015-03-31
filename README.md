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

* void ***flatToXml***(File flatFile, File xmlFile, ...)
* void ***xmlToFlat***(File xmlFile, File flatFile, ...)
* NaaccrData ***readXmlFile***(File xmlFile, ...)
* void ***writeXmlFile***(NaaccrData data, File xmlFile, ...)
* NaaccrData ***readFlatFile***(File flatFile, ...)
* void ***writeFlatFile***(NaaccrData data, File flatFile, ...)

There are other utility methods, but those are the main ones.

### Using the Graphical User Interface

The library contains an experimental GUI that wraps some of the utility methods and provides a more user-friendly environment for translating files.

To start the GUI, just double-click the JAR file created from this project; it will invoke the main GUI class 
([Standlone](https://github.com/depryf/naaccr-xml/blob/master/src/main/java/org/naaccr/xml/gui/Standalone.java)).