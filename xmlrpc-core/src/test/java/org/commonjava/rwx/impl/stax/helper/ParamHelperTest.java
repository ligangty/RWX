/*
 *  Copyright (C) 2010 John Casey.
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.commonjava.rwx.impl.stax.helper;

import static org.commonjava.rwx.impl.estream.testutil.EventAssertions.assertRecordedEvents;

import org.commonjava.rwx.error.XmlRpcException;
import org.commonjava.rwx.impl.estream.model.ArrayEvent;
import org.commonjava.rwx.impl.estream.model.Event;
import org.commonjava.rwx.impl.estream.model.ParameterEvent;
import org.commonjava.rwx.impl.estream.model.StructEvent;
import org.commonjava.rwx.impl.estream.model.ValueEvent;
import org.commonjava.rwx.impl.estream.testutil.ExtList;
import org.commonjava.rwx.impl.estream.testutil.ExtMap;
import org.commonjava.rwx.impl.estream.testutil.RecordedEvent;
import org.commonjava.rwx.impl.estream.testutil.RecordingListener;
import org.commonjava.rwx.impl.stax.helper.ParamHelper;
import org.commonjava.rwx.vocab.EventType;
import org.commonjava.rwx.vocab.ValueType;
import org.jdom.JDOMException;
import org.junit.Test;


import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.IOException;
import java.util.List;

public class ParamHelperTest
    extends AbstractStaxHelperTest
{

    @Test
    public void oneParam()
        throws JDOMException, IOException, XMLStreamException, XmlRpcException
    {
        final XMLStreamReader reader = getXML( "oneSimpleParam" );

        final RecordingListener listener = new RecordingListener();
        ParamHelper.parse( reader, listener );

        final List<RecordedEvent> events = listener.getRecordedEvents();

        final List<Event<?>> check =
            new ExtList<Event<?>>( new ParameterEvent( 0 ), new ValueEvent( "foo", ValueType.STRING ),
                                   new ParameterEvent( 0, "foo", ValueType.STRING ), new ParameterEvent() );

        assertRecordedEvents( check, events );
    }

    @Test
    public void twoParams()
        throws JDOMException, IOException, XMLStreamException, XmlRpcException
    {
        final XMLStreamReader reader = getXML( "twoSimpleParams" );
        //        gotoElement( reader );

        final RecordingListener listener = new RecordingListener();
        ParamHelper.parse( reader, listener );

        final List<RecordedEvent> events = listener.getRecordedEvents();

        final List<Event<?>> check =
            new ExtList<Event<?>>( new ParameterEvent( 0 ), new ValueEvent( "foo", ValueType.STRING ),
                                   new ParameterEvent( 0, "foo", ValueType.STRING ), new ParameterEvent(),
                                   new ParameterEvent( 1 ), new ValueEvent( "foo2", ValueType.STRING ),
                                   new ParameterEvent( 1, "foo2", ValueType.STRING ), new ParameterEvent() );

        assertRecordedEvents( check, events );
    }

    @Test
    public void structValue()
        throws JDOMException, IOException, XMLStreamException, XmlRpcException
    {
        final XMLStreamReader reader = getXML( "structInParam" );

        final RecordingListener listener = new RecordingListener();
        ParamHelper.parse( reader, listener );

        final List<RecordedEvent> events = listener.getRecordedEvents();

        final ExtMap<String, String> map = new ExtMap<String, String>( "key", "foo" );

        final List<Event<?>> check =
            new ExtList<Event<?>>( new ParameterEvent( 0 ), new StructEvent( EventType.START_STRUCT ),
                                   new StructEvent( "key" ), new ValueEvent( "foo", ValueType.STRING ),
                                   new StructEvent( "key", "foo", ValueType.STRING ),
                                   new StructEvent( EventType.END_STRUCT_MEMBER ),
                                   new StructEvent( EventType.END_STRUCT ), new ValueEvent( map, ValueType.STRUCT ),
                                   new ParameterEvent( 0, map, ValueType.STRUCT ), new ParameterEvent() );

        assertRecordedEvents( check, events );
    }

    @Test
    public void arrayValue()
        throws JDOMException, IOException, XMLStreamException, XmlRpcException
    {
        final XMLStreamReader reader = getXML( "arrayInParam" );

        final RecordingListener listener = new RecordingListener();
        ParamHelper.parse( reader, listener );

        final List<RecordedEvent> events = listener.getRecordedEvents();

        final ExtList<String> lst = new ExtList<String>( "foo" );

        final List<Event<?>> check =
            new ExtList<Event<?>>( new ParameterEvent( 0 ), new ArrayEvent( EventType.START_ARRAY ),
                                   new ArrayEvent( 0 ), new ValueEvent( "foo", ValueType.STRING ),
                                   new ArrayEvent( 0, "foo", ValueType.STRING ),
                                   new ArrayEvent( EventType.END_ARRAY_ELEMENT ),
                                   new ArrayEvent( EventType.END_ARRAY ), new ValueEvent( lst, ValueType.ARRAY ),
                                   new ParameterEvent( 0, lst, ValueType.ARRAY ), new ParameterEvent() );

        assertRecordedEvents( check, events );
    }

}