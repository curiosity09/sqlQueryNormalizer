package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SqlQueryNormalizer {

    public static final String BIND = "bind => [";
    public static final String SELECT = "select";
    public static final String END_BRACKET = "]";
    public static final int ORACLE_LANG_NUM = 1;
    public static final String PSQL_LANG_NUM = "0";

    public static void main(String[] args) {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in))){
            System.out.println(normalizeQuery(bufferedReader));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String normalizeQuery(final BufferedReader bufferedReader) throws IOException {
        String initialQuery = readInitialQuery(bufferedReader);
        if (initialQuery.contains(SELECT) && initialQuery.contains(BIND)) {
            System.out.println("If you use Postgres language - press 0, Oracle - press 1");
            int languageNumber = Integer.parseInt(bufferedReader.readLine().matches("\\d+") ? bufferedReader.readLine() : PSQL_LANG_NUM);

            String query = initialQuery.substring(initialQuery.indexOf(SELECT), initialQuery.indexOf(BIND)).trim();
            String bind = initialQuery.substring(initialQuery.indexOf(BIND) + BIND.length(), initialQuery.lastIndexOf(END_BRACKET)).trim();
            return replaceParams(bind, query, languageNumber);
        } else {
            System.out.println("Not exist SELECT or BIND statement in the request.");
        }
        return "";
    }

    private static String readInitialQuery(final BufferedReader bufferedReader) throws IOException {
        System.out.println("Enter your SQL query:");
        String initialQuery = bufferedReader.readLine().trim();
        while (bufferedReader.ready()) {
            initialQuery = initialQuery.concat(String.format("%s ", bufferedReader.readLine().trim()));
        }
        return initialQuery.toLowerCase();
    }

    public static String replaceParams(final String bind, String query, final int languageNumber) {
        String[] split = bind.split(", ");
        for (String param : split) {
            if (param.matches("^(\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2}):(\\d{2}).\\d+$")) {
                param = String.format("TO_TIMESTAMP('%s', 'YYYY-MM-DD HH24:MI:SS.FF')", param);
            } else if (!param.matches("\\d+") && !param.matches("^(true|false|TRUE|FALSE)$")) {
                param = String.format("'%s'", param);
            } else if (param.matches("^(true|false|TRUE|FALSE)$") && languageNumber == ORACLE_LANG_NUM) {
                param = String.format("%s", param.matches("^(true|TRUE)$") ? 1 : 0);
            }
            query = query.replaceFirst("[?]", param);
        }
        return query;
    }
}