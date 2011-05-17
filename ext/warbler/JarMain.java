/**
 * Copyright (c) 2010-2011 Engine Yard, Inc.
 * Copyright (c) 2007-2009 Sun Microsystems, Inc.
 * This source code is available under the MIT license.
 * See the file LICENSE.txt for details.
 */

package warbler;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarMain extends WarblerSupport {
    public JarMain(String[] args) throws Exception {
        super(args);
        Runtime.getRuntime().addShutdownHook(new Thread(this));
    }

    private URL[] extractJRuby() throws Exception {
        JarFile jf = new JarFile(this.archive_file);
        List<String> jarNames = new ArrayList<String>();
        for (Enumeration<JarEntry> eje = jf.entries(); eje.hasMoreElements(); ) {
            String name = eje.nextElement().getName();
            if (name.startsWith("META-INF/lib") && name.endsWith(".jar")) {
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
                    "require 'META-INF/init.rb'\n" +
                    "require 'META-INF/main.rb'\n" +
                    "0\n" +
                    "rescue SystemExit => e\n" +
                    "e.status\n" +
                    "end"
                })).intValue();
    }

    private int start() throws Exception {
        URL[] u = extractJRuby();
        return launchJRuby(u);
    }

    public static void main(String[] args) {
        try {
            int exit = new JarMain(args).start();
            System.exit(exit);
        } catch (Exception e) {
            Throwable t = e;
            while (t.getCause() != null && t.getCause() != t) {
                t = t.getCause();
            }

            if (isDebug()) {
                t.printStackTrace();
            }
            System.exit(1);
        }
    }
}
