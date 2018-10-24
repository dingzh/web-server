package edu.yale.network;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.*;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

public class Tests {
    public static void main(String args[]) {
        Path path = Paths.get("/Users/dean/dummy.txt");

        try {
            FileTime ft = Files.getLastModifiedTime(path);
            Instant ftInstant = ft.toInstant();
            System.out.println(ftInstant.toString());
            String RFCStr = ZonedDateTime.ofInstant(ft.toInstant(), ZoneOffset.UTC).format(RFC_1123_DATE_TIME);
            System.out.println(RFCStr);
            Instant parsedInstant = ZonedDateTime.parse(RFCStr, RFC_1123_DATE_TIME).toInstant();
            String parsedInstantStr = parsedInstant.toString();
            System.out.println(parsedInstantStr);
            Instant parsedAgain = Instant.parse(parsedInstantStr);
            System.out.println(parsedAgain.toString());
//            FileTime ft = Files.getLastModifiedTime(path);
//            System.out.println(ft.toString());
//            LocalDateTime ftLdt = LocalDateTime.ofInstant(ft.toInstant(), ZoneId.systemDefault());
//            System.out.println(ftLdt.toString());
//            String lastModifed = ftLdt.atZone(ZoneOffset.UTC).format(RFC_1123_DATE_TIME);
//            System.out.println(lastModifed);
//            LocalDateTime ifModifiedSince = ZonedDateTime.parse(lastModifed, RFC_1123_DATE_TIME).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();

//            System.out.println(ifModifiedSince.toString());
        } catch (IOException ex) {
            System.out.println("Exception thrown;");
        }
    }
}
