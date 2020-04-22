/************************************************************************************************************;
    This programs demonstrates how to include and call the "read_naaccr_xml_macro.sas" macro.
 ************************************************************************************************************/;

%include "read_naaccr_xml_macro.sas";
%readNaaccrXml(
  libpath="naaccr-xml-6.7-sas.jar",
  sourcefile="synthetic-data_naaccr-180-incidence_10-tumors.xml",
  naaccrversion="180", 
  recordtype="I",
  dataset=fromxml,
  items="patientIdNumber,primarySite",
  dictfile="my-own-dictionary.csv"
);

proc freq data=fromxml;
    tables primarySite;
run;