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

import java.util.HashMap;
import java.util.Map;

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

    private void startWinstone() throws Exception {
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

    private int launchJRuby(URL[] jars) throws Exception {
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
                    "  require 'META-INF/init.rb'\n" +
                    "  require 'rubygems'\n" +
                    "  require 'META-INF/main.rb'\n" +
                    "  0\n" +
                    "rescue SystemExit => e\n" +
                    "  e.status\n" +
                    "end"
                })).intValue();
    }

    private int startBinary() throws Exception {
        URL[] u = extractJRuby();
        return launchJRuby(u);
    }

    private Map<String, String> findExecutables() throws Exception {
        HashMap<String, String> paths = new HashMap<String, String>();
        JarFile jf = new JarFile(this.archive_file);

        for (Enumeration<JarEntry> eje = jf.entries(); eje.hasMoreElements(); ) {
            String name = eje.nextElement().getName();
            String[] parts = name.split("/");

            if (parts.length < 2) continue;
            if (parts[parts.length-2].equals("bin")) {
                debug("Adding binary " + parts[parts.length-1] + " (" + name + ")");
                paths.put(parts[parts.length-1], name);
            }
        }

        return paths;
    }

    private void start() throws Exception {
        if (args.length == 0) {
            startWinstone();
        } else {
            if (args[0].equals("server")) {
                String[] remaining_args = new String[args.length-1];
                System.arraycopy(args, 1, remaining_args, 0, args.length-1);
                this.args = remaining_args;

                startWinstone();
            } else {
                Map<String, String> paths = findExecutables();

                if (paths.containsKey(args[0])) {
                    args[0] = paths.get(args[0]);
                    startBinary();
                } else {
                    startWinstone();
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            new WarMain(args).start();
        } catch (Exception e) {
            System.err.println("error: " + e.toString());
            if (isDebug()) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }
}

