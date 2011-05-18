/**
 * Copyright (c) 2010-2011 Engine Yard, Inc.
 * Copyright (c) 2007-2009 Sun Microsystems, Inc.
 * This source code is available under the MIT license.
 * See the file LICENSE.txt for details.
 */

package warbler;

import java.net.URLClassLoader;
import java.net.URL;
import java.lang.reflect.Method;
import java.io.IOException;
import java.io.File;
import java.util.Arrays;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class WarMain extends WarblerSupport {
    public static final String WINSTONE_JAR = "/WEB-INF/winstone.jar";

    private File webroot;

    public WarMain(String[] args) throws Exception {
        super(args);
        this.webroot = new File(this.extractRoot, "webroot");
        Runtime.getRuntime().addShutdownHook(new Thread(this));
    }

    private URL extractWinstone() throws Exception {
        return extractJar(WINSTONE_JAR);
    }

    private void launchWinstone(URL jar) throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[] {jar});
        Class klass = Class.forName("winstone.Launcher", true, loader);
        Method main = klass.getDeclaredMethod("main", new Class[] {String[].class});
        String[] newargs = new String[args.length + 3];
        newargs[0] = "--warfile=" + archive_file;
        newargs[1] = "--webroot=" + webroot;
        newargs[2] = "--directoryListings=false";
        System.arraycopy(args, 0, newargs, 3, args.length);
        debug("invoking Winstone with: " + Arrays.deepToString(newargs));
        main.invoke(null, new Object[] {newargs});
    }

    private void start() throws Exception {
        URL u = extractWinstone();
        launchWinstone(u);
    }

    private URL[] extractJRuby() throws Exception {
        JarFile jf = new JarFile(this.archive_file);
        List<String> jarNames = new ArrayList<String>();
        for (Enumeration<JarEntry> eje = jf.entries(); eje.hasMoreElements(); ) {
            String name = eje.nextElement().getName();
            // change
            if (name.startsWith("WEB-INF/lib") && name.endsWith(".jar")) {
                jarNames.add("/" + name);
            }
        }

        List<URL> urls = new ArrayList<URL>();
        for (String name : jarNames) {
            urls.add(extractJar(name));
        }

        return (URL[]) urls.toArray(new URL[urls.size()]);
    }

    private int launchJRuby(URL[] jars, String binary_path) throws Exception {
        System.setProperty("org.jruby.embed.class.path", "");
        URLClassLoader loader = new URLClassLoader(jars);
        Class scriptingContainerClass = Class.forName("org.jruby.embed.ScriptingContainer", true, loader);
        Object scriptingContainer = scriptingContainerClass.newInstance();

        Method argv = scriptingContainerClass.getDeclaredMethod("setArgv", new Class[] {String[].class});
        argv.invoke(scriptingContainer, new Object[] {args});
        Method setClassLoader = scriptingContainerClass.getDeclaredMethod("setClassLoader", new Class[] {ClassLoader.class});
        setClassLoader.invoke(scriptingContainer, new Object[] {loader});
        debug("invoking " + archive_file + " with: " + Arrays.deepToString(args));

        Method runScriptlet = scriptingContainerClass.getDeclaredMethod("runScriptlet", new Class[] {String.class});
        return ((Number) runScriptlet.invoke(scriptingContainer, new Object[] {
                    "begin\n" +
                    "puts $LOAD_PATH.join(':')\n" +
                    "require 'META-INF/init.rb'\n" +
                    //                    "require '" + binary_path + "'\n" + // change
                    "require 'META-INF/main.rb'\n" +
                    "0\n" +
                    "rescue SystemExit => e\n" +
                    "e.status\n" +
                    "end"
                })).intValue();
    }

    private int startBinary(String binary) throws Exception {
        debug("starting the binary '" + binary + "'");
        URL[] u = extractJRuby();
        return launchJRuby(u, binary);
    }

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                new WarMain(args).start();
            } else {
                new WarMain(args).startBinary(args[0]);
            }
        } catch (Exception e) {
            System.err.println("error: " + e.toString());
            if (isDebug()) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }
}

