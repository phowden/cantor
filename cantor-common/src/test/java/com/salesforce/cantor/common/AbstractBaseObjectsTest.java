/*
 * Copyright (c) 2019, Salesforce.com, Inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cantor.common;

import com.salesforce.cantor.Objects;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public abstract class AbstractBaseObjectsTest extends AbstractBaseCantorTest {
    // override to store less (for impls that storing is more expensive) by setting to less than 1
    protected double getStoreMagnitude() {
        return 1.0D;
    }

    @Test
    public void testBadInput() throws Exception {
        final Objects objects = getObjects();
        final String namespace = UUID.randomUUID().toString();
        objects.create(namespace);

        assertThrows(IllegalArgumentException.class, () -> objects.store(namespace, null, new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> objects.store(namespace, "", new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> objects.store(namespace, "abc", null));

        assertThrows(IllegalArgumentException.class, () -> objects.get(namespace, (String) null));
        assertThrows(IllegalArgumentException.class, () -> objects.get(namespace, ""));
        assertThrows(IllegalArgumentException.class, () -> objects.get(namespace, (Collection<String>) null));

        assertThrows(IllegalArgumentException.class, () -> objects.delete(namespace, (String) null));
        assertThrows(IllegalArgumentException.class, () -> objects.delete(namespace, ""));
        assertThrows(IllegalArgumentException.class, () -> objects.delete(namespace, (Collection<String>) null));

        // trying to store an object in a namespace that is not created yet should throw ioexception
        assertThrows(IOException.class, () -> objects.store(UUID.randomUUID().toString(), "foo", "bar".getBytes()));
        objects.drop(namespace);
    }

    @Test
    public void testNamespaces() throws Exception {
        final Objects objects = getObjects();
        final List<String> namespaces = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            final String namespace = UUID.randomUUID().toString();
            namespaces.add(namespace);
            assertFalse(objects.namespaces().contains(namespace));

            objects.create(namespace);
            assertTrue(objects.namespaces().contains(namespace));
        }

        for (final String namespace : namespaces) {
            objects.drop(namespace);
            assertFalse(objects.namespaces().contains(namespace));
        }
    }

    @Test
    public void testStoreGetDelete() throws Exception {
        final Objects objects = getObjects();
        final String namespace = UUID.randomUUID().toString();
        objects.create(namespace);

        final String key = UUID.randomUUID().toString();
        final byte[] value = UUID.randomUUID().toString().getBytes();

        assertNull(objects.get(namespace, key));
        assertFalse(objects.delete(namespace, key));
        objects.store(namespace, key, value);
        assertEquals(value, objects.get(namespace, key));
        assertTrue(objects.delete(namespace, key));
        assertNull(objects.get(namespace, key));

        final Map<String, byte[]> batch = new HashMap<>(100);
        storeRandom(objects, namespace, batch, 100);

        for (final String k : batch.keySet()) {
            assertEquals(batch.get(k), objects.get(namespace, k));
        }

        objects.delete(namespace, batch.keySet());
        for (final String k : batch.keySet()) {
            assertNull(objects.get(namespace, k));
        }
        objects.drop(namespace);
    }

    @Test
    public void testStoreGetBatch() throws Exception {
        final Objects objects = getObjects();
        final String namespace = UUID.randomUUID().toString();
        objects.create(namespace);

        final Map<String, byte[]> empty = objects.get(namespace, Collections.emptyList());
        assertTrue(empty.isEmpty());

        final Map<String, byte[]> kvs = new HashMap<>();
        for (int i = 0; i < 100; ++i) {
            final String key = UUID.randomUUID().toString();
            final byte[] value = UUID.randomUUID().toString().getBytes();
            kvs.put(key, value);
        }

        for (final Map.Entry<String, byte[]> entry : kvs.entrySet()) {
            assertNull(objects.get(namespace, entry.getKey()));
        }
        objects.store(namespace, kvs);
        final Map<String, byte[]> results = objects.get(namespace, kvs.keySet());
        assertEquals(kvs.size(), results.size());
        for (final Map.Entry<String, byte[]> entry : kvs.entrySet()) {
            assertEquals(results.get(entry.getKey()), entry.getValue());
        }
        objects.delete(namespace, kvs.keySet());
        for (final Map.Entry<String, byte[]> entry : kvs.entrySet()) {
            assertNull(objects.get(namespace, entry.getKey()));
        }

        objects.drop(namespace);
    }

    @Test
    public void testStoreKeys() throws Exception {
        final Objects objects = getObjects();
        final String namespace = UUID.randomUUID().toString();
        objects.create(namespace);

        final Map<String, byte[]> empty = objects.get(namespace, Collections.emptyList());
        assertTrue(empty.isEmpty());

        final Map<String, byte[]> kvs = new HashMap<>();
        for (int i = 0; i < 100; ++i) {
            final String key = UUID.randomUUID().toString();
            final byte[] value = UUID.randomUUID().toString().getBytes();
            kvs.put(key, value);
        }

        for (final Map.Entry<String, byte[]> entry : kvs.entrySet()) {
            assertNull(objects.get(namespace, entry.getKey()));
        }
        objects.store(namespace, kvs);
        final int count = ThreadLocalRandom.current().nextInt(1, 99);
        final Collection<String> partialResults = objects.keys(namespace, 0, count);
        assertEquals(partialResults.size(), count);

        final Collection<String> results = objects.keys(namespace, 0, -1);
        assertEquals(kvs.size(), results.size());
        for (final Map.Entry<String, byte[]> entry : kvs.entrySet()) {
            assertTrue(results.contains(entry.getKey()));
        }
        objects.delete(namespace, kvs.keySet());
        for (final Map.Entry<String, byte[]> entry : kvs.entrySet()) {
            assertNull(objects.get(namespace, entry.getKey()));
        }

        objects.drop(namespace);
    }

    @Test
    public void testSize() throws Exception {
        final Objects objects = getObjects();
        final String namespace = UUID.randomUUID().toString();
        objects.create(namespace);

        // delete everything, should leave 0 objects
        objects.create(namespace);
        objects.drop(namespace);
        objects.create(namespace);

        assertEquals(objects.size(namespace), 0);

        // store and check size
        final Map<String, byte[]> batch = new HashMap<>();
        storeRandom(objects, namespace, batch, 1_000);
        assertEquals(objects.size(namespace), batch.size());

        // delete and check size
        int removed = 0;
        for (final String key : batch.keySet()) {
            if (removed >= batch.size() / 2) {
                break;
            }
            objects.delete(namespace, key);
            removed++;
        }
        assertEquals(objects.size(namespace), batch.size() - removed);

        // delete and check again
        objects.delete(namespace, batch.keySet());
        assertEquals(objects.size(namespace), 0);

        objects.drop(namespace);
    }

    private void storeRandom(final Objects objects,
                             final String namespace,
                             final Map<String, byte[]> map,
                             final int times) throws IOException {
        final double storeCount = Math.floor(times * getStoreMagnitude());
        for (int i = 0; i < storeCount; i++) {
            final String key = UUID.randomUUID().toString();
            final byte[] value = UUID.randomUUID().toString().getBytes();
            map.put(key, value);
            objects.store(namespace, key, value);
        }
    }

    private Objects getObjects() throws IOException {
        return getCantor().objects();
    }

}
