/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jctools.queues.unpadded;

import static org.jctools.util.internal.UnsafeAccess.UNSAFE;
import static org.jctools.util.internal.UnsafeAccess.fieldOffset;
import static org.jctools.util.internal.UnsafeRefArrayAccess.*;
import org.jctools.queues.*;

/**
 * NOTE: This class was automatically generated by org.jctools.queues.unpadded.JavaParsingUnpaddedQueueGenerator
 * which can found in the jctools-build module. The original source file is SpmcArrayQueue.java.
 */
abstract class SpmcUnpaddedArrayQueueL1Pad<E> extends ConcurrentCircularUnpaddedArrayQueue<E> {

    SpmcUnpaddedArrayQueueL1Pad(int capacity) {
        super(capacity);
    }
}

/**
 * NOTE: This class was automatically generated by org.jctools.queues.unpadded.JavaParsingUnpaddedQueueGenerator
 * which can found in the jctools-build module. The original source file is SpmcArrayQueue.java.
 */
abstract class SpmcUnpaddedArrayQueueProducerIndexField<E> extends SpmcUnpaddedArrayQueueL1Pad<E> {

    protected final static long P_INDEX_OFFSET = fieldOffset(SpmcUnpaddedArrayQueueProducerIndexField.class, "producerIndex");

    private volatile long producerIndex;

    SpmcUnpaddedArrayQueueProducerIndexField(int capacity) {
        super(capacity);
    }

    @Override
    public final long lvProducerIndex() {
        return producerIndex;
    }

    final long lpProducerIndex() {
        return UNSAFE.getLong(this, P_INDEX_OFFSET);
    }

    final void soProducerIndex(long newValue) {
        UNSAFE.putOrderedLong(this, P_INDEX_OFFSET, newValue);
    }
}

/**
 * NOTE: This class was automatically generated by org.jctools.queues.unpadded.JavaParsingUnpaddedQueueGenerator
 * which can found in the jctools-build module. The original source file is SpmcArrayQueue.java.
 */
abstract class SpmcUnpaddedArrayQueueL2Pad<E> extends SpmcUnpaddedArrayQueueProducerIndexField<E> {

    SpmcUnpaddedArrayQueueL2Pad(int capacity) {
        super(capacity);
    }
}

/**
 * NOTE: This class was automatically generated by org.jctools.queues.unpadded.JavaParsingUnpaddedQueueGenerator
 * which can found in the jctools-build module. The original source file is SpmcArrayQueue.java.
 */
abstract class SpmcUnpaddedArrayQueueConsumerIndexField<E> extends SpmcUnpaddedArrayQueueL2Pad<E> {

    protected final static long C_INDEX_OFFSET = fieldOffset(SpmcUnpaddedArrayQueueConsumerIndexField.class, "consumerIndex");

    private volatile long consumerIndex;

    SpmcUnpaddedArrayQueueConsumerIndexField(int capacity) {
        super(capacity);
    }

    @Override
    public final long lvConsumerIndex() {
        return consumerIndex;
    }

    final boolean casConsumerIndex(long expect, long newValue) {
        return UNSAFE.compareAndSwapLong(this, C_INDEX_OFFSET, expect, newValue);
    }
}

/**
 * NOTE: This class was automatically generated by org.jctools.queues.unpadded.JavaParsingUnpaddedQueueGenerator
 * which can found in the jctools-build module. The original source file is SpmcArrayQueue.java.
 */
abstract class SpmcUnpaddedArrayQueueMidPad<E> extends SpmcUnpaddedArrayQueueConsumerIndexField<E> {

    SpmcUnpaddedArrayQueueMidPad(int capacity) {
        super(capacity);
    }
}

/**
 * NOTE: This class was automatically generated by org.jctools.queues.unpadded.JavaParsingUnpaddedQueueGenerator
 * which can found in the jctools-build module. The original source file is SpmcArrayQueue.java.
 */
abstract class SpmcUnpaddedArrayQueueProducerIndexCacheField<E> extends SpmcUnpaddedArrayQueueMidPad<E> {

    // This is separated from the consumerIndex which will be highly contended in the hope that this value spends most
    // of it's time in a cache line that is Shared(and rarely invalidated)
    private volatile long producerIndexCache;

