/******************************************************************************************;
    This program reads a NAACCR XML Incidence data file and writes it in that same format.
 ******************************************************************************************/;

filename inxml 'synthetic-data_naaccr-180-incidence_10-tumors.xml';
filename xmldef 'naaccr-xml-sas-def-180-incidence.map';
libname inxml XMLV2 xmlmap=xmldef access=READONLY;
filename outxml 'synthetic-data_naaccr-180-incidence_10-tumors_copy.xml';

/* we have to read the data to be able to write it, let's uses the XML mapper */
data naaccrdata;
  set inxml.naaccrdata; 
data patients; 
  set inxml.patients; 
data tumors; 
  set inxml.tumors; 
data patients;
  merge naaccrdata patients; 
    by naaccrdatakey; 
data xmldata;
  merge patients tumors; 
    by patientkey; 

/* turn of the listing, it's too verbose */
ods listing close;

/* load the NAACCR XML tagset */
ods path(prepend) work.templat(update);
%include "naaccr-xml-sas-tags.tpl";

/* write the data using the NAACCR XML tagset */
ods markup tagset=naaccrxml file=outxml options(naaccr_version='180' record_type='I');
proc print data=xmldata;
run;
ods markup close;
