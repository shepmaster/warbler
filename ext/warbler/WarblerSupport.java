/**
 * Copyright (c) 2010-2011 Engine Yard, Inc.
 * Copyright (c) 2007-2009 Sun Microsystems, Inc.
 * This source code is available under the MIT license.
 * See the file LICENSE.txt for details.
 */

package warbler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

public class WarblerSupport implements Runnable {
    protected File extractRoot;
    protected String[] args;
    protected String main;
    protected String path;
    protected boolean debug;
    protected String archive_file;

    WarblerSupport(String[] args) throws Exception
    {
        this.args = args;
        this.extractRoot = File.createTempFile("warbler", "extract");
        this.extractRoot.delete();
        this.extractRoot.mkdirs();
        Class klass = getClass();
        this.main = "/" + klass.getName().replace('.', '/') + ".class";
        URL mainClass = klass.getResource(this.main);
        this.path = mainClass.toURI().getSchemeSpecificPart();
        this.debug = isDebug();
        this.archive_file = this.path.replace("!" + this.main, "").replace("file:", "");
    }

    protected URL extractJar(String jarpath) throws Exception {
        InputStream jarStream = new URL("jar:" + path.replace(main, jarpath)).openStream();
        String jarname = jarpath.substring(jarpath.lastIndexOf("/") + 1, jarpath.lastIndexOf("."));
        File jarFile = new File(extractRoot, jarname + ".jar");
        jarFile.deleteOnExit();
        FileOutputStream outStream = new FileOutputStream(jarFile);
        try {
            byte[] buf = new byte[65536];
            int bytesRead = 0;
            while ((bytesRead = jarStream.read(buf)) != -1) {
                outStream.write(buf, 0, bytesRead);
            }
        } finally {
            jarStream.close();
            outStream.close();
        }
        debug(jarname + ".jar extracted to " + jarFile.getPath());
        return jarFile.toURI().toURL();
    }

    protected void delete(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            for (int i = 0; i < children.length; i++) {
                delete(children[i]);
            }
        }
        f.delete();
    }

    protected void debug(String msg) {
        if (isDebug()) {
            System.out.println(msg);
        }
    }

    protected static boolean isDebug() {
        return System.getProperty("warbler.debug") != null;
    }

    public void run() {
        delete(extractRoot);
    }
}