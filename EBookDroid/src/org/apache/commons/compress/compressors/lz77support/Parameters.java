/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.compressors.lz77support;

/**
 * Parameters of the {@link LZ77Compressor compressor}.
 */
public final class Parameters {
    /**
     * The hard-coded absolute minimal length of a back-reference.
     */
    public static final int TRUE_MIN_BACK_REFERENCE_LENGTH = LZ77Compressor.NUMBER_OF_BYTES_IN_HASH;

    /**
     * Initializes the builder for the compressor's parameters with a
     * <code>minBackReferenceLength</code> of 3 and <code>max*Length</code>
     * equal to <code>windowSize - 1</code>.
     *
     * <p>It is recommended to not use this method directly but rather
     * tune a pre-configured builder created by a format specific
     * factory like {@link
     * org.apache.commons.compress.compressors.snappy.SnappyCompressorOutputStream#createParameterBuilder}.</p>
     *
     * @param windowSize the size of the sliding window - this
     * determines the maximum offset a back-reference can take. Must
     * be a power of two.
     * @throws IllegalArgumentException if windowSize is not a power of two.
     * @return a builder configured for the given window size
     */
    public static Builder builder(int windowSize) {
        return new Builder(windowSize);
    }

    /**
     * Builder for {@link Parameters} instances.
     */
    public static class Builder {
        private final int windowSize;
        private int minBackReferenceLength, maxBackReferenceLength, maxOffset, maxLiteralLength;
        private Integer niceBackReferenceLength, maxCandidates, lazyThreshold;
        private Boolean lazyMatches;

        private Builder(int windowSize) {
            if (windowSize < 2 || !isPowerOfTwo(windowSize)) {
                throw new IllegalArgumentException("windowSize must be a power of two");
            }
            this.windowSize = windowSize;
            minBackReferenceLength = TRUE_MIN_BACK_REFERENCE_LENGTH;
            maxBackReferenceLength = windowSize - 1;
            maxOffset = windowSize - 1;
            maxLiteralLength = windowSize;
        }

        /**
         * Sets the mininal length of a back-reference.
         *
         * <p>Ensures <code>maxBackReferenceLength</code> is not
         * smaller than <code>minBackReferenceLength</code>.
         *
         * <p>It is recommended to not use this method directly but
         * rather tune a pre-configured builder created by a format
         * specific factory like {@link
         * org.apache.commons.compress.compressors.snappy.SnappyCompressorOutputStream#createParameterBuilder}.</p>
         *
         * @param minBackReferenceLength the minimal length of a back-reference found. A
         * true minimum of 3 is hard-coded inside of this implemention
         * but bigger lengths can be configured.
         * @throws IllegalArgumentException if <code>windowSize</code>
         * is smaller than <code>minBackReferenceLength</code>.
         * @return the builder
         */
        public Builder withMinBackReferenceLength(int minBackReferenceLength) {
            this.minBackReferenceLength = Math.max(TRUE_MIN_BACK_REFERENCE_LENGTH, minBackReferenceLength);
            if (windowSize < this.minBackReferenceLength) {
                throw new IllegalArgumentException("minBackReferenceLength can't be bigger than windowSize");
            }
            if (maxBackReferenceLength < this.minBackReferenceLength) {
                maxBackReferenceLength = this.minBackReferenceLength;
            }
            return this;
        }

        /**
         * Sets the maximal length of a back-reference.
         *
         * <p>It is recommended to not use this method directly but
         * rather tune a pre-configured builder created by a format
         * specific factory like {@link
         * org.apache.commons.compress.compressors.snappy.SnappyCompressorOutputStream#createParameterBuilder}.</p>
         *
         * @param maxBackReferenceLength maximal length of a
         * back-reference found. A value smaller than
         * <code>minBackReferenceLength</code> is interpreted as
         * <code>minBackReferenceLength</code>. <code>maxBackReferenceLength</code>
         * is capped at <code>windowSize - 1</code>.
         * @return the builder
         */
        public Builder withMaxBackReferenceLength(int maxBackReferenceLength) {
            this.maxBackReferenceLength = maxBackReferenceLength < minBackReferenceLength ? minBackReferenceLength
                : Math.min(maxBackReferenceLength, windowSize - 1);
            return this;
        }

