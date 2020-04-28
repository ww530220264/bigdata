package com.ww.data.generator;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class DataGeneratoryParamParser {

    protected final String DBTYPE = "--db-type";
    protected final String TABLE = "--table";
    protected final String DBURL = "--db-url";
    protected final String HELP = "--help";
    protected final String VERBOSE = "--verbose";
    protected final String VERSION = "--version";

    final String[][] opts = {
            {DBTYPE,"--dbtype"},
            {DBURL},
            {TABLE},
    };
    final String[][] switches = {
            {HELP, "-h"},
            {VERBOSE, "-v"},
            {VERSION},
    };

    public void parse(String[] args) {
        Pattern compile = Pattern.compile("(--[^=]+)=(.+)");
        int idx;
        for (idx = 0; idx < args.length; idx++) {
            String arg = args[idx];
            String value = null;

            Matcher m = compile.matcher(arg);
            if (m.matches()) {
                arg = m.group(0);
                value = m.group(2);
            }
            String name = findCliOption(arg, opts);
            if (name != null) {
                if (value == null) {
                    if (idx == args.length - 1) {
                        throw new IllegalArgumentException(String.format("Missing argument for option '%s'.", arg));
                    }
                    idx++;
                    value = args[idx];
                }
                if (!handle(name, value)) {
                    break;
                }
                continue;
            }
            name = findCliOption(arg, switches);
            if (name != null) {
                if (!handle(name, null)) {
                    break;
                }
                continue;
            }
        }

        if (idx < args.length) {
            idx++;
        }
        List<String> argList = Arrays.asList(args);
        handleExtraArgs(argList.subList(idx, args.length));
    }

    private String findCliOption(String name, String[][] available) {
        for (String[] candidates : available) {
            for (String candidate : candidates) {
                if (candidate.equals(name)) {
                    return candidates[0];
                }
            }
        }
        return null;
    }

    abstract boolean handle(String opt, String value);

    abstract void handleExtraArgs(List<String> extraArgs);

}
