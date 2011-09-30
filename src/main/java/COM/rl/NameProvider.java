package COM.rl;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;

import COM.rl.obf.*;
import COM.rl.obf.classfile.ClassFileException;

public class NameProvider
{
    public static final int CLASSIC_MODE = 0;
    public static final int CHANGE_NOTHING_MODE = 1;
    public static final int DEOBFUSCATION_MODE = 2;
    public static final int REOBFUSCATION_MODE = 3;

    public static final String DEFAULT_CFG_FILE_NAME = "retroguard.cfg";

    public static int uniqueStart = 100000;
    public static int currentMode = NameProvider.CLASSIC_MODE;
    public static boolean quiet = false;
    public static boolean oldHash = false;
    public static boolean repackage = false;

    private static File packagesFile = null;
    private static File classesFile = null;
    private static File methodsFile = null;
    private static File fieldsFile = null;
    private static File reobFile = null;
    private static File npLog = null;
    private static File roLog = null;

    private static List<String> protectedPackages = new ArrayList<String>();
    private static Map<String, String> packageNameLookup = new HashMap<String, String>();

    private static List<PackageEntry> packageDefs = new ArrayList<PackageEntry>();
    private static List<ClassEntry> classDefs = new ArrayList<ClassEntry>();
    private static List<MethodEntry> methodDefs = new ArrayList<MethodEntry>();
    private static List<FieldEntry> fieldDefs = new ArrayList<FieldEntry>();

    private static Map<String, PackageEntry> packagesObf2Deobf = new HashMap<String, PackageEntry>();
    private static Map<String, PackageEntry> packagesDeobf2Obf = new HashMap<String, PackageEntry>();
    private static Map<String, ClassEntry> classesObf2Deobf = new HashMap<String, ClassEntry>();
    private static Map<String, ClassEntry> classesDeobf2Obf = new HashMap<String, ClassEntry>();
    private static Map<String, MethodEntry> methodsObf2Deobf = new HashMap<String, MethodEntry>();
    private static Map<String, MethodEntry> methodsDeobf2Obf = new HashMap<String, MethodEntry>();
    private static Map<String, FieldEntry> fieldsObf2Deobf = new HashMap<String, FieldEntry>();
    private static Map<String, FieldEntry> fieldsDeobf2Obf = new HashMap<String, FieldEntry>();

    public static String[] parseCommandLine(String[] args)
    {
        if ((args.length > 0)
            && (args[0].equalsIgnoreCase("-searge") || args[0].equalsIgnoreCase("-notch") || args[0].equalsIgnoreCase("-mojang")))
        {
            return NameProvider.parseNameSheetModeArgs(args);
        }

        if (args.length < 5)
        {
            return args;
        }

        int idx;
        try
        {
            idx = Integer.parseInt(args[4]);
        }
        catch (NumberFormatException e)
        {
            System.err.println("ERROR: Invalid start index: " + args[4]);
            throw e;
        }

        NameProvider.uniqueStart = idx;

        String[] newArgs = new String[4];

        for (int i = 0; i < 4; ++i)
        {
            newArgs[i] = args[i];
        }

        return newArgs;
    }

