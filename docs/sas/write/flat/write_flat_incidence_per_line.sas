/*************************************************************************************************************************;
   This program reads a NAACCR Incidence file and re-creates the same file, it uses a single variable to read each line.
 ************************************************************************************************************************/;
filename inflat 'synthetic-data_naaccr-180-incidence_10-records.txt';
filename outflat 'synthetic-data_naaccr-180-incidence_10-records_copy.txt';

data testdata;
  infile inflat lrecl=4048;
    input @1 full_record $char4048.;

data _null_;
  set testdata;
    file outflat lrecl=4048;
    put @1 full_record $char4048.; 
run;