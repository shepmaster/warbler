/**
 * Copyright (c) 2010-2011 Engine Yard, Inc.
 * Copyright (c) 2007-2009 Sun Microsystems, Inc.
 * This source code is available under the MIT license.
 * See the file LICENSE.txt for details.
 */

package warbler;

import java.io.File;

public class WarblerSupport implements Runnable {
    protected File extractRoot;

    WarblerSupport() throws Exception {
        this.extractRoot = File.createTempFile("warbler", "extract");
        this.extractRoot.delete();
        this.extractRoot.mkdirs();
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