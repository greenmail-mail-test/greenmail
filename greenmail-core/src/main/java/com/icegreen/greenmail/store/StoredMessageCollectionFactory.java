/* -------------------------------------------------------------------
* This software is released under the Apache license 2.0
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.store;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
public enum StoredMessageCollectionFactory {
    LIST_BASED_FACTORY {
        @Override
        StoredMessageCollection createCollection() {
            return new ListBasedStoredMessageCollection();
        }
    },
    MAP_BASED_FACTORY {
        private static final int DEFAULT_MAXIMUM_MAP_SIZE = 5000;

        @Override
        protected Set<String> getAcceptedConfigurationKeys() {
            final Set acceptedKeys = super.getAcceptedConfigurationKeys();
            acceptedKeys.add(Constants.CONFIGURATION_KEY_MAXIMUM_MAP_SIZE);
            return acceptedKeys;
        }

        @Override
        StoredMessageCollection createCollection() {
            final int maximumMapSize = hasConfigurationFor(Constants.CONFIGURATION_KEY_MAXIMUM_MAP_SIZE) ?
                    (Integer) getConfiguredValue
                            (Constants.CONFIGURATION_KEY_MAXIMUM_MAP_SIZE) : DEFAULT_MAXIMUM_MAP_SIZE;
            return new MapBasedStoredMessageCollection(maximumMapSize);
        }
    };

    private final Map<String, Object> configuration;

    StoredMessageCollectionFactory() {
        configuration = new ConcurrentHashMap<String, Object>();
    }

    abstract StoredMessageCollection createCollection();

    protected Set<String> getAcceptedConfigurationKeys() {
        return new HashSet<String>();
    }

    protected final Object getConfiguredValue(final String configurationKey) {
        return configuration.get(configurationKey);
    }

    protected final boolean hasConfigurationFor(final String configurationKey) {
        return configuration.containsKey(configurationKey);
    }

    public final StoredMessageCollectionFactory withConfigurationValue(final String key, final Object value) {
        final Set<String> acceptedKeys = getAcceptedConfigurationKeys();
        if (acceptedKeys.contains(key)) {
            configuration.put(key, value);
            return this;
        }

        throw new IllegalArgumentException(String.format("Configuration key '%s' unknown. Accepted keys are:%s", key,
                acceptedKeys));
    }

    public static class Constants {
        public static final String CONFIGURATION_KEY_MAXIMUM_MAP_SIZE = "CONFIGURATION_KEY_MAXIMUM_MAP_SIZE";
    }
}
