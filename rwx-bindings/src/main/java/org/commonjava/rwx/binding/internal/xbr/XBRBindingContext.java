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

package org.commonjava.rwx.binding.internal.xbr;

import static org.commonjava.rwx.binding.anno.AnnotationUtils.hasAnnotation;
import static org.commonjava.rwx.binding.anno.AnnotationUtils.isMessage;

import org.apache.xbean.recipe.ArrayRecipe;
import org.apache.xbean.recipe.CollectionRecipe;
import org.apache.xbean.recipe.DefaultRepository;
import org.apache.xbean.recipe.MapRecipe;
import org.apache.xbean.recipe.ObjectRecipe;
import org.apache.xbean.recipe.Recipe;
import org.apache.xbean.recipe.Repository;
import org.commonjava.rwx.binding.anno.ArrayPart;
import org.commonjava.rwx.binding.anno.BindVia;
import org.commonjava.rwx.binding.anno.Contains;
import org.commonjava.rwx.binding.anno.StructPart;
import org.commonjava.rwx.binding.error.BindException;
import org.commonjava.rwx.binding.internal.xbr.helper.ArrayBinder;
import org.commonjava.rwx.binding.internal.xbr.helper.ArrayMappingBinder;
import org.commonjava.rwx.binding.internal.xbr.helper.CollectionBinder;
import org.commonjava.rwx.binding.internal.xbr.helper.MapBinder;
import org.commonjava.rwx.binding.internal.xbr.helper.MessageBinder;
import org.commonjava.rwx.binding.internal.xbr.helper.StructMappingBinder;
import org.commonjava.rwx.binding.mapping.ArrayMapping;
import org.commonjava.rwx.binding.mapping.FieldBinding;
import org.commonjava.rwx.binding.mapping.Mapping;
import org.commonjava.rwx.binding.mapping.StructMapping;
import org.commonjava.rwx.binding.spi.Binder;
import org.commonjava.rwx.binding.spi.BindingContext;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