    private static String[] parseNameSheetModeArgs(String[] args)
    {
        if (args.length < 2)
        {
            return null;
        }

        String configFileName = args[1];
        File configFile = new File(configFileName);
        if (!configFile.exists() || !configFile.isFile())
        {
            System.err.println("ERROR: could not find config file " + configFileName);
            return null;
        }

        String reobinput = null;
        String reoboutput = null;
        FileReader fileReader = null;
        BufferedReader reader = null;
        String[] newArgs = new String[4];
        try
        {
            fileReader = new FileReader(configFile);
            reader = new BufferedReader(fileReader);
            String line = "";
            while (line != null)
            {
                line = reader.readLine();
                if ((line == null) || line.trim().startsWith("#"))
                {
                    continue;
                }

                String[] defines = line.split("=");
                if (defines.length > 1)
                {
                    defines[1] = line.substring(defines[0].length() + 1).trim();
                    defines[0] = defines[0].trim();

                    if (defines[0].equalsIgnoreCase("packages"))
                    {
                        NameProvider.packagesFile = new File(defines[1]);
                        if (!NameProvider.packagesFile.exists() || !NameProvider.packagesFile.isFile())
                        {
                            NameProvider.packagesFile = null;
                        }
                    }
                    else if (defines[0].equalsIgnoreCase("classes"))
                    {
                        NameProvider.classesFile = new File(defines[1]);
                        if (!NameProvider.classesFile.exists() || !NameProvider.classesFile.isFile())
                        {
                            NameProvider.classesFile = null;
                        }
                    }
                    else if (defines[0].equalsIgnoreCase("methods"))
                    {
                        NameProvider.methodsFile = new File(defines[1]);
                        if (!NameProvider.methodsFile.exists() || !NameProvider.methodsFile.isFile())
                        {
                            NameProvider.methodsFile = null;
                        }
                    }
                    else if (defines[0].equalsIgnoreCase("fields"))
                    {
                        NameProvider.fieldsFile = new File(defines[1]);
                        if (!NameProvider.fieldsFile.exists() || !NameProvider.fieldsFile.isFile())
                        {
                            NameProvider.fieldsFile = null;
                        }
                    }
                    else if (defines[0].equalsIgnoreCase("reob"))
                    {
                        NameProvider.reobFile = new File(defines[1]);
                        if (!NameProvider.reobFile.exists() || !NameProvider.reobFile.isFile())
                        {
                            NameProvider.reobFile = null;
                        }
                    }
                    else if (defines[0].equalsIgnoreCase("input"))
                    {
                        newArgs[0] = defines[1];
                    }
                    else if (defines[0].equalsIgnoreCase("output"))
                    {
                        newArgs[1] = defines[1];
                    }
                    else if (defines[0].equalsIgnoreCase("reobinput"))
                    {
                        reobinput = defines[1];
                    }
                    else if (defines[0].equalsIgnoreCase("reoboutput"))
                    {
                        reoboutput = defines[1];
                    }
                    else if (defines[0].equalsIgnoreCase("script"))
                    {
                        newArgs[2] = defines[1];
                    }
                    else if (defines[0].equalsIgnoreCase("log"))
                    {
                        newArgs[3] = defines[1];
                    }
                    else if (defines[0].equalsIgnoreCase("nplog"))
                    {
                        NameProvider.npLog = new File(defines[1]);
                        if (NameProvider.npLog.exists() && !NameProvider.npLog.isFile())
                        {
                            NameProvider.npLog = null;
                        }
                    }
                    else if (defines[0].equalsIgnoreCase("rolog"))
                    {
                        NameProvider.roLog = new File(defines[1]);
                        if (NameProvider.roLog.exists() && !NameProvider.roLog.isFile())
                        {
                            NameProvider.roLog = null;
                        }
                    }
                    else if (defines[0].equalsIgnoreCase("startindex"))
                    {
                        try
                        {
                            int start = Integer.parseInt(defines[1]);
                            NameProvider.uniqueStart = start;
                        }
                        catch (NumberFormatException e)
                        {
                            System.err.println("Invalid start index: " + args[4]);
                            throw e;
                        }
                    }
                    else if (defines[0].equalsIgnoreCase("protectedpackage"))
                    {
                        NameProvider.protectedPackages.add(defines[1]);
                    }
                    else if (defines[0].equalsIgnoreCase("quiet"))
                    {
                        String value = defines[1].substring(0, 1);
                        if (value.equalsIgnoreCase("1") || value.equalsIgnoreCase("t") || value.equalsIgnoreCase("y"))
                        {
                            NameProvider.quiet = true;
                        }
                    }
                    else if (defines[0].equalsIgnoreCase("oldhash"))
                    {
                        String value = defines[1].substring(0, 1);
                        if (value.equalsIgnoreCase("1") || value.equalsIgnoreCase("t") || value.equalsIgnoreCase("y"))
                        {
                            NameProvider.oldHash = true;
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            return null;
        }
        finally
        {
            try
            {
                if (reader != null)
                {
                    reader.close();
                }
                if (fileReader != null)
                {
                    fileReader.close();
                }
            }
            catch (IOException e)
            {
                // ignore
            }
        }

        if (args[0].equalsIgnoreCase("-searge"))
        {
            NameProvider.currentMode = NameProvider.DEOBFUSCATION_MODE;
        }
        else if (args[0].equalsIgnoreCase("-notch"))
        {
            NameProvider.currentMode = NameProvider.REOBFUSCATION_MODE;
        }
        else if (args[0].equalsIgnoreCase("-mojang"))
        {
            NameProvider.currentMode = NameProvider.DEOBFUSCATION_MODE;
            NameProvider.repackage = true;
        }
        else
        {
            return null;
        }

        if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE)
        {
            newArgs[0] = reobinput;
            newArgs[1] = reoboutput;
        }

        if ((newArgs[0] == null) || (newArgs[1] == null) || (newArgs[2] == null) || (newArgs[3] == null))
        {
            return null;
        }

        try
        {
            NameProvider.initLogfiles();
            NameProvider.readSRGFiles();
        }
        catch (IOException e)
        {
            return null;
        }

        return newArgs;
    }

    private static void initLogfiles() throws IOException
    {
        if (NameProvider.currentMode == NameProvider.DEOBFUSCATION_MODE)
        {
            if (NameProvider.npLog != null)
            {
                FileWriter writer = null;
                try
                {
                    writer = new FileWriter(NameProvider.npLog);
                }
                finally
                {
                    if (writer != null)
                    {
                        try
                        {
                            writer.close();
                        }
                        catch (IOException e)
                        {
                            // ignore
                        }
                    }
                }
            }
        }
        else if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE)
        {
            if (NameProvider.roLog != null)
            {
                FileWriter writer = null;
                try
                {
                    writer = new FileWriter(NameProvider.roLog);
                }
                finally
                {
                    if (writer != null)
                    {
                        try
                        {
                            writer.close();
                        }
                        catch (IOException e)
                        {
                            // ignore
                        }
                    }
                }
            }
        }
    }

    private static void readSRGFiles() throws IOException
    {
        if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE)
        {
            NameProvider.packagesFile = NameProvider.reobFile;
            NameProvider.classesFile = NameProvider.reobFile;
            NameProvider.methodsFile = NameProvider.reobFile;
            NameProvider.fieldsFile = NameProvider.reobFile;
        }

        NameProvider.readPackagesSRG();
        NameProvider.readClassesSRG();
        NameProvider.readMethodsSRG();
        NameProvider.readFieldsSRG();

        NameProvider.updateAllXrefs();
    }

    private static void updateAllXrefs()
    {
        for (PackageEntry entry : NameProvider.packageDefs)
        {
            NameProvider.packagesObf2Deobf.put(entry.obfName, entry);
            NameProvider.packagesDeobf2Obf.put(entry.deobfName, entry);
        }

        for (ClassEntry entry : NameProvider.classDefs)
        {
            NameProvider.classesObf2Deobf.put(entry.obfName, entry);
            NameProvider.classesDeobf2Obf.put(entry.deobfName, entry);
        }

        for (MethodEntry entry : NameProvider.methodDefs)
        {
            NameProvider.methodsObf2Deobf.put(entry.obfName + entry.obfDesc, entry);
            NameProvider.methodsDeobf2Obf.put(entry.deobfName + entry.deobfDesc, entry);
        }

        for (FieldEntry entry : NameProvider.fieldDefs)
        {
            NameProvider.fieldsObf2Deobf.put(entry.obfName, entry);
            NameProvider.fieldsDeobf2Obf.put(entry.deobfName, entry);
        }
    }

    private static void readPackagesSRG() throws IOException
    {
        if (NameProvider.packagesFile == null)
        {
            return;
        }

        List<String> lines = NameProvider.readAllLines(NameProvider.packagesFile);

        for (String line : lines)
        {
            String[] lineParts = line.split(" ");
            if ((lineParts.length != 3) || !lineParts[0].startsWith("PK:"))
            {
                continue;
            }

            PackageEntry entry = new PackageEntry();
            if (lineParts[1].equals("."))
            {
                entry.obfName = "";
            }
            else
            {
                entry.obfName = lineParts[1];
            }
            if (lineParts[2].equals("."))
            {
                entry.deobfName = "";
            }
            else
            {
                entry.deobfName = lineParts[2];
            }
            NameProvider.packageDefs.add(entry);
        }
    }

    private static void readClassesSRG() throws IOException
    {
        if (NameProvider.classesFile == null)
        {
            return;
        }

        List<String> lines = NameProvider.readAllLines(NameProvider.classesFile);

        for (String line : lines)
        {
            String[] lineParts = line.split(" ");
            if ((lineParts.length != 3) || !lineParts[0].startsWith("CL:"))
            {
                continue;
            }

            ClassEntry entry = new ClassEntry();
            entry.obfName = lineParts[1];
            entry.deobfName = lineParts[2];
            NameProvider.classDefs.add(entry);
        }
    }

    private static void readMethodsSRG() throws IOException
    {
        if (NameProvider.methodsFile == null)
        {
            return;
        }

        List<String> lines = NameProvider.readAllLines(NameProvider.methodsFile);

        for (String line : lines)
        {
            String[] lineParts = line.split(" ");
            if ((lineParts.length < 4) || !lineParts[0].startsWith("MD:"))
            {
                continue;
            }

            MethodEntry entry = new MethodEntry();
            entry.obfName = lineParts[1];
            entry.obfDesc = lineParts[2];
            entry.deobfName = lineParts[3];
            if (lineParts.length > 4)
            {
                entry.deobfDesc = lineParts[4];
            }
            NameProvider.methodDefs.add(entry);
        }
    }

    private static void readFieldsSRG() throws IOException
    {
        if (NameProvider.fieldsFile == null)
        {
            return;
        }

        List<String> lines = NameProvider.readAllLines(NameProvider.fieldsFile);

        for (String line : lines)
        {
            String[] lineParts = line.split(" ");
            if ((lineParts.length != 3) || !lineParts[0].startsWith("FD:"))
            {
                continue;
            }

            FieldEntry entry = new FieldEntry();
            entry.obfName = lineParts[1];
            entry.deobfName = lineParts[2];
            NameProvider.fieldDefs.add(entry);
        }
    }

    private static List<String> readAllLines(File file) throws IOException
    {
        List<String> lines = new ArrayList<String>();

        FileReader fileReader = null;
        BufferedReader reader = null;
        try
        {
            fileReader = new FileReader(file);
            reader = new BufferedReader(fileReader);

            String line = reader.readLine();
            while (line != null)
            {
                lines.add(line);
                line = reader.readLine();
            }
        }
        finally
        {
            try
            {
                if (reader != null)
                {
                    reader.close();
                }
                if (fileReader != null)
                {
                    fileReader.close();
                }
            }
            catch (IOException e)
            {
                // ignore
            }
        }

        return lines;
    }

    public static void log(String text)
    {
        if (!NameProvider.quiet)
        {
            System.out.println(text);
        }

        File log = null;
        if (NameProvider.currentMode == NameProvider.DEOBFUSCATION_MODE)
        {
            log = NameProvider.npLog;
        }
        else if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE)
        {
            log = NameProvider.roLog;
        }

        if (log == null)
        {
            return;
        }

        FileWriter fileWriter = null;
        BufferedWriter writer = null;
        try
        {
            fileWriter = new FileWriter(log, true);
            writer = new BufferedWriter(fileWriter);

            writer.write(text);
            writer.newLine();

            writer.flush();
        }
        catch (IOException e)
        {
            return;
        }
        finally
        {
            try
            {
                if (writer != null)
                {
                    writer.close();
                }
                if (fileWriter != null)
                {
                    fileWriter.close();
                }
            }
            catch (IOException e)
            {
                // ignore
            }
        }
    }

