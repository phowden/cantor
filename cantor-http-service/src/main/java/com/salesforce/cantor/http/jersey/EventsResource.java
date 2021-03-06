/*
 * Copyright (c) 2019, Salesforce.com, Inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cantor.http.jersey;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.salesforce.cantor.Cantor;
import com.salesforce.cantor.Events;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.salesforce.cantor.Events.Event;

@Component
@Path("/events")
@Tag(name = "Events Resource", description = "Api for handling Cantor Events")
public class EventsResource {
    private static final Logger logger = LoggerFactory.getLogger(EventsResource.class);
    private static final String serverErrorMessage = "Internal server error occurred";
    private static final String jsonFieldCount = "count";

    private static final Pattern queryPatterns = Pattern.compile("(?<key>.*?)(?<value>(>|<|=|~|<=|>=).*)");
    // custom gson parser to auto-convert payload to byte[]
    private static final Gson parser = new GsonBuilder()
            .registerTypeHierarchyAdapter(byte[].class, new ByteArrayHandler()).create();

    private final Cantor cantor;

    @Autowired
    public EventsResource(final Cantor cantor) {
        this.cantor = cantor;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all events namespaces")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Provides the list of all namespaces",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))),
            @ApiResponse(responseCode = "500", description = serverErrorMessage)
    })
    public Response getNamespaces() throws IOException {
        logger.info("received request for all events namespaces");
        return Response.ok(parser.toJson(this.cantor.events().namespaces())).build();
    }

    @GET
    @Path("/{namespace}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get events in a namespace")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                     description = "Provides the list of events matching query parameters",
                     content = @Content(array = @ArraySchema(schema = @Schema(implementation = HttpModels.EventModel.class)))),
        @ApiResponse(responseCode = "400", description = "One of the query parameters has a bad value"),
        @ApiResponse(responseCode = "500", description = serverErrorMessage)
    })
    public Response getEvents(@Parameter(description = "Namespace identifier") @PathParam("namespace") final String namespace,
                              @BeanParam final EventsDataSourceBeanWithPayload bean) throws IOException {
        logger.info("received request for events in namespace {}", namespace);
        logger.debug("request parameters: {}", bean);
        final List<Event> results = this.cantor.events().get(
                namespace,
                bean.getStart(),
                bean.getEnd(),
                bean.getMetadataQuery(),
                bean.getDimensionQuery(),
                bean.isIncludePayloads()
        );

        return Response.ok(parser.toJson(results)).build();
    }

    @GET
    @Path("/{namespace}/metadata/{metadata}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all values for a metadata")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                     description = "Provides the set of values the given metadata has been, matching query parameters",
                     content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))),
        @ApiResponse(responseCode = "400", description = "One of the query parameters has a bad value"),
        @ApiResponse(responseCode = "500", description = serverErrorMessage)
    })
    public Response getMetadata(@Parameter(description = "Namespace identifier") @PathParam("namespace") final String namespace,
                                @Parameter(description = "Specific metadata to search") @PathParam("metadata") final String metadata,
                                @BeanParam final EventsDataSourceBean bean) throws IOException {
        logger.info("received request for metadata {} in namespace {}", metadata, namespace);
        logger.debug("request parameters: {}", bean);
        final Set<String> metadataValueSet = this.cantor.events().metadata(
                namespace,
                metadata,
                bean.getStart(),
                bean.getEnd(),
                bean.getMetadataQuery(),
                bean.getDimensionQuery());

        return Response.ok(parser.toJson(metadataValueSet)).build();
    }

    @GET
    @Path("/{namespace}/aggregate/{aggregate}/{dimension}/{bucket}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Aggregate a dimension")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                     description = "Provides a json with each property being the bucketed timestamp to the aggregated value",
                     content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "Aggregate function was not a valid type or one of the query parameters has a bad value"),
        @ApiResponse(responseCode = "500", description = serverErrorMessage)
    })
    public Response getAggregate(@Parameter(description = "Namespace identifier") @PathParam("namespace") final String namespace,
                                 @Parameter(description = "Specific dimension to aggregate") @PathParam("dimension") final String dimension,
                                 @Parameter(description = "The aggregation function (AVG, MIN, MAX, SUM, COUNT, STDDEV_POP, STDDEV_SAMP, VAR_POP, VAR_SAMP)") @PathParam("aggregate") final String aggregate,
                                 @Parameter(description = "The type of grouping (hour, minute, [number])") @PathParam("bucket") final String bucket,
                                 @BeanParam final EventsDataSourceBean bean) throws IOException {
        logger.info("received request for aggregate {} of dimension {} in namespace {} with buckets {}", aggregate, dimension, namespace, bucket);
        logger.debug("request parameters: {}", bean);
        final int bucketMillis;
        switch (bucket) {
            case "hour":
                bucketMillis = (int) TimeUnit.HOURS.toMillis(1) - 1; // buckets are still maxed at an hour
                break;
            case "minute":
                bucketMillis = (int) TimeUnit.MINUTES.toMillis(1);
                break;
            default:
                try {
                    bucketMillis = Integer.valueOf(bucket);
                } catch (final NumberFormatException nfe) {
                    throw new IllegalArgumentException("bucket" + bucket + " is not hour/minute/NUMBER", nfe);
                }
        }

        Events.AggregationFunction aggregateFunction = null;
        for (final Events.AggregationFunction agg : Events.AggregationFunction.values()) {
            if (aggregate.equalsIgnoreCase(agg.name())) {
                aggregateFunction = agg;
                break;
            }
        }
        if (aggregateFunction == null) {
            throw new IllegalArgumentException("invalid aggregate function: " + aggregate);
        }

        final Map<Long, Double> results = this.cantor.events().aggregate(
                namespace,
                dimension,
                bean.getStart(),
                bean.getEnd(),
                bean.getMetadataQuery(),
                bean.getDimensionQuery(),
                bucketMillis,
                aggregateFunction
        );
        return Response.ok(parser.toJson(results)).build();
    }

    @PUT
    @Path("/{namespace}")
    @Operation(summary = "Create an event namespace")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event namespace was successfully created or already existed"),
        @ApiResponse(responseCode = "500", description = serverErrorMessage)
    })
    public Response createNamespace(@Parameter(description = "Namespace identifier") @PathParam("namespace") final String namespace) throws IOException {
        logger.info("received request for creation of namespace {}", namespace);
        this.cantor.events().create(namespace);
        return Response.ok().build();
    }

    @POST
    @Path("/{namespace}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Add list of events")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Events were added. (All events are immutable and will not overwrite each other)"),
            @ApiResponse(responseCode = "500", description = serverErrorMessage)
    })
    public Response storeMultipleEvents(@Parameter(description = "Namespace identifier") @PathParam("namespace") final String namespace,
                                        final List<HttpModels.EventModel> jsonEvents) throws IOException {
        logger.info("received request for json event upload in namespace {}", namespace);
        logger.debug("received request event: {}", jsonEvents);
        final List<Event> events = jsonEvents.stream().map(HttpModels.EventModel::toCantorEvent).collect(Collectors.toList());
        this.cantor.events().store(namespace, events);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{namespace}")
    @Operation(summary = "Drop an event namespace")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event namespace was dropped or didn't exist"),
        @ApiResponse(responseCode = "500", description = serverErrorMessage)
    })
    public Response dropNamespace(@Parameter(description = "Namespace identifier") @PathParam("namespace") final String namespace) throws IOException {
        logger.info("received request to drop namespace {}", namespace);
        this.cantor.events().drop(namespace);
        return Response.ok().build();
    }

    @DELETE
    @Path("/expire/{namespace}/{endTimestampMillis}")
    @Operation(summary = "Expire old events")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All events older than specified timestamp were deleted"),
        @ApiResponse(responseCode = "500", description = serverErrorMessage)
    })
    public Response expire(@Parameter(description = "Namespace identifier") @PathParam("namespace") final String namespace,
                           @Parameter(description = "Time which all events up to will be expired") @PathParam("endTimestampMillis") final long endTimestampMillis) throws IOException {
        logger.info("received request to expire events since {} in namespace {}", endTimestampMillis, namespace);
        this.cantor.events().expire(namespace, endTimestampMillis);
        return Response.ok().build();
    }

    @DELETE
    @Path("/delete/{namespace}")
    @Operation(summary = "Delete events")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "All specified events were deleted",
                         content = @Content(schema = @Schema(implementation = HttpModels.CountResponse.class))),
            @ApiResponse(responseCode = "500", description = serverErrorMessage)
    })
    public Response dropEvents(@Parameter(description = "Namespace identifier") @PathParam("namespace") final String namespace,
                               @BeanParam final EventsDataSourceBean bean) throws IOException {
        logger.info("received request to drop namespace {}", namespace);
        final int eventsDeleted = this.cantor.events().delete(
                namespace,
                bean.getStart(),
                bean.getEnd(),
                bean.getMetadataQuery(),
                bean.getDimensionQuery());
        final Map<String, Integer> countResponse = new HashMap<>();
        countResponse.put(jsonFieldCount, eventsDeleted);
        return Response.ok(parser.toJson(countResponse)).build();
    }

    protected static class EventsDataSourceBean {
        @Parameter(description = "The earliest timestamp in milliseconds the query can match", example = "0")
        @QueryParam("start")
        private long start;

        @Parameter(description = "The latest timestamp in milliseconds the query can match", example = "0")
        @QueryParam("end")
        private long end;

        @Parameter(description = "Metadata and its value that an event should have to match (regex can be used with `~`, like `name=~examp.*`)")
        @QueryParam("metadata_query")
        private List<String> acceptedMetadataQuery;

        @Parameter(description = "Dimension and its value that an event should have to match (operators: [..,>,<,=,~,<=,>=])")
        @QueryParam("dimension_query")
        private List<String> acceptedDimensionQuery;

        private static Map<String, String> queryToMap(final List<String> queryList) {
            if (queryList.isEmpty()) {
                return Collections.emptyMap();
            }

            final Map<String, String> queryMap = new HashMap<>();
            for (final String query : queryList) {
                if (query == null || query.isEmpty()) {
                    continue;
                }

                final Matcher matcher = queryPatterns.matcher(query);
                if (matcher.matches()) {
                    if (query.contains("..") || query.contains("~")) {
                        // remove the equals when using these operators
                        queryMap.put(matcher.group("key"), matcher.group("value").substring(1));
                    } else {
                        queryMap.put(matcher.group("key"), matcher.group("value"));
                    }
                } else {
                    throw new IllegalArgumentException("Invalid query format: " + query);
                }
            }
            return queryMap;
        }

        /**
         * Metadata query converted to Cantor query format
         */
        Map<String, String> getMetadataQuery() {
            return queryToMap(this.acceptedMetadataQuery);
        }

        /**
         * Dimension query converted to Cantor query format
         */
        Map<String, String> getDimensionQuery() {
            return queryToMap(this.acceptedDimensionQuery);
        }

        /*
         * Getters and setter are required for Swagger to process the Jersey bean.
         * Swagger does not currently support the BeanParam annotation with a constructor
         */

        public long getStart() {
            return start;
        }

        public long getEnd() {
            if (this.end == -1) {
                this.end = Long.MAX_VALUE;
                logger.info("setting bean end to Long.MAX_VALUE");
            }
            return end;
        }

        public List<String> getAcceptedMetadataQuery() {
            return acceptedMetadataQuery;
        }

        public List<String> getAcceptedDimensionQuery() {
            return acceptedDimensionQuery;
        }

        public void setStart(final long start) {
            this.start = start;
        }

        public void setEnd(final long end) {
            this.end = end;
        }

        public void setAcceptedMetadataQuery(final List<String> acceptedMetadataQuery) {
            this.acceptedMetadataQuery = acceptedMetadataQuery;
        }

        public void setAcceptedDimensionQuery(final List<String> acceptedDimensionQuery) {
            this.acceptedDimensionQuery = acceptedDimensionQuery;
        }
    }

    protected static class EventsDataSourceBeanWithPayload extends EventsDataSourceBean {
        @Parameter(description = "Defaulted to false, will include the payload of the event", example = "false")
        @QueryParam("include_payloads")
        private boolean includePayloads;

        public boolean isIncludePayloads() {
            return includePayloads;
        }

        public void setIncludePayloads(final boolean includePayloads) {
            this.includePayloads = includePayloads;
        }
    }

    /**
     * Serialize override to allow conversion of payload string to byte array
     */
    private static class ByteArrayHandler implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {

        @Override
        public byte[] deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            return jsonElement.getAsString().getBytes();
        }

        @Override
        public JsonElement serialize(final byte[] bytes, final Type type, final JsonSerializationContext jsonSerializationContext) {
            final String encodedString = Base64.getEncoder().encodeToString(bytes);
            return new JsonPrimitive(encodedString);
        }
    }
}