        /**
         * Sets the maximal offset of a back-reference.
         *
         * <p>It is recommended to not use this method directly but
         * rather tune a pre-configured builder created by a format
         * specific factory like {@link
         * org.apache.commons.compress.compressors.snappy.SnappyCompressorOutputStream#createParameterBuilder}.</p>
         *
         * @param maxOffset maximal offset of a back-reference. A
         * non-positive value as well as values bigger than
         * <code>windowSize - 1</code> are interpreted as <code>windowSize
         * - 1</code>.
         * @return the builder
         */
        public Builder withMaxOffset(int maxOffset) {
            this.maxOffset = maxOffset < 1 ? windowSize - 1 : Math.min(maxOffset, windowSize - 1);
            return this;
        }

        /**
         * Sets the maximal length of a literal block.
         *
         * <p>It is recommended to not use this method directly but
         * rather tune a pre-configured builder created by a format
         * specific factory like {@link
         * org.apache.commons.compress.compressors.snappy.SnappyCompressorOutputStream#createParameterBuilder}.</p>
         *
         * @param maxLiteralLength maximal length of a literal
         * block. Negative numbers and 0 as well as values bigger than
         * <code>windowSize</code> are interpreted as
         * <code>windowSize</code>.
         * @return the builder
         */
        public Builder withMaxLiteralLength(int maxLiteralLength) {
            this.maxLiteralLength = maxLiteralLength < 1 ? windowSize
                : Math.min(maxLiteralLength, windowSize);
            return this;
        }

        /**
         * Sets the "nice length" of a back-reference.
         *
         * <p>When a back-references if this size has been found, stop searching for longer back-references.</p>
         *
         * <p>This settings can be used to tune the tradeoff between compression speed and compression ratio.</p>
         * @param niceLen the "nice length" of a back-reference
         * @return the builder
         */
        public Builder withNiceBackReferenceLength(int niceLen) {
            niceBackReferenceLength = niceLen;
            return this;
        }

        /**
         * Sets the maximum number of back-reference candidates that should be consulted.
         *
         * <p>This settings can be used to tune the tradeoff between compression speed and compression ratio.</p>
         * @param maxCandidates maximum number of back-reference candidates
         * @return the builder
         */
        public Builder withMaxNumberOfCandidates(int maxCandidates) {
            this.maxCandidates = maxCandidates;
            return this;
        }

        /**
         * Sets whether lazy matching should be performed.
         *
         * <p>Lazy matching means that after a back-reference for a certain position has been found the compressor will
         * try to find a longer match for the next position.</p>
         *
         * <p>Lazy matching is enabled by default and disabled when tuning for speed.</p>
         * @param lazy whether lazy matching should be performed
         * @return the builder
         */
        public Builder withLazyMatching(boolean lazy) {
            lazyMatches = lazy;
            return this;
        }

        /**
         * Sets the threshold for lazy matching.
         *
         * <p>Even if lazy matching is enabled it will not be performed if the length of the back-reference found for
         * the current position is longer than this value.</p>
         * @param threshold the threshold for lazy matching
         * @return the builder
         */
        public Builder withLazyThreshold(int threshold) {
            lazyThreshold = threshold;
            return this;
        }

        /**
         * Changes the default setting for "nice back-reference length" and "maximum number of candidates" for improved
         * compression speed at the cost of compression ratio.
         *
         * <p>Use this method after configuring "maximum back-reference length".</p>
         * @return the builder
         */
        public Builder tunedForSpeed() {
            niceBackReferenceLength = Math.max(minBackReferenceLength, maxBackReferenceLength / 8);
            maxCandidates = Math.max(32, windowSize / 1024);
            lazyMatches = false;
            lazyThreshold = minBackReferenceLength;
            return this;
        }

