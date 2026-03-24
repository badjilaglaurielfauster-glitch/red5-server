/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2023 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.classloading;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

/**
 * Class used to get the Servlet Class loader. The class loader returned is a child first class loader.
 *
 * <br>
 * <i>This class is based on original code from the XINS project, by Anthony Goubard (anthony.goubard@japplis.com)</i>
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public final class ClassLoaderBuilder {

    /*
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6500212 http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6516909 http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4976356
     */

    /**
     * Load the Servlet code from the WAR file and use the current classpath for the libraries.
     */
    public static final int USE_CLASSPATH_LIB = 1;

    /**
     * Load the servlet code from the WAR file and try to find the libraries in the common red5 lib directory.
     */
    public static final int USE_RED5_LIB = 2;

    /**
     * Load the servlet code and the libraries from the WAR file. This may take some time as the libraries need to be extracted from the WAR
     * file.
     */
    public static final int USE_WAR_LIB = 3;

    /**
     * Default build uses Red5 common lib without a parent classloader.
     *
     * @return the class loader
     */
    public static ClassLoader build() {
        return ClassLoaderBuilder.build(null, USE_RED5_LIB, null);
    }

    /**
     * Adds a Path as URL into the set.
     *
     * @param urls the set of URLs
     * @param path the file to convert into URL
     */
    private static void addUrl(Set<URL> urls, Path path) throws MalformedURLException {
        try {
            urls.add(path.toUri().toURL());
        } catch (MalformedURLException e) {
            throw new MalformedURLException("Exception malformated URL" + e.getMessage());
        }
    }

    /**
     * Adds all JAR files from a directory into the URL list.
     *
     * @param urls the set of URLs
     * @param dir the directory to scan
     */
    private static void addJarFiles(Set<URL> urls, Path dir) throws IOException {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".jar")) {
                        addUrl(urls, file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Adds the main Red5 server JAR to the classpath.
     */
    private static void addRed5Jar(Set<URL> urlList, Path homeDir) {
        try {
            Path red5jar = homeDir.resolve("red5-server.jar");

            if (!Files.exists(red5jar)) {
                System.out.println("Red5 server jar was not found, using fallback");
                red5jar = homeDir.resolve("red5.jar");
            } else {
                System.out.println("Red5 server jar was found");
            }

            urlList.add(red5jar.toUri().toURL());

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds all libraries from the lib directory.
     */
    private static void addLibJars(Set<URL> urlList, Path homeDir) throws IOException {
        Path libDir = homeDir.resolve("lib");
        addJarFiles(urlList, libDir);
    }

    /**
     * Adds the configuration directory.
     */
    private static void addConfig(Set<URL> urlList, Path homeDir) {
        try {
            String conf = System.getProperty("red5.config_root");
            Path confDir = (conf != null) ? Paths.get(conf) : homeDir.resolve("conf");

            urlList.add(confDir.toUri().toURL());

        } catch (MalformedURLException e) {
            System.err.printf("Exception %s\n", e);
        }
    }

    /**
     * Adds plugins directory and its JARs.
     */
    private static void addPlugins(Set<URL> urlList, Path homeDir) {
        try {
            String pluginsPath = System.getProperty("red5.plugins_root");

            if (pluginsPath == null) {
                pluginsPath = homeDir + "/plugins";
                System.setProperty("red5.plugins_root", pluginsPath);
            }

            Path pluginsDir = Files.createDirectories(Paths.get(pluginsPath));

            urlList.add(pluginsDir.toUri().toURL());
            addJarFiles(urlList, pluginsDir);

        } catch (Exception e) {
            System.err.printf("Exception %s\n", e);
        }
    }

    private static void addPath(Set<URL> urls, Path path) {
        if (path != null) {
            try {
                urls.add(path.toUri().toURL());
                URL classesURL = URI.create("jar:file:" + path.toFile().getAbsolutePath().replace(File.separatorChar, '/') + "!/WEB-INF/classes/").toURL();
                urls.add(classesURL);
            } catch (Exception e) {
                System.err.printf("Erreur lors de l'ajout du path : %s\n", e.getMessage());
            }
        }
    }

    private static void addClasspath(Set<URL> urls) {
        String classPath = System.getProperty("java.class.path");
        StringTokenizer stClassPath = new StringTokenizer(classPath, File.pathSeparator);

        while (stClassPath.hasMoreTokens()) {
            String nextPath = stClassPath.nextToken();
            try {
                urls.add(Paths.get(nextPath).toUri().toURL());
            } catch (MalformedURLException e) {
                System.err.printf("Exception sur le classpath : %s\n", e.getMessage());
            }
        }
    }

    private static void addWarLibs(Set<URL> urls, Path path) {
        if (path == null)
            return;

        if (Files.isDirectory(path)) {
            // Cas 1 : C'est un répertoire
            try {
                Path libDir = path.resolve("WEB-INF").resolve("lib");
                if (Files.exists(libDir)) {
                    addJarFiles(urls, libDir);
                }
            } catch (IOException e) {
                System.err.printf("Erreur lecture lib dossier : %s\n", e.getMessage());
            }
        } else {
            // Cas 2 : C'est un fichier JAR/WAR,
            try (InputStream is = Files.newInputStream(path); JarInputStream jarStream = new JarInputStream(is)) {

                JarEntry entry = jarStream.getNextJarEntry();
                while (entry != null) {
                    String name = entry.getName();
                    if (name.startsWith("WEB-INF/lib/") && name.endsWith(".jar")) {
                        Path tempJar = unpack(jarStream, name);
                        urls.add(tempJar.toUri().toURL());
                    }
                    entry = jarStream.getNextJarEntry();
                }
            } catch (IOException e) {
                System.err.printf("Erreur extraction WAR : %s\n", e.getMessage());
            }
        }
    }

    /**
     * Collects all URLs depending on the selected mode.
     *
     * @param path base path (war or directory)
     * @param mode loading mode
     * @return set of URLs to include in the classloader
     */
    private static Set<URL> collectUrls(Path path, int mode) {
        Set<URL> urlList = new HashSet<>();
        addPath(urlList, path);
        
        if (mode == USE_CLASSPATH_LIB) {
            addClasspath(urlList);
        }
        if (mode == USE_WAR_LIB) {
            addWarLibs(urlList, path);
        }

        return urlList;
    }

    /**
     * Resolves the Red5 home directory from system properties or environment.
     *
     * @return the Red5 home path
     */
    private static Path resolveRed5Home() {
        String home = System.getProperty("red5.root", System.getenv("RED5_HOME"));

        if (home != null) {
            return Paths.get(home);
        }

        String classLocation = ClassLoaderBuilder.class.getProtectionDomain().getCodeSource().getLocation().toString();

        String fallbackHome = classLocation.substring(0, classLocation.lastIndexOf('/'));

        return Paths.get(fallbackHome);
    }

    /**
     * Collects URLs when using RED5 mode.
     */
    private static void collectRed5Urls(Set<URL> urlList) {
        Path homeDir = resolveRed5Home();
        try {
            addRed5Jar(urlList, homeDir);
            addLibJars(urlList, homeDir);
            addConfig(urlList, homeDir);
            addPlugins(urlList, homeDir);
            scrubURLList(urlList);
        } catch (IOException e) {
            System.err.printf("Erreur lors de la collecte des JARs Red5: %s\n", e.getMessage());
        }
    }

    /**
     * Gets a class loader based on mode.
     *
     * @param path
     *            the directory or file containing classes
     * @param mode
     *            the mode in which the servlet should be loaded. The possible values are
     *
     *            <pre>
     * USE_CURRENT_CLASSPATH, USE_CLASSPATH_LIB, USE_WAR_LIB
     * </pre>
     * @param parent
     *            the parent class loader or null if you want the current threads class loader
     * @return the Class loader to use to load the required class(es)
     */
    public static ClassLoader build(Path path, int mode, ClassLoader parent) {
        Set<URL> urlList = new HashSet<>();
        ClassLoader loader = null;

        if (mode == USE_RED5_LIB) {
            collectRed5Urls(urlList);
            URL[] urls = urlList.toArray(new URL[0]);

            loader = (parent == null) ? new URLClassLoader(urls) : new URLClassLoader(urls, parent);
        } else {
            urlList = collectUrls(path, mode);
            URL[] urls = urlList.toArray(new URL[0]);

            loader = new ChildFirstClassLoader(urls, parent);
        }

        Thread.currentThread().setContextClassLoader(loader);
        return loader;
    }

    /**
     * Unpack the specified entry from the JAR file.
     *
     * @param jarStream
     *            The input stream of the JAR file positioned at the entry
     * @param entryName
     *            The name of the entry to extract
     * @return The extracted file. The created file is a temporary file in the temporary directory
     * @throws IOException
     *             if the JAR file cannot be read or is incorrect
     */
    private static Path unpack(JarInputStream jarStream, String entryName) throws IOException {
        String libName = entryName.substring(entryName.lastIndexOf('/') + 1, entryName.length() - 4);
        Path tempJarFile = Files.createTempFile("tmp_" + libName, ".jar");
        try (OutputStream os = Files.newOutputStream(tempJarFile)) {
            // Transfer bytes from the JAR file to the output file
            byte[] buf = new byte[1024];
            int len;
            while ((len = jarStream.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
        }
        return tempJarFile;
    }

    /**
     * Determines whether a library should be removed based on its name.
     *
     * @param name the parsed library name
     * @return true if the library should be removed
     */
    private static boolean shouldRemove(String name) {
        return name.isEmpty() || name.startsWith("grobo") || name.startsWith("junit") || name.startsWith("ivy") || name.contains("javadoc") || name.contains("sources") || name.endsWith("-");
    }

    /**
     * Extracts the prefix of a library name (used to identify similar libraries).
     *
     * Example:
     *   spring-core-5.0 → spring
     *
     * @param name the library name
     * @return the prefix
     */
    private static String getPrefix(String name) {
        int dash = name.indexOf('-');
        return dash != -1 ? name.substring(0, dash) : name.substring(0, Math.min(3, name.length()));
    }

    /**
     * Checks whether two libraries belong to the same family based on their prefix.
     *
     * @param name1 first library name
     * @param name2 second library name
     * @return true if both libraries are considered the same
     */
    private static boolean isSameLibrary(String name1, String name2) {
        return getPrefix(name1).equals(getPrefix(name2));
    }

    /**
     * Extracts the version string from a library name.
     *
     * Example:
     *   spring-core-5.0.1 → 5.0.1
     *
     * @param name the library name
     * @return the version string
     */
    private static String extractVersion(String name) {
        int lastDash = name.lastIndexOf('-');
        return lastDash != -1 ? name.substring(lastDash + 1) : "";
    }

    /**
     * Compares two version strings.
     *
     * Example:
     *   1.2.10 > 1.2.3
     *
     * @param v1 first version
     * @param v2 second version
     * @return positive if v1 > v2, negative if v1 < v2, 0 if equal
     */
    private static int compareVersions(String v1, String v2) {
        String[] a = v1.split("\\.");
        String[] b = v2.split("\\.");

        int max = Math.max(a.length, b.length);

        for (int i = 0; i < max; i++) {
            int n1 = i < a.length ? Integer.parseInt(a[i]) : 0;
            int n2 = i < b.length ? Integer.parseInt(b[i]) : 0;

            if (n1 != n2) {
                return n1 - n2;
            }
        }
        return 0;
    }

    /**
     * Removes older versions of libraries from the given list.
     *
     * The method:
     * - filters invalid libraries
     * - compares libraries with the same prefix
     * - keeps only the most recent version
     *
     * @param list collection of library URLs
     */
    private static void scrubURLList(Collection<URL> list) {
        Set<URL> toRemove = new HashSet<>();

        for (URL u1 : list) {
            String name1 = parseUrl(u1);

            if (shouldRemove(name1)) {
                toRemove.add(u1);
                continue;
            }

            for (URL u2 : list) {
                if (u1.equals(u2))
                    continue;

                String name2 = parseUrl(u2);

                if (!isSameLibrary(name1, name2))
                    continue;

                String v1 = extractVersion(name1);
                String v2 = extractVersion(name2);

                if (v1.isEmpty() || v2.isEmpty())
                    continue;

                if (compareVersions(v1, v2) >= 0) {
                    toRemove.add(u2);
                } else {
                    toRemove.add(u1);
                }
            }
        }

        list.removeAll(toRemove);
    }

    /**
     * Parses url and returns the jar filename stripped of the ending .jar
     *
     * @param url
     * @return
     */
    private static String parseUrl(URL url) {
        String external = url.toExternalForm().toLowerCase();
        //System.out.printf("parseUrl %s%n", external);
        // get everything after the last slash
        String[] parts = external.split("/");
        // last part
        String libName = parts[parts.length - 1];
        // strip .jar
        libName = libName.substring(0, libName.length() - 4);
        return libName;
    }

    private static String deleteAny(String str, String removalChars) {
        StringBuilder sb = new StringBuilder(str);
        // System.out.println("Before alpha delete: " + sb.toString());
        String[] chars = removalChars.split("");
        // System.out.println("Chars length: " + chars.length);
        for (String c : chars) {
            int index = -1;
            while ((index = sb.indexOf(c)) > 0) {
                sb.deleteCharAt(index);
            }
        }
        // System.out.println("After alpha delete: " + sb.toString());
        return sb.toString();
    }
}
