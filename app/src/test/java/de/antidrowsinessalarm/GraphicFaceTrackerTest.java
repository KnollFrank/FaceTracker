package de.antidrowsinessalarm;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame.Metadata;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.common.collect.FluentIterable;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import de.antidrowsinessalarm.event.Event;
import de.antidrowsinessalarm.event.EyesClosedEvent;
import de.antidrowsinessalarm.event.EyesOpenedEvent;
import de.antidrowsinessalarm.event.NormalEyeBlinkEvent;
import de.antidrowsinessalarm.event.SlowEyelidClosureEvent;
import de.antidrowsinessalarm.eventproducer.DrowsyEventProducer;
import de.antidrowsinessalarm.eventproducer.EyesClosedEventProducer;
import de.antidrowsinessalarm.eventproducer.EyesOpenedEventProducer;
import de.antidrowsinessalarm.eventproducer.NormalEyeBlinkEventProducer;
import de.antidrowsinessalarm.eventproducer.SlowEyelidClosureEventProducer;
import de.antidrowsinessalarm.eventproducer.SlowEyelidClosureEventsProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.doReturn;

public class GraphicFaceTrackerTest {

    private EventListener listener;
    private Tracker<Face> tracker;

    static Detector.Detections<Face> getFaceDetections(final long timestampMillis) {
        final Metadata metaData = Mockito.mock(Metadata.class);
        doReturn(timestampMillis).when(metaData).getTimestampMillis();

        final Detector.Detections<Face> detections = Mockito.mock(Detector.Detections.class);
        doReturn(metaData).when(detections).getFrameMetadata();

        return detections;
    }

    static Face createFaceWithEyesClosed() {
        return createFace(0.4f, 0.4f);
    }

    static Face createFace(final float isLeftEyeOpenProbability, final float isRightEyeOpenProbability) {
        final Face face = Mockito.mock(Face.class);
        doReturn(isLeftEyeOpenProbability).when(face).getIsLeftEyeOpenProbability();
        doReturn(isRightEyeOpenProbability).when(face).getIsRightEyeOpenProbability();
        return face;
    }

    @Before
    public void setup() {
        // Given
        this.listener = new EventListener();
        final EventBus eventBus = new EventBus();
        eventBus.register(this.listener);
        eventBus.register(new NormalEyeBlinkEventProducer(eventBus));
        eventBus.register(new SlowEyelidClosureEventProducer(eventBus));
        eventBus.register(new EyesOpenedEventProducer(eventBus));
        eventBus.register(new EyesClosedEventProducer(eventBus));

        this.tracker = new GraphicFaceTracker(eventBus, new DrowsyEventProducer(eventBus, new SlowEyelidClosureEventsProvider(15000)), new SystemClock());
    }

    @Test
    public void shouldCreateEyesClosedEvent() {
        this.shouldCreateEvent(0.4f, 0.4f, new EyesClosedEvent(100));
    }

    @Test
    public void shouldCreateEyesClosedEvent2() {
        this.shouldCreateEvent(0.3f, 0.4f, new EyesClosedEvent(101));
    }

    private void shouldCreateEvent(final float isLeftEyeOpenProbability, final float isRightEyeOpenProbability, final Event event) {
        // When
        this.tracker.onUpdate(getFaceDetections(event.getTimestampMillis()), createFace(isLeftEyeOpenProbability, isRightEyeOpenProbability));

        // Then
        assertThat(this.listener.getEvents(), hasItem(event));
    }

    @Test
    public void shouldNotCreateEyesClosedEventOnUNCOMPUTED_PROBABILITIES() {
        // When
        this.tracker.onUpdate(getFaceDetections(100), createFace(Face.UNCOMPUTED_PROBABILITY, Face.UNCOMPUTED_PROBABILITY));

        // Then
        assertThat(this.listener.getEvents(), not(hasItem(Matchers.<Event>instanceOf(EyesClosedEvent.class))));
    }

    @Test
    public void shouldNotCreateEyesOpenedEventOnUNCOMPUTED_PROBABILITIES() {
        // When
        this.tracker.onUpdate(getFaceDetections(100), createFace(Face.UNCOMPUTED_PROBABILITY, Face.UNCOMPUTED_PROBABILITY));

        // Then
        assertThat(this.listener.getEvents(), not(hasItem(Matchers.<Event>instanceOf(EyesOpenedEvent.class))));
    }

    @Test
    public void shouldCreateEyesOpenedEvent() {
        this.shouldCreateEvent(0.8f, 0.8f, new EyesOpenedEvent(123));
    }

    @Test
    public void shouldCreateEyesOpenedEvent2() {
        this.shouldCreateEvent(0.8f, 0.9f, new EyesOpenedEvent(1234));
    }

    @Test
    public void shouldCreateNormalEyeBlink() {
        // When
        this.tracker.onUpdate(getFaceDetections(0), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(499), this.createFaceWithEyesOpened());

        // Then
        assertThat(this.listener.getEvents(), hasItem(new NormalEyeBlinkEvent(0, 499)));
    }

