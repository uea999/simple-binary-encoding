/* -*- mode: java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*- */
/*
 * Copyright 2013 Real Logic Ltd.
 *
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
package uk.co.real_logic.sbe.ir;

import uk.co.real_logic.sbe.PrimitiveType;
import uk.co.real_logic.sbe.PrimitiveValue;

import java.nio.ByteOrder;

/**
 * Class to encapsulate an atom of data. This Intermediate Representation (IR)
 * is intended to be language, schema, platform independent.
 * <p>
 * Processing and optimization could be run over a list of IrNodes to perform various functions
 * <ul>
 *     <li>re-ordering of fields based on size</li>
 *     <li>padding of fields in order to provide expansion room</li>
 *     <li>computing offsets of individual fields</li>
 *     <li>etc.</li>
 * </ul>
 *<p>
 * IR could be used to generate code or other specifications. It should be possible to do the
 * following:
 * <ul>
 *     <li>generate a FIX/SBE schema from IR</li>
 *     <li>generate an ASN.1 spec from IR</li>
 *     <li>generate a GPB spec from IR</li>
 *     <li>etc.</li>
 * </ul>
 *<p>
 * IR could be serialized to storage or network via code generated by SBE. Then read back in to
 * a List of IrNodes.
 *<p>
 *
 * The entire IR of an entity is a {@link java.util.List} of IrNode objects. The order of this list is
 * very important. Encoding of fields is done by nodes pointing to specific encoding {@link PrimitiveType}
 * objects. Each encoding node contains size, offset, byte order, and {@link Metadata}. Entities relevant
 * to the encoding such as fields, messages, repeating groups, etc. are encapsulated in the list as nodes
 * themselves. Although, they will in most cases never be serialized. The boundaries of these entities
 * are delimited by START and END {@link Flag} values in the node {@link Metadata}. A list structure like
 * this allows for each concatenation of encodings as well as easy traversal.
 *<p>
 * An example encoding of a message header might be like this.
 * <ul>
 *     <li>Node 0 - Flag = MESSAGE_START, id = 100</li>
 *     <li>Node 1 - Flag = FIELD_START, id = 25</li>
 *     <li>Node 2 - Flag = NONE, PrimitiveType = uint32, size = 4, offset = 0</li>
 *     <li>Node 3 - Flag = FIELD_END</li>
 *     <li>Node 4 - Flag = MESSAGE_END</li>
 * </ul>
 *<p>
 * Specific nodes have IR IDs. These IDs are used to cross reference entities that have a relationship. Such as
 * length fields for variable length data elements, and entry count fields for repeating groups.
 * {@link Metadata#getIrId()} can be used to return the nodes IR ID. While {@link Metadata#getXRefIrId()} can
 * be used to return the nodes cross reference IR ID. Cross referencing is always two-way.
 */
public class IrNode
{
    /** Size not determined */
    public static final int VARIABLE_SIZE = -1;

    /** Offset not computed or set */
    public static final int UNKNOWN_OFFSET = -1;

    private final PrimitiveType primitiveType;
    private final int size;
    private final int offset;
    private final ByteOrder byteOrder;
    private final Metadata metadata;

    /**
     * Construct an {@link IrNode} by providing values for all fields.
     *
     * @param primitiveType representing this node or null.
     * @param size          of the node in bytes.
     * @param offset        within the {@link uk.co.real_logic.sbe.xml.Message}.
     * @param byteOrder     for the encoding.
     * @param metadata      for the {@link uk.co.real_logic.sbe.xml.Message}.
     */
    public IrNode(final PrimitiveType primitiveType,
                  final int size,
                  final int offset,
                  final ByteOrder byteOrder,
                  final Metadata metadata)
    {
        this.primitiveType = primitiveType;
        this.size = size;
        this.offset = offset;
        this.byteOrder = byteOrder;
        this.metadata = metadata;

        if (metadata == null)
        {
            throw new RuntimeException("metadata of IrNode must not be null");
        }
    }

    /**
     * Construct a default {@link IrNode} based on {@link Metadata} with defaults for other fields.
     *
     * @param metadata for this node.
     */
    public IrNode(final Metadata metadata)
    {
        this.primitiveType = null;
        this.size = 0;
        this.offset = 0;
        this.byteOrder = null;
        this.metadata = metadata;

        if (metadata == null)
        {
            throw new RuntimeException("metadata of IrNode must not be null");
        }
    }

    /**
     * Return the primitive type of this node. This value is only relevant for nodes that are encodings.
     */
    public PrimitiveType getPrimitiveType()
    {
        return primitiveType;
    }

    /**
     * Return the size of this node. A value of 0 means the node has no size when encoded. A value of
     * {@link IrNode#VARIABLE_SIZE} means this node represents a variable length field.
     */
    public int size()
    {
        return size;
    }

    /**
     * Return the offset of this node. A value of 0 means the node has no relevant offset. A value of
     * {@link IrNode#UNKNOWN_OFFSET} means this nodes true offset is dependent on variable length fields ahead
     * of it in the encoding.
     */
    public int getOffset()
    {
        return offset;
    }

    /**
     * Return the {@link Metadata} of the {@link IrNode}.
     *
     * @return metadata of the {@link IrNode}
     */
    public Metadata getMetadata()
    {
        return metadata;
    }

