/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.storage;

import java.util.Arrays;
import org.opengis.geometry.Envelope;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.logging.WarningListeners;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.metadata.spatial.DimensionNameType;


/**
 * Base class for implementations of {@link GridCoverageResource}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
public abstract class AbstractGridResource extends AbstractResource implements GridCoverageResource {
    /**
     * Creates a new resource.
     *
     * @param listeners  the set of registered warning listeners for the data store, or {@code null} if none.
     */
    protected AbstractGridResource(final WarningListeners<DataStore> listeners) {
        super(listeners);
    }

    /**
     * Creates a new resource with the same warning listeners than the given resource,
     * or {@code null} if the listeners are unknown.
     *
     * @param resource  the resources from which to get the listeners, or {@code null} if none.
     */
    protected AbstractGridResource(final Resource resource) {
        super(resource);
    }

    /**
     * Returns the grid geometry envelope, or {@code null} if unknown.
     * This implementation fetches the envelope from the grid geometry instead than from metadata.
     *
     * @return the grid geometry envelope, or {@code null}.
     * @throws DataStoreException if an error occurred while computing the grid geometry.
     */
    @Override
    public Envelope getEnvelope() throws DataStoreException {
        final GridGeometry gg = getGridGeometry();
        if (gg != null && gg.isDefined(GridGeometry.ENVELOPE)) {
            return gg.getEnvelope();
        }
        return null;
    }

    /**
     * Invoked the first time that {@link #getMetadata()} is invoked. The default implementation populates
     * metadata based on information provided by {@link #getIdentifier()} and {@link #getGridGeometry()}.
     * Subclasses should override if they can provide more information.
     *
     * @param  metadata  the builder where to set metadata properties.
     * @throws DataStoreException if an error occurred while reading metadata from the data store.
     */
    @Override
    protected void createMetadata(final MetadataBuilder metadata) throws DataStoreException {
        super.createMetadata(metadata);
        metadata.addSpatialRepresentation(null, getGridGeometry(), false);
        for (final SampleDimension band : getSampleDimensions()) {
            metadata.addNewBand(band);
        }
    }

    /**
     * Validate the {@code range} argument given to {@link #read(GridGeometry, int...)}.
     * This method verifies that all indices are between 0 and {@code numSampleDimensions}
     * and that there is no duplicated index.
     *
     * @param  numSampleDimensions  number of sample dimensions.
     * @param  range  the {@code range} argument given by the user. May be null or empty.
     * @return the {@code range} argument encapsulated with a set of convenience tools.
     * @throws IllegalArgumentException if a range index is invalid.
     */
    protected final RangeArgument validateRangeArgument(final int numSampleDimensions, final int[] range) {
        ArgumentChecks.ensureStrictlyPositive("numSampleDimensions", numSampleDimensions);
        final long[] packed;
        if (range == null || range.length == 0) {
            packed = new long[numSampleDimensions];
            for (int i=1; i<numSampleDimensions; i++) {
                packed[i] = (((long) i) << Integer.SIZE) | i;
            }
            return new RangeArgument(packed);
        } else {
            packed = new long[range.length];
            for (int i=0; i<range.length; i++) {
                final int r = range[i];
                if (r < 0 || r >= numSampleDimensions) {
                    throw new IllegalArgumentException(Resources.forLocale(getLocale()).getString(
                            Resources.Keys.InvalidSampleDimensionIndex_2, r, numSampleDimensions - 1));
                }
                packed[i] = (((long) r) << Integer.SIZE) | i;
            }
            Arrays.sort(packed);
            int previous = -1;
            for (int i=0; i<packed.length; i++) {
                final int r = (int) (packed[i] >>> Integer.SIZE);
                if (r == previous) {
                    throw new IllegalArgumentException(Resources.forLocale(getLocale()).getString(
                            Resources.Keys.DuplicatedSampleDimensionIndex_1, r));
                }
                previous = r;
            }
        }
        return new RangeArgument(packed);
    }

    /**
     * The user-provided {@code range} argument, together with a set of convenience tools.
     */
    protected static final class RangeArgument {
        /**
         * Name of the extent dimension for bands.
         */
        private static final DimensionNameType BAND = DimensionNameType.valueOf("BAND");

        /**
         * The user-specified range indices in high bits, together with indices order in the low bits.
         * This array is sorted.
         */
        private final long[] packed;

        /**
         * A builder for sample dimensions, created when first needed.
         */
        private SampleDimension.Builder builder;

        /**
         * Encapsulates the given {@code range} argument packed in high bits.
         */
        RangeArgument(final long[] packed) {
            this.packed = packed;
        }

        /**
         * Returns the number of sample dimensions. This is the length of the range array supplied by user.
         *
         * @return the number of sample dimensions.
         */
        public int getNumBands() {
            return packed.length;
        }

        /**
         * Returns the i<sup>th</sup> index of the band to read from the resource.
         * Indices are returned in strictly increasing order.
         *
         * @param  i  index of the range index to get, from 0 inclusive to {@link #getNumBands()} exclusive.
         * @return index of the i<sup>th</sup> band to read from the resource.
         */
        public int getSourceIndex(final int i) {
            return (int) (packed[i] >>> Integer.SIZE);
        }

        /**
         * Returns the i<sup>th</sup> band position. This is the index in the user-supplied {@code range} array
         * where was specified the {@code getBandIndex(i)} value.
         *
         * @param  i  index of the range index to get, from 0 inclusive to {@link #getNumBands()} exclusive.
         * @return index in user-supplied {@code range} array where was specified the {@code getBandIndex(i)} value.
         */
        public int getTargetIndex(final int i) {
            return (int) packed[i];
        }

        /**
         * Returns the value of the first index specified by the user. This is not necessarily equal to
         * {@code getBandIndex(0)} if the user specified bands out of order.
         *
         * @return index of the first value in the user-specified {@code range} array.
         */
        public int first() {
            for (final long p : packed) {
                if (((int) p) == 0) {
                    return (int) (p >>> Integer.SIZE);
                }
            }
            throw new IllegalStateException();              // Should never happen.
        }

        /**
         * Returns the given extent with a new dimension added for the bands. The extent in the new dimension
         * will range from the minimum {@code range} value to the maximum {@code range} value inclusive.
         *
         * @param  areaOfInterest  the extent to which to add a new dimension for bands.
         * @param  bandDimension   index of the band dimension.
         * @return a new extent with the same value than the given extent plus one dimension for bands.
         */
        public GridExtent insertBandDimension(final GridExtent areaOfInterest, final int bandDimension) {
            return areaOfInterest.insert(bandDimension, BAND, getSourceIndex(0), getSourceIndex(packed.length - 1), true);
        }

        /**
         * Returns a builder for sample dimensions. This method recycles the same builder on every calls.
         * If the builder has been returned by a previous call to this method,
         * then it is {@linkplain SampleDimension.Builder#clear() cleared} before to be returned again.
         *
         * @return a recycled builder for sample dimensions.
         */
        public SampleDimension.Builder builder() {
            if (builder == null) {
                builder = new SampleDimension.Builder();
            } else {
                builder.clear();
            }
            return builder;
        }
    }
}
