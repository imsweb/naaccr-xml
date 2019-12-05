/************************************************************************************************************;
    This programs demonstrates how to include and call the "write_naaccr_xml_macro.sas" macro.
 ************************************************************************************************************/;
%include "read_naaccr_xml_macro.sas";
%readNaaccrXml(
  libpath="naaccr-xml-6.4-sas.jar",
  sourcefile="synthetic-data_naaccr-180-incidence_10-tumors.xml",
  naaccrversion="180", 
  recordtype="I",
  dataset=fromxml,
  dictfile="my-own-dictionary.csv"
);

proc freq data=fromxml;
    tables primarySite;
run;


%include "write_naaccr_xml_macro.sas";
%writeNaaccrXml(
  libpath="naaccr-xml-6.4-sas.jar",
  targetfile="test.xml.gz",
  naaccrversion="180", 
  recordtype="I",
  dataset=fromxml,
  dictfile="my-own-dictionary.csv"
);