    public static String getNewTreeItemName(TreeItem ti) throws ClassFileException
    {
        if (ti instanceof Pk)
        {
            return NameProvider.getNewPackageName((Pk)ti);
        }
        else if (ti instanceof Cl)
        {
            return NameProvider.getNewClassName((Cl)ti);
        }
        else if (ti instanceof Md)
        {
            return NameProvider.getNewMethodName((Md)ti);
        }
        else if (ti instanceof Fd)
        {
            return NameProvider.getNewFieldName((Fd)ti);
        }
        else
        {
            NameProvider.log("# Warning: trying to rename unknown type " + ti.getFullInName());
        }
        return null;
    }

    public static String getNewPackageName(Pk pk)
    {
        if (NameProvider.currentMode == NameProvider.CHANGE_NOTHING_MODE)
        {
            pk.setOutput();
            return null;
        }

        if (NameProvider.currentMode == NameProvider.CLASSIC_MODE)
        {
            String packageName = "p_" + (++NameProvider.uniqueStart) + "_" + pk.getInName();
            pk.setOutput();
            return packageName;
        }

        String packageName = pk.getFullInName();
        boolean known = NameProvider.packageNameLookup.containsKey(packageName);

        if (!NameProvider.isInProtectedPackage(packageName))
        {
            if (NameProvider.currentMode == NameProvider.DEOBFUSCATION_MODE)
            {
                if (NameProvider.packagesObf2Deobf.containsKey(pk.getFullInName()))
                {
                    String deobfName = NameProvider.packagesObf2Deobf.get(pk.getFullInName()).deobfName;
                    packageName = deobfName;
                    pk.setRepackageName(packageName);
                }
                else
                {
                    // check if parent got remapped
                    TreeItem parent = pk.getParent();
                    if ((parent != null) && (parent instanceof Pk) && (parent.getParent() != null))
                    {
                        packageName = NameProvider.getNewPackageName(parent.getFullOutName()) + pk.getOutName();
                        pk.setRepackageName(packageName);
                    }
                }
            }
            else if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE)
            {
                if (NameProvider.packagesDeobf2Obf.containsKey(pk.getFullInName()))
                {
                    String obfName = NameProvider.packagesDeobf2Obf.get(pk.getFullInName()).obfName;
                    packageName = obfName;
                    pk.setRepackageName(packageName);
                }
                else
                {
                    // check if parent got remapped
                    TreeItem parent = pk.getParent();
                    if ((parent != null) && (parent instanceof Pk) && (parent.getParent() != null))
                    {
                        packageName = NameProvider.getNewPackageName(parent.getFullOutName()) + pk.getOutName();
                        pk.setRepackageName(packageName);
                    }
                }
            }

            if (NameProvider.repackage)
            {
                packageName = ".";
                pk.setRepackageName(packageName);
            }

            NameProvider.packageNameLookup.put(pk.getFullInName(), packageName);
        }