    SpmcUnpaddedArrayQueueProducerIndexCacheField(int capacity) {
        super(capacity);
    }

    protected final long lvProducerIndexCache() {
        return producerIndexCache;
    }

    protected final void svProducerIndexCache(long newValue) {
        producerIndexCache = newValue;
    }
}

/**
 * NOTE: This class was automatically generated by org.jctools.queues.unpadded.JavaParsingUnpaddedQueueGenerator
 * which can found in the jctools-build module. The original source file is SpmcArrayQueue.java.
 */
abstract class SpmcUnpaddedArrayQueueL3Pad<E> extends SpmcUnpaddedArrayQueueProducerIndexCacheField<E> {

    SpmcUnpaddedArrayQueueL3Pad(int capacity) {
        super(capacity);
    }
}

/**
 * NOTE: This class was automatically generated by org.jctools.queues.unpadded.JavaParsingUnpaddedQueueGenerator
 * which can found in the jctools-build module. The original source file is SpmcArrayQueue.java.
 */
public class SpmcUnpaddedArrayQueue<E> extends SpmcUnpaddedArrayQueueL3Pad<E> {

    public SpmcUnpaddedArrayQueue(final int capacity) {
        super(capacity);
    }

    @Override
    public boolean offer(final E e) {
        if (null == e) {
            throw new NullPointerException();
        }
        final E[] buffer = this.buffer;
        final long mask = this.mask;
        final long currProducerIndex = lvProducerIndex();
        final long offset = calcCircularRefElementOffset(currProducerIndex, mask);
        if (null != lvRefElement(buffer, offset)) {
            long size = currProducerIndex - lvConsumerIndex();
            if (size > mask) {
                return false;
            } else {
                // Bubble: This can happen because `poll` moves index before placing element.
                // spin wait for slot to clear, buggers wait freedom
                while (null != lvRefElement(buffer, offset)) {
                    // BURN
                }
            }
        }
        soRefElement(buffer, offset, e);
        // single producer, so store ordered is valid. It is also required to correctly publish the element
        // and for the consumers to pick up the tail value.
        soProducerIndex(currProducerIndex + 1);
        return true;
    }

    @Override
    public E poll() {
        long currentConsumerIndex;
        long currProducerIndexCache = lvProducerIndexCache();
        do {
            currentConsumerIndex = lvConsumerIndex();
            if (currentConsumerIndex >= currProducerIndexCache) {
                long currProducerIndex = lvProducerIndex();
                if (currentConsumerIndex >= currProducerIndex) {
                    return null;
                } else {
                    currProducerIndexCache = currProducerIndex;
                    svProducerIndexCache(currProducerIndex);
                }
            }
        } while (!casConsumerIndex(currentConsumerIndex, currentConsumerIndex + 1));
        // consumers are gated on latest visible tail, and so can't see a null value in the queue or overtake
        // and wrap to hit same location.
        return removeElement(buffer, currentConsumerIndex, mask);
    }

    private E removeElement(final E[] buffer, long index, final long mask) {
        final long offset = calcCircularRefElementOffset(index, mask);
        // load plain, element happens before it's index becomes visible
        final E e = lpRefElement(buffer, offset);
        // store ordered, make sure nulling out is visible. Producer is waiting for this value.
        soRefElement(buffer, offset, null);
        return e;
    }

    @Override
    public E peek() {
        final E[] buffer = this.buffer;
        final long mask = this.mask;
        long currProducerIndexCache = lvProducerIndexCache();
        long currentConsumerIndex;
        long nextConsumerIndex = lvConsumerIndex();
        E e;
        do {
            currentConsumerIndex = nextConsumerIndex;
            if (currentConsumerIndex >= currProducerIndexCache) {
                long currProducerIndex = lvProducerIndex();
                if (currentConsumerIndex >= currProducerIndex) {
                    return null;
                } else {
                    currProducerIndexCache = currProducerIndex;
                    svProducerIndexCache(currProducerIndex);
                }
            }
            e = lvRefElement(buffer, calcCircularRefElementOffset(currentConsumerIndex, mask));
            // sandwich the element load between 2 consumer index loads
            nextConsumerIndex = lvConsumerIndex();
        } while (null == e || nextConsumerIndex != currentConsumerIndex);
        return e;
    }

