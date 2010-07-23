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

package org.commonjava.rwx.binding.mapping;

import org.commonjava.rwx.binding.spi.value.ValueBinder;
import org.commonjava.rwx.binding.spi.value.ValueUnbinder;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

public class FieldBinding
    implements Externalizable, Serializable
{

    private static final long serialVersionUID = 1L;

    private String name;

    private Class<?> type;

    private Class<? extends ValueBinder> valueBinder;

    private Class<? extends ValueUnbinder> valueUnbinder;

    public FieldBinding( final String name, final Class<?> type )
    {
        this.name = name;
        this.type = type;
    }

    public FieldBinding withValueBinderType( final Class<? extends ValueBinder> valueBinder )
    {
        this.valueBinder = valueBinder;
        return this;
    }

    public Class<? extends ValueBinder> getValueBinderType()
    {
        return valueBinder;
    }

    public FieldBinding withValueUnbinderType( final Class<? extends ValueUnbinder> valueUnbinder )
    {
        this.valueUnbinder = valueUnbinder;
        return this;
    }

    public Class<? extends ValueUnbinder> getValueUnbinderType()
    {
        return valueUnbinder;
    }

    public String getFieldName()
    {
        return name;
    }

    public Class<?> getFieldType()
    {
        return type;
    }

    @Override
    public void readExternal( final ObjectInput in )
        throws IOException, ClassNotFoundException
    {
        name = (String) in.readObject();
        type = Class.forName( (String) in.readObject() );
    }

    @Override
    public void writeExternal( final ObjectOutput out )
        throws IOException
    {
        out.writeObject( name );
        out.writeObject( type.getName() );
    }

    @Override
    public String toString()
    {
        return "FieldBinding [name=" + name + ", type=" + type + ", valueBinder=" + valueBinder + ", valueUnbinder="
            + valueUnbinder + "]";
    }

}
