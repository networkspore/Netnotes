package io.netnotes.system;

import java.util.HashMap;
import java.util.Map;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.io.input.IEventFactory;
import io.netnotes.engine.io.input.Keyboard;
import io.netnotes.engine.io.input.ephemeralEvents.EphemeralEvent;
import io.netnotes.engine.io.input.ephemeralEvents.EphemeralKeyCharEvent;
import io.netnotes.engine.io.input.ephemeralEvents.EphemeralKeyDownEvent;
import io.netnotes.engine.io.input.ephemeralEvents.EphemeralKeyUpEvent;
import io.netnotes.engine.io.input.ephemeralEvents.EphemeralRoutedEvent;
import io.netnotes.engine.io.input.events.EventBytes;
import io.netnotes.engine.io.input.events.RoutedEvent;
import io.netnotes.engine.messaging.NoteMessaging.Keys;
import io.netnotes.engine.messaging.NoteMessaging.ProtocolMesssages;
import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesArrayEphemeral;
import io.netnotes.noteBytes.NoteBytesEphemeral;
import io.netnotes.noteBytes.NoteBytesObjectEphemeral;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.noteBytes.collections.NoteBytesPairEphemeral;
import io.netnotes.noteBytes.processing.NoteBytesMetaData;

public class EphemeralKeyboardFactory implements IEventFactory {
    public static final NoteBytesReadOnly NULL_CODE_POINT = new NoteBytesReadOnly(9216);

    private static EphemeralKeyboardFactory INSTANCE = null;

    @FunctionalInterface
    private interface EphemeralEventDeserializer {
        EphemeralRoutedEvent create(ContextPath sourcePath, NoteBytesEphemeral type, int stateFlags, NoteBytesEphemeral[] payload);
    }

    private Map<NoteBytesReadOnly, EphemeralEventDeserializer > registry = new HashMap<>();

    private EphemeralKeyboardFactory()
    {
        setupRegistryMap();
    }

    public static EphemeralKeyboardFactory getInstance(){
        if(INSTANCE == null){
            INSTANCE = new EphemeralKeyboardFactory();
        }
        return INSTANCE;
    }

    private void setupRegistryMap(){
        this.registry.put(EventBytes.EVENT_KEY_DOWN, this::onKeyDown);
        this.registry.put(EventBytes.EVENT_KEY_UP, this::onKeyUp);
        this.registry.put(EventBytes.EVENT_KEY_CHAR, this::onKeyChar);
    }

    private EphemeralRoutedEvent onKeyDown(ContextPath src, NoteBytesEphemeral type, int flags, NoteBytesEphemeral[] p){
        return new EphemeralKeyDownEvent(src, type, flags, p.length > 0 ? p[0] : getEphemeralNoneKeyCode(), p.length > 1 ? p[1]: getEphemeralNoneKeyCode());
    }

    private EphemeralRoutedEvent onKeyUp(ContextPath src, NoteBytesEphemeral type, int flags, NoteBytesEphemeral[] p){
        return new EphemeralKeyUpEvent(src, type, flags, p.length > 0 ? p[0] : getEphemeralNoneKeyCode(), p.length > 1 ? p[1]: getEphemeralNoneKeyCode());
    }
    
    private EphemeralRoutedEvent onKeyChar(ContextPath src, NoteBytesEphemeral type, int flags, NoteBytesEphemeral[] p){
        return new EphemeralKeyCharEvent(src, type, flags, p.length > 0 ? p[0] : getEphemeralNull());
    }

    private NoteBytesEphemeral getEphemeralUnknown(){
        return new NoteBytesEphemeral(ProtocolMesssages.UNKNOWN.get());
    }

    private NoteBytesEphemeral getEphemeralNull(){
        return new NoteBytesEphemeral(NULL_CODE_POINT.get());
    }

    private NoteBytesEphemeral getEphemeralNoneKeyCode(){
        return new NoteBytesEphemeral(Keyboard.KeyCodeBytes.NONE.get());
        
    }

    @Override
    public RoutedEvent from(ContextPath sourcePath, NoteBytes noteBytes) {
        if(noteBytes.getType() != NoteBytesMetaData.NOTE_BYTES_OBJECT_TYPE){
            return new EphemeralEvent(sourcePath, getEphemeralUnknown() , 0, new NoteBytesEphemeral[]{ NoteBytesEphemeral.fromExisting(noteBytes)});
        }
        // Deserialize into ephemeral object
        try (
            NoteBytesObjectEphemeral body = new NoteBytesObjectEphemeral(noteBytes.get());
            NoteBytesPairEphemeral typePair = body.get(Keys.EVENT);
            NoteBytesPairEphemeral flagsPair = body.get(Keys.STATE_FLAGS);
            NoteBytesPairEphemeral payloadPair = body.get(Keys.PAYLOAD);
        ) {
            NoteBytesEphemeral typeBytes = typePair != null ? typePair.getValue().copy() : getEphemeralUnknown();
            
            // Extract state flags (optional)
            int flags = flagsPair != null ? flags = flagsPair.getValue().getAsInt() :0;
            
            // Extract payload array (ephemeral)
            // payloadPair.getValue().get() cleaned up in above try (close warning suppressed)
            NoteBytesEphemeral payload = payloadPair != null ? payloadPair.getValue() : null;

            NoteBytesEphemeral[] payloadArray = new NoteBytesEphemeral[0];
            if (payload != null && payload.getType() == NoteBytesMetaData.NOTE_BYTES_ARRAY_TYPE) {
                try (NoteBytesArrayEphemeral arrayEphemeral = new NoteBytesArrayEphemeral(payload.get())) {
                    payloadArray = arrayEphemeral.getAsArray();
                }
            }
            
      
            // Lookup constructor for event type (payload ownership transferred to event)
  
            @SuppressWarnings("unlikely-arg-type")
            EphemeralEventDeserializer deserializer = registry.get(typeBytes);

            if (deserializer == null) {
                return new EphemeralEvent(sourcePath,typeBytes, flags, payloadArray);
            }
                
            // Create event 
            return deserializer.create(sourcePath, typeBytes, flags, payloadArray);
            
        }catch(NullPointerException e){
            throw new IllegalStateException("Missing type field in encrypted event", e);
        }
    }
}