public class XBRBindingContext
    implements BindingContext
{

    private final Repository repository = new DefaultRepository();

    private final Map<Class<?>, Mapping<?>> mappings;

    public XBRBindingContext( final Map<Class<?>, Mapping<?>> mappings )
        throws BindException
    {
        this.mappings = mappings;

        for ( final Mapping<?> recipe : mappings.values() )
        {
            final Recipe builder = new ObjectRecipe( recipe.getObjectType() );

            repository.add( recipe.getObjectType().getName(), builder );
        }

        // TODO: setup MapRecipe / CollectionRecipe / ArrayRecipe to deal with fields appropriately!
        for ( final Mapping<?> recipe : mappings.values() )
        {
            final ObjectRecipe r = (ObjectRecipe) repository.get( recipe.getObjectType().getName() );

            final Map<? extends Object, FieldBinding> bindings = recipe.getFieldBindings();

            final Object[] ctorKeys = recipe.getConstructorKeys();
            final String[] ctorArgs = new String[ctorKeys.length];
            for ( int i = 0; i < ctorKeys.length; i++ )
            {
                final FieldBinding binding = bindings.get( ctorKeys[i] );
                ctorArgs[i] = binding.getFieldName();
            }

            r.setConstructorArgNames( ctorArgs );

            for ( final Map.Entry<? extends Object, FieldBinding> entry : bindings.entrySet() )
            {
                final FieldBinding binding = entry.getValue();
                if ( mappings.containsKey( binding.getFieldType() ) )
                {
                    final Recipe ref = (Recipe) repository.get( binding.getFieldType().getName() );
                    if ( ref == null )
                    {
                        throw new BindException( "Cannot find XBR recipe: " + binding.getFieldType() + " for field: "
                            + entry.getKey() + " in: " + recipe.getObjectType() );
                    }

                    r.setProperty( binding.getFieldName(), ref );
                }
                else if ( Map.class.isAssignableFrom( binding.getFieldType() ) )
                {
                    final MapRecipe fr = new MapRecipe( binding.getFieldType() );
                    r.setProperty( binding.getFieldName(), fr );
                }
                else if ( Collection.class.isAssignableFrom( binding.getFieldType() ) )
                {
                    final CollectionRecipe cr = new CollectionRecipe( binding.getFieldType() );
                    r.setProperty( binding.getFieldName(), cr );
                }
                else if ( binding.getFieldType().isArray() )
                {
                    final ArrayRecipe ar = new ArrayRecipe( binding.getFieldType().getComponentType() );
                    r.setProperty( binding.getFieldName(), ar );
                }
            }
        }
    }

    public static ObjectRecipe setupObjectRecipe( final Mapping<?> mapping )
    {
        final ObjectRecipe recipe = new ObjectRecipe( mapping.getObjectType() );

        final Map<? extends Object, FieldBinding> bindings = mapping.getFieldBindings();

        final Object[] ctorKeys = mapping.getConstructorKeys();
        final String[] ctorArgs = new String[ctorKeys.length];
        final Class<?>[] ctorArgTypes = new Class<?>[ctorKeys.length];
        for ( int i = 0; i < ctorKeys.length; i++ )
        {
            final FieldBinding binding = bindings.get( ctorKeys[i] );
            ctorArgs[i] = binding.getFieldName();
            ctorArgTypes[i] = binding.getFieldType();
        }

        recipe.setConstructorArgNames( ctorArgs );
        recipe.setConstructorArgTypes( ctorArgTypes );

        return recipe;
    }

    public Field findField( final FieldBinding binding, final Class<?> parentCls )
        throws BindException
    {
        Field field = null;
        Class<?> fieldOwner = parentCls;
        while ( field == null && fieldOwner != null )
        {
            try
            {
                field = fieldOwner.getDeclaredField( binding.getFieldName() );
            }
            catch ( final NoSuchFieldException e )
            {
                // TODO: log this to debug log-level.
                field = null;
                fieldOwner = fieldOwner.getSuperclass();
            }
        }

        if ( field == null )
        {
            throw new BindException( "Cannot find field: " + binding.getFieldName()
                + " in class (or parent classes of): " + parentCls.getName() );
        }

        return field;
    }

    public MessageBinder newMessageBinder( final Class<?> messageType )
        throws BindException
    {
        if ( !isMessage( messageType ) )
        {
            throw new BindException( "Invalid entry-point class. " + messageType
                + " must be annotated with either @Request or @Response!" );
        }

        final ArrayMapping mapping = (ArrayMapping) mappings.get( messageType );
        if ( mapping == null )
        {
            throw new BindException( "Cannot find mapping for entry-point class: " + messageType );
        }

        return new MessageBinder( messageType, mapping, this );
    }

    public Binder newBinder( final Binder parent, final Class<?> type )
        throws BindException
    {
        return newBinder( parent, type, null, null );
    }

    public Binder newBinder( final Binder parent, final Field field )
        throws BindException
    {
        return newBinder( parent, field.getType(), field.getAnnotation( Contains.class ),
                          field.getAnnotation( BindVia.class ) );
    }

    @SuppressWarnings( "unchecked" )
    protected Binder newBinder( final Binder parent, final Class<?> type, final Contains containsAnno,
                                final BindVia bindVia )
        throws BindException
    {
        if ( bindVia != null )
        {
            return XBRBinderInstantiator.newValueBinder( bindVia, parent, type, this );
        }
        else if ( mappings.containsKey( type ) )
        {
            final Mapping<?> mapping = mappings.get( type );

            if ( isMessage( type ) )
            {
                return new MessageBinder( type, (ArrayMapping) mapping, this );
            }
            else if ( hasAnnotation( type, ArrayPart.class ) )
            {
                return new ArrayMappingBinder( parent, type, (ArrayMapping) mapping, this );
            }
            else if ( hasAnnotation( type, StructPart.class ) )
            {
                return new StructMappingBinder( parent, type, (StructMapping) mapping, this );
            }
            else
            {
                throw new BindException( "Unknown Mapping: " + mapping );
            }
        }
        else if ( Map.class.isAssignableFrom( type ) )
        {
            return new MapBinder( parent, type, containsAnno == null ? Object.class : containsAnno.value(), this );
        }
        else if ( Collection.class.isAssignableFrom( type ) )
        {
            return new CollectionBinder( parent, type, containsAnno == null ? Object.class : containsAnno.value(), this );
        }
        else if ( type.isArray() )
        {
            return new ArrayBinder( parent, type.getComponentType(), this );
        }

        return null;
    }

}