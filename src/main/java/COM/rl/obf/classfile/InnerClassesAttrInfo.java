/* ===========================================================================
 * $RCSfile: InnerClassesAttrInfo.java,v $
 * ===========================================================================
 *
 * RetroGuard -- an obfuscation package for Java classfiles.
 *
 * Copyright (c) 1998-2006 Mark Welsh (markw@retrologic.com)
 *
 * This program can be redistributed and/or modified under the terms of the
 * Version 2 of the GNU General Public License as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */

package COM.rl.obf.classfile;

import java.io.*;
import java.util.*;

/**
 * Representation of an attribute.
 * 
 * @author Mark Welsh
 */
public class InnerClassesAttrInfo extends AttrInfo
{
    // Constants -------------------------------------------------------------


    // Fields ----------------------------------------------------------------
    private int u2numberOfClasses;
    private List classes;


    // Class Methods ---------------------------------------------------------


    // Instance Methods ------------------------------------------------------
    protected InnerClassesAttrInfo(ClassFile cf, int attrNameIndex, int attrLength)
    {
        super(cf, attrNameIndex, attrLength);
    }

    /** Return the String name of the attribute; over-ride this in sub-classes. */
    @Override
    protected String getAttrName()
    {
        return ClassConstants.ATTR_InnerClasses;
    }

    /** Return the array of inner classes data. */
    protected InnerClassesInfo[] getInfo()
    {
        return (InnerClassesInfo[])this.classes.toArray(new InnerClassesInfo[0]);
    }

    /**
     * Check for Utf8 references in the 'info' data to the constant pool and mark them.
     * 
     * @throws ClassFileException
     */
    @Override
    protected void markUtf8RefsInInfo(ConstantPool pool) throws ClassFileException
    {
        for (Iterator iter = this.classes.iterator(); iter.hasNext();)
        {
            InnerClassesInfo cl = (InnerClassesInfo)iter.next();
            cl.markUtf8Refs(pool);
        }
    }

    /**
     * Read the data following the header.
     * 
     * @throws IOException
     * @throws ClassFileException
     */
    @Override
    protected void readInfo(DataInput din) throws IOException, ClassFileException
    {
        this.u2numberOfClasses = din.readUnsignedShort();
        this.classes = new ArrayList(this.u2numberOfClasses);
        for (int i = 0; i < this.u2numberOfClasses; i++)
        {
            this.classes.add(InnerClassesInfo.create(din));
        }
    }

    /**
     * Export data following the header to a DataOutput stream.
     * 
     * @throws IOException
     * @throws ClassFileException
     */
    @Override
    public void writeInfo(DataOutput dout) throws IOException, ClassFileException
    {
        dout.writeShort(this.u2numberOfClasses);
        for (Iterator iter = this.classes.iterator(); iter.hasNext();)
        {
            InnerClassesInfo cl = (InnerClassesInfo)iter.next();
            cl.write(dout);
        }
    }

    /**
     * Do necessary name remapping.
     * 
     * @throws ClassFileException
     */
    @Override
    protected void remap(ClassFile cf, NameMapper nm) throws ClassFileException
    {
        for (Iterator iter = this.classes.iterator(); iter.hasNext();)
        {
            InnerClassesInfo cl = (InnerClassesInfo)iter.next();
            cl.remap(cf, nm);
        }
    }
}
