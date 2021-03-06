package de.drowsydriveralarm.eventproducer;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;
import com.google.common.eventbus.EventBus;

import org.hamcrest.Matchers;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.drowsydriveralarm.EventListener;
import de.drowsydriveralarm.SystemClock;
import de.drowsydriveralarm.event.Event;
import de.drowsydriveralarm.event.EyesClosedEvent;
import de.drowsydriveralarm.event.EyesOpenedEvent;
import de.drowsydriveralarm.event.NormalEyeBlinkEvent;
import de.drowsydriveralarm.event.SlowEyelidClosureEvent;

import static de.drowsydriveralarm.eventproducer.VisionHelper.createFace;
import static de.drowsydriveralarm.eventproducer.VisionHelper.createFaceWithEyesClosed;
import static de.drowsydriveralarm.eventproducer.VisionHelper.createFaceWithEyesOpened;
import static de.drowsydriveralarm.eventproducer.VisionHelper.createFaceWithLandmarks;
import static de.drowsydriveralarm.eventproducer.VisionHelper.createLandmark;
import static de.drowsydriveralarm.eventproducer.VisionHelper.getFaceDetections;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsNot.not;

public class EventProducingGraphicFaceTrackerTest {

    private EventListener eventListener;
    private Tracker<Face> tracker;

    @Before
    public void setup() {
        // Given
        this.eventListener = new EventListener();
        final EventBus eventBus = new EventBus();
        eventBus.register(this.eventListener);
        final IDrowsyEventDetectorConfig config = new TestingDrowsyEventDetectorConfig(SharedPreferencesTestFactory.createSharedPreferences());
        final EventSubscriberProvider eventSubscriberProvider = new EventSubscriberProvider(eventBus, config);
        DrowsyEventDetector.registerEventSubscribersOnEventBus(eventSubscriberProvider.getEventSubscribers(), eventBus);

        this.tracker =
                new EventProducingGraphicFaceTracker(
                        eventBus,
                        new DrowsyEventProducer(
                                config.getConfig(),
                                eventBus,
                                eventSubscriberProvider.getSlowEyelidClosureEventsProvider()),
                        new SystemClock());
    }

    @Test
    public void shouldCreateEyesClosedEvent() {
        this.shouldCreateEvent(0.4f, 0.4f, new EyesClosedEvent(new Instant(100)));
    }

    @Test
    public void shouldCreateEyesClosedEvent2() {
        this.shouldCreateEvent(0.3f, 0.4f, new EyesClosedEvent(new Instant(101)));
    }

    private void shouldCreateEvent(final float isLeftEyeOpenProbability, final float isRightEyeOpenProbability, final Event event) {
        // When
        this.tracker.onUpdate(getFaceDetections(event.getInstant()), createFace(isLeftEyeOpenProbability, isRightEyeOpenProbability));

        // Then
        assertThat(this.eventListener.getEvents(), hasItem(event));
    }

    @Test
    public void shouldNotCreateEyesClosedEventOnUNCOMPUTED_PROBABILITIES() {
        // When
        this.tracker.onUpdate(getFaceDetections(new Instant(100)), createFace(Face.UNCOMPUTED_PROBABILITY, Face.UNCOMPUTED_PROBABILITY));

        // Then
        assertThat(this.eventListener.getEvents(), not(hasItem(Matchers.<Event>instanceOf(EyesClosedEvent.class))));
    }

    @Test
    public void shouldNotCreateEyesOpenedEventOnUNCOMPUTED_PROBABILITIES() {
        // When
        this.tracker.onUpdate(getFaceDetections(new Instant(100)), createFace(Face.UNCOMPUTED_PROBABILITY, Face.UNCOMPUTED_PROBABILITY));

        // Then
        assertThat(this.eventListener.getEvents(), not(hasItem(Matchers.<Event>instanceOf(EyesOpenedEvent.class))));
    }

    @Test
    public void shouldCreateEyesOpenedEvent() {
        this.shouldCreateEvent(0.8f, 0.8f, new EyesOpenedEvent(new Instant(123)));
    }

    @Test
    public void shouldCreateEyesOpenedEvent2() {
        this.shouldCreateEvent(0.8f, 0.9f, new EyesOpenedEvent(new Instant(1234)));
    }

    @Test
    public void shouldCreateNormalEyeBlink() {
        // When
        this.tracker.onUpdate(getFaceDetections(new Instant(0)), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(new Instant(499)), createFaceWithEyesOpened());

        // Then
        assertThat(this.eventListener.getEvents(), hasItem(new NormalEyeBlinkEvent(new Instant(0), new Duration(499))));
    }

    @Test
    public void shouldCreateSlowEyelidClosureEvent() {
        // When
        this.tracker.onUpdate(getFaceDetections(new Instant(0)), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(new Instant(501)), createFaceWithEyesOpened());

        // Then
        assertThat(this.eventListener.getEvents(), hasItem(new SlowEyelidClosureEvent(new Instant(0), new Duration(501))));
    }

