package com.salesforce.cantor.elasticsearch;

import com.salesforce.cantor.Events;
import org.elasticsearch.client.Client;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EventsOnElasticSearch implements Events {

    private final Client client;

    public EventsOnElasticSearch(final Client client) {
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
    public void store(final String namespace, final Collection<Event> batch) throws IOException {

    }

    @Override
    public List<Event> get(final String namespace, final long startTimestampMillis, final long endTimestampMillis, final Map<String, String> metadataQuery, final Map<String, String> dimensionsQuery, final boolean includePayloads) throws IOException {
        return null;
    }

    @Override
    public int delete(final String namespace, final long startTimestampMillis, final long endTimestampMillis, final Map<String, String> metadataQuery, final Map<String, String> dimensionsQuery) throws IOException {
        return 0;
    }

    @Override
    public Map<Long, Double> aggregate(final String namespace, final String dimension, final long startTimestampMillis, final long endTimestampMillis, final Map<String, String> metadataQuery, final Map<String, String> dimensionsQuery, final int aggregateIntervalMillis, final AggregationFunction aggregationFunction) throws IOException {
        return null;
    }

    @Override
    public Set<String> metadata(final String namespace, final String metadataKey, final long startTimestampMillis, final long endTimestampMillis, final Map<String, String> metadataQuery, final Map<String, String> dimensionsQuery) throws IOException {
        return null;
    }

    @Override
    public void expire(final String namespace, final long endTimestampMillis) throws IOException {

    }
}
