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

