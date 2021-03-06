/*
 * Copyright (c) 2019, Salesforce.com, Inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cantor.metrics;

import com.codahale.metrics.MetricRegistry;
import com.salesforce.cantor.Maps;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

public class MetricCollectingMaps extends BaseMetricCollectingCantor implements Maps {
    private final Maps delegate;

    public MetricCollectingMaps(final MetricRegistry metrics, final Maps delegate) {
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
    public void store(final String namespace, final Map<String, String> map) throws IOException {
        metrics(() -> this.delegate.store(namespace, map), "store", "cantor");
    }

    @Override
    public Collection<Map<String, String>> get(final String namespace, final Map<String, String> query) throws IOException {
        return metrics(() -> this.delegate.get(namespace, query), "get", "cantor", super::size);
    }

    @Override
    public int delete(final String namespace, final Map<String, String> query) throws IOException {
        return metrics(() -> this.delegate.delete(namespace, query), "delete", "cantor", Function.identity());
    }
}