        pk.setOutName(packageName);

        String inName = pk.getFullInName(true);

        if (!NameProvider.isInProtectedPackage(inName + "/") && !known)
        {
            pk.setOutput();
        }

        return packageName;
    }

    private static String getNewPackageName(String pkgName)
    {
        if (NameProvider.packageNameLookup.containsKey(pkgName))
        {
            pkgName = NameProvider.packageNameLookup.get(pkgName);
        }

        if (pkgName.equals(""))
        {
            return "";
        }

        return pkgName + "/";
    }

    public static String getNewClassName(Cl cl)
    {
        if (NameProvider.currentMode == NameProvider.CHANGE_NOTHING_MODE)
        {
            cl.setOutput();
            return null;
        }

        if (NameProvider.currentMode == NameProvider.CLASSIC_MODE)
        {
            String newClassName = "C_" + (++NameProvider.uniqueStart) + "_" + cl.getInName();
            cl.setOutput();
            return newClassName;
        }

        String newClassName = null;

        if (NameProvider.currentMode == NameProvider.DEOBFUSCATION_MODE)
        {
            if (NameProvider.classesObf2Deobf.containsKey(cl.getFullInName()))
            {
                newClassName = NameProvider.classesObf2Deobf.get(cl.getFullInName()).deobfName;
                if (!NameProvider.repackage)
                {
                    newClassName = NameProvider.getShortName(newClassName);
                }
            }
            else
            {
                if (!NameProvider.isInProtectedPackage(cl.getFullInName()))
                {
                    if (NameProvider.uniqueStart > 0)
                    {
                        newClassName = "C_" + (NameProvider.uniqueStart++) + "_" + cl.getInName();
                    }
                }
            }
        }
        else if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE)
        {
            if (NameProvider.classesDeobf2Obf.containsKey(cl.getFullInName()))
            {
                newClassName = NameProvider.classesDeobf2Obf.get(cl.getFullInName()).obfName;
                if (!NameProvider.repackage)
                {
                    newClassName = NameProvider.getShortName(newClassName);
                }
            }
        }

        if (!NameProvider.isInProtectedPackage(cl.getFullInName()))
        {
            cl.setOutput();
        }

        return newClassName;
    }

    public static String getNewMethodName(Md md) throws ClassFileException
    {
        if (NameProvider.currentMode == NameProvider.CHANGE_NOTHING_MODE)
        {
            md.setOutput();
            return null;
        }

        if (NameProvider.currentMode == NameProvider.CLASSIC_MODE)
        {
            String newMethodName = "func_" + (++NameProvider.uniqueStart) + "_" + md.getInName();
            md.setOutput();
            return newMethodName;
        }

        String newMethodName = null;

        if (NameProvider.currentMode == NameProvider.DEOBFUSCATION_MODE)
        {
            if (NameProvider.methodsObf2Deobf.containsKey(md.getFullInName() + md.getDescriptor()))
            {
                newMethodName = NameProvider.getShortName(NameProvider.methodsObf2Deobf.get(md.getFullInName()
                    + md.getDescriptor()).deobfName);
            }
            else
            {
                if (!NameProvider.isInProtectedPackage(md.getFullInName()))
                {
                    if (NameProvider.uniqueStart > 0)
                    {
                        newMethodName = "func_" + (NameProvider.uniqueStart++) + "_" + md.getInName();
                    }
                }
            }
        }
        else if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE)
        {
            if (NameProvider.methodsDeobf2Obf.containsKey(md.getFullInName() + md.getDescriptor()))
            {
                newMethodName = NameProvider.getShortName(NameProvider.methodsDeobf2Obf.get(md.getFullInName()
                    + md.getDescriptor()).obfName);
            }
            else
            {
                Cl cls = (Cl)md.getParent();
                Iterator<Cl> children = cls.getDownClasses();

                Md tmpMd = new Md(cls, md.isSynthetic(), md.getInName(), md.getDescriptor(), md.getModifiers());

                boolean goingDown = false;
                do
                {
                    tmpMd.setParent(cls);
                    if (NameProvider.methodsDeobf2Obf.containsKey(tmpMd.getFullInName() + tmpMd.getDescriptor()))
                    {
                        newMethodName = NameProvider.getShortName(NameProvider.methodsDeobf2Obf.get(tmpMd.getFullInName()
                            + tmpMd.getDescriptor()).obfName);
                        break;
                    }

                    boolean found = false;
                    try
                    {
                        for (Cl iface : cls.getSuperInterfaces())
                        {
                            tmpMd.setParent(iface);
                            if (NameProvider.methodsDeobf2Obf.containsKey(tmpMd.getFullInName() + tmpMd.getDescriptor()))
                            {
                                newMethodName = NameProvider.getShortName(NameProvider.methodsDeobf2Obf.get(tmpMd.getFullInName()
                                    + tmpMd.getDescriptor()).obfName);
                                found = true;
                            }
                        }
                    }
                    catch (ClassFileException e)
                    {
                        // ignore
                    }

                    if (found)
                    {
                        break;
                    }

                    if (!goingDown)
                    {
                        try
                        {
                            cls = cls.getSuperCl();
                        }
                        catch (ClassFileException e)
                        {
                            // ignore
                            cls = null;
                        }

                        if (cls == null)
                        {
                            goingDown = true;
                        }
                    }

                    if (goingDown)
                    {
                        if (children.hasNext())
                        {
                            cls = children.next();
                        }
                        else
                        {
                            cls = null;
                        }
                    }
                } while (cls != null);
            }
        }

        if (!NameProvider.isInProtectedPackage(md.getFullInName()))
        {
            md.setOutput();
        }

        return newMethodName;
    }

    public static String getNewFieldName(Fd fd)
    {
        if (NameProvider.currentMode == NameProvider.CHANGE_NOTHING_MODE)
        {
            fd.setOutput();
            return null;
        }

        if (NameProvider.currentMode == NameProvider.CLASSIC_MODE)
        {
            String newFieldName = "field_" + (++NameProvider.uniqueStart) + "_" + fd.getInName();
            fd.setOutput();
            return newFieldName;
        }

        String newFieldName = null;

        if (NameProvider.currentMode == NameProvider.DEOBFUSCATION_MODE)
        {
            if (NameProvider.fieldsObf2Deobf.containsKey(fd.getFullInName()))
            {
                newFieldName = NameProvider.getShortName(NameProvider.fieldsObf2Deobf.get(fd.getFullInName()).deobfName);
            }
            else
            {
                if (!NameProvider.isInProtectedPackage(fd.getFullInName()))
                {
                    if (NameProvider.uniqueStart > 0)
                    {
                        newFieldName = "field_" + (NameProvider.uniqueStart++) + "_" + fd.getInName();
                    }
                }
            }
        }
        else if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE)
        {
            if (NameProvider.fieldsDeobf2Obf.containsKey(fd.getFullInName()))
            {
                newFieldName = NameProvider.getShortName(NameProvider.fieldsDeobf2Obf.get(fd.getFullInName()).obfName);
            }
        }

        if (!NameProvider.isInProtectedPackage(fd.getFullInName()))
        {
            fd.setOutput();
        }

        return newFieldName;
    }

    private static boolean isInProtectedPackage(String fullInName)
    {
        for (String pkg : NameProvider.protectedPackages)
        {
            if (fullInName.startsWith(pkg))
            {
                return true;
            }
        }
        return false;
    }

    private static String getShortName(String name)
    {
        if ((name != null) && name.contains("/"))
        {
            name = name.substring(name.lastIndexOf('/') + 1);
        }

        return name;
    }

    public static void outputPackage(Pk pk)
    {
        NameProvider.log("PK: " + pk.getFullInName(true) + " " + pk.getFullOutName(true));
    }

    public static void outputClass(Cl cl)
    {
        NameProvider.log("CL: " + cl.getFullInName(true) + " " + cl.getFullOutName(true));
    }

    public static void outputMethod(Md md)
    {
        NameProvider.log("MD: " + md.getFullInName(true) + " " + md.getDescriptor() + " " + md.getFullOutName(true) + " "
            + md.getOutDescriptor());
    }

    public static void outputField(Fd fd)
    {
        NameProvider.log("FD: " + fd.getFullInName(true) + " " + fd.getFullOutName(true));
    }
}

class PackageEntry
{
    public String obfName;
    public String deobfName;
}

class ClassEntry
{
    public String obfName;
    public String deobfName;
}

class MethodEntry
{
    public String obfName;
    public String obfDesc;
    public String deobfName;
    public String deobfDesc;
}

class FieldEntry
{
    public String obfName;
    public String deobfName;
}
