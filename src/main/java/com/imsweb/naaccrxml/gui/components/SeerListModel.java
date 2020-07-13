/*
 * Copyright (C) 2018 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml.gui.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractListModel;

public class SeerListModel<E> extends AbstractListModel<E> {

    protected List<E> _originalData;

    protected List<E> _filteredData;

    protected Comparator<E> _comparator;

    protected int _filteringMode;

    protected String _currentFilter;

    public SeerListModel(List<E> data, int filteringMode, Comparator<E> comparator) {
        if (data == null)
            throw new RuntimeException("Data list is required");

        _originalData = new ArrayList<>(data);
        _filteredData = new ArrayList<>(data.size());
        _filteringMode = filteringMode;
        _comparator = comparator;

        // it's better if original data uses the same order as the filter one
        if (comparator != null)
            _originalData.sort(_comparator);

        filter(null);
    }

    public void removeElement(E elem) {
        int idx = _originalData.indexOf(elem);
        if (idx > -1) {
            _originalData.remove(elem);
            filter(_currentFilter);
        }
    }

    public void addElement(E elem) {
        addElement(_originalData.size(), elem);
    }

    public void addElement(int idx, E elem) {
        _originalData.add(idx, elem);
        filter(_currentFilter);
    }

    public void filter(String filter) {
        // remove previous data
        int size = _filteredData.size();
        _filteredData.clear();
        fireIntervalRemoved(this, 0, size);

        // filter new data
        for (E obj : _originalData) {
            if (obj == null)
                continue;

            if (filterElement(obj, filter))
                _filteredData.add(obj);
        }
        if (_comparator != null)
            _filteredData.sort(_comparator);
        _currentFilter = filter;
        fireIntervalAdded(this, 0, _filteredData.size());
    }

    @SuppressWarnings("IfStatementWithIdenticalBranches")
    protected boolean filterElement(E element, String filter) {
        String s = element instanceof String ? ((String)element).toLowerCase() : element.toString().toLowerCase();

        boolean add = false;
        if (filter == null || filter.isEmpty())
            add = true;
        else {
            String filterLower = filter.toLowerCase();
            if (_filteringMode == SeerList.FILTERING_MODE_EQUALS && s.equals(filterLower))
                add = true;
            else if (_filteringMode == SeerList.FILTERING_MODE_STARTS_WITH && s.startsWith(filterLower))
                add = true;
            else if (_filteringMode == SeerList.FILTERING_MODE_CONTAINED && s.contains(filterLower))
                add = true;
        }

        return add;
    }

    @Override
    public E getElementAt(int index) {
        return _filteredData.get(index);
    }

    @Override
    public int getSize() {
        return _filteredData.size();
    }

    public int indexOf(E element) {
        return _filteredData.indexOf(element);
    }

    public List<E> getOriginalData() {
        return Collections.unmodifiableList(_originalData);
    }

    public List<E> getFilteredData() {
        return Collections.unmodifiableList(_filteredData);
    }
}
