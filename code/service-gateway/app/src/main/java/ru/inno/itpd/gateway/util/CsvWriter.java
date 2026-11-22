package ru.inno.itpd.gateway.util;

import java.io.IOException;
import java.io.Writer;

/** CSV (RFC 4180) с защитой от formula injection: ячейки, начинающиеся с = + - @, экранируются апострофом. */
public final class CsvWriter {
    private CsvWriter() {
    }

    public static void row(Writer w, String... cells) throws IOException {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) w.write(',');
            w.write(cell(cells[i]));
        }
        w.write("\r\n");
    }

    static String cell(String value) {
        if (value == null) return "";
        String v = value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0 && !isNumber(v)) v = "'" + v;
        if (v.indexOf(',') >= 0 || v.indexOf('"') >= 0 || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    private static boolean isNumber(String v) {
        try {
            Double.parseDouble(v);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
