/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.Closeable;

import com.imsweb.naaccrxml.entity.Patient;

public interface PatientWriter extends Closeable {

    /**
     * Writes the given patient on this stream.
     * @throws NaaccrIOException if anything goes wrong
     */
    void writePatient(Patient patient) throws NaaccrIOException;

    /**
     * Writes the final node of the document, without closing the stream.
     * @throws NaaccrIOException if anything goes wrong
     */
    void closeAndKeepAlive() throws NaaccrIOException;
}
