package com.salesforce.cantor.misc.distributed;

import com.salesforce.cantor.Objects;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static com.salesforce.cantor.common.CommonPreconditions.checkCreate;
import static com.salesforce.cantor.common.CommonPreconditions.checkDrop;
import static com.salesforce.cantor.common.ObjectsPreconditions.checkDelete;
import static com.salesforce.cantor.common.ObjectsPreconditions.checkGet;
import static com.salesforce.cantor.common.ObjectsPreconditions.checkKeys;
import static com.salesforce.cantor.common.ObjectsPreconditions.checkSize;
import static com.salesforce.cantor.common.ObjectsPreconditions.checkStore;

public class DistributedObjects extends BaseDistributedCantor<Objects> implements Objects {
    protected DistributedObjects(final List<Objects> delegates, final double ratio) {
        super(delegates, ratio);
    }

    @Override
    public Collection<String> namespaces() throws IOException {
        return getDistributedNamespaces(Objects::namespaces);
    }

    @Override
    public void create(final String namespace) throws IOException {
        checkCreate(namespace);
        write("create", o -> o.create(namespace));
    }

    @Override
    public void drop(final String namespace) throws IOException {
        checkDrop(namespace);
        write("drop", o -> o.drop(namespace));
    }

    @Override
    public void store(final String namespace, final String key, final byte[] bytes) throws IOException {
        checkStore(namespace, key, bytes);
        write("store", o -> o.store(namespace, key, bytes));
    }

    @Override
    public byte[] get(final String namespace, final String key) throws IOException {
        checkGet(namespace, key);
        return read("get", o -> o.get(namespace, key));
    }

    @Override
    public boolean delete(final String namespace, final String key) throws IOException {
        checkDelete(namespace, key);
        final Boolean deleted = writeAndGetResult("delete", o -> o.delete(namespace, key));
        return deleted == null ? false : deleted;
    }

    @Override
    public Collection<String> keys(final String namespace, final int start, final int count) throws IOException {
        checkKeys(namespace, start, count);
        return read("keys", o -> o.keys(namespace, start, count));
    }

    @Override
    public int size(final String namespace) throws IOException {
        checkSize(namespace);
        final Integer size = read("size", o -> o.size(namespace));
        return size == null ? 0 : size;
    }
}