    /**
     * Return the byte order of this field.
     */
    public ByteOrder getByteOrder()
    {
        return byteOrder;
    }

    /**
     * Flag used to determine the {@link IrNode} type. These flags start/end various entities such as
     * fields, composites, messages, repeating groups, enumerations, bitsets, etc.
     */
    public enum Flag
    {
        /** flag denoting the start of a message */
        MESSAGE_START,

        /** flag denoting the end of a message */
        MESSAGE_END,

        /** flag denoting the start of a composite */
        COMPOSITE_START,

        /** flag denoting the end of a composite */
        COMPOSITE_END,

        /** flag denoting the start of a field */
        FIELD_START,

        /** flag denoting the end of a field */
        FIELD_END,

        /** flag denoting the start of a repeating group */
        GROUP_START,

        /** flag denoting the end of a repeating group */
        GROUP_END,

        /** flag denoting the start of an enumeration */
        ENUM_START,

        /** flag denoting a value of an enumeration */
        ENUM_VALUE,

        /** flag denoting the end of an enumeration */
        ENUM_END,

        /** flag denoting the start of a bitset */
        SET_START,

        /** flag denoting a bit value (choice) of a bitset */
        SET_CHOICE,

        /** flag denoting the end of a bitset */
        SET_END,

        /** flag denoting the {@link IrNode} is a basic encoding node */
        NONE
    }

    /** Metadata describing an {@link IrNode} */
    public static class Metadata
    {
        /** Invalid ID value. */
        public static final long INVALID_ID = Long.MAX_VALUE;

        private final String name;
        private final long id;
        private final long irId;
        private final long xRefIrId;
        private final Flag flag;
        private final PrimitiveValue minValue;
        private final PrimitiveValue maxValue;
        private final PrimitiveValue nullValue;
        private final PrimitiveValue constValue;
        private final String description;
        private final String fixUsage;

        /**
         * Constructor that uses a builder
         *
         * @param builder to use to build the metadata
         */
        public Metadata(final Builder builder)
        {
            name = builder.name;
            id = builder.id;
            irId = builder.irId;
            xRefIrId = builder.xRefIrId;
            flag = builder.flag;
            minValue = builder.minValue;
            maxValue = builder.maxValue;
            nullValue = builder.nullValue;
            constValue = builder.constValue;
            description = builder.description;
            fixUsage = builder.fixUsage;
        }

        /**
         * Return the name of the node
         */
        public String getName()
        {
            return name;
        }

        /**
         * Return the ID of the node assigned by the specification
         */
        public long getId()
        {
            return id;
        }

        /**
         * Return the IR ID of the node.
         */
        public long getIrId()
        {
            return irId;
        }

        /**
         * Return the cross reference IR ID of the node.
         */
        public long getXRefIrId()
        {
            return xRefIrId;
        }

        /**
         * Return the node flag
         */
        public Flag getFlag()
        {
            return flag;
        }

        /**
         * Return the minValue for the node or null if not set.
         */
        public PrimitiveValue getMinValue()
        {
            return minValue;
        }

        /**
         * Return the maxValue for the node or null if not set.
         */
        public PrimitiveValue getMaxValue()
        {
            return maxValue;
        }

        /**
         * Return the nullValue for the node or null if not set.
         */
        public PrimitiveValue getNullValue()
        {
            return nullValue;
        }

        /**
         * Return the constant value for the node or null if not set.
         */
        public PrimitiveValue getConstValue()
        {
            return constValue;
        }

        /**
         * Return the description for the node or null if not set.
         */
        public String getDescription()
        {
            return description;
        }

        /**
         * Builder to make setting {@link IrNode.Metadata} easier to create.
         */
        public static class Builder
        {
            private String name;
            private long id;
            private long irId;
            private long xRefIrId;
            private Flag flag;
            private PrimitiveValue minValue;
            private PrimitiveValue maxValue;
            private PrimitiveValue nullValue;
            private PrimitiveValue constValue;
            private String description;
            private String fixUsage;

            public Builder(final String name)
            {
                this.name = name;
                id = Metadata.INVALID_ID;
                irId = Metadata.INVALID_ID;
                xRefIrId = Metadata.INVALID_ID;
                this.flag = Flag.NONE;
                minValue = null;
                maxValue = null;
                nullValue = null;
                constValue = null;
                description = null;
                fixUsage = null;
            }

            public void setId(final long id)
            {
                this.id = id;
            }

            public void setIrId(final long irId)
            {
                this.irId = irId;
            }

            public void setXRefIrId(final long xRefIrId)
            {
                this.xRefIrId = xRefIrId;
            }

            public void setFlag(final Flag flag)
            {
                this.flag = flag;
            }

            public void setMinValue(final PrimitiveValue minValue)
            {
                this.minValue = minValue;
            }

            public void setMaxValue(final PrimitiveValue maxValue)
            {
                this.maxValue = maxValue;
            }

            public void setNullValue(final PrimitiveValue nullValue)
            {
                this.nullValue = nullValue;
            }

            public void setConstValue(final PrimitiveValue constValue)
            {
                this.constValue = constValue;
            }

            public void setDescription(final String description)
            {
                this.description = description;
            }

            public void setFixUsage(final String fixUsage)
            {
                this.fixUsage = fixUsage;
            }
        }
    }
}