        /**
         * Changes the default setting for "nice back-reference length" and "maximum number of candidates" for improved
         * compression ratio at the cost of compression speed.
         *
         * <p>Use this method after configuring "maximum back-reference length".</p>
         * @return the builder
         */
        public Builder tunedForCompressionRatio() {
            niceBackReferenceLength = lazyThreshold = maxBackReferenceLength;
            maxCandidates = Math.max(32, windowSize / 16);
            lazyMatches = true;
            return this;
        }

        /**
         * Creates the {@link Parameters} instance.
         * @return the configured {@link Parameters} instance.
         */
        public Parameters build() {
            // default settings tuned for a compromise of good compression and acceptable speed
            int niceLen = niceBackReferenceLength != null ? niceBackReferenceLength
                : Math.max(minBackReferenceLength, maxBackReferenceLength / 2);
            int candidates = maxCandidates != null ? maxCandidates : Math.max(256, windowSize / 128);
            boolean lazy = lazyMatches == null || lazyMatches;
            int threshold = lazy ? (lazyThreshold != null ? lazyThreshold : niceLen) : minBackReferenceLength;

            return new Parameters(windowSize, minBackReferenceLength, maxBackReferenceLength,
                maxOffset, maxLiteralLength, niceLen, candidates, lazy, threshold);
        }
    }

    private final int windowSize, minBackReferenceLength, maxBackReferenceLength, maxOffset, maxLiteralLength,
        niceBackReferenceLength, maxCandidates, lazyThreshold;
    private final boolean lazyMatching;

    private Parameters(int windowSize, int minBackReferenceLength, int maxBackReferenceLength, int maxOffset,
            int maxLiteralLength, int niceBackReferenceLength, int maxCandidates, boolean lazyMatching,
            int lazyThreshold) {
        this.windowSize = windowSize;
        this.minBackReferenceLength = minBackReferenceLength;
        this.maxBackReferenceLength = maxBackReferenceLength;
        this.maxOffset = maxOffset;
        this.maxLiteralLength = maxLiteralLength;
        this.niceBackReferenceLength = niceBackReferenceLength;
        this.maxCandidates = maxCandidates;
        this.lazyMatching = lazyMatching;
        this.lazyThreshold = lazyThreshold;
    }

    /**
     * Gets the size of the sliding window - this determines the
     * maximum offset a back-reference can take.
     * @return the size of the sliding window
     */
    public int getWindowSize() {
        return windowSize;
    }
    /**
     * Gets the minimal length of a back-reference found.
     * @return the minimal length of a back-reference found
     */
    public int getMinBackReferenceLength() {
        return minBackReferenceLength;
    }
    /**
     * Gets the maximal length of a back-reference found.
     * @return the maximal length of a back-reference found
     */
    public int getMaxBackReferenceLength() {
        return maxBackReferenceLength;
    }
    /**
     * Gets the maximal offset of a back-reference found.
     * @return the maximal offset of a back-reference found
     */
    public int getMaxOffset() {
        return maxOffset;
    }
    /**
     * Gets the maximal length of a literal block.
     * @return the maximal length of a literal block
     */
    public int getMaxLiteralLength() {
        return maxLiteralLength;
    }

    /**
     * Gets the length of a back-reference that is considered nice enough to stop searching for longer ones.
     * @return the length of a back-reference that is considered nice enough to stop searching
     */
    public int getNiceBackReferenceLength() {
        return niceBackReferenceLength;
    }

    /**
     * Gets the maximum number of back-reference candidates to consider.
     * @return the maximum number of back-reference candidates to consider
     */
    public int getMaxCandidates() {
        return maxCandidates;
    }

    /**
     * Gets whether to perform lazy matching.
     * @return whether to perform lazy matching
     */
    public boolean getLazyMatching() {
        return lazyMatching;
    }

    /**
     * Gets the threshold for lazy matching.
     * @return the threshold for lazy matching
     */
    public int getLazyMatchingThreshold() {
        return lazyThreshold;
    }

    private static final boolean isPowerOfTwo(int x) {
        // pre-condition: x > 0
        return (x & (x - 1)) == 0;
    }
}
