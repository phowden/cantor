/*
 * Copyright (c) 2019, Salesforce.com, Inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cantor.metrics;

import com.codahale.metrics.MetricRegistry;
import com.salesforce.cantor.Objects;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;

public class MetricCollectingObjects extends BaseMetricCollectingCantor implements Objects {
    private final Objects delegate;
    
    public MetricCollectingObjects(final MetricRegistry metrics, final Objects delegate) {
        super(metrics, delegate);
        this.delegate = delegate;
    }

    @Override
    public Collection<String> namespaces() throws IOException {
        return metrics(this.delegate::namespaces, "namespaces", "cantor", super::size);
    }

    @Override
    public void create(final String namespace) throws IOException {
        metrics(() -> this.delegate.create(namespace), "create", "cantor");
    }

    @Override
    public void drop(final String namespace) throws IOException {
        metrics(() -> this.delegate.drop(namespace), "drop", "cantor");
    }

    @Override
    public void store(final String namespace, final String key, final byte[] bytes) throws IOException {
        metrics(() -> this.delegate.store(namespace, key, bytes), "store", "cantor");
    }

    @Override
    public byte[] get(final String namespace, final String key) throws IOException {
        return metrics(() -> this.delegate.get(namespace, key), "get", "cantor", bytes -> bytes.length);
    }

    @Override
    public boolean delete(final String namespace, final String key) throws IOException {
        return metrics(() -> this.delegate.delete(namespace, key), "delete", "cantor", bool -> bool ? 1 : 0);
    }

    @Override
    public Collection<String> keys(final String namespace, final int start, final int count) throws IOException {
        return metrics(() -> this.delegate.keys(namespace, start, count), "keys", "cantor", super::size);
    }

    @Override
    public int size(final String namespace) throws IOException {
        return metrics(() -> this.delegate.size(namespace), "size", "cantor", Function.identity());
    }
}
