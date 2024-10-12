package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

    public static final String BIND = "bind => [";
    public static final String SELECT = "select";

    public static void main(String[] args) {
        try {
            extracted();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void extracted() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter your SQL request:");
        String initialRequest = bufferedReader.readLine().trim();
        while (bufferedReader.ready()) {
            initialRequest = initialRequest.concat(String.format("%s ", bufferedReader.readLine().trim()));
        }
        initialRequest = initialRequest.toLowerCase();
        if (initialRequest.contains(SELECT) && initialRequest.contains(BIND)) {
            System.out.println("If you use Postgres language - press 0, Oracle - press 1");
            int languageNumber = Integer.parseInt(bufferedReader.readLine());

            String select = initialRequest.substring(initialRequest.indexOf(SELECT), initialRequest.indexOf(BIND)).trim();
            String bind = initialRequest.substring(initialRequest.indexOf(BIND) + BIND.length(), initialRequest.lastIndexOf("]")).trim();
            String[] split = bind.split(", ");
            for (String param : split) {
                if (param.matches("^(\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2}):(\\d{2}).\\d+$")) {
                    param = String.format("TO_TIMESTAMP('%s', 'YYYY-MM-DD HH24:MI:SS.FF')", param);
                } else if (!param.matches("\\d+") && !param.matches("^(true|false|TRUE|FALSE)$")) {
                    param = String.format("'%s'", param);
                } else if (param.matches("^(true|false|TRUE|FALSE)$") && languageNumber == 1) {
                    param = String.format("%s", param.matches("^(true|TRUE)$") ? 1 : 0);
                }
                select = select.replaceFirst("[?]", param);
            }
            System.out.println(select);
        } else {
            System.out.println("Not exist SELECT or BIND statement in the request.");
        }
        extracted();
    }
}