    @Test
    public void shouldCreateASingleEyesOpenedEventForIntermediateIndefiniteEyesState() {
        // When
        this.tracker.onUpdate(getFaceDetections(new Instant(0)), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(new Instant(1)), createFaceWithEyesOpened());
        this.tracker.onUpdate(getFaceDetections(new Instant(2)), this.createFaceWithLeftEyeOpenRightEyeClosed());
        this.tracker.onUpdate(getFaceDetections(new Instant(3)), createFaceWithEyesOpened());

        // Then
        assertThat(this.eventListener.filterEventsBy(EyesOpenedEvent.class), contains(new EyesOpenedEvent(new Instant(1))));
    }

    @Test
    public void shouldCreateASingleEyesClosedEventForIntermediateIndefiniteEyesState() {
        // When
        this.tracker.onUpdate(getFaceDetections(new Instant(0)), createFaceWithEyesOpened());
        this.tracker.onUpdate(getFaceDetections(new Instant(1)), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(new Instant(2)), this.createFaceWithLeftEyeOpenRightEyeClosed());
        this.tracker.onUpdate(getFaceDetections(new Instant(3)), createFaceWithEyesClosed());

        // Then
        assertThat(this.eventListener.filterEventsBy(EyesClosedEvent.class), contains(new EyesClosedEvent(new Instant(1))));
    }

    @Test
    public void shouldCreateASingleEyesClosedEvent() {
        // When
        this.tracker.onUpdate(getFaceDetections(new Instant(100)), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(new Instant(101)), createFaceWithEyesClosed());

        // Then
        assertThat(this.eventListener.filterEventsBy(EyesClosedEvent.class), contains(new EyesClosedEvent(new Instant(100))));
    }

    @Test
    public void shouldCreateASingleEyesOpenedEvent() {
        // When
        this.tracker.onUpdate(getFaceDetections(new Instant(100)), createFaceWithEyesOpened());
        this.tracker.onUpdate(getFaceDetections(new Instant(101)), createFaceWithEyesOpened());

        // Then
        assertThat(this.eventListener.filterEventsBy(EyesOpenedEvent.class), contains(new EyesOpenedEvent(new Instant(100))));
    }

    @Test
    public void shouldCreateEvents() {
        // When
        this.tracker.onNewItem(1, createFaceWithEyesOpened());
        this.tracker.onUpdate(getFaceDetections(new Instant(100)), createFaceWithEyesOpened());
        this.tracker.onUpdate(getFaceDetections(new Instant(101)), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(new Instant(102)), createFaceWithEyesOpened());
        this.tracker.onUpdate(getFaceDetections(new Instant(103)), createFaceWithEyesClosed());

        // Then
        assertThat(this.eventListener.getEvents(), hasItems(
                new EyesOpenedEvent(new Instant(100)),
                new EyesClosedEvent(new Instant(101)),
                new EyesOpenedEvent(new Instant(102)),
                new EyesClosedEvent(new Instant(103))));
    }

    @Test
    public void shouldCreateEvents2() {
        // When
        this.tracker.onNewItem(1, createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(new Instant(100)), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(new Instant(101)), createFaceWithEyesOpened());
        this.tracker.onUpdate(getFaceDetections(new Instant(102)), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(new Instant(103)), createFaceWithEyesOpened());

        // Then
        assertThat(this.eventListener.getEvents(), hasItems(
                new EyesClosedEvent(new Instant(100)),
                new EyesOpenedEvent(new Instant(101)),
                new EyesClosedEvent(new Instant(102)),
                new EyesOpenedEvent(new Instant(103))));
    }

    @Test
    public void shouldCreateEvents3() {
        // When
        this.tracker.onNewItem(1, createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(new Instant(100)), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(new Instant(101)), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(new Instant(102)), createFaceWithEyesOpened());
        this.tracker.onUpdate(getFaceDetections(new Instant(103)), createFaceWithEyesOpened());
        this.tracker.onUpdate(getFaceDetections(new Instant(104)), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(new Instant(105)), createFaceWithEyesClosed());

        // Then
        assertThat(this.eventListener.getEvents(), hasItems(
                new EyesClosedEvent(new Instant(100)),
                new EyesOpenedEvent(new Instant(102)),
                new EyesClosedEvent(new Instant(104))));
    }

    @Test
    public void shouldCreateNoEventsWhenNeitherLEFT_EYENorRIGHT_EYEWasDetected() {
        this.shouldCreateNoEventsForFaceWithLandmarks(Collections.<Landmark> emptyList());
    }

    @Test
    public void shouldCreateNoEventsWhenLEFT_EYENWasNotDetected() {
        this.shouldCreateNoEventsForFaceWithLandmarks(Arrays.asList(createLandmark(Landmark.RIGHT_EYE)));
    }

    @Test
    public void shouldCreateNoEventsWhenRIGHT_EYENWasNotDetected() {
        this.shouldCreateNoEventsForFaceWithLandmarks(Arrays.asList(createLandmark(Landmark.LEFT_EYE)));
    }

    private void shouldCreateNoEventsForFaceWithLandmarks(final List<Landmark> landmarks) {
        // Given
        final Face face = createFaceWithLandmarks(landmarks);

        // When
        this.tracker.onUpdate(getFaceDetections(new Instant(100)), face);

        // Then
        assertThat(this.eventListener.getEvents(), is(empty()));
    }

    private Face createFaceWithLeftEyeOpenRightEyeClosed() {
        return createFace(0.8f, 0.4f);
    }
}