    @Override
    public boolean relaxedOffer(E e) {
        if (null == e) {
            throw new NullPointerException("Null is not a valid element");
        }
        final E[] buffer = this.buffer;
        final long mask = this.mask;
        final long producerIndex = lpProducerIndex();
        final long offset = calcCircularRefElementOffset(producerIndex, mask);
        if (null != lvRefElement(buffer, offset)) {
            return false;
        }
        soRefElement(buffer, offset, e);
        // single producer, so store ordered is valid. It is also required to correctly publish the element
        // and for the consumers to pick up the tail value.
        soProducerIndex(producerIndex + 1);
        return true;
    }

    @Override
    public E relaxedPoll() {
        return poll();
    }

    @Override
    public E relaxedPeek() {
        final E[] buffer = this.buffer;
        final long mask = this.mask;
        long currentConsumerIndex;
        long nextConsumerIndex = lvConsumerIndex();
        E e;
        do {
            currentConsumerIndex = nextConsumerIndex;
            e = lvRefElement(buffer, calcCircularRefElementOffset(currentConsumerIndex, mask));
            // sandwich the element load between 2 consumer index loads
            nextConsumerIndex = lvConsumerIndex();
        } while (nextConsumerIndex != currentConsumerIndex);
        return e;
    }

    @Override
    public int drain(final Consumer<E> c, final int limit) {
        if (null == c)
            throw new IllegalArgumentException("c is null");
        if (limit < 0)
            throw new IllegalArgumentException("limit is negative: " + limit);
        if (limit == 0)
            return 0;
        final E[] buffer = this.buffer;
        final long mask = this.mask;
        long currProducerIndexCache = lvProducerIndexCache();
        int adjustedLimit = 0;
        long currentConsumerIndex;
        do {
            currentConsumerIndex = lvConsumerIndex();
            // is there any space in the queue?
            if (currentConsumerIndex >= currProducerIndexCache) {
                long currProducerIndex = lvProducerIndex();
                if (currentConsumerIndex >= currProducerIndex) {
                    return 0;
                } else {
                    currProducerIndexCache = currProducerIndex;
                    svProducerIndexCache(currProducerIndex);
                }
            }
            // try and claim up to 'limit' elements in one go
            int remaining = (int) (currProducerIndexCache - currentConsumerIndex);
            adjustedLimit = Math.min(remaining, limit);
        } while (!casConsumerIndex(currentConsumerIndex, currentConsumerIndex + adjustedLimit));
        for (int i = 0; i < adjustedLimit; i++) {
            c.accept(removeElement(buffer, currentConsumerIndex + i, mask));
        }
        return adjustedLimit;
    }

    @Override
    public int fill(final Supplier<E> s, final int limit) {
        if (null == s)
            throw new IllegalArgumentException("supplier is null");
        if (limit < 0)
            throw new IllegalArgumentException("limit is negative:" + limit);
        if (limit == 0)
            return 0;
        final E[] buffer = this.buffer;
        final long mask = this.mask;
        long producerIndex = this.lpProducerIndex();
        for (int i = 0; i < limit; i++) {
            final long offset = calcCircularRefElementOffset(producerIndex, mask);
            if (null != lvRefElement(buffer, offset)) {
                return i;
            }
            producerIndex++;
            soRefElement(buffer, offset, s.get());
            // ordered store -> atomic and ordered for size()
            soProducerIndex(producerIndex);
        }
        return limit;
    }

    @Override
    public int drain(final Consumer<E> c) {
        return MessagePassingQueueUtil.drain(this, c);
    }

    @Override
    public int fill(final Supplier<E> s) {
        return fill(s, capacity());
    }

    @Override
    public void drain(final Consumer<E> c, final WaitStrategy w, final ExitCondition exit) {
        MessagePassingQueueUtil.drain(this, c, w, exit);
    }

    @Override
    public void fill(final Supplier<E> s, final WaitStrategy w, final ExitCondition e) {
        MessagePassingQueueUtil.fill(this, s, w, e);
    }
}
