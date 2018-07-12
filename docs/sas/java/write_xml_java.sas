/************************************************************************************************************;
    This programs reads a NAACCR Incidence data files, runs a frequency and re-creates the original file.

    Paramaters (for the write macro):
	- libpath needs to point to the Java SAS library (path can be relative or absolute)
	- targetfile needs to point to the XML to export; if path ends with ".gz" it will be processed as a GZIP 
	  compressed file, otherwise it will be processed as an uncompressed file (path can be relative or absolute)
	- naaccrversion should be "140", "150", "160" or "180" (defaults to "180")
	- recordtype should be "A", "M", "C" or "I" (defaults to "I")
    - dataset should be the name of the dataset from which the data should be taken (defaults to alldata)

    Note that the macro creates a tmp CSV file in the same folder as the target file; that file will be 
    automatically deleted by the macro when it's done executing.
 ************************************************************************************************************/;
%include "read_naaccr_xml_macro.sas";
%readNaaccrXml(
  libpath="naaccr-xml-4.10-sas.jar",
  sourcefile="synthetic-data_naaccr-180-incidence_10-tumors.xml",
  naaccrversion="180", 
  recordtype="I",
  dataset=fromxml
);

proc freq data=fromxml;
    tables primarySite;
run;


%include "write_naaccr_xml_macro.sas";
%writeNaaccrXml(
  libpath="naaccr-xml-4.10-sas.jar",
  targetfile="test.xml.gz",
  naaccrversion="180", 
  recordtype="I",
  dataset=fromxml
);
