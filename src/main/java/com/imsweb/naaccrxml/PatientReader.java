/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import com.imsweb.naaccrxml.entity.NaaccrData;
import com.imsweb.naaccrxml.entity.Patient;

/**
 * Common interface for the two types of patient readers supported by this library.
 */
public interface PatientReader extends AutoCloseable {

    /**
     * Reads the next patient on this stream.
     * @return the next available patient, null if not such patient
     * @throws NaaccrIOException if anything goes wrong
     */
    Patient readPatient() throws NaaccrIOException;

    /**
     * Returns the "root" data; it includes root attributes and the root items.
     * @return the root data, never null
     */
    NaaccrData getRootData();

    /**
     * Reads the final node of the document, without closing the stream.
     */
    void closeAndKeepAlive();
}
