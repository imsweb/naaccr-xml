/*
 * Copyright (C) 2018 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

/**
 * This exception class is used specifically to deal with duplicate items.
 */
public class DuplicateItemException extends RuntimeException {

    // the duplicate item ID
    private String _itemId;

    // the full exception message
    private String _message;

    /**
     * Constructor.
     * @param itemId duplicate item ID
     * @param message error message
     */
    public DuplicateItemException(String itemId, String message) {
        super(message);
        _itemId = itemId;
    }

    /**
     * Returns the duplicate item ID.
     * @return duplicate item ID
     */
    public String getItemId() {
        return _itemId;
    }
}
