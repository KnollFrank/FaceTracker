package de.drowsydriveralarm.eventproducer;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import de.drowsydriveralarm.event.SlowEyelidClosureEvent;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

public class SlowEyelidClosureEventsProviderTest {

    private SlowEyelidClosureEventsProvider eventsProvider;

    @Before
    public void setup() {
        this.eventsProvider = new SlowEyelidClosureEventsProvider(new Duration(10));
    }

    @Test
    public void testEventCompletelyWithinTimewindow() {
        // Given
        final SlowEyelidClosureEvent event = new SlowEyelidClosureEvent(new Instant(0), new Duration(5));

        // When
        this.eventsProvider.recordSlowEyelidClosureEvent(event);
        List<SlowEyelidClosureEvent> recordedEventsPartlyWithinTimeWindow = this.eventsProvider.getRecordedEventsPartlyWithinTimeWindow(new Instant(5));

        // Then
        assertThat(recordedEventsPartlyWithinTimeWindow, contains(event));
        assertThat(this.eventsProvider.getEvents(), contains(event));
    }

    @Test
    public void testEventPartlyWithinTimewindow() {
        // Given
        final SlowEyelidClosureEvent event = new SlowEyelidClosureEvent(new Instant(0), new Duration(5));

        // When
        this.eventsProvider.recordSlowEyelidClosureEvent(event);
        List<SlowEyelidClosureEvent> recordedEventsPartlyWithinTimeWindow = this.eventsProvider.getRecordedEventsPartlyWithinTimeWindow(new Instant(12));

        // Then
        assertThat(recordedEventsPartlyWithinTimeWindow, contains(event));
        assertThat(this.eventsProvider.getEvents(), contains(event));
    }

    @Test
    public void testEventOutsideTimewindow() {
        // Given
        final SlowEyelidClosureEvent event = new SlowEyelidClosureEvent(new Instant(0), new Duration(5));

        // When
        this.eventsProvider.recordSlowEyelidClosureEvent(event);
        List<SlowEyelidClosureEvent> recordedEventsPartlyWithinTimeWindow = this.eventsProvider.getRecordedEventsPartlyWithinTimeWindow(new Instant(16));

        // Then
        assertThat(recordedEventsPartlyWithinTimeWindow, is(empty()));
        assertThat(this.eventsProvider.getEvents(), contains(event));
    }

    @Test
    public void testEventRemovedFromEvents() {
        // Given
        final SlowEyelidClosureEvent event1 = new SlowEyelidClosureEvent(new Instant(0), new Duration(5));
        final SlowEyelidClosureEvent event2 = new SlowEyelidClosureEvent(new Instant(11), new Duration(5));

        // When
        this.eventsProvider.recordSlowEyelidClosureEvent(event1);
        this.eventsProvider.recordSlowEyelidClosureEvent(event2);
        List<SlowEyelidClosureEvent> recordedEventsPartlyWithinTimeWindow = this.eventsProvider.getRecordedEventsPartlyWithinTimeWindow(new Instant(16));

        // Then
        assertThat(recordedEventsPartlyWithinTimeWindow, contains(event2));
        assertThat(this.eventsProvider.getEvents(), contains(event2));
    }
}
