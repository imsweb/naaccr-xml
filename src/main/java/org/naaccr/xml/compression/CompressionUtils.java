/*
 * Copyright (C) 2014 Information Management Services, Inc.
 */
package org.naaccr.xml.compression;

/**
 * Test on 11/22/2014
 * NAACCR 14 Incidence file with 10,000 records (fake data, lots of repeated data)
 *     flat file: 32.6MB
 *     compressed flat file (7zip, using 7zip software and default compression level): 0.3MB
 *     compressed flat file (gzip, using 7zip software and default compression level): 3.6MB
 *     compressed flat file (gzip, embedded java, favor compression speed): 4.3MB
 *     compressed flat file (gzip, embedded java, favor compression speed): 4.3M
 *     compressed flat file file (xz, embedded java, favor compression speed): 4.0MB
 *     compressed flat file file (xz, embedded java, favor compression size): 0.3MB
 *     
 *     xml file: 141MB
 *     compressed xml file (gzip, using 7zip software and default compression level): 7.7MB
 *     compressed xml file (7zip, using 7zip software and default compression level): 0.7MB
 *     compressed xml file (exi, non-commercial): 11.7MB
 *     compressed xml file (gzip, embedded java, favor compression speed): 22.8MB
 *     compressed xml file (gzip, embedded java, favor compression size): 8.3MB
 *     compressed xml file (xz, embedded java, favor compression speed): 6.1MB
 *     compressed xml file (xz, embedded java, favor compression size): 0.5MB
 *        -> looks to me gzip is not very well implemented in pure Java; at least not as efficiently; 
 *           ultimately I will need another developer to re-do these tests and confirm!
 *        -> that xz library is pretty awesome, and the result is fully compatible with 7zip!
 *     
 *  Testing line feed and indentation:
 *     xml file without line feeds and indentation: 121MB
 *     compressed xml file (gzip): 7.6MB
 *     compressed xml file (7zip): 0.7MB
 *         -> the indentation characters make no difference with the compression...
 *     
 *  Testing the time it takes to write the data file:
 *     xml file using no compression: 121MB, took 54 seconds
 *     xml file using an XZ output stream, favor compression speed: 6.1MB, took 65 seconds
 *     xml file using an XZ output stream, favor compression size: 0.5MB, took 232 seconds (almost 4 minutes!)
 *         -> best compression comes with a price; we will probably need to compromise...
 *         -> on the other hand, reading/writing is usually a tiny part of the full processing of a file, so maybe not...
 *
 *  Testing processing the data file (in this case we built the record/patient, and just incremented a count):  
 *     processing compressed flat file (gzip compression; reading records): 25 seconds
 *     processing compressed xml file (xz compression; reading patients): 12 seconds
 *     
 *     
 *     MORE TESTS...
 *     
 *     Incidence file with 1,607,167 Incidence records
 *
 *      uncompressed file: 1072MB
 *      gzip file: 160MB
 *      xz file (heavy compression): 76MB (created in about 20 minutes)
 *      xz file (medium compression): 82MB (created in about 18 minutes)
 *      xz file (light compression): 91MB (created in about 3 minutes)
 *
 *      processing uncompressed file: N/A
 *      processing gzip file: 32 seconds
 *      processing xz file (heavy compression): 42 seconds
 *      processing xz file (medium compression): 43 seconds
 *      processing xz file (light compression): 43 seconds
 *
 */
public class CompressionUtils {

    public static void main(String[] args) throws Exception {
        System.out.println("TODO")
    }

}