    @Test
    public void shouldCreateSlowEyelidClosureEvent() {
        // When
        this.tracker.onUpdate(getFaceDetections(0), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(501), this.createFaceWithEyesOpened());

        // Then
        assertThat(this.listener.getEvents(), hasItem(new SlowEyelidClosureEvent(0, 501)));
    }

    @Test
    public void shouldCreateASingleEyesOpenedEventForIntermediateIndefiniteEyesState() {
        // When
        this.tracker.onUpdate(getFaceDetections(0), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(1), this.createFaceWithEyesOpened());
        this.tracker.onUpdate(getFaceDetections(2), this.createFaceWithLeftEyeOpenRightEyeClosed());
        this.tracker.onUpdate(getFaceDetections(3), this.createFaceWithEyesOpened());

        // Then
        assertThat(this.filterEvents(EyesOpenedEvent.class), contains(new EyesOpenedEvent(1)));
    }

    @Test
    public void shouldCreateASingleEyesClosedEventForIntermediateIndefiniteEyesState() {
        // When
        this.tracker.onUpdate(getFaceDetections(0), this.createFaceWithEyesOpened());
        this.tracker.onUpdate(getFaceDetections(1), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(2), this.createFaceWithLeftEyeOpenRightEyeClosed());
        this.tracker.onUpdate(getFaceDetections(3), createFaceWithEyesClosed());

        // Then
        assertThat(this.filterEvents(EyesClosedEvent.class), contains(new EyesClosedEvent(1)));
    }

    @Test
    public void shouldCreateASingleEyesClosedEvent() {
        // When
        this.tracker.onUpdate(getFaceDetections(100), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(101), createFaceWithEyesClosed());

        // Then
        assertThat(this.filterEvents(EyesClosedEvent.class), contains(new EyesClosedEvent(100)));
    }

    private <T> List<T> filterEvents(final Class<T> clazz) {
        return FluentIterable.from(this.listener.getEvents()).filter(clazz).toList();
    }

    @Test
    public void shouldCreateASingleEyesOpenedEvent() {
        // When
        this.tracker.onUpdate(getFaceDetections(100), this.createFaceWithEyesOpened());
        this.tracker.onUpdate(getFaceDetections(101), this.createFaceWithEyesOpened());

        // Then
        assertThat(this.filterEvents(EyesOpenedEvent.class), contains(new EyesOpenedEvent(100)));
    }

    @Test
    public void shouldCreateEvents() {
        // When
        this.tracker.onNewItem(1, this.createFaceWithEyesOpened());
        this.tracker.onUpdate(getFaceDetections(100), this.createFaceWithEyesOpened());
        this.tracker.onUpdate(getFaceDetections(101), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(102), this.createFaceWithEyesOpened());
        this.tracker.onUpdate(getFaceDetections(103), createFaceWithEyesClosed());

        // Then
        assertThat(this.listener.getEvents(), hasItems(
                new EyesOpenedEvent(100),
                new EyesClosedEvent(101),
                new EyesOpenedEvent(102),
                new EyesClosedEvent(103)));
    }

    @Test
    public void shouldCreateEvents2() {
        // When
        this.tracker.onNewItem(1, createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(100), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(101), this.createFaceWithEyesOpened());
        this.tracker.onUpdate(getFaceDetections(102), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(103), this.createFaceWithEyesOpened());

        // Then
        assertThat(this.listener.getEvents(), hasItems(
                new EyesClosedEvent(100),
                new EyesOpenedEvent(101),
                new EyesClosedEvent(102),
                new EyesOpenedEvent(103)));
    }

    @Test
    public void shouldCreateEvents3() {
        // When
        this.tracker.onNewItem(1, createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(100), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(101), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(102), this.createFaceWithEyesOpened());
        this.tracker.onUpdate(getFaceDetections(103), this.createFaceWithEyesOpened());
        this.tracker.onUpdate(getFaceDetections(104), createFaceWithEyesClosed());
        this.tracker.onUpdate(getFaceDetections(105), createFaceWithEyesClosed());

        // Then
        assertThat(this.listener.getEvents(), hasItems(
                new EyesClosedEvent(100),
                new EyesOpenedEvent(102),
                new EyesClosedEvent(104)));
    }

    private Face createFaceWithEyesOpened() {
        return createFace(0.8f, 0.8f);
    }

    private Face createFaceWithLeftEyeOpenRightEyeClosed() {
        return createFace(0.8f, 0.4f);
    }

    // TODO: remove, EventTest already has one such method. see http://blog.danlew.net/2015/11/02/sharing-code-between-unit-tests-and-instrumentation-tests-on-android/
    static class EventListener {

        private final List<Event> events = new ArrayList<Event>();

        @Subscribe
        public void recordEvent(final Event event) {
            this.events.add(event);
        }

        Event getEvent() {
            return !this.events.isEmpty() ? this.events.get(this.events.size() - 1) : null;
        }

        List<Event> getEvents() {
            return this.events;
        }
    }
}