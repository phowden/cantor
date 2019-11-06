package com.salesforce.cantor.elasticsearch;

import com.salesforce.cantor.Sets;
import org.elasticsearch.client.Client;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class SetsOnElasticSearch implements Sets {

    private final Client client;

    public SetsOnElasticSearch(final Client client) {
        this.client = client;
    }

    @Override
    public Collection<String> namespaces() throws IOException {
        return null;
    }

    @Override
    public void create(final String namespace) throws IOException {

    }

    @Override
    public void drop(final String namespace) throws IOException {

    }

    @Override
    public void add(final String namespace, final String set, final String entry, final long weight) throws IOException {

    }

    @Override
    public void add(final String namespace, final String set, final Map<String, Long> entries) throws IOException {

    }

    @Override
    public Collection<String> entries(final String namespace, final String set, final long min, final long max, final int start, final int count, final boolean ascending) throws IOException {
        return null;
    }

    @Override
    public Map<String, Long> get(final String namespace, final String set, final long min, final long max, final int start, final int count, final boolean ascending) throws IOException {
        return null;
    }

    @Override
    public void delete(final String namespace, final String set, final long min, final long max) throws IOException {

    }

    @Override
    public boolean delete(final String namespace, final String set, final String entry) throws IOException {
        return false;
    }

    @Override
    public void delete(final String namespace, final String set, final Collection<String> entries) throws IOException {

    }

    @Override
    public Map<String, Long> union(final String namespace, final Collection<String> sets, final long min, final long max, final int start, final int count, final boolean ascending) throws IOException {
        return null;
    }

    @Override
    public Map<String, Long> intersect(final String namespace, final Collection<String> sets, final long min, final long max, final int start, final int count, final boolean ascending) throws IOException {
        return null;
    }

    @Override
    public Map<String, Long> pop(final String namespace, final String set, final long min, final long max, final int start, final int count, final boolean ascending) throws IOException {
        return null;
    }

    @Override
    public Collection<String> sets(final String namespace) throws IOException {
        return null;
    }

    @Override
    public int size(final String namespace, final String set) throws IOException {
        return 0;
    }

    @Override
    public Long weight(final String namespace, final String set, final String entry) throws IOException {
        return null;
    }

    @Override
    public void inc(final String namespace, final String set, final String entry, final long count) throws IOException {

    }
}
