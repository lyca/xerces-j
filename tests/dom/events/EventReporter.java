/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dom.events;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.MutationEvent;

public class EventReporter implements EventListener {

    public static class EventRecord {
        public final String type;
        public final String currentTargetName;
        public final String targetName;
        public final short phase;
        public final boolean bubbles;
        public final boolean cancelable;
        public final String attrName;
        public final String prevValue;
        public final String newValue;
        public final short attrChange;

        public EventRecord(Event evt) {
            this.type = evt.getType();
            Node ct = (Node) evt.getCurrentTarget();
            this.currentTargetName = (ct != null) ? ct.getNodeName() : null;
            Node t = (Node) evt.getTarget();
            this.targetName = (t != null) ? t.getNodeName() : null;
            this.phase = evt.getEventPhase();
            this.bubbles = evt.getBubbles();
            this.cancelable = evt.getCancelable();

            if (evt instanceof MutationEvent) {
                MutationEvent me = (MutationEvent) evt;
                this.attrName = me.getAttrName();
                this.prevValue = me.getPrevValue();
                this.newValue = me.getNewValue();
                this.attrChange = me.getAttrChange();
            }
            else {
                this.attrName = null;
                this.prevValue = null;
                this.newValue = null;
                this.attrChange = 0;
            }
        }
    }

    private boolean silent = true;
    private final List<EventRecord> events = new ArrayList<EventRecord>();

    public void on() {
        silent = false;
    }

    public void off() {
        silent = true;
    }

    public void clear() {
        events.clear();
    }

    public int getCount() {
        return events.size();
    }

    public List<EventRecord> getEvents() {
        return events;
    }

    public List<EventRecord> getEventsOfType(String type) {
        List<EventRecord> matched = new ArrayList<EventRecord>();
        for (EventRecord r : events) {
            if (r.type.equals(type)) {
                matched.add(r);
            }
        }
        return matched;
    }

    public void handleEvent(Event evt) {
        if (silent) {
            return;
        }
        events.add(new EventRecord(evt));
    }
}
