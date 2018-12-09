
Introduction
************

The NAACCR XML Utility tool can be used to process NAACCR XML files. It provides the following features:
 - Transform a NAACCR XML file into a NAACCR fixed-columns file.
 - Transform a NAACCR fixed-columns file into a NAACCR XML file.
 - Validate a NAACCR XML file.
 - View all standard dictionaries.
 - Create or edit user-defined dictionaries.

The tool implements the NAACCR XML Specifications provided by NAACCR (https://www.naaccr.org/xml-data-exchange-standard/).

It was developed by Information Management Services, Inc. (https://www.imsweb.com/) under contract to the National Cancer Institute.

More information is available on the project wiki (https://github.com/imsweb/naaccr-xml/wiki).


Installation
************

To install the tool, download the distributed ZIP file, and unzip it anywhere you would like.

The program does not copy/modify any other files or environment on your computer.


Running the application on Windows
**********************************

To run the application on Windows, simply double-click the EXE file.

By default, the application will use its own embedded Java Runtime Environment.
It is possible to start it with a different environment using the following command line in a DOS prompt:

java -jar lib\naaccr-xml-utility.jar


Running the application on MAC
******************************

For MAC users, the application requires a JDK installed on the computer (the embedded JRE is only for Windows).

To check if a JDK is available, open a terminal and type

java -version

If you do not see a proper version, download a JDK (http://jdk.java.net/ is one free option but there are many others).
Once the JDK is available, you should be able to double click the naaccr-xml-utility.jar in the lib folder.

You can also start the application from a terminal using the following command:

java -jar lib/naaccr-xml-utility.jar
