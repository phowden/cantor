package com.salesforce.cantor.elasticsearch;

import com.salesforce.cantor.Objects;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.salesforce.cantor.common.ObjectsPreconditions.checkDelete;
import static com.salesforce.cantor.common.ObjectsPreconditions.checkGet;
import static com.salesforce.cantor.common.ObjectsPreconditions.checkKeys;
import static com.salesforce.cantor.common.ObjectsPreconditions.checkNamespace;
import static com.salesforce.cantor.common.ObjectsPreconditions.checkStore;

public class ObjectsOnElasticSearch implements Objects {
    private static final Logger logger = LoggerFactory.getLogger(ObjectsOnElasticSearch.class);

    private static final String objectsIndexName = "cantor-objects-index";
    private static final String objectIdFormat = "%s.%s";

    private static final String fieldNameNamespace = "namespace";
    private static final String fieldNameKey = "key";
    private static final String fieldNameValue = "value";

    private static final RequestOptions options = RequestOptions.DEFAULT;

    private static final Map<String, Object> objectsMappingSource = initializeMappingSource();

    private final RestHighLevelClient client;

    public ObjectsOnElasticSearch(final RestHighLevelClient client) throws IOException {
        this.client = client;
        // create the index where all objects are stored
        try {
            this.client.indices().create(new CreateIndexRequest(objectsIndexName).mapping(objectsMappingSource), options);
        } catch (final ElasticsearchException e) {
            logger.debug("elasticsearch exception creating index:", e);
            if (!e.getMessage().contains("resource_already_exists_exception")) {
                logger.warn("exception creating objects index:", e);
                throw e;
            }
        }
    }

    @Override
    public Collection<String> namespaces() throws IOException {
        // todo: aggregation on field 'namespace'
        // todo: implement
        return null;
    }

    @Override
    public void create(final String namespace) throws IOException {
        checkNamespace(namespace);
        doCreate(namespace);
    }

    @Override
    public void drop(final String namespace) throws IOException {
        // todo: search and remove all keys matching 'namespace'
        // todo: also drop the type if we keep it
        // todo: implement
    }

    @Override
    public void store(final String namespace, final String key, final byte[] bytes) throws IOException {
        checkStore(namespace, key, bytes);
        doStore(namespace, key, bytes);
    }

    @Override
    public byte[] get(final String namespace, final String key) throws IOException {
        checkGet(namespace, key);
        return doGet(namespace, key);
    }

    @Override
    public boolean delete(final String namespace, final String key) throws IOException {
        checkDelete(namespace, key);
        return doDelete(namespace, key);
    }

    @Override
    public Collection<String> keys(final String namespace, final int start, final int count) throws IOException {
        checkKeys(namespace, start, count);
        return doKeys(namespace, start, count);
    }

    @Override
    public int size(final String namespace) throws IOException {
        // todo: implement
        return 0;
    }

    private Collection<String> doKeys(final String namespace, final int start, final int count) throws IOException {
        final SearchSourceBuilder searchSource = new SearchSourceBuilder()
                .query(new TermQueryBuilder(fieldNameNamespace, namespace))
                .from(start);
        if (count != -1) {
            searchSource.size(count);
        }

        logger.info("getting {} keys from '{}' starting at {}", count, namespace, start);
        final SearchResponse response = this.client.search(new SearchRequest(objectsIndexName).source(searchSource), options);
        final SearchHits hits = response.getHits();
        if (hits.getTotalHits() == null || hits.getTotalHits().value == 0) {
            logger.info("got null/zero hits back for keys in '{}'", namespace);
            return null;
        }

        logger.info("got {} total hits for '{}'", hits.getTotalHits().value, namespace);
        final List<String> keys = new ArrayList<>((int) hits.getTotalHits().value);
        for (final SearchHit hit : hits) {
            final DocumentField field = hit.field(fieldNameKey);
            if (field == null || !(field.getValue() instanceof String)) {
                logger.warn("got null field, or null/non-string value: {}", field);
                continue;
            }
            keys.add(field.getValue());
        }
        logger.info("got {} valid keys from {} hits", keys.size(), hits.getTotalHits().value);
        return keys;
    }

    private byte[] doGet(final String namespace, final String key) throws IOException {
        final GetResponse response = this.client.get(new GetRequest(objectsIndexName, getId(namespace, key)).storedFields(fieldNameValue), options);
        logger.info("SOURCE: {}", response.getSource());
        final DocumentField valueField = response.getField(fieldNameValue);
        if (valueField == null) {
            logger.warn("response missing value field, fields: {}", response.getFields());
            return null;
        }

        if (valueField.getValue() instanceof String) {
            // todo: check that it's actually base 64
            return Base64.getDecoder().decode((String) valueField.getValue());
        }
        return null;
    }

    private boolean doDelete(final String namespace, final String key) throws IOException {
        // search to get ID for this object
        final SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                // create query for namespace/key
                .query(getQuery(namespace, key));
        final SearchResponse response = this.client.search(new SearchRequest(objectsIndexName).source(sourceBuilder), options);
        final SearchHits hits = response.getHits();
        if (hits.getTotalHits() == null || hits.getTotalHits().value == 0) {
            logger.info("can't delete, got null/zero hits back for object '{}.'{}'", namespace, key);
            return false;
        }

        final SearchHit hit = hits.getAt(0);
        if (hit == null || hit.getId() == null || hit.getId().length() == 0) {
            logger.warn("can't delete, got invalid hit/id from search: {}", hit);
            return false;
        }

        final DeleteResponse deleteResponse = this.client.delete(new DeleteRequest(objectsIndexName, hit.getId()), options);
        // status is only NOT_FOUND if item wasn't deleted
        return deleteResponse.status() != RestStatus.NOT_FOUND;
    }

    private QueryBuilder getQuery(final String namespace, final String key) {
        return new BoolQueryBuilder()
                .must(new TermQueryBuilder(fieldNameNamespace, namespace))
                .must(new TermQueryBuilder(fieldNameKey, key));
    }


    private void doStore(final String namespace, final String key, final byte[] bytes) throws IOException {
        final IndexRequest request = new IndexRequest(objectsIndexName)
                .id(getId(namespace, key))
                .source(XContentFactory.jsonBuilder()
                        .startObject()
                        .field(fieldNameNamespace, namespace)
                        .field(fieldNameKey, key)
                        .field(fieldNameValue, bytes)
                        .endObject()
                );
        final IndexResponse response = this.client.index(request, options);
        logger.info("got result '{}' indexing {} bytes for '{}'.'{}'", response.status(), bytes.length, namespace, key);
    }

    private void doCreate(final String namespace) throws IOException {
        // todo: what to do here?
    }

    private String getId(final String namespace, final String key) {
        return String.format(objectIdFormat, sanitize(namespace), sanitize(key));
    }

    private String sanitize(final String s) {
        return s.replaceAll("[^a-zA-Z0-9\\-]", "-");
    }

    private static Map<String, Object> initializeMappingSource() {
        final Map<String, Object> source = new HashMap<>(1);
        final Map<String, Object> properties = new HashMap<>(3);
        // keyword fields 'namespace' and 'key' are how objects are queried
        properties.put(fieldNameNamespace, Collections.singletonMap("type", "keyword"));
        properties.put(fieldNameKey, Collections.singletonMap("type", "keyword"));
        // value field requires 'store' to be true to be stored/retrieved
        final Map<String, Object> valueProperties = new HashMap<>(2);
        valueProperties.put("type", "binary");
        valueProperties.put("store", true);
        properties.put(fieldNameValue, valueProperties);

        source.put("properties", properties);
        return source;
    }
}
