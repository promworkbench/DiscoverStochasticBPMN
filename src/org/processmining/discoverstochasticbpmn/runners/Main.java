package org.processmining.newpackageivy.runners;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.log.models.EventLogArray;
import org.processmining.log.models.impl.EventLogArrayFactory;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

import java.io.*;

public class Main {
    public static void main(String[] args) {
        try {
            EventLogArray logs = EventLogArrayFactory.createEventLogArray();
            String filename = "Log.xes";
            File file = new File(filename);
            String parent = file.getParent();
            InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
            StochasticNetUtils.DummyConsolePluginContext context = new StochasticNetUtils.DummyConsolePluginContext();
            logs.importFromStream(context, inputStream, parent);
            setLabel(context, logs, filename);
            System.out.println(logs.getLog(0));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Hello");
    }

    /*
     * Sets a proper default name for the event log array.
     */
    private static void setLabel(PluginContext context, EventLogArray logs, String filename) {
        String prefix = null;
        String postfix = null;
        boolean allSame = true;
        for (int i = 0; i < logs.getSize(); i++) {
            XLog log = logs.getLog(i);
            String name = XConceptExtension.instance().extractName(log);
            if (name != null) {
                if (prefix == null) {
                    prefix = name;
                } else {
                    if (!name.equals(prefix)) {
                        allSame = false;
                        prefix = greatestCommonPrefix(prefix, name);
                    }
                }
                if (postfix == null) {
                    postfix = name;
                } else {
                    if (!name.equals(postfix)) {
                        allSame = false;
                        postfix = new StringBuilder(greatestCommonPrefix(
                                new StringBuilder(prefix).reverse().toString(), new StringBuilder(name).reverse()
                                        .toString())).reverse().toString();
                    }
                }
            }
        }
        if ((prefix != null && prefix.length() > 0) || (postfix != null && postfix.length() > 0)) {
            StringBuffer buf = new StringBuffer();
            if (prefix != null) {
                buf.append(prefix);
            }
            if (!allSame) {
                buf.append(" ... ");
                if (postfix != null) {
                    buf.append(postfix);
                }
            }
            context.getFutureResult(0).setLabel(buf.toString());
        } else {
            context.getFutureResult(0).setLabel("Event log array from file '" + filename + "'");
        }
    }

    private static String greatestCommonPrefix(String a, String b) {
        int minLength = Math.min(a.length(), b.length());
        for (int i = 0; i < minLength; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return a.substring(0, i);
            }
        }
        return a.substring(0, minLength);
    }
}
