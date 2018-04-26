filename testfile pipe 'gunzip -c synthetic-data_naaccr-18-incidence_1000-recs.txt.gz';

data testdata;
  infile testfile lrecl=4048 truncover;
    input race1	                $207 -	208  @;	label race1	= 'Race 1';
	input sex	                $222 -	222  @;	label sex	= 'Sex';
	input ageAtDiagnosis	    $223 -	225  @;	label ageAtDiagnosis	= 'Age at DX';
	input primarySite	        $554 -	557  @;	label primarySite	= 'Primary Site';
	input histologicTypeIcdO3	$564 -	567  @;	label histologicTypeIcdO3	= 'Histology ICD-O-3';

proc contents; 

proc freq; 
    tables primarySite;
run;