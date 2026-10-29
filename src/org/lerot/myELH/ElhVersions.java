package org.lerot.myELH;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ELH versions are files named <name>_v<N>.elh in the same folder. Files
// named any other way (library25b.xml) count as version 0 of their name.
public final class ElhVersions
{
    private static final Pattern VERSIONED = Pattern.compile("(.+)_v(\\d+)\\.(elh|event)", Pattern.CASE_INSENSITIVE);

    private ElhVersions()
    {
    }

    // the ELH name the file is a version of: library_v3.elh -> library
    public static String baseName(File file)
    {
        Matcher m = VERSIONED.matcher(file.getName());
        if (m.matches())
        {
            return m.group(1);
        }
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    public static int version(File file)
    {
        Matcher m = VERSIONED.matcher(file.getName());
        return m.matches() ? Integer.parseInt(m.group(2)) : 0;
    }

    public static boolean isVersioned(File file)
    {
        return VERSIONED.matcher(file.getName()).matches();
    }

    public static File versionFile(File folder, String basename, int version)
    {
        return new File(folder, basename + "_v" + version + ".elh");
    }

    // the version after the highest one of basename in folder
    public static File nextVersion(File folder, String basename)
    {
        File latest = latestVersions(folder).get(basename);
        return versionFile(folder, basename, latest == null ? 1 : version(latest) + 1);
    }

    // each ELH name in the folder, with the file holding its highest version
    public static Map<String, File> latestVersions(File folder)
    {
        Map<String, File> latest = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        File[] files = folder.listFiles(ElhVersions::isElhFile);
        if (files != null)
        {
            for (File file : files)
            {
                latest.merge(baseName(file), file, (a, b) -> version(b) > version(a) ? b : a);
            }
        }
        return latest;
    }

    // every version of the ELH name in the folder
    public static List<File> allVersions(File folder, String basename)
    {
        File[] files = folder.listFiles(f -> isElhFile(f) && baseName(f).equalsIgnoreCase(basename));
        return files == null ? List.of() : List.of(files);
    }

    public static int countVersions(File folder, String basename)
    {
        return allVersions(folder, basename).size();
    }

    // The files of a project entity or event: <basename>_v<N><extension>,
    // with extension ".elh" or ".event". Only versioned names count here.

    public static File versionFile(File folder, String basename, int version, String extension)
    {
        return new File(folder, basename + "_v" + version + extension);
    }

    // every version of basename with the extension, lowest first
    public static List<File> versions(File folder, String basename, String extension)
    {
        Pattern named = Pattern.compile(Pattern.quote(basename) + "_v(\\d+)" + Pattern.quote(extension),
                Pattern.CASE_INSENSITIVE);
        File[] files = folder.listFiles(f -> f.isFile() && named.matcher(f.getName()).matches());
        if (files == null) return List.of();
        return java.util.Arrays.stream(files)
                .sorted(java.util.Comparator.comparingInt(ElhVersions::version))
                .toList();
    }

    // the version after the highest one of basename with the extension
    public static File nextVersion(File folder, String basename, String extension)
    {
        List<File> existing = versions(folder, basename, extension);
        int next = existing.isEmpty() ? 1 : version(existing.getLast()) + 1;
        return versionFile(folder, basename, next, extension);
    }

    // a name made safe to use in a file name
    public static String fileBaseName(String name)
    {
        String safe = name.trim().replaceAll("[/\\\\:*?\"<>|]", "_");
        return safe.isEmpty() ? "unnamed" : safe;
    }

    static boolean isElhFile(File file)
    {
        String name = file.getName().toLowerCase();
        return file.isFile() && (name.endsWith(".elh") || name.endsWith(".xml"))
                && !file.getName().equals(Project.PROJECTFILE);
    }
}
